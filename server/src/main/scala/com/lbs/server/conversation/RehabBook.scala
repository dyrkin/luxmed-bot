package com.lbs.server.conversation

import com.lbs.api.json.model.*
import com.lbs.bot.*
import com.lbs.bot.model.{Button, Command}
import com.lbs.server.conversation.DatePicker.{DateFromMode, DateRange, DateToMode}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Pager.SimpleItemsProvider
import com.lbs.server.conversation.RehabBook.*
import com.lbs.server.conversation.StaticData.{FindOptions, FoundOptions, LatestOptions, StaticDataConfig}
import com.lbs.server.conversation.TimePicker.{TimeFromMode, TimeToMode}
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.service.{ApiService, DataService, MonitoringService}
import com.lbs.server.util.MessageExtractors.*
import com.lbs.server.util.ServerModelConverters.*
import org.apache.pekko.actor.ActorSystem

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, LocalDate, LocalDateTime, LocalTime, MonthDay}
import scala.util.Try

class RehabBook(
  val userId: UserId,
  bot: Bot,
  apiService: ApiService,
  dataService: DataService,
  monitoringService: MonitoringService,
  val localization: Localization,
  datePickerFactory: UserIdWithOriginatorTo[DatePicker],
  timePickerFactory: UserIdWithOriginatorTo[TimePicker],
  referralPagerFactory: UserIdWithOriginatorTo[Pager[Referral]],
  staticDataFactory: UserIdWithOriginatorTo[StaticData],
  physiotherapistPagerFactory: UserIdWithOriginatorTo[Pager[IdName]],
  termsPagerFactory: UserIdWithOriginatorTo[Pager[TermExt]]
)(val actorSystem: ActorSystem)
    extends Conversation[RehabBookingData]
    with Localizable {

  private val datePicker = datePickerFactory(userId, self)
  private val timePicker = timePickerFactory(userId, self)
  private val referralPager = referralPagerFactory(userId, self)
  private[conversation] val staticData = staticDataFactory(userId, self)
  private val physiotherapistPager = physiotherapistPagerFactory(userId, self)
  private val termsPager = termsPagerFactory(userId, self)

  entryPoint(fetchReferrals, RehabBookingData())

  private def fetchReferrals: Step =
    process { data =>
      val referralsEither = apiService.getRehabReferrals(userId.accountId)
      referralsEither match {
        case Left(ex) =>
          logger.error("Failed to fetch referrals", ex)
          bot.sendMessage(userId.source, ex.getMessage)
          end()
        case Right(referrals) if referrals.isEmpty =>
          bot.sendMessage(userId.source, lang.noRehabReferralsFound)
          end()
        case Right(List(single)) =>
          // Auto-select single referral
          handleReferralSelected(single, data)
        case Right(referrals) =>
          referralPager.restart()
          referralPager ! Right(new SimpleItemsProvider(referrals))
          goto(selectReferral).using(data)
      }
    }

  private def handleReferralSelected(referral: Referral, data: RehabBookingData) = {
    val serviceInstanceId = referral.serviceInstanceId
    apiService.getServiceReferral(userId.accountId, serviceInstanceId) match {
      case Left(ex) =>
        logger.error("Failed to get service referral", ex)
        bot.sendMessage(userId.source, ex.getMessage)
        end()
      case Right(serviceReferral) =>
        val primary = serviceReferral.primaryReferral
        val serviceVariantId = primary.map(_.serviceVariantId).getOrElse(0L)
        val serviceVariantName = primary.map(_.serviceName).getOrElse("")
        val referralId = primary.map(_.id).getOrElse(0L)
        val newData = data.copy(
          referral = Some(referral),
          serviceReferral = Some(serviceReferral),
          sourceVisitId = serviceInstanceId,
          referralId = referralId,
          referralTypeId = 1,
          serviceVariantId = serviceVariantId,
          serviceVariantName = serviceVariantName,
          remainingProcedures = referral.proceduresAmount - 1
        )
        // Now fetch facilities and go to city selection
        apiService.getRehabFacilities(userId.accountId, serviceVariantId) match {
          case Left(ex) =>
            logger.error("Failed to get rehab facilities", ex)
            bot.sendMessage(userId.source, ex.getMessage)
            end()
          case Right(facilities) =>
            goto(selectCity).using(newData.copy(rehabFacilities = Some(facilities)))
        }
    }
  }

  private def selectReferral: Step =
    ask { _ => () } onReply {
      case Msg(cmd: Command, _) =>
        referralPager ! cmd
        stay()
      case Msg(referral: Referral, data: RehabBookingData) =>
        handleReferralSelected(referral, data)
      case Msg(Pager.NoItemsFound, _) =>
        bot.sendMessage(userId.source, lang.noRehabReferralsFound)
        end()
    }

  private def selectCity: Step =
    staticData(rehabCityConfig) { data =>
      val locations = data.rehabFacilities.map(_.locations).getOrElse(Nil)
      staticOptions(
        staticOptions = Right(locations),
        applyId = city => data.copy(cityId = city)
      )
    }(requestNext = selectFacility)

  private def selectFacility: Step =
    staticData(rehabFacilityConfig) { data =>
      val facilities =
        data.rehabFacilities
          .map(_.facilities.filter(_.locationId == data.cityId.id))
          .getOrElse(Nil)
      staticOptions(
        staticOptions = Right(facilities),
        applyId = facility => data.withFacility(facility)
      )
    }(requestNext = continueAfterFacility)

  private def continueAfterFacility: Step =
    process { data =>
      if (data.hasAnyFacility) goto(askPhysiotherapist)
      else goto(askAddAnotherFacility)
    }

  private def askAddAnotherFacility: Step =
    ask { data =>
      bot.sendMessage(
        userId.source,
        lang.selectedRehabFacilities(data),
        inlineKeyboard =
          createInlineKeyboard(
            Seq(Button(lang.addAnotherClinic, Tags.AddAnotherFacility), Button(lang.continueBooking, Tags.Continue))
          )
      )
    } onReply {
      case Msg(CallbackCommand(Tags.AddAnotherFacility), _) =>
        goto(selectFacility)
      case Msg(CallbackCommand(Tags.Continue), _) =>
        goto(askPhysiotherapist)
    }

  private def askPhysiotherapist: Step =
    process { data =>
      apiService.getAllDoctors(userId.accountId, data.cityId.id, data.serviceVariantId) match {
        case Left(_) =>
          goto(requestDateFrom).using(data)
        case Right(Nil) =>
          goto(requestDateFrom).using(data)
        case Right(doctors) =>
          val facilityFilter = data.facilityFilter
          val doctorItems = doctors
            .filter(doc => facilityFilter.isEmpty || doc.facilityGroupIds.exists(ids => facilityFilter.exists(ids.contains)))
            .map(_.toIdName)
          physiotherapistPager.restart()
          physiotherapistPager ! Right(new SimpleItemsProvider(doctorItems))
          bot.sendMessage(
            userId.source,
            lang.choosePhysiotherapist,
            inlineKeyboard = createInlineKeyboard(Seq(Button(lang.anyPhysiotherapist, Tags.AnyPhysiotherapist)))
          )
          goto(selectPhysiotherapist).using(data)
      }
    }

  private def selectPhysiotherapist: Step =
    ask { _ => () } onReply {
      case Msg(CallbackCommand(Tags.AnyPhysiotherapist), data: RehabBookingData) =>
        goto(requestDateFrom).using(data)
      case Msg(cmd: Command, _) =>
        physiotherapistPager ! cmd
        stay()
      case Msg(doctor: IdName, data: RehabBookingData) =>
        goto(requestDateFrom).using(data.copy(physiotherapistId = doctor))
      case Msg(Pager.NoItemsFound, data: RehabBookingData) =>
        goto(requestDateFrom).using(data)
    }

  private def requestDateFrom: Step =
    ask { data =>
      datePicker.restart()
      datePicker ! DateFromMode
      datePicker ! data.dateFrom
    } onReply {
      case Msg(cmd: Command, _) =>
        datePicker ! cmd
        stay()
      case Msg(dateRange: DateRange, data: RehabBookingData) =>
        goto(askExcludedWeekdays).using(data.copy(
          dateFrom = dateRange.from,
          dateTo = capDateTo(dateRange.from, dateRange.to),
          excludedWeekdays = Set.empty,
          excludedDates = Set.empty
        ))
      case Msg(date: LocalDateTime, data: RehabBookingData) =>
        goto(requestDateTo).using(data.copy(
          dateFrom = date,
          excludedWeekdays = Set.empty,
          excludedDates = Set.empty
        ))
    }

  private def requestDateTo: Step =
    ask { data =>
      datePicker.restart()
      datePicker ! DateToMode
      datePicker ! data.dateFrom.plusDays(1)
    } onReply {
      case Msg(cmd: Command, _) =>
        datePicker ! cmd
        stay()
      case Msg(date: LocalDateTime, data: RehabBookingData) =>
        // Enforce MaxIntervalInDays = 13
        goto(askExcludedWeekdays).using(data.copy(dateTo = capDateTo(data.dateFrom, date)))
    }

  private def requestTimeFrom: Step =
    ask { data =>
      timePicker.restart()
      timePicker ! TimeFromMode
      timePicker ! data.timeFrom
    } onReply {
      case Msg(cmd: Command, _) =>
        timePicker ! cmd
        stay()
      case Msg(time: LocalTime, data: RehabBookingData) =>
        goto(requestTimeTo).using(data.copy(timeFrom = time))
    }

  private def requestTimeTo: Step =
    ask { data =>
      timePicker.restart()
      timePicker ! TimeToMode
      timePicker ! data.timeTo
    } onReply {
      case Msg(cmd: Command, _) =>
        timePicker ! cmd
        stay()
      case Msg(time: LocalTime, data: RehabBookingData) =>
        goto(requestAction).using(data.copy(timeTo = time))
    }

  private def requestAction: Step =
    ask { data =>
      bot.sendMessage(
        userId.source,
        lang.rehabBookingSummary(data),
        inlineKeyboard =
          createInlineKeyboard(Seq(Button(lang.findTerms, Tags.FindTerms), Button(lang.modifyDate, Tags.ModifyDate)))
      )
    } onReply {
      case Msg(CallbackCommand(Tags.FindTerms), _) =>
        goto(requestTerm)
      case Msg(CallbackCommand(Tags.ModifyDate), data: RehabBookingData) =>
        goto(requestDateFrom).using(data.copy(
          dateFrom = LocalDateTime.now(),
          dateTo = LocalDateTime.now().plusDays(1L),
          excludedWeekdays = Set.empty,
          excludedDates = Set.empty
        ))
    }

  private def requestTerm: Step =
    ask { data =>
      val selectedFacilityIds = data.facilityFilter
      val availableTerms = apiService.getAvailableRehabTerms(
        userId.accountId,
        data.cityId.id,
        data.serviceVariantId,
        data.referralId,
        data.referralTypeId,
        data.dateFrom,
        data.dateTo,
        data.timeFrom,
        data.timeTo,
        data.singleFacilityId,
        Option(data.physiotherapistId).flatMap(d => d.optionalId)
      ).map(filterSelectedFacilities(_, selectedFacilityIds))
        .map(_.filterNot(term => data.excludedWeekdays.contains(term.term.dateTimeFrom.get.getDayOfWeek)))
        .map(_.filterNot(term => data.excludedDates.contains(term.term.dateTimeFrom.get.toLocalDate)))
        .map(_.sortBy(_.term.dateTimeFrom.get))
      termsPager.restart()
      termsPager ! availableTerms.map(new SimpleItemsProvider(_))
    } onReply {
      case Msg(cmd: Command, _) =>
        termsPager ! cmd
        stay()
      case Msg(term: TermExt, data: RehabBookingData) =>
        val response = for {
          xsrfToken <- apiService.getXsrfToken(userId.accountId)
          locktermRequest = (term, data.referralId, data.referralTypeId).mapTo[ReservationLocktermRequest]
          lockTermResponse <- apiService.reservationLockterm(userId.accountId, xsrfToken, locktermRequest)
        } yield (lockTermResponse, xsrfToken)
        response match {
          case Left(ex) =>
            logger.error("Cannot lock term", ex)
            bot.sendMessage(userId.source, ex.getMessage)
            end()
          case Right((reservationLocktermResponse, xsrfToken)) =>
            bot.sendMessage(
              userId.source,
              lang.confirmAppointment(term),
              inlineKeyboard =
                createInlineKeyboard(Seq(Button(lang.cancel, Tags.Cancel), Button(lang.book, Tags.Book)))
            )
            goto(awaitReservation).using(data.copy(
              term = Some(term),
              xsrfToken = Some(xsrfToken),
              reservationLocktermResponse = Some(reservationLocktermResponse)
            ))
        }
      case Msg(Pager.NoItemsFound, data: RehabBookingData) =>
        bot.sendMessage(
          userId.source,
          lang.noTermsFound,
          inlineKeyboard = createInlineKeyboard(
            Seq(Button(lang.modifyDate, Tags.ModifyDate), Button(lang.createMonitoring, Tags.CreateMonitoring))
          )
        )
        goto(askNoTermsAction).using(data)
    }

  private def askNoTermsAction: Step =
    monologue {
      case Msg(CallbackCommand(Tags.ModifyDate), data: RehabBookingData) =>
        goto(requestDateFrom).using(data.copy(
          dateFrom = LocalDateTime.now(),
          dateTo = LocalDateTime.now().plusDays(1L),
          excludedWeekdays = Set.empty,
          excludedDates = Set.empty
        ))
      case Msg(CallbackCommand(Tags.CreateMonitoring), data: RehabBookingData) =>
        val settingsMaybe = dataService.findSettings(userId.userId)
        val (defaultOffset, askOffset) = settingsMaybe match {
          case Some(settings) => (settings.defaultOffset, settings.alwaysAskOffset)
          case None           => (0, false)
        }
        val newData = data.copy(offset = defaultOffset)
        if (askOffset) goto(askMonitoringOffsetOption).using(newData)
        else goto(askMonitoringAutobookOption).using(newData)
    }

  private def askMonitoringOffsetOption: Step =
    ask { _ =>
      bot.sendMessage(
        userId.source,
        lang.pleaseSpecifyOffset,
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No)))
      )
    } onReply {
      case Msg(TextCommand(IntString(offset)), data: RehabBookingData) =>
        goto(askMonitoringAutobookOption).using(data.copy(offset = offset))
      case Msg(CallbackCommand(BooleanString(false)), _) =>
        goto(askMonitoringAutobookOption)
    }

  private def askExcludedWeekdays: Step =
    ask { data =>
      bot.sendMessage(
        userId.source,
        lang.chooseExcludedWeekdays(data.excludedWeekdays),
        inlineKeyboard = weekdayKeyboard(data.excludedWeekdays)
      )
    } onReply {
      case Msg(CallbackCommand(Tags.Done), _) =>
        goto(askExcludedDates)
      case Msg(CallbackCommand(WeekdayTag(dayOfWeek)), data: RehabBookingData) =>
        val weekdays =
          if (data.excludedWeekdays.contains(dayOfWeek)) data.excludedWeekdays - dayOfWeek
          else data.excludedWeekdays + dayOfWeek
        goto(askExcludedWeekdays).using(data.copy(excludedWeekdays = weekdays))
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
      case Msg(TextCommand(text), data: RehabBookingData) =>
        parseExcludedDates(text, data.dateFrom.toLocalDate) match {
          case Left(error) =>
            bot.sendMessage(userId.source, lang.unableToParseExcludedDates(error))
            stay()
          case Right(dates) =>
            goto(requestTimeFrom).using(data.copy(excludedDates = dates.toSet))
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
    } onReply { case Msg(CallbackCommand(BooleanString(autobook)), data: RehabBookingData) =>
      val newData = data.copy(autobook = autobook)
      if (autobook) goto(askMonitoringRebookOption).using(newData)
      else goto(createRehabMonitoring).using(newData)
    }

  private def askMonitoringRebookOption: Step =
    ask { _ =>
      bot.sendMessage(
        userId.source,
        lang.rebookIfExists,
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No), Button(lang.yes, Tags.Yes)))
      )
    } onReply { case Msg(CallbackCommand(BooleanString(rebookIfExists)), data: RehabBookingData) =>
      goto(createRehabMonitoring).using(data.copy(rebookIfExists = rebookIfExists))
    }

  private def createRehabMonitoring: Step =
    process { data =>
      logger.debug(s"Creating rehab monitoring for $data")
      try {
        monitoringService.createMonitoring((userId -> data).mapTo[Monitoring])
        bot.sendMessage(userId.source, lang.monitoringHasBeenCreated)
      } catch {
        case ex: Exception =>
          logger.error("Unable to create rehab monitoring", ex)
          bot.sendMessage(userId.source, lang.unableToCreateMonitoring(ex.getMessage))
      }
      end()
    }

  private def awaitReservation: Step =
    monologue {
      case Msg(CallbackCommand(Tags.Cancel), data: RehabBookingData) =>
        apiService.deleteTemporaryReservation(
          userId.accountId,
          data.xsrfToken.get,
          data.reservationLocktermResponse.get.value.temporaryReservationId
        )
        goto(requestTerm).using(data)
      case Msg(CallbackCommand(Tags.Book), data: RehabBookingData) =>
        val reservationRequestMaybe = for {
          reservationLocktermResponse <- data.reservationLocktermResponse
          term <- data.term
        } yield (reservationLocktermResponse, term).mapTo[ReservationConfirmRequest]
        reservationRequestMaybe match {
          case Some(reservationRequest) =>
            apiService.reservationConfirm(userId.accountId, data.xsrfToken.get, reservationRequest) match {
              case Left(ex) =>
                logger.error("Error during rehab reservation", ex)
                bot.sendMessage(userId.source, ex.getMessage)
                end()
              case Right(_) =>
                val remaining = data.remainingProcedures
                bot.sendMessage(userId.source, lang.rehabAppointmentIsConfirmed(remaining))
                if (remaining > 0) {
                  bot.sendMessage(
                    userId.source,
                    lang.bookNextProcedure(remaining),
                    inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No), Button(lang.yes, Tags.Yes)))
                  )
                  goto(awaitChainDecision).using(data)
                } else {
                  end()
                }
            }
          case None =>
            logger.error(s"Cannot prepare reservation request from data $data")
            end()
        }
    }

  private def awaitChainDecision: Step =
    monologue {
      case Msg(CallbackCommand(Tags.Yes), data: RehabBookingData) =>
        // Restart with updated remaining count
        val newData = data.copy(remainingProcedures = data.remainingProcedures - 1)
        goto(requestDateFrom).using(newData.copy(
          dateFrom = LocalDateTime.now(),
          dateTo = LocalDateTime.now().plusDays(1L),
          term = None,
          xsrfToken = None,
          reservationLocktermResponse = None
        ))
      case Msg(CallbackCommand(Tags.No), _) =>
        end()
    }

  private def staticData(staticDataConfig: => StaticDataConfig)(
    functions: RehabBookingData => Step => MessageProcessorFn
  )(requestNext: Step)(implicit functionName: sourcecode.Name): Step = {
    ask { _ =>
      staticData.restart()
      staticData ! staticDataConfig
    } onReply { case msg @ Msg(_, data: RehabBookingData) =>
      val fn = functions(data)(requestNext)
      fn(msg)
    }
  }

  private def staticOptions[T <: Identified](
    staticOptions: => Either[Throwable, List[T]],
    applyId: IdName => RehabBookingData
  ): Step => MessageProcessorFn = { nextStep =>
    {
      case Msg(cmd: Command, _) =>
        staticData ! cmd
        stay()
      case Msg(LatestOptions, _) =>
        staticData ! LatestOptions(Nil)
        stay()
      case Msg(FindOptions(searchText), _) =>
        staticData ! FoundOptions(staticOptions.map(_.filter(_.name.toLowerCase.contains(searchText))))
        stay()
      case Msg(id: IdName, _) =>
        goto(nextStep).using(applyId(id))
    }
  }

  private def weekdayKeyboard(excludedWeekdays: Set[DayOfWeek]) = {
    val weekdays = DayOfWeek.values.toSeq
    val buttons = weekdays.map { day =>
      val label = s"${if (excludedWeekdays.contains(day)) "✅ " else ""}${lang.weekdayName(day)}"
      Button(label, Tags.WeekdayPrefix + day.getValue)
    }
    createInlineKeyboard(buttons :+ Button(lang.done, Tags.Done), columns = 2)
  }

  private def parseExcludedDates(text: String, dateFrom: LocalDate): Either[String, Seq[LocalDate]] = {
    val parts = text.split("[,;\\s]+").map(_.trim).filter(_.nonEmpty).toSeq
    val parsed = parts.map(parseExcludedDate(_, dateFrom))
    parsed.collectFirst { case Left(value) => value } match {
      case Some(error) => Left(error)
      case None        => Right(parsed.collect { case Right(date) => date })
    }
  }

  private def parseExcludedDate(text: String, dateFrom: LocalDate): Either[String, LocalDate] = {
    val fullDate = Try(LocalDate.parse(text)).toOption
    val dayMonth = Try {
      val parsed = MonthDay.parse(text, DateTimeFormatter.ofPattern("dd-MM"))
      val candidate = parsed.atYear(dateFrom.getYear)
      if (candidate.isBefore(dateFrom)) candidate.plusYears(1) else candidate
    }.toOption

    fullDate.orElse(dayMonth).toRight(text)
  }

  private def filterSelectedFacilities(terms: List[TermExt], facilityIds: Seq[Long]): List[TermExt] =
    if (facilityIds.size <= 1) terms
    else terms.filter(term => facilityIds.contains(term.term.clinicGroupId))

  private def capDateTo(dateFrom: LocalDateTime, dateTo: LocalDateTime): LocalDateTime = {
    val maxDate = dateFrom.plusDays(13)
    if (dateTo.isAfter(maxDate)) maxDate else dateTo
  }

  private def rehabCityConfig = StaticDataConfig(lang.city, "wro", "Wrocław", isAnyAllowed = false)

  private def rehabFacilityConfig = StaticDataConfig(lang.clinic, "swob", "Swobodna 1", isAnyAllowed = true)

  beforeDestroy {
    datePicker.destroy()
    timePicker.destroy()
    referralPager.destroy()
    staticData.destroy()
    physiotherapistPager.destroy()
    termsPager.destroy()
  }
}

