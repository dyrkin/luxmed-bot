package com.lbs.server.lang

import com.lbs.api.json.model.*
import com.lbs.server.conversation.Book
import com.lbs.server.conversation.RehabBook.RehabBookingData
import com.lbs.server.conversation.StaticData.StaticDataConfig
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.util.DateTimeUtil.*

import java.time.{LocalDateTime, LocalTime}
import java.util.Locale

object En extends Lang {

  override def id: Int = 0

  override def locale: Locale = Locale.ENGLISH

  override def label: String = "🇺🇸󠁧󠁢󠁥󠁮󠁧󠁿English"

  override protected def withPages(message: String, page: Int, pages: Int): String = {
    if (pages > 1) s"$message. Page <b>${page + 1}</b> of <b>$pages</b>"
    else message
  }

  override def unableToCancelUpcomingVisit(reason: String): String =
    s"⚠ Unable to cancel the upcoming visit! Reason: $reason"

  override def appointmentHasBeenCancelled: String =
    s"👍 Your appointment has been canceled!"

  override def yes: String = "Yes"

  override def no: String = "No"

  override def noUpcomingVisits: String =
    "ℹ No upcoming visits found"

  override def areYouSureToCancelAppointment(event: Event): String =
    s"""<b>➡</b> Are you sure you want to cancel the appointment?
       |
       |⏱ <b>${formatDateTime(event.date, locale)}</b>
       |${capitalize(doctor)}: ${event.doctor
        .map(d => s"${capitalize(d.name)} ${capitalize(d.lastname)}")
        .getOrElse("No specified")}
       |${capitalize(service)}: ${event.title}
       |${capitalize(clinic)}: ${event.clinic
        .map(c => s"${capitalize(c.city)} - ${capitalize(c.address)}")
        .getOrElse("Telemedicine")}
       |""".stripMargin

  override def chooseDateFrom(exampleDate: LocalDateTime): String =
    s"<b>➡</b> Please choose date from or write it manually using format dd-MM, e.g. ${formatDateShort(exampleDate)}"

  override def chooseDateTo(exampleDate: LocalDateTime): String =
    s"<b>➡</b> Please choose a date to or write it manually using format dd-MM, e.g. ${formatDateShort(exampleDate)}"

  override def findTerms: String = "🔍 Find terms"

  override def modifyDate: String = "📅 Modify date"

  override def bookingSummary(bookingData: Book.BookingData): String =
    s"🦄 Ok! We are going to book the service <b>${bookingData.serviceId.name}</b>" +
      s" with the doctor chosen <b>${bookingData.doctorId.name}</b>" +
      s" in <b>${bookingData.clinicId.name}</b> clinic" +
      s" of <b>${bookingData.cityId.name}</b> city." +
      s"\nDesired dates: <b>${formatDate(bookingData.dateFrom, locale)}</b> -> <b>${formatDate(bookingData.dateTo, locale)}</b>" +
      s"\nTime: <b>${formatTime(bookingData.timeFrom)} -> ${formatTime(bookingData.timeTo)}</b>" +
      s"\n\n<b>➡</b> Now choose an action"

  override def noTermsFound: String =
    s"""ℹ No available terms has been found
       |
       |What do you want to do next?""".stripMargin

  override def createMonitoring: String = "👀 Create monitoring"

  override def cancel: String = "Cancel"

  override def book: String = "Book"

  override def confirmAppointment(term: TermExt): String =
    s"""<b>➡</b> Would you like to confirm your appointment?
       |
       |⏱ <b>${formatDateTime(term.term.dateTimeFrom, locale)}</b>
       |${capitalize(doctor)}: ${term.term.doctor.firstName} ${term.term.doctor.lastName}
       |${capitalize(clinic)}: ${term.term.clinic}""".stripMargin

  override def appointmentIsConfirmed: String = "👍 Your appointment has been confirmed!"

  override def monitoringHasBeenCreated: String = "👍 Monitoring has been created! List of active /monitorings"

  override def unableToCreateMonitoring(reason: String): String = s"👎 Unable to create monitoring. Reason: $reason."

  override def chooseTypeOfMonitoring: String = "<b>➡</b> Please choose the type of monitoring you want"

  override def bookByApplication: String = "👾 Book by the application"

  override def bookManually: String = "👤 Book manually"

  override def rebookIfExists: String = "<b>➡</b> Do you want to update a term if a reservation already exists?"

  override def pleaseSpecifyOffset: String = "<b>➡</b> Please send me an offset in hours or press the No button"

  override def visitAlreadyExists: String =
    "<b>➡</b> The same service is already booked. Do you want to update the term?"

  override def city: String = "city"

  override def clinic: String = "clinic"

  override def service: String = "service"

