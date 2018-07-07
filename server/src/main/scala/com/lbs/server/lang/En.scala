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
import com.lbs.server.actor.Book
import com.lbs.server.actor.StaticData.StaticDataConfig
import com.lbs.server.repository.model.{Bug, Monitoring}
import com.lbs.server.util.DateTimeUtil.{formatDate, formatDateTime, minutesSinceBeginOf2018}

object En extends Lang {

  override def id: Int = 0

  override def locale: Locale = Locale.ENGLISH

  override def label: String = "🇺🇸󠁧󠁢󠁥󠁮󠁧󠁿English"

  override protected def withPages(message: String, page: Int, pages: Int): String = {
    if (pages > 1) s"$message. Page <b>${page + 1}</b> of <b>$pages</b>"
    else message
  }

  override def unableToCancelUpcomingVisit(reason: String): String =
    s"⚠ Unable to cancel upcoming visit! Reason: $reason"

  override def appointmentHasBeenCancelled: String =
    s"👍 Your appointment has been cancelled!"

  override def yes: String = "Yes"

  override def no: String = "No"

  override def noUpcomingVisits: String =
    "ℹ No upcoming visits found"

  override def areYouSureToCancelAppointment(visit: ReservedVisit): String =
    s"""<b>➡</b> Are you sure want to cancel appointment?
       |
       |⏱ <b>${formatDateTime(visit.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${visit.doctorName}
       |${capitalizeFirstLetter(service)}: ${visit.service.name}
       |${capitalizeFirstLetter(clinic)}: ${visit.clinic.name}
       |""".stripMargin

  override def chooseDateFrom: String = "<b>➡</b> Please choose date from"

  override def chooseDateTo: String = "<b>➡</b> Please choose date to"

  override def findTerms: String = "🔍 Find terms"

  override def modifyDate: String = "📅 Modify date"

  override def bookingSummary(bookingData: Book.BookingData): String =
    s"🦄 Ok! We are going to book a service <b>${bookingData.serviceId.name}</b>" +
      s" with a doctor chosen <b>${bookingData.doctorId.name}</b>" +
      s" in <b>${bookingData.clinicId.name}</b> clinic" +
      s" of the <b>${bookingData.cityId.name}</b> city." +
      s"\nDesired dates: <b>${formatDate(bookingData.dateFrom, locale)}</b> -> <b>${formatDate(bookingData.dateTo, locale)}</b>" +
      s"\nTime: <b>${timeOfDay(bookingData.timeOfDay)}</b>" +
      s"\n\n<b>➡</b> Now choose your action"

  override def noTermsFound: String =
    s"""ℹ No available terms found
       |
       |What do you want to do next?""".stripMargin

  override def createMonitoring: String = "👀 Create monitoring"

  override def cancel: String = "Cancel"

  override def book: String = "Book"

  override def confirmAppointment(term: AvailableVisitsTermPresentation, valuations: ValuationsResponse): String =

    s"""<b>➡</b> ${valuations.optionsQuestion.getOrElse("Would you like to confirm your appointment booking?")}
       |
       |⏱ <b>${formatDateTime(term.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${term.doctor.name}
       |${capitalizeFirstLetter(clinic)}: ${term.clinic.name}
       |
       |ℹ${valuations.visitTermVariants.head.infoMessage}""".stripMargin

  override def appointmentIsConfirmed: String = "👍 Your appointment has been confirmed!"

  override def monitoringHasBeenCreated: String = "👍 Monitoring has been created! List of active /monitorings"

  override def unableToCreateMonitoring: String = s"👎 Unable to create monitoring. Please create a /bug"

  override def chooseTypeOfMonitoring: String = "<b>➡</b> Please choose type of monitoring you want"

  override def bookByApplication: String = "👾 Book by application"

  override def bookManually: String = "👤 Book manually"

  override def city: String = "city"

  override def clinic: String = "clinic"

  override def service: String = "service"

  override def doctor: String = "doctor"

  override def previous: String = "Previous"

  override def next: String = "Next"

  override def noActiveMonitorings: String = "ℹ You don't have active monitorings. Create new one /book"

  override def deactivateMonitoring(monitoring: Monitoring): String =
    s"""<b>➡</b> Are you sure want to deactivate monitoring?
       |
       |📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${timeOfDay(monitoring.timeOfDay)}</b>
       |${capitalizeFirstLetter(doctor)}: ${monitoring.doctorName}
       |${capitalizeFirstLetter(service)}: ${monitoring.serviceName}
       |${capitalizeFirstLetter(clinic)}: ${monitoring.clinicName}""".stripMargin

  override def deactivated: String = "👍 Deactivated! List of active /monitorings"

