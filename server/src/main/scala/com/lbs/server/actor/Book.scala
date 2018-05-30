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

import akka.actor.{ActorRef, PoisonPill, Props}
import com.lbs.api.json.model._
import com.lbs.bot._
import com.lbs.bot.model.{Button, Command}
import com.lbs.server.actor.Book._
import com.lbs.server.actor.Chat.Init
import com.lbs.server.actor.DatePicker.{DateFromMode, DateToMode}
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.StaticData.StaticDataConfig
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.service.{ApiService, DataService, MonitoringService}
import com.lbs.server.util.ServerModelConverters._

import scala.util.{Failure, Success, Try}

class Book(val userId: UserId, bot: Bot, apiService: ApiService, dataService: DataService, monitoringService: MonitoringService,
           val localization: Localization, datePickerActorFactory: (UserId, ActorRef) => ActorRef, staticDataActorFactory: (UserId, ActorRef) => ActorRef,
           termsPagerActorFactory: (UserId, ActorRef) => ActorRef) extends SafeFSM[FSMState, FSMData] with StaticDataForBooking with Localizable {

  private val datePicker = datePickerActorFactory(userId, self)
  protected val staticData = staticDataActorFactory(userId, self)
  private val termsPager = termsPagerActorFactory(userId, self)

  startWith(RequestCity, BookingData())

  requestStaticData(RequestCity, AwaitCity, cityConfig) { bd: BookingData =>
    withFunctions(
      latestOptions = dataService.getLatestCities(userId.userId),
      staticOptions = apiService.getAllCities(userId.userId),
      applyId = id => bd.copy(cityId = id))
  }(requestNext = RequestClinic)

  requestStaticData(RequestClinic, AwaitClinic, clinicConfig) { bd: BookingData =>
    withFunctions(
      latestOptions = dataService.getLatestClinicsByCityId(userId.userId, bd.cityId.id),
      staticOptions = apiService.getAllClinics(userId.userId, bd.cityId.id),
      applyId = id => bd.copy(clinicId = id))
  }(requestNext = RequestService)

  requestStaticData(RequestService, AwaitService, serviceConfig) { bd: BookingData =>
    withFunctions(
      latestOptions = dataService.getLatestServicesByCityIdAndClinicId(userId.userId, bd.cityId.id, bd.clinicId.optionalId),
      staticOptions = apiService.getAllServices(userId.userId, bd.cityId.id, bd.clinicId.optionalId),
      applyId = id => bd.copy(serviceId = id))
  }(requestNext = RequestDoctor)

  requestStaticData(RequestDoctor, AwaitDoctor, doctorConfig) { bd: BookingData =>
    withFunctions(
      latestOptions = dataService.getLatestDoctorsByCityIdAndClinicIdAndServiceId(userId.userId, bd.cityId.id, bd.clinicId.optionalId, bd.serviceId.id),
      staticOptions = apiService.getAllDoctors(userId.userId, bd.cityId.id, bd.clinicId.optionalId, bd.serviceId.id),
      applyId = id => bd.copy(doctorId = id))
  }(requestNext = RequestDateFrom)

  whenSafe(RequestDateFrom) {
    case Event(_, bookingData: BookingData) =>
      datePicker ! DateFromMode
      datePicker ! bookingData.dateFrom
      goto(AwaitDateFrom)
  }

  whenSafe(AwaitDateFrom) {
    case Event(cmd: Command, _) =>
      datePicker ! cmd
      stay()
    case Event(date: ZonedDateTime, bookingData: BookingData) =>
      invokeNext()
      goto(RequestDateTo) using bookingData.copy(dateFrom = date)
  }

  whenSafe(RequestDateTo) {
    case Event(_, bookingData: BookingData) =>
      datePicker ! DateToMode
      datePicker ! bookingData.dateFrom.plusDays(1)
      goto(AwaitDateTo)
  }

  whenSafe(AwaitDateTo) {
    case Event(cmd: Command, _) =>
      datePicker ! cmd
      stay()
    case Event(date: ZonedDateTime, bookingData: BookingData) =>
      invokeNext()
      goto(RequestDayTime) using bookingData.copy(dateTo = date)
  }

  whenSafe(RequestDayTime) {
    case Event(Next, _: BookingData) =>
      bot.sendMessage(userId.source, lang.chooseTimeOfDay,
        inlineKeyboard = createInlineKeyboard(lang.timeOfDay.map { case (id, label) => Button(label, id.toString) }.toSeq, columns = 1))
      goto(AwaitDayTime)
  }

  whenSafe(AwaitDayTime) {
    case Event(Command(_, msg, Some(timeIdStr)), bookingData: BookingData) =>
      invokeNext()
      val timeId = timeIdStr.toInt
      bot.sendEditMessage(userId.source, msg.messageId, lang.preferredTimeIs(timeId))
      goto(RequestAction) using bookingData.copy(timeOfDay = timeId)
  }

  whenSafe(RequestAction) {
    case Event(Next, bookingData: BookingData) =>
      dataService.storeAppointment(userId.userId, bookingData)
      bot.sendMessage(userId.source,
        lang.bookingSummary(bookingData),
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.findTerms, Tags.FindTerms), Button(lang.modifyDate, Tags.ModifyDate))))
      goto(AwaitAction)
  }

  whenSafe(AwaitAction) {
    case Event(Command(_, _, Some(Tags.FindTerms)), _) =>
      invokeNext()
      goto(RequestTerm)
    case Event(Command(_, _, Some(Tags.ModifyDate)), _) =>
      invokeNext()
      goto(RequestDateFrom)
  }

  whenSafe(RequestTerm) {
    case Event(Next, bookingData: BookingData) =>
      val availableTerms = apiService.getAvailableTerms(userId.userId, bookingData.cityId.id,
        bookingData.clinicId.optionalId, bookingData.serviceId.id, bookingData.doctorId.optionalId,
        bookingData.dateFrom, Some(bookingData.dateTo), timeOfDay = bookingData.timeOfDay)
      termsPager ! availableTerms
      goto(AwaitTerm)
  }

  whenSafe(AwaitTerm) {
    case Event(Command(_, _, Some(Tags.ModifyDate)), _) =>
      invokeNext()
      goto(RequestDateFrom)
    case Event(Command(_, _, Some(Tags.CreateMonitoring)), _) =>
      invokeNext()
      goto(AskMonitoringOptions)
    case Event(cmd: Command, _) =>
      termsPager ! cmd
      stay()
    case Event(term: AvailableVisitsTermPresentation, _) =>
      self ! term
      goto(RequestReservation)
    case Event(Pager.NoItemsFound, _) =>
      bot.sendMessage(userId.source, lang.noTermsFound, inlineKeyboard =
        createInlineKeyboard(Seq(Button(lang.modifyDate, Tags.ModifyDate), Button(lang.createMonitoring, Tags.CreateMonitoring))))
      stay()
  }

  whenSafe(RequestReservation) {
    case Event(term: AvailableVisitsTermPresentation, bookingData: BookingData) =>
      val response = apiService.temporaryReservation(userId.userId, term.mapTo[TemporaryReservationRequest], term.mapTo[ValuationsRequest])
      response match {
        case Left(ex) =>
          bot.sendMessage(userId.source, ex.getMessage)
          invokeNext()
          stay()
        case Right((temporaryReservation, valuations)) =>
          bot.sendMessage(userId.source, lang.confirmAppointment(term, valuations),
            inlineKeyboard = createInlineKeyboard(Seq(Button(lang.cancel, Tags.Cancel), Button(lang.book, Tags.Book))))
          goto(AwaitReservation) using bookingData.copy(term = Some(term), temporaryReservationId = Some(temporaryReservation.id), valuations = Some(valuations))
      }
  }

  whenSafe(AwaitReservation) {
    case Event(Command(_, _, Some(Tags.Cancel)), bookingData: BookingData) =>
      apiService.deleteTemporaryReservation(userId.userId, bookingData.temporaryReservationId.get)
      stay()
    case Event(Command(_, _, Some(Tags.Book)), bookingData: BookingData) =>
      val reservationRequestMaybe = for {
        tmpReservationId <- bookingData.temporaryReservationId
        valuations <- bookingData.valuations
        visitTermVariant <- valuations.visitTermVariants.headOption
        term <- bookingData.term
      } yield (tmpReservationId, visitTermVariant, term).mapTo[ReservationRequest]

      reservationRequestMaybe match {
        case Some(reservationRequest) =>
          apiService.reservation(userId.userId, reservationRequest) match {
            case Left(ex) =>
              bot.sendMessage(userId.source, ex.getMessage)
              invokeNext()
              stay()
            case Right(success) =>
              log.debug(s"Successfully confirmed: $success")
              bot.sendMessage(userId.source, lang.appointmentIsConfirmed)
              stay()
          }
      }

  }

  whenSafe(AskMonitoringOptions) {
    case Event(Next, _) =>
      bot.sendMessage(userId.source, lang.chooseTypeOfMonitoring,
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.bookByApplication, Tags.BookByApplication), Button(lang.bookManually, Tags.BookManually)), columns = 1))
      stay()
    case Event(Command(_, _, Some(autobookStr)), bookingData: BookingData) =>
      val autobook = autobookStr.toBoolean
      invokeNext()
      goto(CreateMonitoring) using bookingData.copy(autobook = autobook)
  }

  whenSafe(CreateMonitoring) {
    case Event(Next, bookingData: BookingData) =>
      LOG.debug(s"Creating monitoring for $bookingData")
      Try(monitoringService.createMonitoring((userId -> bookingData).mapTo[Monitoring])) match {
        case Success(_) => bot.sendMessage(userId.source, lang.monitoringHasBeenCreated)
        case Failure(ex) =>
          LOG.error("Unable to create monitoring", ex)
          bot.sendMessage(userId.source, lang.unableToCreateMonitoring)
      }
      goto(RequestCity) using BookingData()
  }

  whenUnhandledSafe {
    case Event(Init, _) =>
      reinit()
    case e: Event =>
      LOG.error(s"Unhandled event in state:$stateName. Event: $e")
      stay()
  }

  private def cityConfig = StaticDataConfig(lang.city, "Wrocław", isAnyAllowed = false)

  private def clinicConfig = StaticDataConfig(lang.clinic, "Swobodna 1", isAnyAllowed = true)

  private def serviceConfig = StaticDataConfig(lang.service, "Stomatolog", isAnyAllowed = false)

  private def doctorConfig = StaticDataConfig(lang.doctor, "Bartniak", isAnyAllowed = true)

  private def reinit() = {
    invokeNext()
    datePicker ! Init
    staticData ! Init
    termsPager ! Init
    goto(RequestCity) using BookingData()
  }

  initialize()

  override def postStop(): Unit = {
    datePicker ! PoisonPill
    staticData ! PoisonPill
    termsPager ! PoisonPill
    super.postStop()
  }
}

