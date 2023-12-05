package com.lbs.server.lang

import com.lbs.api.json.model.{Event, TermExt}
import com.lbs.server.conversation.Book.BookingData
import com.lbs.server.conversation.StaticData.StaticDataConfig
import com.lbs.server.repository.model.Monitoring

import java.time.{LocalDateTime, LocalTime}
import java.util.Locale
import scala.io.Source
import scala.util.Try

object Lang {

  val Langs: Seq[Lang] = Seq(En, Ua, Pl)

  private val LangsMap = Langs.map(e => e.id -> e).toMap

  def apply(id: Int): Lang = {
    LangsMap.getOrElse(id, sys.error(s"Unknown language id $id"))
  }

  val version: String = Try(Source.fromFile("version").getLines().mkString).getOrElse("Unknown")
}

trait Lang {

  def id: Int

  def locale: Locale

  def label: String

  protected def capitalize(str: String): String = {
    if (str != null && str != "") {
      val fistCapitalLetter = str.head.toTitleCase
      s"$fistCapitalLetter${str.tail.toLowerCase}"
    } else ""
  }

  protected def withPages(message: String, page: Int, pages: Int): String

  def unableToCancelUpcomingVisit(reason: String): String

  def appointmentHasBeenCancelled: String

  def yes: String

  def no: String

  def noUpcomingVisits: String

  def areYouSureToCancelAppointment(visit: Event): String

  def chooseDateFrom(exampleDate: LocalDateTime): String

  def chooseDateTo(exampleDate: LocalDateTime): String

  def chooseTimeFrom(exampleTime: LocalTime): String

  def chooseTimeTo(exampleTime: LocalTime): String

  def findTerms: String

  def modifyDate: String

  def moreParameters: String

  def bookingSummary(bookingData: BookingData): String

  def noTermsFound: String

  def createMonitoring: String

  def cancel: String

  def book: String

  def confirmAppointment(term: TermExt): String

  def appointmentIsConfirmed: String

  def monitoringHasBeenCreated: String

  def unableToCreateMonitoring(reason: String): String

  def chooseTypeOfMonitoring: String

  def bookByApplication: String

  def bookManually: String

  def rebookIfExists: String

  def pleaseSpecifyOffset: String

  def visitAlreadyExists: String

  def canNotDetectPayer(error: String): String

  def pleaseChoosePayer: String

  def city: String

  def visitLanguage: String

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

  def eventsListIsEmpty: String

  def help: String

  def dateFromIs(dateFrom: LocalDateTime): String

  def dateToIs(dateTo: LocalDateTime): String

  def timeFromIs(timeFrom: LocalTime): String

  def timeToIs(timeTo: LocalTime): String

  def termEntry(term: TermExt, page: Int, index: Int): String

  def termsHeader(page: Int, pages: Int): String

  def historyEntry(event: Event, page: Int, index: Int): String

  def historyHeader(page: Int, pages: Int): String

  def reservedVisitEntry(visit: Event, page: Int, index: Int): String

  def reservedVisitsHeader(page: Int, pages: Int): String

  def bugsHeader(page: Int, pages: Int): String

  def monitoringEntry(monitoring: Monitoring, page: Int, index: Int): String

  def monitoringHistoryEntry(monitoring: Monitoring, page: Int, index: Int): String

  def monitoringsHeader(page: Int, pages: Int): String

  def monitoringsHistoryHeader(page: Int, pages: Int): String

  def invalidLoginOrPassword: String

  def availableTermEntry(term: TermExt, monitoring: Monitoring, index: Int): String

  def availableTermsHeader(size: Int): String

  def nothingWasFoundByMonitoring(monitoring: Monitoring): String

  def appointmentIsBooked(term: TermExt, monitoring: Monitoring): String

  def maximumMonitoringsLimitExceeded: String

  def termIsOutdated: String

  def loginHasChangedOrWrong: String

  def settingsHeader: String

  def language: String

  def offset: String

  def chooseLanguage: String

  def configureOffset: String

  def pleaseEnterOffset(current: Int): String

  def alwaysAskOffset(enabled: Boolean): String

  def changeDefaultOffset(current: Int): String

  def languageUpdated: String

  def appointmentWasNotCancelled: String

  def monitoringWasNotDeactivated: String

  def createNewBug: String

  def showSubmittedBugs: String

  def bugAction: String

  def bugHasBeenCreated(bugId: Long): String

  def noSubmittedIssuesFound: String

  def enterIssueDetails: String

  def deleteAccount: String

  def addAccount: String

  def pleaseChooseAccount(currentAccountName: String): String

  def accountSwitched(username: String): String
}
