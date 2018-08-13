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
package com.lbs.server.service

import java.time.ZonedDateTime
import java.util.concurrent.ScheduledFuture

import com.lbs.api.exception.InvalidLoginOrPasswordException
import com.lbs.api.json.model.{AvailableVisitsTermPresentation, ReservationRequest, TemporaryReservationRequest, ValuationsRequest}
import com.lbs.bot.Bot
import com.lbs.bot.model.{MessageSource, MessageSourceSystem}
import com.lbs.common.{Logger, Scheduler}
import com.lbs.server.lang.Localization
import com.lbs.server.repository.model._
import com.lbs.server.util.DateTimeUtil._
import com.lbs.server.util.ServerModelConverters._
import javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Random

@Service
class MonitoringService extends Logger {

  @Autowired
  private var bot: Bot = _
  @Autowired
  private var dataService: DataService = _
  @Autowired
  private var apiService: ApiService = _
  @Autowired
  private var localization: Localization = _

  private var activeMonitorings = mutable.Map.empty[JLong, (Monitoring, ScheduledFuture[_])]

  private val dbChecker = new Scheduler(1)

  private val monitoringExecutor = new Scheduler(10)

  private val MaxDelay = 10.minute

  private val PeriodBase = 10.minute

  private val PeriodMaxDelta = 5.minute

  private def delay = Random.nextInt(MaxDelay.toSeconds.toInt).seconds

  private def period = (PeriodBase.toSeconds + Random.nextInt(PeriodMaxDelta.toSeconds.toInt)).seconds

  private var checkedOn: ZonedDateTime = _

  def notifyUserAboutTerms(terms: Seq[AvailableVisitsTermPresentation], monitoring: Monitoring): Unit = {
    deactivateMonitoring(monitoring.recordId)

    val fiveTerms = terms.take(5).zipWithIndex //send only 5 closest terms
    val messages = lang(monitoring.userId)

    val message = messages.availableTermsHeader(terms.length) + "\n\n" +
      fiveTerms.map { case (term, index) =>
        messages.availableTermEntry(term, monitoring, index)
      }.mkString

    bot.sendMessage(monitoring.source, message)
  }

  private def monitor(monitoring: Monitoring): Unit = {
    debug(s"Looking for available terms. Monitoring [#${monitoring.recordId}]")
    val dateFrom = optimizeDateFrom(monitoring.dateFrom)
    val termsEither = apiService.getAvailableTerms(monitoring.accountId, monitoring.cityId, monitoring.clinicId, monitoring.serviceId,
      monitoring.doctorId, dateFrom, Some(monitoring.dateTo), timeFrom = monitoring.timeFrom, timeTo = monitoring.timeTo)
    termsEither match {
      case Right(terms) =>
        if (terms.nonEmpty) {
          debug(s"Found ${terms.length} terms by monitoring [#${monitoring.recordId}]")
          if (monitoring.autobook) {
            val term = terms.head
            bookAppointment(term, monitoring)
          } else {
            notifyUserAboutTerms(terms, monitoring)
          }
        } else {
          debug(s"No new terms found for monitoring [#${monitoring.recordId}]")
        }
      case Left(ex: InvalidLoginOrPasswordException) =>
        error(s"User entered invalid name or password. Monitoring will be disabled", ex)
        bot.sendMessage(monitoring.source, lang(monitoring.userId).invalidLoginOrPassword)
        val activeUserMonitorings = dataService.getActiveMonitorings(monitoring.accountId)
        activeUserMonitorings.foreach { m =>
          deactivateMonitoring(m.recordId)
        }
      case Left(ex) => error(s"Unable to receive terms by monitoring [#${monitoring.recordId}]", ex)
    }
  }

  private def optimizeDateFrom(date: ZonedDateTime) = {
    val now = ZonedDateTime.now()
    if (date.isBefore(now)) now else date
  }

  private def initializeMonitorings(allMonitorings: Seq[Monitoring]): Unit = {
    allMonitorings.foreach { monitoring =>
      if (monitoring.active && !activeMonitorings.contains(monitoring.recordId)) {
        val delaySnapshot = delay
        val periodSnapshot = period
        val future = monitoringExecutor.schedule(monitor(monitoring), delaySnapshot, periodSnapshot)
        debug(s"Scheduled monitoring: [#${monitoring.recordId}] with delay: $delaySnapshot and period: $periodSnapshot")
        activeMonitorings += (monitoring.recordId -> (monitoring -> future))
      }
    }
  }

  private def initializeNewMonitorings(): Unit = {
    debug(s"Looking for new monitorings created since $checkedOn")
    val currentTime = ZonedDateTime.now()
    val monitorings = dataService.getActiveMonitoringsSince(checkedOn)
    debug(s"New active monitorings found: ${monitorings.length}")
    checkedOn = currentTime
    initializeMonitorings(monitorings)
  }

  def notifyChatAboutDisabledMonitoring(monitoring: Monitoring): Unit = {
    bot.sendMessage(monitoring.source, lang(monitoring.userId).nothingWasFoundByMonitoring(monitoring))
  }