object Book {

  def props(userId: UserId, bot: Bot, apiService: ApiService, dataService: DataService, monitoringService: MonitoringService,
            localization: Localization, datePickerActorFactory: (UserId, ActorRef) => ActorRef,
            staticDataActorFactory: (UserId, ActorRef) => ActorRef, termsPagerActorFactory: (UserId, ActorRef) => ActorRef): Props =
    Props(classOf[Book], userId, bot, apiService, dataService, monitoringService, localization, datePickerActorFactory,
      staticDataActorFactory, termsPagerActorFactory)

  object RequestCity extends FSMState

  object AwaitCity extends FSMState

  object RequestClinic extends FSMState

  object AwaitClinic extends FSMState

  object RequestService extends FSMState

  object AwaitService extends FSMState

  object RequestDoctor extends FSMState

  object AwaitDoctor extends FSMState

  object CreateMonitoring extends FSMState

  object AskMonitoringOptions extends FSMState

  object RequestDateFrom extends FSMState

  object AwaitDateFrom extends FSMState

  object RequestDateTo extends FSMState

  object AwaitDateTo extends FSMState

  object RequestDayTime extends FSMState

  object AwaitDayTime extends FSMState

  object RequestAction extends FSMState

  object AwaitAction extends FSMState

  object RequestTerm extends FSMState

  object AwaitTerm extends FSMState

  object RequestReservation extends FSMState

  object AwaitReservation extends FSMState

  case class BookingData(cityId: IdName = null, clinicId: IdName = null,
                         serviceId: IdName = null, doctorId: IdName = null, dateFrom: ZonedDateTime = ZonedDateTime.now(),
                         dateTo: ZonedDateTime = ZonedDateTime.now().plusDays(1L), timeOfDay: Int = 0, autobook: Boolean = false, term: Option[AvailableVisitsTermPresentation] = None,
                         temporaryReservationId: Option[Long] = None, valuations: Option[ValuationsResponse] = None) extends FSMData

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
