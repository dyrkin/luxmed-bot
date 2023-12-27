package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.api.json.model._
import com.lbs.bot._
import com.lbs.bot.model.{Button, Command}
import com.lbs.server.conversation.Book._
import com.lbs.server.conversation.DatePicker.{DateFromMode, DateToMode}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Pager.SimpleItemsProvider
import com.lbs.server.conversation.StaticData.StaticDataConfig
import com.lbs.server.conversation.TimePicker.{TimeFromMode, TimeToMode}
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.service.{ApiService, DataService, MonitoringService}
import com.lbs.server.util.MessageExtractors._
import com.lbs.server.util.ServerModelConverters._

import java.time.{LocalDateTime, LocalTime}

class Book(
  val userId: UserId,
  bot: Bot,
  apiService: ApiService,
  dataService: DataService,
  monitoringService: MonitoringService,
  val localization: Localization,
  datePickerFactory: UserIdWithOriginatorTo[DatePicker],
  timePickerFactory: UserIdWithOriginatorTo[TimePicker],
  staticDataFactory: UserIdWithOriginatorTo[StaticData],
  termsPagerFactory: UserIdWithOriginatorTo[Pager[TermExt]]
)(implicit val actorSystem: ActorSystem)
    extends Conversation[BookingData]
    with StaticDataForBooking
    with Localizable {

  private val datePicker = datePickerFactory(userId, self)
  private val timePicker = timePickerFactory(userId, self)
  private[conversation] val staticData = staticDataFactory(userId, self)
  private val termsPager = termsPagerFactory(userId, self)

  entryPoint(askCity, BookingData())

  private def askCity: Step =
    staticData(cityConfig) { bd: BookingData =>
      withFunctions[DictionaryCity](
        latestOptions = dataService.getLatestCities(userId.accountId),
        staticOptions = apiService.getAllCities(userId.accountId),
        applyId = id => bd.copy(cityId = id.toIdName)
      )
    }(requestNext = askService)

  private def askService: Step =
    staticData(serviceConfig) { bd: BookingData =>
      withFunctions[DictionaryServiceVariants](
        latestOptions = dataService.getLatestServicesByCityIdAndClinicId(userId.accountId, bd.cityId.id, None),
        staticOptions = apiService.getAllServices(userId.accountId),
        applyId = id => bd.copy(serviceId = id.toIdName)
      )
    }(requestNext = askClinic)

  private def askClinic: Step =
    staticData(clinicConfig) { bd: BookingData =>
      withFunctions[IdName](
        latestOptions = dataService.getLatestClinicsByCityId(userId.accountId, bd.cityId.id),
        staticOptions = apiService.getAllFacilities(userId.accountId, bd.cityId.id, bd.serviceId.id),
        applyId = id => bd.copy(clinicId = id)
      )
    }(requestNext = askDoctor)

  private def askDoctor: Step =
    staticData(doctorConfig) { bd: BookingData =>
      withFunctions[IdName](
        latestOptions = dataService.getLatestDoctorsByCityIdAndClinicIdAndServiceId(
          userId.accountId,
          bd.cityId.id,
          bd.clinicId.optionalId,
          bd.serviceId.id
        ),
        staticOptions = apiService
          .getAllDoctors(userId.accountId, bd.cityId.id, bd.serviceId.id)
          .map(
              _.filter(doc => {
                val clinicId = bd.clinicId.optionalId
                clinicId.isEmpty || doc.facilityGroupIds.exists(_.contains(clinicId.get))
              })
              .map(_.toIdName)
          ),
        applyId = id => bd.copy(doctorId = id.toIdName)
      )
    }(requestNext = requestDateFrom)

  private def requestDateFrom: Step =
    ask { bookingData =>
      datePicker.restart()
      datePicker ! DateFromMode
      datePicker ! bookingData.dateFrom
    } onReply {
      case Msg(cmd: Command, _) =>
        datePicker ! cmd
        stay()
      case Msg(date: LocalDateTime, bookingData: BookingData) =>
        goto(requestDateTo) using bookingData.copy(dateFrom = date)
    }

  private def requestDateTo: Step =
    ask { bookingData =>
      datePicker.restart()
      datePicker ! DateToMode
      datePicker ! bookingData.dateFrom.plusDays(1)
    } onReply {
      case Msg(cmd: Command, _) =>
        datePicker ! cmd
        stay()
      case Msg(date: LocalDateTime, bookingData: BookingData) =>
        goto(requestTimeFrom) using bookingData.copy(dateTo = date)
    }

  private def requestTimeFrom: Step =
    ask { bookingData =>
      timePicker.restart()
      timePicker ! TimeFromMode
      timePicker ! bookingData.timeFrom
    } onReply {
      case Msg(cmd: Command, _) =>
        timePicker ! cmd
        stay()
      case Msg(time: LocalTime, bookingData: BookingData) =>
        goto(requestTimeTo) using bookingData.copy(timeFrom = time)
    }

  private def requestTimeTo: Step =
    ask { bookingData =>
      timePicker.restart()
      timePicker ! TimeToMode
      timePicker ! bookingData.timeTo
    } onReply {
      case Msg(cmd: Command, _) =>
        timePicker ! cmd
        stay()
      case Msg(time: LocalTime, bookingData: BookingData) =>
        goto(requestAction) using bookingData.copy(timeTo = time)
    }

  private def requestAction: Step =
    ask { bookingData =>
      dataService.storeAppointment(userId.accountId, bookingData)
      bot.sendMessage(
        userId.source,
        lang.bookingSummary(bookingData),
        inlineKeyboard =
          createInlineKeyboard(Seq(Button(lang.findTerms, Tags.FindTerms), Button(lang.modifyDate, Tags.ModifyDate)))
      )
    } onReply {
      case Msg(CallbackCommand(Tags.FindTerms), _) =>
        goto(requestTerm)
      case Msg(CallbackCommand(Tags.ModifyDate), bookingData) =>
        goto(requestDateFrom) using bookingData.copy(
          dateFrom = LocalDateTime.now(),
          dateTo = LocalDateTime.now().plusDays(1L)
        )
    }

  private def requestTerm: Step =
    ask { bookingData =>
      val availableTerms = apiService.getAvailableTerms(
        userId.accountId,
        bookingData.cityId.id,
        bookingData.clinicId.optionalId,
        bookingData.serviceId.id,
        bookingData.doctorId.optionalId,
        bookingData.dateFrom,
        bookingData.dateTo,
        timeFrom = bookingData.timeFrom,
        timeTo = bookingData.timeTo
      )
      termsPager.restart()
      termsPager ! availableTerms.map(new SimpleItemsProvider(_))
    } onReply {
      case Msg(cmd: Command, _) =>
        termsPager ! cmd
        stay()
      case Msg(term: TermExt, bookingData) =>
        val response = for {
          xsrfToken <- apiService.getXsrfToken(userId.accountId)
          lockTermResponse <- apiService.reservationLockterm(
            userId.accountId,
            xsrfToken,
            term.mapTo[ReservationLocktermRequest]
          )
        } yield (lockTermResponse, xsrfToken)
        response match {
          case Left(ex) =>
            logger.error("Can not lock term", ex)
            bot.sendMessage(userId.source, ex.getMessage)
            end()
          case Right((reservationLocktermResponse, xsrfToken)) =>
            if (reservationLocktermResponse.value.changeTermAvailable) {
              logger.warn(s"Service [${bookingData.serviceId.name}] is already booked. Ask to update term")
              bot.sendMessage(
                userId.source,
                lang.visitAlreadyExists,
                inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No), Button(lang.yes, Tags.Yes)))
              )
              goto(awaitRebookDecision) using bookingData.copy(
                term = Some(term),
                xsrfToken = Some(xsrfToken),
                reservationLocktermResponse = Some(reservationLocktermResponse)
              )
            } else {
              bot.sendMessage(
                userId.source,
                lang.confirmAppointment(term),
                inlineKeyboard =
                  createInlineKeyboard(Seq(Button(lang.cancel, Tags.Cancel), Button(lang.book, Tags.Book)))
              )
              goto(awaitReservation) using bookingData.copy(
                term = Some(term),
                xsrfToken = Some(xsrfToken),
                reservationLocktermResponse = Some(reservationLocktermResponse)
              )
            }
        }
      case Msg(Pager.NoItemsFound, _) =>
        goto(askNoTermsAction)
    }

  private def askNoTermsAction: Step =
    ask { _ =>
      bot.sendMessage(
        userId.source,
        lang.noTermsFound,
        inlineKeyboard = createInlineKeyboard(
          Seq(Button(lang.modifyDate, Tags.ModifyDate), Button(lang.createMonitoring, Tags.CreateMonitoring))
        )
      )
    } onReply {
      case Msg(CallbackCommand(Tags.ModifyDate), bookingData) =>
        goto(requestDateFrom) using bookingData.copy(
          dateFrom = LocalDateTime.now(),
          dateTo = LocalDateTime.now().plusDays(1L)
        )
      case Msg(CallbackCommand(Tags.CreateMonitoring), bookingData) =>
        val settingsMaybe = dataService.findSettings(userId.userId)
        val (defaultOffset, askOffset) = settingsMaybe match {
          case Some(settings) => (settings.defaultOffset, settings.alwaysAskOffset)
          case None           => (0, false)
        }
        val newData = bookingData.copy(offset = defaultOffset)
        if (askOffset) goto(askMonitoringOffsetOption) using newData
        else goto(askMonitoringAutobookOption) using newData
    }

  private def awaitRebookDecision: Step =
    monologue {
      case Msg(CallbackCommand(Tags.Yes), bookingData: BookingData) =>
        apiService.reservationChangeTerm(
          userId.accountId,
          bookingData.xsrfToken.get,
          (bookingData.reservationLocktermResponse.get, bookingData.term.get).mapTo[ReservationChangetermRequest]
        ) match {
          case Right(success) =>
            logger.debug(s"Successfully confirmed: $success")
            bot.sendMessage(userId.source, lang.appointmentIsConfirmed)
          case Left(ex) =>
            logger.error("Error during reservation", ex)
            bot.sendMessage(userId.source, ex.getMessage)
        }
        end()
      case Msg(CallbackCommand(Tags.No), _) =>
        logger.info("User doesn't want to change term")
        end()
    }

  private def awaitReservation: Step =
    monologue {
      case Msg(CallbackCommand(Tags.Cancel), bookingData: BookingData) =>
        apiService.deleteTemporaryReservation(
          userId.accountId,
          bookingData.xsrfToken.get,
          bookingData.reservationLocktermResponse.get.value.temporaryReservationId
        )
        stay()
      case Msg(CallbackCommand(Tags.Book), bookingData: BookingData) =>
        makeReservation(bookingData)
        end()
    }

  private def makeReservation(bookingData: BookingData): Unit = {
    val reservationRequestMaybe = for {
      reservationLocktermResponse <- bookingData.reservationLocktermResponse
      term <- bookingData.term
    } yield (reservationLocktermResponse, term).mapTo[ReservationConfirmRequest]

    reservationRequestMaybe match {
      case Some(reservationRequest) =>
        apiService.reservationConfirm(userId.accountId, bookingData.xsrfToken.get, reservationRequest) match {
          case Left(ex) =>
            logger.error("Error during reservation", ex)
            bot.sendMessage(userId.source, ex.getMessage)
          case Right(success) =>
            logger.debug(s"Successfully confirmed: $success")
            bot.sendMessage(userId.source, lang.appointmentIsConfirmed)
        }
      case _ => sys.error(s"Can not prepare reservation request using booking data $bookingData")
    }
  }

  private def askMonitoringOffsetOption: Step =
    ask { _ =>
      bot.sendMessage(
        userId.source,
        lang.pleaseSpecifyOffset,
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No)))
      )
    } onReply {
      case Msg(TextCommand(IntString(offset)), bookingData: BookingData) =>
        goto(askMonitoringAutobookOption) using bookingData.copy(offset = offset)
      case Msg(CallbackCommand(BooleanString(false)), _) =>
        goto(askMonitoringAutobookOption)
    }

  private def askMonitoringAutobookOption: Step =
    ask { _ =>
      bot.sendMessage(
        userId.source,
        lang.chooseTypeOfMonitoring,
        inlineKeyboard = createInlineKeyboard(
          Seq(Button(lang.bookByApplication, Tags.BookByApplication), Button(lang.bookManually, Tags.BookManually)),
          columns = 1
        )
      )
    } onReply { case Msg(CallbackCommand(BooleanString(autobook)), bookingData: BookingData) =>
      val data = bookingData.copy(autobook = autobook)
      if (autobook) goto(askMonitoringRebookOption) using data
      else goto(createMonitoring) using data
    }

  private def askMonitoringRebookOption: Step =
    ask { _ =>
      bot.sendMessage(
        userId.source,
        lang.rebookIfExists,
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No), Button(lang.yes, Tags.Yes)))
      )
    } onReply { case Msg(CallbackCommand(BooleanString(rebookIfExists)), bookingData: BookingData) =>
      goto(createMonitoring) using bookingData.copy(rebookIfExists = rebookIfExists)
    }

  private def createMonitoring: Step =
    process { bookingData =>
      logger.debug(s"Creating monitoring for $bookingData")
      try {
        monitoringService.createMonitoring((userId -> bookingData).mapTo[Monitoring])
        bot.sendMessage(userId.source, lang.monitoringHasBeenCreated)
      } catch {
        case ex: Exception =>
          logger.error("Unable to create monitoring", ex)
          bot.sendMessage(userId.source, lang.unableToCreateMonitoring(ex.getMessage))
      }
      end()
    }

  private def cityConfig = StaticDataConfig(lang.city, "wro", "Wrocław", isAnyAllowed = false)

  private def clinicConfig = StaticDataConfig(lang.clinic, "swob", "Swobodna 1", isAnyAllowed = true)

  private def serviceConfig = StaticDataConfig(lang.service, "stomat", "Stomatolog", isAnyAllowed = false)

  private def doctorConfig = StaticDataConfig(lang.doctor, "kowal", "Kowalski", isAnyAllowed = true)

  beforeDestroy {
    datePicker.destroy()
    staticData.destroy()
    termsPager.destroy()
    timePicker.destroy()
  }
}

object Book {

  case class BookingData(
    cityId: IdName = null,
    clinicId: IdName = null,
    serviceId: IdName = null,
    doctorId: IdName = null,
    dateFrom: LocalDateTime = LocalDateTime.now(),
    dateTo: LocalDateTime = LocalDateTime.now().plusDays(1L),
    timeFrom: LocalTime = LocalTime.of(7, 0),
    timeTo: LocalTime = LocalTime.of(21, 0),
    autobook: Boolean = false,
    rebookIfExists: Boolean = false,
    term: Option[TermExt] = None,
    reservationLocktermResponse: Option[ReservationLocktermResponse] = None,
    offset: Int = 0,
    payerId: Long = 0,
    payers: Seq[IdName] = Seq(),
    xsrfToken: Option[XsrfToken] = None
  )

  object Tags {
    val Cancel = "cancel"
    val Book = "book"
    val FindTerms = "find_terms"
    val ModifyDate = "modify_date"
    val CreateMonitoring = "create_monitoring"
    val BookManually = "false"
    val BookByApplication = "true"
    val Yes = "true"
    val No = "false"
  }

}