object RehabBook {

  case class RehabBookingData(
    referral: Option[Referral] = None,
    serviceReferral: Option[ServiceReferralResponse] = None,
    sourceVisitId: Long = 0L,
    referralId: Long = 0L,
    referralTypeId: Int = 1,
    serviceVariantId: Long = 0L,
    serviceVariantName: String = "",
    cityId: IdName = null,
    facilityId: IdName = null,
    facilityIds: Seq[IdName] = Seq(),
    physiotherapistId: IdName = null,
    rehabFacilities: Option[RehabFacilitiesResponse] = None,
    dateFrom: LocalDateTime = LocalDateTime.now(),
    dateTo: LocalDateTime = LocalDateTime.now().plusDays(13L),
    timeFrom: LocalTime = LocalTime.of(7, 0),
    timeTo: LocalTime = LocalTime.of(21, 0),
    term: Option[TermExt] = None,
    xsrfToken: Option[XsrfToken] = None,
    reservationLocktermResponse: Option[ReservationLocktermResponse] = None,
    remainingProcedures: Int = 0,
    offset: Int = 0,
    autobook: Boolean = false,
    rebookIfExists: Boolean = false,
    excludedWeekdays: Set[DayOfWeek] = Set.empty,
    excludedDates: Set[LocalDate] = Set.empty
  ) {
    def selectedFacilities: Seq[IdName] = {
      if (facilityIds.nonEmpty) facilityIds
      else Option(facilityId).toSeq
    }

    def facilityOptions: Seq[Option[Long]] = selectedFacilities.map(_.optionalId) match {
      case Nil => Seq(None)
      case xs  => xs
    }

    def facilityFilter: Seq[Long] = facilityOptions.flatten.distinct

    def singleFacilityId: Option[Long] = {
      val selected = facilityOptions.distinct
      if (selected.size == 1) selected.head else None
    }

    def hasAnyFacility: Boolean = facilityOptions.exists(_.isEmpty)

    def withFacility(facility: IdName): RehabBookingData = {
      val updatedFacilities =
        if (facility.optionalId.isEmpty) Seq(facility)
        else (selectedFacilities.filter(_.optionalId.nonEmpty) :+ facility).distinctBy(_.id)
      copy(facilityId = updatedFacilities.head, facilityIds = updatedFacilities)
    }
  }

  object Tags {
    val Cancel = "cancel"
    val Book = "book"
    val FindTerms = "find_terms"
    val ModifyDate = "modify_date"
    val CreateMonitoring = "create_monitoring"
    val AnyPhysiotherapist = "any_physiotherapist"
    val BookManually = "false"
    val BookByApplication = "true"
    val Yes = "true"
    val No = "false"
    val AddAnotherFacility = "add_another_facility"
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