  private def disableOutdated(): Unit = {
    val now = ZonedDateTime.now()
    val toDisable = activeMonitorings.collect { case (id, (monitoring, _)) if monitoring.dateTo.isBefore(now) =>
      id -> monitoring
    }

    toDisable.foreach { case (id, monitoring) =>
      debug(s"Monitoring [#$id] is going to be disable as outdated")
      notifyChatAboutDisabledMonitoring(monitoring)
      deactivateMonitoring(id)
    }
  }

  private def updateMonitorings(): Unit = {
    initializeNewMonitorings()
    disableOutdated()
  }

  private def initializeDbChecker(): Unit = {
    dbChecker.schedule(updateMonitorings(), 1.minute)
  }

  private def bookAppointment(term: AvailableVisitsTermPresentation, monitoring: Monitoring): Unit = {
    val temporaryReservationRequest = term.mapTo[TemporaryReservationRequest]
    val valuationsRequest = term.mapTo[ValuationsRequest]
    val reservationMaybe = for {
      okResponse <- apiService.temporaryReservation(monitoring.accountId, temporaryReservationRequest, valuationsRequest)
      (temporaryReservation, valuations) = okResponse
      temporaryReservationId = temporaryReservation.id
      visitTermVariant = valuations.visitTermVariants.head
      reservationRequest = (temporaryReservationId, visitTermVariant, term).mapTo[ReservationRequest]
      reservation <- apiService.reservation(monitoring.accountId, reservationRequest)
    } yield reservation

    reservationMaybe match {
      case Right(_) =>
        bot.sendMessage(monitoring.source, lang(monitoring.userId).appointmentIsBooked(term, monitoring))
        deactivateMonitoring(monitoring.recordId)
      case Left(ex) =>
        error(s"Unable to book appointment by monitoring [${monitoring.recordId}]", ex)
    }

  }

  def deactivateMonitoring(monitoringId: JLong): Unit = {
    activeMonitorings.remove(monitoringId).foreach {
      case (monitoring, future) =>
        debug(s"Deactivating monitoring [#$monitoringId]")
        if (!future.isCancelled) {
          future.cancel(true)
        }
        monitoring.active = false
        dataService.saveMonitoring(monitoring)
    }
  }

  def createMonitoring(monitoring: Monitoring): Monitoring = {
    val userMonitoringsCount = dataService.getActiveMonitoringsCount(monitoring.accountId)
    require(userMonitoringsCount + 1 <= 10, lang(monitoring.userId).maximumMonitoringsLimitExceeded)
    val activeMonitoring = dataService.findActiveMonitoring(monitoring.accountId, monitoring.cityId, monitoring.serviceId, monitoring.doctorId)
    require(activeMonitoring.isEmpty, lang(monitoring.userId).monitoringOfTheSameTypeExists)
    dataService.saveMonitoring(monitoring)
  }

  def getActiveMonitorings(accountId: Long): Seq[Monitoring] = {
    dataService.getActiveMonitorings(accountId)
  }

  def bookAppointmentByScheduleId(accountId: Long, monitoringId: Long, scheduleId: Long, time: Long): Unit = {
    val monitoringMaybe = dataService.findMonitoring(accountId, monitoringId)
    monitoringMaybe match {
      case Some(monitoring) =>
        val termsEither = apiService.getAvailableTerms(monitoring.accountId, monitoring.cityId, monitoring.clinicId, monitoring.serviceId,
          monitoring.doctorId, monitoring.dateFrom, Some(monitoring.dateTo), timeFrom = monitoring.timeFrom, timeTo = monitoring.timeTo)
        termsEither match {
          case Right(terms) =>
            val termMaybe = terms.find(term => term.scheduleId == scheduleId && minutesSinceBeginOf2018(term.visitDate.startDateTime) == time)
            termMaybe match {
              case Some(term) =>
                bookAppointment(term, monitoring)
              case None =>
                bot.sendMessage(monitoring.source, lang(monitoring.userId).termIsOutdated)
            }
          case Left(ex: InvalidLoginOrPasswordException) =>
            error(s"User entered invalid name or password. Monitoring will be disabled", ex)
            bot.sendMessage(monitoring.source, lang(monitoring.userId).loginHasChangedOrWrong)
          case Left(ex) => error(s"Error occurred during receiving terms for monitoring [#${monitoring.recordId}]", ex)
        }
      case None =>
        debug(s"Monitoring [#$monitoringId] not found in db")
    }
  }

  implicit class MonitoringAsSource(monitoring: Monitoring) {
    def source: MessageSource = MessageSource(
      MessageSourceSystem(monitoring.sourceSystemId), monitoring.chatId
    )
  }

  private def lang(userId: Long) = localization.lang(userId)

  @PostConstruct
  private def initialize(): Unit = {
    checkedOn = ZonedDateTime.now()
    val monitorings = dataService.getActiveMonitorings
    debug(s"Active monitorings found: ${monitorings.length}")
    initializeMonitorings(monitorings)
    disableOutdated()
    initializeDbChecker()
  }
}