  override def any: String = "Any"

  override def pressAny: String = s"or press <b>$any</b> button"

  override def pleaseEnterStaticDataNameOrAny(config: StaticDataConfig): String =
    withAnyVariant(
      s"""<b>➡</b> Please enter a ${config.name} name
         |For example: <b>${config.example}</b>""".stripMargin,
      config.isAnyAllowed)

  override def pleaseEnterStaticDataNameOrPrevious(config: StaticDataConfig): String =
    s"""<b>➡</b> Please enter a ${config.name} name
       |For example: <b>${config.example}</b>
       |
       |or choose a ${config.name} from previous searches""".stripMargin

  override def staticDataIs(config: StaticDataConfig, label: String): String =
    s"<b>✅</b> ${capitalizeFirstLetter(config.name)} is <b>$label</b>"

  override def pleaseChooseStaticDataNameOrAny(config: StaticDataConfig): String =
    withAnyVariant(s"<b>➡</b> Please choose a ${config.name}", config.isAnyAllowed)

  override def staticNotFound(config: StaticDataConfig): String =
    withAnyVariant(
      s"""<b>➡</b> Nothing was found 😔
         |Please enter a ${config.name} name again""", config.isAnyAllowed)

  override def loginAndPasswordAreOk: String =
    s"""✅ Congrats! Login and password are OK!
       |Now you can change the language /settings
     """.stripMargin

  override def provideUsername: String =
    s"""ℹ You must be logged in using your <b>Luxmed</b> credentials
       |
       |<b>➡</b> Please provide username""".stripMargin

  override def providePassword: String = "<b>➡</b> Please provide password"

  override def visitsHistoryIsEmpty: String = "ℹ No visits in your history"

  override def help: String =
    s"""ℹ This is non official bot for <b>Portal Pacjenta LUX MED</b>.
       |With its help you can book a visit to the doctor, create term monitorings, view upcoming visits and visit history.
       |
       |Ping @dyrkin_me if you have any questions.
       |
       |<b>➡</b> Supported commands
       |/book - reserve a visit or create monitoring
       |/monitorings - available terms monitoring
       |/reserved - upcoming visits
       |/history - visits history
       |/accounts - manage Luxmed accounts
       |/settings - settings, e.g. lang
       |/bug - submit an issue""".stripMargin

  override def dateFromIs(dateFrom: ZonedDateTime): String = s"📅 Date from is ${formatDate(dateFrom, locale)}"

  override def dateToIs(dateTo: ZonedDateTime): String = s"📅 Date to is ${formatDate(dateTo, locale)}"