  override def doctor: String = "doctor"

  override def previous: String = "Previous"

  override def next: String = "Next"

  override def noActiveMonitorings: String = "ℹ You don't have active monitorings. Create a new one /book"

  override def deactivateMonitoring(monitoring: Monitoring): String =
    s"""<b>➡</b> Are you sure you want to deactivate the monitoring?
       |
       |📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${formatTime(monitoring.timeFrom)}</b> -> <b>${formatTime(monitoring.timeTo)}</b>
       |${capitalize(doctor)}: ${monitoring.doctorName}
       |${capitalize(service)}: ${monitoring.serviceName}
       |${capitalize(clinic)}: ${monitoring.clinicName}""".stripMargin

  override def deactivated: String = "👍 Deactivated! List of active /monitorings"

  override def any: String = "Any"

  override def pressAny: String = s"or press <b>$any</b> button"

  override def pleaseEnterStaticDataNameOrAny(config: StaticDataConfig): String =
    withAnyVariant(
      s"""<b>➡</b> Please enter a partial ${config.name} name
         |For example: <b>${config.partialExample}</b> if you are looking for <b>${config.example}</b>""".stripMargin,
      config.isAnyAllowed
    )

  override def pleaseEnterStaticDataNameOrPrevious(config: StaticDataConfig): String =
    s"""<b>➡</b> Please enter a partial ${config.name} name
       |For example: <b>${config.partialExample}</b> if you are looking for <b>${config.example}</b>
       |
       |or choose a ${config.name} from previous searches""".stripMargin

  override def staticDataIs(config: StaticDataConfig, label: String): String =
    s"<b>✅</b> ${capitalize(config.name)} is <b>$label</b>"

  override def pleaseChooseStaticDataNameOrAny(config: StaticDataConfig): String =
    withAnyVariant(s"<b>➡</b> Please choose a ${config.name}", config.isAnyAllowed)

  override def staticNotFound(config: StaticDataConfig): String =
    withAnyVariant(
      s"""<b>➡</b> Nothing has been found 😔
         |Please enter a ${config.name} name again""",
      config.isAnyAllowed
    )

  override def loginAndPasswordAreOk: String =
    s"""✅ Congrats! Login and password are OK!
       |Now you can change the language /settings
     """.stripMargin

  override def provideUsername: String =
    s"""ℹ You must be logged in using your <b>Luxmed</b> credentials
       |
       |<b>➡</b> Please provide username""".stripMargin

  override def providePassword: String = "<b>➡</b> Please provide password"

  override def eventsListIsEmpty: String = "ℹ No visits in your history"

  override def help: String =
    s"""ℹ Non official bot for <b>Portal Pacjenta LUX MED (v.${Lang.version})</b>.
       |It can help you to book a visit to a doctor, create term monitoring, view upcoming appointments and visit history.
       |
       |<b>➡</b> Supported commands
       |/book - reserve a visit, or create a monitoring
       |/rehab - book rehabilitation visit
       |/monitorings - available terms monitoring
       |/monitorings_history - previous monitoring
       |/reserved - upcoming visits
       |/history - visits history
       |/accounts - manage Luxmed accounts
       |/login - login again
       |/settings - settings, e.g. lang
       |/help - the help""".stripMargin

  override def dateFromIs(dateFrom: LocalDateTime): String = s"📅 Date from is ${formatDate(dateFrom, locale)}"

  override def dateToIs(dateTo: LocalDateTime): String = s"📅 Date to is ${formatDate(dateTo, locale)}"

  override def termEntry(term: TermExt, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(term.term.dateTimeFrom, locale)}</b>
       |${capitalize(doctor)}: ${term.term.doctor.firstName} ${term.term.doctor.lastName}
       |${capitalize(clinic)}: ${term.term.clinic}
       |<b>➡</b> /book_$index
       |
       |""".stripMargin

  override def termsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Available terms", page, pages)

  override def historyEntry(event: Event, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(event.date, locale)}</b>
       |${capitalize(doctor)}: ${event.doctor
        .map(d => s"${capitalize(d.name)} ${capitalize(d.lastname)}")
        .getOrElse("No specified")}
       |${capitalize(service)}: ${event.title}
       |${capitalize(clinic)}: ${event.clinic
        .map(c => s"${capitalize(c.city)} - ${capitalize(c.address)}")
        .getOrElse("Telemedicine")}
       |
       |""".stripMargin

  override def historyHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Conducted visits", page, pages)

