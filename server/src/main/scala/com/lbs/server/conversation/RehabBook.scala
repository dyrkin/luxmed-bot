package com.lbs.server.conversation

import org.apache.pekko.actor.ActorSystem
import com.lbs.api.json.model.*
import com.lbs.bot.*
import com.lbs.bot.model.{Button, Command}
import com.lbs.server.conversation.DatePicker.{DateFromMode, DateToMode}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Pager.SimpleItemsProvider
import com.lbs.server.conversation.RehabBook.*
import com.lbs.server.conversation.TimePicker.{TimeFromMode, TimeToMode}
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.service.{ApiService, DataService, MonitoringService}
import com.lbs.server.util.MessageExtractors.*
import com.lbs.server.util.ServerModelConverters.*

import java.time.{LocalDateTime, LocalTime}

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
  locationPagerFactory: UserIdWithOriginatorTo[Pager[RehabLocation]],
  facilityPagerFactory: UserIdWithOriginatorTo[Pager[RehabFacility]],
  physiotherapistPagerFactory: UserIdWithOriginatorTo[Pager[IdName]],
  termsPagerFactory: UserIdWithOriginatorTo[Pager[TermExt]]
)(implicit val actorSystem: ActorSystem)
    extends Conversation[RehabBookingData]
    with Localizable {

  private val datePicker = datePickerFactory(userId, self)
  private val timePicker = timePickerFactory(userId, self)
  private val referralPager = referralPagerFactory(userId, self)
  private val locationPager = locationPagerFactory(userId, self)
  private val facilityPager = facilityPagerFactory(userId, self)
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
          goto(selectReferral) using data
      }
    }

  private def handleReferralSelected(referral: Referral, data: RehabBookingData) = {
    val serviceInstanceId = referral.sourceVisitId
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
            locationPager.restart()
            locationPager ! Right(new SimpleItemsProvider(facilities.locations))
            goto(selectCity) using newData.copy(rehabFacilities = Some(facilities))
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
    ask { _ => () } onReply {
      case Msg(cmd: Command, _) =>
        locationPager ! cmd
        stay()
      case Msg(location: RehabLocation, data: RehabBookingData) =>
        val newData = data.copy(cityId = location.toIdName)
        val facilitiesForCity = data.rehabFacilities.map(_.facilities.filter(_.locationId == location.id)).getOrElse(Nil)
        facilityPager.restart()
        facilityPager ! Right(new SimpleItemsProvider(facilitiesForCity))
        goto(selectFacility) using newData
      case Msg(Pager.NoItemsFound, _) =>
        bot.sendMessage(userId.source, lang.noRehabReferralsFound)
        end()
    }

  private def selectFacility: Step =
    ask { _ => () } onReply {
      case Msg(cmd: Command, _) =>
        facilityPager ! cmd
        stay()
      case Msg(facility: RehabFacility, data: RehabBookingData) =>
        goto(askPhysiotherapist) using data.copy(facilityId = facility.toIdName)
      case Msg(Pager.NoItemsFound, data: RehabBookingData) =>
        // No facilities, treat as "Any"
        goto(askPhysiotherapist) using data
    }

  private def askPhysiotherapist: Step =
    process { data =>
      apiService.getAllDoctors(userId.accountId, data.cityId.id, data.serviceVariantId) match {
        case Left(_) =>
          goto(requestDateFrom) using data
        case Right(Nil) =>
          goto(requestDateFrom) using data
        case Right(doctors) =>
          val doctorItems = doctors.map(_.toIdName)
          physiotherapistPager.restart()
          physiotherapistPager ! Right(new SimpleItemsProvider(doctorItems))
          bot.sendMessage(
            userId.source,
            lang.choosePhysiotherapist,
            inlineKeyboard = createInlineKeyboard(Seq(Button(lang.anyPhysiotherapist, Tags.AnyPhysiotherapist)))
          )
          goto(selectPhysiotherapist) using data
      }
    }

  private def selectPhysiotherapist: Step =
    ask { _ => () } onReply {
      case Msg(CallbackCommand(Tags.AnyPhysiotherapist), data: RehabBookingData) =>
        goto(requestDateFrom) using data
      case Msg(cmd: Command, _) =>
        physiotherapistPager ! cmd
        stay()
      case Msg(doctor: IdName, data: RehabBookingData) =>
        goto(requestDateFrom) using data.copy(physiotherapistId = doctor)
      case Msg(Pager.NoItemsFound, data: RehabBookingData) =>
        goto(requestDateFrom) using data
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
      case Msg(date: LocalDateTime, data: RehabBookingData) =>
        goto(requestDateTo) using data.copy(dateFrom = date)
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
        val maxDate = data.dateFrom.plusDays(13)
        val effectiveDate = if (date.isAfter(maxDate)) maxDate else date
        goto(requestTimeFrom) using data.copy(dateTo = effectiveDate)
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
        goto(requestTimeTo) using data.copy(timeFrom = time)
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
        goto(requestAction) using data.copy(timeTo = time)
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
        goto(requestDateFrom) using data.copy(
          dateFrom = LocalDateTime.now(),
          dateTo = LocalDateTime.now().plusDays(1L)
        )
    }

  private def requestTerm: Step =
    ask { data =>
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
        Option(data.facilityId).flatMap(f => f.optionalId),
        Option(data.physiotherapistId).flatMap(d => d.optionalId)
      )
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
            goto(awaitReservation) using data.copy(
              term = Some(term),
              xsrfToken = Some(xsrfToken),
              reservationLocktermResponse = Some(reservationLocktermResponse)
            )
        }
      case Msg(Pager.NoItemsFound, data: RehabBookingData) =>
        bot.sendMessage(
          userId.source,
          lang.noTermsFound,
          inlineKeyboard = createInlineKeyboard(
            Seq(Button(lang.modifyDate, Tags.ModifyDate), Button(lang.createMonitoring, Tags.CreateMonitoring))
          )
        )
        goto(askNoTermsAction) using data
    }

  private def askNoTermsAction: Step =
    monologue {
      case Msg(CallbackCommand(Tags.ModifyDate), data: RehabBookingData) =>
        goto(requestDateFrom) using data.copy(
          dateFrom = LocalDateTime.now(),
          dateTo = LocalDateTime.now().plusDays(1L)
        )
      case Msg(CallbackCommand(Tags.CreateMonitoring), data: RehabBookingData) =>
        val settingsMaybe = dataService.findSettings(userId.userId)
        val (defaultOffset, askOffset) = settingsMaybe match {
          case Some(settings) => (settings.defaultOffset, settings.alwaysAskOffset)
          case None           => (0, false)
        }
        val newData = data.copy(offset = defaultOffset)
        if (askOffset) goto(askMonitoringOffsetOption) using newData
        else goto(askMonitoringAutobookOption) using newData
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
        goto(askMonitoringAutobookOption) using data.copy(offset = offset)
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
    } onReply { case Msg(CallbackCommand(BooleanString(autobook)), data: RehabBookingData) =>
      val newData = data.copy(autobook = autobook)
      if (autobook) goto(askMonitoringRebookOption) using newData
      else goto(createRehabMonitoring) using newData
    }

  private def askMonitoringRebookOption: Step =
    ask { _ =>
      bot.sendMessage(
        userId.source,
        lang.rebookIfExists,
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No), Button(lang.yes, Tags.Yes)))
      )
    } onReply { case Msg(CallbackCommand(BooleanString(rebookIfExists)), data: RehabBookingData) =>
      goto(createRehabMonitoring) using data.copy(rebookIfExists = rebookIfExists)
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
        goto(requestTerm) using data
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
                  goto(awaitChainDecision) using data
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
        goto(requestDateFrom) using newData.copy(
          dateFrom = LocalDateTime.now(),
          dateTo = LocalDateTime.now().plusDays(1L),
          term = None,
          xsrfToken = None,
          reservationLocktermResponse = None
        )
      case Msg(CallbackCommand(Tags.No), _) =>
        end()
    }

  beforeDestroy {
    datePicker.destroy()
    timePicker.destroy()
    referralPager.destroy()
    locationPager.destroy()
    facilityPager.destroy()
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
    rebookIfExists: Boolean = false
  )

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
  }
}