  override def termEntry(term: AvailableVisitsTermPresentation, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(term.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${term.doctor.name}
       |${capitalizeFirstLetter(clinic)}: ${term.clinic.name}
       |<b>➡</b> /book_${page}_$index
       |
       |""".stripMargin

  override def termsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Available terms", page, pages)

  override def historyEntry(visit: HistoricVisit, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(visit.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${visit.doctorName}
       |${capitalizeFirstLetter(service)}: ${visit.service.name}
       |${capitalizeFirstLetter(clinic)}: ${visit.clinicName}
       |<b>➡</b> /repeat_${page}_$index
       |
       |""".stripMargin

  override def historyHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Conducted visits", page, pages)

  override def upcomingVisitEntry(visit: ReservedVisit, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(visit.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${visit.doctorName}
       |${capitalizeFirstLetter(service)}: ${visit.service.name}
       |${capitalizeFirstLetter(clinic)}: ${visit.clinic.name}
       |<b>➡</b> /cancel_${page}_$index
       |
       |""".stripMargin

  override def upcomingVisitsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Reserved visits", page, pages)

  override def bugEntry(bug: Bug, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(bug.submitted, locale)}</b>
       |Description: ${bug.details}
       |State: <b>${if (bug.resolved) "✅ Resolved" else "🚫 Unresolved"}</b>
       |
       |""".stripMargin

  override def bugsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Submitted issues", page, pages)

  override def monitoringEntry(monitoring: Monitoring, page: Int, index: Int): String =
    s"""📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${timeOfDay(monitoring.timeOfDay)}</b>
       |${capitalizeFirstLetter(doctor)}: ${monitoring.doctorName}
       |${capitalizeFirstLetter(service)}: ${monitoring.serviceName}
       |${capitalizeFirstLetter(clinic)}: ${monitoring.clinicName}
       |${capitalizeFirstLetter(city)}: ${monitoring.cityName}
       |Type: ${if (monitoring.autobook) "Auto" else "Manual"}
       |<b>➡</b> /cancel_${page}_$index
       |
       |""".stripMargin

  override def monitoringsHeader(page: Int, pages: Int): String =
    s"<b>➡</b> Active monitorings."

  override def invalidLoginOrPassword: String =
    """❗ You have entered invalid login or password or changed it via site.
      |Your monitorings were removed. Please /login again and create new monitorings.
    """.stripMargin

  override def availableTermEntry(term: AvailableVisitsTermPresentation, monitoring: Monitoring, index: Int): String =
    s"""⏱ <b>${formatDateTime(term.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${term.doctor.name}
       |${capitalizeFirstLetter(service)}: ${monitoring.serviceName}
       |${capitalizeFirstLetter(clinic)}: ${term.clinic.name}
       |${capitalizeFirstLetter(city)}: ${monitoring.cityName}
       |/reserve_${monitoring.recordId}_${term.scheduleId}_${minutesSinceBeginOf2018(term.visitDate.startDateTime)}
       |
       |""".stripMargin

  override def availableTermsHeader(size: Int): String =
    s"""✅ <b>$size</b> terms were found by monitoring. We showed you the closest 5.
       |
       |<b>➡</b> Please choose one to reserve""".stripMargin

  override def nothingWasFoundByMonitoring(monitoring: Monitoring): String =
    s"""❗ Nothing was found by your monitoring. Monitoring has been <b>disabled</b> as outdated.
       |
       |📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${timeOfDay(monitoring.timeOfDay)}</b>
       |${capitalizeFirstLetter(doctor)}: ${monitoring.doctorName}
       |${capitalizeFirstLetter(service)}: ${monitoring.serviceName}
       |${capitalizeFirstLetter(clinic)}: ${monitoring.clinicName}
       |${capitalizeFirstLetter(city)}: ${monitoring.cityName}
       |
       |<b>➡</b> Create new monitoring /book""".stripMargin

  override def appointmentIsBooked(term: AvailableVisitsTermPresentation, monitoring: Monitoring): String =
    s"""👍 We just booked appointment for you!
       |
       |⏱ <b>${formatDateTime(term.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${term.doctor.name}
       |${capitalizeFirstLetter(service)}: ${monitoring.serviceName}
       |${capitalizeFirstLetter(clinic)}: ${term.clinic.name}
       |${capitalizeFirstLetter(city)}: ${monitoring.cityName}""".stripMargin

  override def maximumMonitoringsLimitExceeded: String = "Maximum monitorings per user is 5"

  override def monitoringOfTheSameTypeExists: String = "You already have active monitoring for the same service /monitorings"

  override def termIsOutdated: String =
    s"""❗️ Looks like the term is already booked by someone else
       |
       |Please try another one or create a new monitoring /book""".stripMargin

  override def loginHasChangedOrWrong: String =
    """❗ You have entered invalid <b>login</b> or <b>password</b> or changed it via site.
      |
      |Please /login again and create a new monitoring /book.
    """.stripMargin

  override def settingsHeader: String = "<b>➡</b> Please choose an action"

  override def language: String = "🌐 Change language"

  override def chooseLanguage: String = "<b>➡</b> Please choose a language"

  override def languageUpdated: String = "👍 Language was successfully changed!"

  override def appointmentWasNotCancelled: String = "👍 Appointment was not cancelled"

  override def monitoringWasNotDeactivated: String = "👍 Monitoring was not deactivated"

  override def bugAction: String = "<b>➡</b> Please choose an action"

  override def createNewBug: String = "🐞 Submit new"

  override def showSubmittedBugs: String = "👀 Show submitted"

  override def enterIssueDetails: String = "<b>➡</b> Please provide issue details"

  override def noSubmittedIssuesFound: String = "ℹ No submitted issues found"

  override def bugHasBeenCreated(bugId: Long): String = s"✅ Thank you for submitting bug <b>#$bugId</b>!"

  override def chooseTimeOfDay: String = "<b>➡</b> Please choose preferred time of day"

  override def afterFive: String = "After 17:00"

  override def nineToFive: String = "From 09:00 to 17:00"

  override def beforeNine: String = "Before 09:00"

  override def allDay: String = "All day"

  override def preferredTimeIs(time: Int): String = s"⏱ Preferred time is ${timeOfDay(time)}"

  override def deleteAccount: String = "➖ Delete account"

  override def addAccount: String = "➕ Add account"

  override def accountSwitched(username: String): String =
    s"✅ Account has been switched to <b>$username</b>"

  override def pleaseChooseAccount(currentAccountName: String): String =
    s"""Current account is <b>$currentAccountName</b>
       |
       |<b>➡</b> Please choose an <b>action</b> or select <b>account</b>""".stripMargin

  override def moreParameters: String = "🛠 More parameters"
}