  override def reservedVisitEntry(event: Event, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(event.date, locale)}</b>
       |${capitalize(doctor)}: ${event.doctor
        .map(d => s"${capitalize(d.name)} ${capitalize(d.lastname)}")
        .getOrElse("No specified")}
       |${capitalize(service)}: ${event.title}
       |${capitalize(clinic)}: ${event.clinic
        .map(c => s"${capitalize(c.city)} - ${capitalize(c.address)}")
        .getOrElse("Telemedicine")}
       |<b>➡</b> /cancel_$index
       |
       |""".stripMargin

  override def reservedVisitsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Reserved visits", page, pages)

  override def bugsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Submitted issues", page, pages)

  override def monitoringEntry(monitoring: Monitoring, page: Int, index: Int): String =
    s"""📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${formatTime(monitoring.timeFrom)}</b> -> <b>${formatTime(monitoring.timeTo)}</b>
       |${capitalize(doctor)}: ${monitoring.doctorName}
       |${capitalize(service)}: ${monitoring.serviceName}
       |${capitalize(clinic)}: ${monitoring.clinicName}
       |${capitalize(city)}: ${monitoring.cityName}
       |Type: ${if (monitoring.autobook) "Auto" else "Manual"}
       |Rebook existing reservation: ${if (monitoring.rebookIfExists) "Yes" else "No"}
       |<b>➡</b> /cancel_$index
       |
       |""".stripMargin

  override def monitoringHistoryEntry(monitoring: Monitoring, page: Int, index: Int): String =
    s"""📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${formatTime(monitoring.timeFrom)}</b> -> <b>${formatTime(monitoring.timeTo)}</b>
       |${capitalize(doctor)}: ${monitoring.doctorName}
       |${capitalize(service)}: ${monitoring.serviceName}
       |${capitalize(clinic)}: ${monitoring.clinicName}
       |${capitalize(city)}: ${monitoring.cityName}
       |Type: ${if (monitoring.autobook) "Auto" else "Manual"}
       |<b>➡</b> /repeat_$index
       |
       |""".stripMargin

  override def monitoringsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Active monitorings", page, pages)

  override def monitoringsHistoryHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Monitorings history", page, pages)

  override def invalidLoginOrPassword: String =
    """❗ You have entered invalid login or password or changed it via the site.
      |Your monitorings were removed. Please /login again and create new monitorings.
    """.stripMargin

  override def availableTermEntry(term: TermExt, monitoring: Monitoring, index: Int): String =
    s"""⏱ <b>${formatDateTime(term.term.dateTimeFrom, locale)}</b>
       |${capitalize(doctor)}: ${term.term.doctor.firstName} ${term.term.doctor.lastName}
       |${capitalize(service)}: ${monitoring.serviceName}
       |${capitalize(clinic)}: ${term.term.clinic}
       |${capitalize(city)}: ${monitoring.cityName}
       |/reserve_${monitoring.recordId}_${term.term.scheduleId}_${minutesSinceBeginOf2018(term.term.dateTimeFrom.get)}
       |
       |""".stripMargin

  override def availableTermsHeader(size: Int): String =
    s"""✅ <b>$size</b> terms have been found. We've shown you the closest 5.
       |
       |<b>➡</b> Please choose one to reserve""".stripMargin

  override def nothingWasFoundByMonitoring(monitoring: Monitoring): String =
    s"""❗ Nothing has been found. Monitoring has been <b>disabled</b> as outdated.
       |
       |📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${formatTime(monitoring.timeFrom)}</b> -> <b>${formatTime(monitoring.timeTo)}</b>
       |${capitalize(doctor)}: ${monitoring.doctorName}
       |${capitalize(service)}: ${monitoring.serviceName}
       |${capitalize(clinic)}: ${monitoring.clinicName}
       |${capitalize(city)}: ${monitoring.cityName}
       |
       |<b>➡</b> Create a new monitoring /book""".stripMargin

  override def appointmentIsBooked(term: TermExt, monitoring: Monitoring): String =
    s"""👍 We've booked the appointment for ${monitoring.username}!
       |
       |⏱ <b>${formatDateTime(term.term.dateTimeFrom, locale)}</b>
       |${capitalize(doctor)}: ${term.term.doctor.firstName} ${term.term.doctor.lastName}
       |${capitalize(service)}: ${monitoring.serviceName}
       |${capitalize(clinic)}: ${term.term.clinic}
       |${capitalize(city)}: ${monitoring.cityName}""".stripMargin

  override def maximumMonitoringsLimitExceeded: String = "Maximum monitorings per user is 10"

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

  override def language: String = "🌐 Language"

  override def offset: String = "⏱ Offset"

  override def chooseLanguage: String = "<b>➡</b> Please choose a language"

  override def configureOffset: String = "<b>➡</b> Please specify offset options"

  override def pleaseEnterOffset(current: Int): String =
    s"<b>➡</b> Please enter default offset. Current: <b>$current</b>"

  override def alwaysAskOffset(enabled: Boolean): String = s"${if (enabled) "✅ " else ""}Always ask offset"

  override def changeDefaultOffset(current: Int): String = s"Change default offset ($current)"

  override def languageUpdated: String = "👍 Language was successfully changed!"

  override def appointmentWasNotCancelled: String = "👍 Appointment was not cancelled"

  override def monitoringWasNotDeactivated: String = "👍 Monitoring was not deactivated"

  override def bugAction: String = "<b>➡</b> Please choose an action"

  override def createNewBug: String = "🐞 Submit new"

  override def showSubmittedBugs: String = "👀 Show submitted"

  override def enterIssueDetails: String = "<b>➡</b> Please provide issue details"

  override def noSubmittedIssuesFound: String = "ℹ No submitted issues found"

  override def bugHasBeenCreated(bugId: Long): String = s"✅ Thank you for submitting bug <b>#$bugId</b>!"

  override def deleteAccount: String = "➖ Delete account"

  override def addAccount: String = "➕ Add account"

  override def accountSwitched(username: String): String =
    s"✅ Account has been switched to <b>$username</b>"

  override def pleaseChooseAccount(currentAccountName: String): String =
    s"""Current account is <b>$currentAccountName</b>
       |
       |<b>➡</b> Please choose an <b>action</b> or select <b>account</b>""".stripMargin

  override def moreParameters: String = "🛠 More parameters"

  override def chooseTimeFrom(exampleTime: LocalTime): String =
    s"<b>➡</b> Please choose time from or write time using format HH:mm, e.g. ${formatTime(exampleTime)}"

  override def chooseTimeTo(exampleTime: LocalTime): String =
    s"<b>➡</b> Please choose time to or write time using format HH:mm, e.g. ${formatTime(exampleTime)}"

  override def timeFromIs(timeFrom: LocalTime): String = s"⏱ Time from is ${formatTime(timeFrom)}"

  override def timeToIs(timeTo: LocalTime): String = s"⏱ Date to is ${formatTime(timeTo)}"

  override def canNotDetectPayer(error: String): String = s"Can't determine payer. Reason: $error"

  override def pleaseChoosePayer: String = "<b>➡</b> Can't determine default payer. Please choose one"

  override def noRehabReferralsFound: String = "ℹ No active rehabilitation referrals found"

  override def referralEntry(referral: Referral, page: Int, index: Int): String =
    s"""🏥 <b>${referral.procedures.map(_.name).mkString(", ")}</b>
       |Procedures: ${referral.proceduresAmount}
       |Expires: ${referral.expiredDate.getOrElse("N/A")}
       |Doctor: ${referral.doctor.getOrElse("N/A")}
       |<b>➡</b> /select_$index
       |""".stripMargin

  override def referralsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Active rehabilitation referrals", page, pages)

  override def rehabLocationEntry(location: RehabLocation, page: Int, index: Int): String =
    s"📍 ${location.name}\n<b>➡</b> /select_$index\n"

  override def rehabLocationsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Choose rehabilitation city", page, pages)

  override def rehabFacilityEntry(facility: RehabFacility, page: Int, index: Int): String =
    s"🏥 ${facility.name}\n<b>➡</b> /select_$index\n"

  override def rehabFacilitiesHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Choose rehabilitation facility", page, pages)

  override def rehabBookingSummary(data: RehabBookingData): String =
    s"""🏥 <b>Rehabilitation booking</b>
       |Service: ${data.serviceVariantName}
       |City: ${data.cityId.name}
       |Facility: ${if (data.facilityId != null) data.facilityId.name else "Any"}
       |Physiotherapist: ${if (data.physiotherapistId != null) data.physiotherapistId.name else "Any"}
       |Date: ${formatDate(data.dateFrom, locale)} — ${formatDate(data.dateTo, locale)}
       |Time: ${formatTime(data.timeFrom)} — ${formatTime(data.timeTo)}
       |
       |<b>➡</b> Now choose an action""".stripMargin

  override def rehabAppointmentIsConfirmed(remaining: Int): String =
    s"✅ Rehabilitation appointment confirmed!${if (remaining > 0) s" ($remaining procedures remaining)" else ""}"

  override def bookNextProcedure(remaining: Int): String =
    s"Book next procedure? ($remaining remaining)"

  override def rehabPhysiotherapistEntry(doctor: IdName, page: Int, index: Int): String =
    s"🧑‍⚕️ ${doctor.name}\n<b>➡</b> /select_$index\n"

  override def rehabPhysiotherapistsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Choose a physiotherapist", page, pages)

  override def choosePhysiotherapist: String = "🧑‍⚕️ Choose a physiotherapist or skip to find any available:"

  override def anyPhysiotherapist: String = "Any physiotherapist"
}
