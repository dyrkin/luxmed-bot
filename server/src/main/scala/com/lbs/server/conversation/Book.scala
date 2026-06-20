package com.lbs.server.conversation

import com.lbs.api.json.model.*
import com.lbs.bot.*
import com.lbs.bot.model.{Button, Command}
import com.lbs.server.conversation.Book.*
import com.lbs.server.conversation.DatePicker.{DateFromMode, DateRange, DateToMode}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Pager.SimpleItemsProvider
import com.lbs.server.conversation.StaticData.StaticDataConfig
import com.lbs.server.conversation.TimePicker.{TimeFromMode, TimeToMode}
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.service.{ApiService, DataService, MonitoringService}
import com.lbs.server.util.MessageExtractors.*
import com.lbs.server.util.ServerModelConverters.*
import org.apache.pekko.actor.ActorSystem

import java.time.{DayOfWeek, LocalDate, LocalDateTime, LocalTime, MonthDay}
import java.time.format.DateTimeFormatter
import scala.util.Try

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
)(val actorSystem: ActorSystem)
    extends Conversation[BookingData]
    with StaticDataForBooking
    with Localizable {

  private val datePicker = datePickerFactory(userId, self)
  private val timePicker = timePickerFactory(userId, self)
  private[conversation] val staticData = staticDataFactory(userId, self)
  private val termsPager = termsPagerFactory(userId, self)

  entryPoint(askCity, BookingData())

  private def askCity: Step =
    staticData(cityConfig) { (bd: BookingData) =>
      withFunctions[DictionaryCity](
        latestOptions = dataService.getLatestCities(userId.accountId),
        staticOptions = apiService.getAllCities(userId.accountId),
        applyId = id => bd.copy(cityId = id.toIdName)
      )
    }(requestNext = askService)

  private def askService: Step =
    staticData(serviceConfig) { (bd: BookingData) =>
      withFunctions[DictionaryServiceVariants](
        latestOptions = dataService.getLatestServicesByCityIdAndClinicId(userId.accountId, bd.cityId.id, None),
        staticOptions = apiService.getAllServices(userId.accountId),
        applyId = id => bd.copy(serviceId = id.toIdName)
      )
    }(requestNext = askClinic)

  private def askClinic: Step =
    staticData(clinicConfig) { (bd: BookingData) =>
      withFunctions[IdName](
        latestOptions = dataService.getLatestClinicsByCityId(userId.accountId, bd.cityId.id),
        staticOptions = apiService.getAllFacilities(userId.accountId, bd.cityId.id, bd.serviceId.id),
        applyId = id => bd.withClinic(id)
      )
    }(requestNext = continueAfterClinic)

  private def continueAfterClinic: Step =
    process { bookingData =>
      if (bookingData.hasAnyClinic) goto(askDoctor)
      else goto(askAddAnotherClinic)
    }

  private def askAddAnotherClinic: Step =
    ask { bookingData =>
      bot.sendMessage(
        userId.source,
        lang.selectedClinics(bookingData),
        inlineKeyboard =
          createInlineKeyboard(
            Seq(Button(lang.addAnotherClinic, Tags.AddAnotherClinic), Button(lang.continueBooking, Tags.Continue))
          )
      )
    } onReply {
      case Msg(CallbackCommand(Tags.AddAnotherClinic), _) =>
        goto(askClinic)
      case Msg(CallbackCommand(Tags.Continue), _) =>
        goto(askDoctor)
    }

  private def askDoctor: Step =
    staticData(doctorConfig) { (bd: BookingData) =>
      withFunctions[IdName](
        latestOptions = dataService.getLatestDoctorsByCityIdAndClinicIdAndServiceId(
          userId.accountId,
          bd.cityId.id,
          bd.singleClinicId,
          bd.serviceId.id
        ),
        staticOptions = apiService
          .getAllDoctors(userId.accountId, bd.cityId.id, bd.serviceId.id)
          .map(
              _.filter(doc => {
                val clinicIds = bd.clinicFilter
                clinicIds.isEmpty || doc.facilityGroupIds.exists(ids => clinicIds.exists(ids.contains))
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
      case Msg(dateRange: DateRange, bookingData: BookingData) =>
        goto(askExcludedWeekdays).using(bookingData.copy(
          dateFrom = dateRange.from,
          dateTo = dateRange.to,
          excludedWeekdays = Set.empty,
          excludedDates = Set.empty
        ))
      case Msg(date: LocalDateTime, bookingData: BookingData) =>
        goto(requestDateTo).using(bookingData.copy(
          dateFrom = date,
          excludedWeekdays = Set.empty,
          excludedDates = Set.empty
        ))
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
        goto(askExcludedWeekdays).using(bookingData.copy(dateTo = date))
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
        goto(requestTimeTo).using(bookingData.copy(timeFrom = time))
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
        goto(requestAction).using(bookingData.copy(timeTo = time))
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
        goto(requestDateFrom).using(bookingData.copy(
          dateFrom = LocalDateTime.now(),
          dateTo = LocalDateTime.now().plusDays(1L),
          excludedWeekdays = Set.empty,
          excludedDates = Set.empty
        ))
    }

  private def requestTerm: Step =
    ask { bookingData =>
      val availableTerms = getAvailableTerms(bookingData)
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
              goto(awaitRebookDecision).using(bookingData.copy(
                term = Some(term),
                xsrfToken = Some(xsrfToken),
                reservationLocktermResponse = Some(reservationLocktermResponse)
              ))
            } else {
              bot.sendMessage(
                userId.source,
                lang.confirmAppointment(term),
                inlineKeyboard =
                  createInlineKeyboard(Seq(Button(lang.cancel, Tags.Cancel), Button(lang.book, Tags.Book)))
              )
              goto(awaitReservation).using(bookingData.copy(
                term = Some(term),
                xsrfToken = Some(xsrfToken),
                reservationLocktermResponse = Some(reservationLocktermResponse)
              ))
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
        goto(requestDateFrom).using(bookingData.copy(
          dateFrom = LocalDateTime.now(),
          dateTo = LocalDateTime.now().plusDays(1L),
          excludedWeekdays = Set.empty,
          excludedDates = Set.empty
        ))
      case Msg(CallbackCommand(Tags.CreateMonitoring), bookingData) =>
        val settingsMaybe = dataService.findSettings(userId.userId)
        val (defaultOffset, askOffset) = settingsMaybe match {
          case Some(settings) => (settings.defaultOffset, settings.alwaysAskOffset)
          case None           => (0, false)
        }
        val newData = bookingData.copy(offset = defaultOffset)
        if (askOffset) goto(askMonitoringOffsetOption).using(newData)
        else goto(askMonitoringAutobookOption).using(newData)
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
        goto(askMonitoringAutobookOption).using(bookingData.copy(offset = offset))
      case Msg(CallbackCommand(BooleanString(false)), _) =>
        goto(askMonitoringAutobookOption)
    }

  private def askExcludedWeekdays: Step =
    ask { bookingData =>
      bot.sendMessage(
        userId.source,
        lang.chooseExcludedWeekdays(bookingData.excludedWeekdays),
        inlineKeyboard = excludedWeekdayKeyboard(bookingData.excludedWeekdays)
      )
    } onReply {
      case Msg(CallbackCommand(Tags.Done), _) =>
        goto(askExcludedDates)
      case Msg(CallbackCommand(WeekdayTag(dayOfWeek)), bookingData: BookingData) =>
        val weekdays =
          if (bookingData.excludedWeekdays.contains(dayOfWeek)) bookingData.excludedWeekdays - dayOfWeek
          else bookingData.excludedWeekdays + dayOfWeek
        goto(askExcludedWeekdays).using(bookingData.copy(excludedWeekdays = weekdays))
    }

  private def askExcludedDates: Step =
    ask { _ =>
      bot.sendMessage(
        userId.source,
        lang.pleaseEnterExcludedDates,
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No)))
      )
    } onReply {
      case Msg(CallbackCommand(BooleanString(false)), _) =>
        goto(requestTimeFrom)
      case Msg(TextCommand(text), bookingData: BookingData) =>
        parseExcludedDates(text, bookingData.dateFrom.toLocalDate) match {
          case Right(dates) =>
            goto(requestTimeFrom).using(bookingData.copy(excludedDates = dates.toSet))
          case Left(error) =>
            bot.sendMessage(userId.source, lang.unableToParseExcludedDates(error))
            stay()
        }
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
      if (autobook) goto(askMonitoringRebookOption).using(data)
      else goto(createMonitoring).using(data)
    }

  private def askMonitoringRebookOption: Step =
    ask { _ =>
      bot.sendMessage(
        userId.source,
        lang.rebookIfExists,
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No), Button(lang.yes, Tags.Yes)))
      )
    } onReply { case Msg(CallbackCommand(BooleanString(rebookIfExists)), bookingData: BookingData) =>
      goto(createMonitoring).using(bookingData.copy(rebookIfExists = rebookIfExists))
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

  private def getAvailableTerms(bookingData: BookingData): Either[Throwable, List[TermExt]] = {
    val selectedClinicIds = bookingData.clinicFilter
    apiService.getAvailableTerms(
      userId.accountId,
      bookingData.cityId.id,
      bookingData.singleClinicId,
      bookingData.serviceId.id,
      bookingData.doctorId.optionalId,
      bookingData.dateFrom,
      bookingData.dateTo,
      timeFrom = bookingData.timeFrom,
      timeTo = bookingData.timeTo
    ).map { terms =>
      if (selectedClinicIds.size <= 1) terms
      else terms.filter(term => selectedClinicIds.contains(term.term.clinicGroupId))
    }.map(_.filterNot(term => bookingData.excludedWeekdays.contains(term.term.dateTimeFrom.get.getDayOfWeek)))
      .map(_.filterNot(term => bookingData.excludedDates.contains(term.term.dateTimeFrom.get.toLocalDate)))
      .map(_.sortBy(_.term.dateTimeFrom.get))
  }

  private def excludedWeekdayKeyboard(excludedWeekdays: Set[DayOfWeek]) = {
    val weekdays = DayOfWeek.values().toSeq
    val buttons = weekdays.map { day =>
      val label = s"${if (excludedWeekdays.contains(day)) "✅ " else ""}${lang.weekdayName(day)}"
      Button(label, Tags.WeekdayPrefix + day.getValue)
    }
    createInlineKeyboard(buttons :+ Button(lang.done, Tags.Done), columns = 2)
  }

  private def parseExcludedDates(text: String, dateFrom: LocalDate): Either[String, Seq[LocalDate]] = {
    val parts = text.split("[,;\\s]+").map(_.trim).filter(_.nonEmpty).toSeq
    val parsed = parts.map(parseExcludedDate(_, dateFrom))
    parsed.collectFirst { case Left(error) => error } match {
      case Some(error) => Left(error)
      case None        => Right(parsed.collect { case Right(date) => date }.distinct)
    }
  }

  private def parseExcludedDate(text: String, dateFrom: LocalDate): Either[String, LocalDate] = {
    val iso = Try(LocalDate.parse(text)).toOption
    val dayMonth = Try {
      val parsed = MonthDay.parse(text, DateTimeFormatter.ofPattern("dd-MM"))
      val date = parsed.atYear(dateFrom.getYear)
      if (date.isBefore(dateFrom)) date.plusYears(1) else date
    }.toOption
    iso.orElse(dayMonth).toRight(text)
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
    clinicIds: Seq[IdName] = Seq(),
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
    xsrfToken: Option[XsrfToken] = None,
    excludedWeekdays: Set[DayOfWeek] = Set.empty,
    excludedDates: Set[LocalDate] = Set.empty
  ) {
    def selectedClinics: Seq[IdName] = {
      if (clinicIds.nonEmpty) clinicIds
      else Option(clinicId).toSeq
    }

    def clinicOptions: Seq[Option[Long]] = selectedClinics.map(_.optionalId) match {
      case Nil => Seq(None)
      case xs  => xs
    }

    def clinicFilter: Seq[Long] = clinicOptions.flatten.distinct

    def singleClinicId: Option[Long] = {
      val selected = clinicOptions.distinct
      if (selected.size == 1) selected.head else None
    }

    def hasAnyClinic: Boolean = clinicOptions.exists(_.isEmpty)

    def withClinic(clinic: IdName): BookingData = {
      val updatedClinics =
        if (clinic.optionalId.isEmpty) Seq(clinic)
        else (selectedClinics.filter(_.optionalId.nonEmpty) :+ clinic).distinctBy(_.id)
      copy(clinicId = updatedClinics.head, clinicIds = updatedClinics)
    }
  }

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
    val AddAnotherClinic = "add_another_clinic"
    val Continue = "continue"
    val Done = "done"
    val WeekdayPrefix = "weekday_"
  }

  object WeekdayTag {
    def unapply(tag: String): Option[DayOfWeek] =
      tag.stripPrefix(Tags.WeekdayPrefix) match {
        case value if tag.startsWith(Tags.WeekdayPrefix) =>
          Try(DayOfWeek.of(value.toInt)).toOption
        case _ => None
      }
  }

}
