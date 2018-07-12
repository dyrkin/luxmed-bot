/**
  * MIT License
  *
  * Copyright (c) 2018 Yevhen Zadyra
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
package com.lbs.server.actor

import java.time.ZonedDateTime

import akka.actor.{PoisonPill, Props}
import com.lbs.api.json.model._
import com.lbs.bot._
import com.lbs.bot.model.{Button, Command}
import com.lbs.server.actor.Book._
import com.lbs.server.actor.DatePicker.{DateFromMode, DateToMode}
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.StaticData.StaticDataConfig
import com.lbs.server.actor.conversation.Conversation
import com.lbs.server.actor.conversation.Conversation.{InitConversation, StartConversation}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.service.{ApiService, DataService, MonitoringService}
import com.lbs.server.util.MessageExtractors.CallbackCommand
import com.lbs.server.util.ServerModelConverters._

class Book(val userId: UserId, bot: Bot, apiService: ApiService, dataService: DataService, monitoringService: MonitoringService,
           val localization: Localization, datePickerActorFactory: ByUserIdWithOriginatorActorFactory, staticDataActorFactory: ByUserIdWithOriginatorActorFactory,
           termsPagerActorFactory: ByUserIdWithOriginatorActorFactory) extends Conversation[BookingData] with StaticDataForBooking with Localizable {

  private val datePicker = datePickerActorFactory(userId, self)
  private[actor] val staticData = staticDataActorFactory(userId, self)
  private val termsPager = termsPagerActorFactory(userId, self)

  entryPoint(askCity, BookingData())

  private def askCity: Step =
    staticData(cityConfig) { bd: BookingData =>
      withFunctions(
        latestOptions = dataService.getLatestCities(userId.accountId),
        staticOptions = apiService.getAllCities(userId.accountId),
        applyId = id => bd.copy(cityId = id))
    }(requestNext = askClinic)

  private def askClinic: Step =
    staticData(clinicConfig) { bd: BookingData =>
      withFunctions(
        latestOptions = dataService.getLatestClinicsByCityId(userId.accountId, bd.cityId.id),
        staticOptions = apiService.getAllClinics(userId.accountId, bd.cityId.id),
        applyId = id => bd.copy(clinicId = id))
    }(requestNext = askService)

  private def askService: Step =
    staticData(serviceConfig) { bd: BookingData =>
      withFunctions(
        latestOptions = dataService.getLatestServicesByCityIdAndClinicId(userId.accountId, bd.cityId.id, bd.clinicId.optionalId),
        staticOptions = apiService.getAllServices(userId.accountId, bd.cityId.id, bd.clinicId.optionalId),
        applyId = id => bd.copy(serviceId = id))
    }(requestNext = askDoctor)

  private def askDoctor: Step =
    staticData(doctorConfig) { bd: BookingData =>
      withFunctions(
        latestOptions = dataService.getLatestDoctorsByCityIdAndClinicIdAndServiceId(userId.accountId, bd.cityId.id, bd.clinicId.optionalId, bd.serviceId.id),
        staticOptions = apiService.getAllDoctors(userId.accountId, bd.cityId.id, bd.clinicId.optionalId, bd.serviceId.id),
        applyId = id => bd.copy(doctorId = id))
    }(requestNext = requestDateFrom)

  private def requestDateFrom: Step =
    ask { bookingData =>
      datePicker ! InitConversation
      datePicker ! StartConversation
      datePicker ! DateFromMode
      datePicker ! bookingData.dateFrom
    } onReply {
      case Msg(cmd: Command, _) =>
        datePicker ! cmd
        stay()
      case Msg(date: ZonedDateTime, bookingData: BookingData) =>
        goto(requestDateTo) using bookingData.copy(dateFrom = date)
    }

  private def requestDateTo: Step =
    ask { bookingData =>
      datePicker ! InitConversation
      datePicker ! StartConversation
      datePicker ! DateToMode
      datePicker ! bookingData.dateFrom.plusDays(1)
    } onReply {
      case Msg(cmd: Command, _) =>
        datePicker ! cmd
        stay()
      case Msg(date: ZonedDateTime, bookingData: BookingData) =>
        goto(requestAction) using bookingData.copy(dateTo = date)
    }

  private def requestAction: Step =
    ask { bookingData =>
      dataService.storeAppointment(userId.accountId, bookingData)
      bot.sendMessage(userId.source,
        lang.bookingSummary(bookingData),
        inlineKeyboard = createInlineKeyboard(
          Seq(Button(lang.findTerms, Tags.FindTerms), Button(lang.modifyDate, Tags.ModifyDate))
        ))
    } onReply {
      case Msg(CallbackCommand(Tags.FindTerms), _) =>
        goto(requestTerm)
      case Msg(CallbackCommand(Tags.ModifyDate), _) =>
        goto(requestDateFrom)
    }

  private def requestTerm: Step =
    ask { bookingData =>
      val availableTerms = apiService.getAvailableTerms(userId.accountId, bookingData.cityId.id,
        bookingData.clinicId.optionalId, bookingData.serviceId.id, bookingData.doctorId.optionalId,
        bookingData.dateFrom, Some(bookingData.dateTo), timeOfDay = bookingData.timeOfDay)
      termsPager ! InitConversation
      termsPager ! StartConversation
      termsPager ! availableTerms
    } onReply {
      case Msg(cmd: Command, _) =>
        termsPager ! cmd
        stay()
      case Msg(term: AvailableVisitsTermPresentation, bookingData) =>
        val response = apiService.temporaryReservation(userId.accountId, term.mapTo[TemporaryReservationRequest], term.mapTo[ValuationsRequest])
        response match {
          case Left(ex) =>
            bot.sendMessage(userId.source, ex.getMessage)
            end()
          case Right((temporaryReservation, valuations)) =>
            bot.sendMessage(userId.source, lang.confirmAppointment(term, valuations),
              inlineKeyboard = createInlineKeyboard(Seq(Button(lang.cancel, Tags.Cancel), Button(lang.book, Tags.Book))))
            goto(awaitReservation) using bookingData.copy(term = Some(term), temporaryReservationId = Some(temporaryReservation.id), valuations = Some(valuations))
        }
      case Msg(Pager.NoItemsFound, _) =>
        goto(askNoTermsAction)
    }

  private def askNoTermsAction: Step =
    ask { _ =>
      bot.sendMessage(userId.source, lang.noTermsFound, inlineKeyboard =
        createInlineKeyboard(Seq(Button(lang.modifyDate, Tags.ModifyDate), Button(lang.createMonitoring, Tags.CreateMonitoring))))
    } onReply {
      case Msg(CallbackCommand(Tags.ModifyDate), _) =>
        goto(requestDateFrom)
      case Msg(CallbackCommand(Tags.CreateMonitoring), _) =>
        goto(askMonitoringOptions)
    }

  private def awaitReservation: Step =
    monologue {
      case Msg(CallbackCommand(Tags.Cancel), bookingData: BookingData) =>
        apiService.deleteTemporaryReservation(userId.accountId, bookingData.temporaryReservationId.get)
        stay()
      case Msg(CallbackCommand(Tags.Book), bookingData: BookingData) =>
        val reservationRequestMaybe = for {
          tmpReservationId <- bookingData.temporaryReservationId
          valuations <- bookingData.valuations
          visitTermVariant <- valuations.visitTermVariants.headOption
          term <- bookingData.term
        } yield (tmpReservationId, visitTermVariant, term).mapTo[ReservationRequest]

        reservationRequestMaybe match {
          case Some(reservationRequest) =>
            apiService.reservation(userId.accountId, reservationRequest) match {
              case Left(ex) =>
                error("Error during reservation", ex)
                bot.sendMessage(userId.source, ex.getMessage)
                end()
              case Right(success) =>
                debug(s"Successfully confirmed: $success")
                bot.sendMessage(userId.source, lang.appointmentIsConfirmed)
                end()
            }
          case _ => sys.error(s"Can not prepare reservation request using booking data $bookingData")
        }
    }

  private def askMonitoringOptions: Step =
    ask { _ =>
      bot.sendMessage(userId.source, lang.chooseTypeOfMonitoring,
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.bookByApplication, Tags.BookByApplication), Button(lang.bookManually, Tags.BookManually)), columns = 1))
    } onReply {
      case Msg(CallbackCommand(autobookStr), bookingData: BookingData) =>
        val autobook = autobookStr.toBoolean
        goto(createMonitoring) using bookingData.copy(autobook = autobook)
    }

  private def createMonitoring: Step =
    process { bookingData =>
      debug(s"Creating monitoring for $bookingData")
      try {
        monitoringService.createMonitoring((userId -> bookingData).mapTo[Monitoring])
        bot.sendMessage(userId.source, lang.monitoringHasBeenCreated)
      } catch {
        case ex: Exception =>
          error("Unable to create monitoring", ex)
          bot.sendMessage(userId.source, lang.unableToCreateMonitoring)
      }
      end()
    }

  private def cityConfig = StaticDataConfig(lang.city, "Wrocław", isAnyAllowed = false)

  private def clinicConfig = StaticDataConfig(lang.clinic, "Swobodna 1", isAnyAllowed = true)

  private def serviceConfig = StaticDataConfig(lang.service, "Stomatolog", isAnyAllowed = false)

  private def doctorConfig = StaticDataConfig(lang.doctor, "Bartniak", isAnyAllowed = true)

  override def postStop(): Unit = {
    datePicker ! PoisonPill
    staticData ! PoisonPill
    termsPager ! PoisonPill
    super.postStop()
  }
}

object Book {

  def props(userId: UserId, bot: Bot, apiService: ApiService, dataService: DataService, monitoringService: MonitoringService,
            localization: Localization, datePickerActorFactory: ByUserIdWithOriginatorActorFactory,
            staticDataActorFactory: ByUserIdWithOriginatorActorFactory, termsPagerActorFactory: ByUserIdWithOriginatorActorFactory): Props =
    Props(new Book(userId, bot, apiService, dataService, monitoringService, localization, datePickerActorFactory,
      staticDataActorFactory, termsPagerActorFactory))

  case class BookingData(cityId: IdName = null, clinicId: IdName = null,
                         serviceId: IdName = null, doctorId: IdName = null, dateFrom: ZonedDateTime = ZonedDateTime.now(),
                         dateTo: ZonedDateTime = ZonedDateTime.now().plusDays(1L), timeOfDay: Int = 0, autobook: Boolean = false, term: Option[AvailableVisitsTermPresentation] = None,
                         temporaryReservationId: Option[Long] = None, valuations: Option[ValuationsResponse] = None)

  object Tags {
    val Cancel = "cancel"
    val Book = "book"
    val FindTerms = "find_terms"
    val ModifyDate = "modify_date"
    val CreateMonitoring = "create_monitoring"
    val BookManually = "false"
    val BookByApplication = "true"
  }

}
