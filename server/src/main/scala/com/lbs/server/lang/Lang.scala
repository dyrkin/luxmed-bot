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
package com.lbs.server.lang

import java.time.ZonedDateTime
import java.util.Locale

import com.lbs.api.json.model.{AvailableVisitsTermPresentation, HistoricVisit, ReservedVisit, ValuationsResponse}
import com.lbs.server.actor.Book.BookingData
import com.lbs.server.actor.StaticData.StaticDataConfig
import com.lbs.server.repository.model
import com.lbs.server.repository.model.Monitoring

object Lang {

  val Langs: Seq[Lang] = Seq(En, Ua)

  private val LangsMap = Seq(En, Ua).map(e => e.id -> e).toMap

  def apply(id: Int): Lang = {
    LangsMap.getOrElse(id, sys.error(s"Unknown language id $id"))
  }
}

trait Lang {
  def id: Int

  def locale: Locale

  def label: String

  val timeOfDay = Map(
    0 -> allDay,
    1 -> beforeNine,
    2 -> nineToFive,
    3 -> afterFive
  )

  protected def capitalizeFirstLetter(str: String): String = {
    val fistCapitalLetter = str.head.toTitleCase
    fistCapitalLetter + str.tail
  }

  protected def withPages(message: String, page: Int, pages: Int): String

  def unableToCancelUpcomingVisit(reason: String): String

  def appointmentHasBeenCancelled: String

  def yes: String

  def no: String

  def noUpcomingVisits: String

  def areYouSureToCancelAppointment(visit: ReservedVisit): String

  def chooseDateFrom: String

  def chooseDateTo: String

  def findTerms: String

  def modifyDate: String

  def moreParameters: String

  def bookingSummary(bookingData: BookingData): String

  def noTermsFound: String

  def createMonitoring: String

  def cancel: String

  def book: String

  def confirmAppointment(term: AvailableVisitsTermPresentation, valuations: ValuationsResponse): String

  def appointmentIsConfirmed: String

  def monitoringHasBeenCreated: String

  def unableToCreateMonitoring: String

  def chooseTypeOfMonitoring: String

  def bookByApplication: String

  def bookManually: String

  def city: String

  def clinic: String

  def service: String

  def doctor: String

  def previous: String

  def next: String

  def noActiveMonitorings: String

  def deactivateMonitoring(monitoring: Monitoring): String

  def deactivated: String

  def any: String

  def pressAny: String

  protected def withAnyVariant(message: String, isAnyAllowed: Boolean): String = {
    if (isAnyAllowed)
      message + "\n\n" + pressAny
    else message
  }

  def pleaseEnterStaticDataNameOrAny(config: StaticDataConfig): String

  def pleaseEnterStaticDataNameOrPrevious(config: StaticDataConfig): String

  def staticDataIs(config: StaticDataConfig, label: String): String

  def pleaseChooseStaticDataNameOrAny(config: StaticDataConfig): String

  def staticNotFound(config: StaticDataConfig): String

  def loginAndPasswordAreOk: String

  def provideUsername: String

  def providePassword: String

  def visitsHistoryIsEmpty: String

  def help: String

  def dateFromIs(dateFrom: ZonedDateTime): String

  def dateToIs(dateTo: ZonedDateTime): String

  def termEntry(term: AvailableVisitsTermPresentation, page: Int, index: Int): String

  def termsHeader(page: Int, pages: Int): String

  def historyEntry(visit: HistoricVisit, page: Int, index: Int): String

  def historyHeader(page: Int, pages: Int): String

  def upcomingVisitEntry(visit: ReservedVisit, page: Int, index: Int): String

  def upcomingVisitsHeader(page: Int, pages: Int): String

  def bugEntry(bug: model.Bug, page: Int, index: Int): String

  def bugsHeader(page: Int, pages: Int): String

  def monitoringEntry(monitoring: Monitoring, page: Int, index: Int): String

  def monitoringsHeader(page: Int, pages: Int): String

  def invalidLoginOrPassword: String

  def availableTermEntry(term: AvailableVisitsTermPresentation, monitoring: Monitoring, index: Int): String

  def availableTermsHeader(size: Int): String

  def nothingWasFoundByMonitoring(monitoring: Monitoring): String

  def appointmentIsBooked(term: AvailableVisitsTermPresentation, monitoring: Monitoring): String

  def maximumMonitoringsLimitExceeded: String

  def monitoringOfTheSameTypeExists: String

  def termIsOutdated: String

  def loginHasChangedOrWrong: String

  def settingsHeader: String

  def language: String

  def chooseLanguage: String

  def languageUpdated: String

  def appointmentWasNotCancelled: String

  def monitoringWasNotDeactivated: String

  def createNewBug: String

  def showSubmittedBugs: String

  def bugAction: String

  def bugHasBeenCreated(bugId: Long): String

  def noSubmittedIssuesFound: String

  def enterIssueDetails: String

  def chooseTimeOfDay: String

  def afterFive: String

  def nineToFive: String

  def beforeNine: String

  def allDay: String

  def preferredTimeIs(time: Int): String

  def deleteAccount: String

  def addAccount: String

  def pleaseChooseAccount(currentAccountName: String): String

  def accountSwitched(username: String): String
}
