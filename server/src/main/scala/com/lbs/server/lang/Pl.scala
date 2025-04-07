package com.lbs.server.lang

import com.lbs.api.json.model.{Doctor, Event, TermExt}
import com.lbs.server.conversation.Book
import com.lbs.server.conversation.StaticData.StaticDataConfig
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.util.DateTimeUtil._

import java.time.{LocalDateTime, LocalTime}
import java.util.Locale

object Pl extends Lang {

  override def id: Int = 2

  override def locale: Locale = new Locale("pl", "PL")

  override def label: String = "🇵🇱 Polski"

  override protected def withPages(message: String, page: Int, pages: Int): String = {
    if (pages > 1) s"$message. Strona <b>${page + 1}</b> z <b>$pages</b>"
    else message
  }

  override def unableToCancelUpcomingVisit(reason: String): String =
    s"⚠ Nie udało się odwołać wizyty! Powód: $reason"

  override def appointmentHasBeenCancelled: String =
    s"👍 Wizyta została odwołana!!"

  override def yes: String = "Tak"

  override def no: String = "Nie"

  override def noUpcomingVisits: String =
    "ℹ Nie znaleziono wizyt"

  override def areYouSureToCancelAppointment(event: Event): String =
    s"""<b>➡</b> Czy na pewno chcesz anulować wizytę?
       |
       |⏱ <b>${formatDateTime(event.date, locale)}</b>
       |${capitalize(doctor)}: ${event.doctor
        .map(d => s"${capitalize(d.name)} ${capitalize(d.lastname)}")
        .getOrElse("Nie podano")}
       |${capitalize(service)}: ${event.title}
       |${capitalize(clinic)}: ${event.clinic
        .map(c => s"${capitalize(c.city)} - ${capitalize(c.address)}")
        .getOrElse("Telemedicine")}
       |""".stripMargin

  override def chooseDateFrom(exampleDate: LocalDateTime): String =
    s"<b>➡</b> Wybierz datę albo zapisz ją w formacie dd-MM, np. ${formatDateShort(exampleDate)}"

  override def chooseDateTo(exampleDate: LocalDateTime): String =
    s"<b>➡</b> Wybierz datę albo zapisz ją w formacie dd-MM, np. ${formatDateShort(exampleDate)}"

  override def findTerms: String = "🔍 Szukaj terminów"

  override def modifyDate: String = "📅 Zmień datę"

  override def bookingSummary(bookingData: Book.BookingData): String =
    s"🦄 Ok! Zarezerwujemy wizytę typu <b>${bookingData.serviceId.name}</b>" +
      s" z lekarzem: <b>${bookingData.doctorId.name}</b>" +
      s" w klinice: <b>${bookingData.clinicId.name}</b>" +
      s" w mieście <b>${bookingData.cityId.name}</b>." +
      s"\nWybrane daty: <b>${formatDate(bookingData.dateFrom, locale)}</b> -> <b>${formatDate(bookingData.dateTo, locale)}</b>" +
      s" w godzinach: <b>${formatTime(bookingData.timeFrom)} -> ${formatTime(bookingData.timeTo)}</b>" +
      s"\n\n<b>➡</b> Wybierz co dalej"

  override def noTermsFound: String =
    s"""ℹ Nie znaleziono dostępnych terminów
       |
       |Co chcesz zrobić?""".stripMargin

  override def createMonitoring: String = "👀 Stwórz monitoring"

  override def cancel: String = "Anuluj"

  override def book: String = "Zarezerwuj"

  override def confirmAppointment(term: TermExt): String =
    s"""<b>➡</b> Czy potwierdzasz wizytę?
       |
       |⏱ <b>${formatDateTime(term.term.dateTimeFrom, locale)}</b>
       |${capitalize(doctor)}: ${term.term.doctor.firstName} ${term.term.doctor.lastName}
       |${capitalize(clinic)}: ${term.term.clinic}""".stripMargin

  override def appointmentIsConfirmed: String = "👍 Twoja wizyta została potwierdzona!"

  override def monitoringHasBeenCreated: String =
    "👍 Stworzono monitoring! Sprawdź aktywne monitoringi przez /monitorings"

  override def unableToCreateMonitoring(reason: String): String =
    s"👎 Nie udało się stworzyć monitoringu. Powód: $reason."

  override def chooseTypeOfMonitoring: String = "<b>➡</b> Wybierz typ monitoringu"

  override def bookByApplication: String = "👾 Automatyczna rezerwacja"

  override def bookManually: String = "👤 Rezerwacja ręczna (otrzymasz powiadomienie o dostępnych terminach)"

  override def rebookIfExists: String = "<b>➡</b> Czy chcesz zaktualizować termin, jeśli rezerwacja już istnieje?"

  override def pleaseSpecifyOffset: String = "<b>➡</b> Podaj offset w godzinach albo kliknij Nie"

  override def visitAlreadyExists: String =
    "<b>➡</b> Wizyta została juz zarezerwowana. Czy chcesz zaktualizować jej termin?"

  override def city: String = "miasto"

  override def clinic: String = "klinika"

  override def service: String = "usługa"

  override def doctor: String = "lekarz"

  override def previous: String = "Wstecz"

  override def next: String = "Dalej"

  override def noActiveMonitorings: String = "ℹ Nie masz aktywnych monitoringów. Stwórz nowy przez /book"

  override def deactivateMonitoring(monitoring: Monitoring): String =
    s"""<b>➡</b> Czy na pewno chcesz wyłączyć monitoring?
       |
       |📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${formatTime(monitoring.timeFrom)}</b> -> <b>${formatTime(monitoring.timeTo)}</b>
       |${capitalize(doctor)}: ${monitoring.doctorName}
       |${capitalize(service)}: ${monitoring.serviceName}
       |${capitalize(clinic)}: ${monitoring.clinicName}""".stripMargin

  override def deactivated: String = "👍 Wyłączony! Sprawdź aktywne monitoringi przez /monitorings"

  override def any: String = "Jakikolwiek"

  override def pressAny: String = s"albo naciśnij przycisk <b>$any</b> "

  override def pleaseEnterStaticDataNameOrAny(config: StaticDataConfig): String =
    withAnyVariant(
      s"""<b>➡</b> Podaj fragment nazwy ${config.name}
         |Na przykład: <b>${config.partialExample}</b> jeśli szukasz <b>${config.example}</b>""".stripMargin,
      config.isAnyAllowed
    )

  override def pleaseEnterStaticDataNameOrPrevious(config: StaticDataConfig): String =
    s"""<b>➡</b> Podaj fragment nazwy ${config.name}
       |Na przykład: <b>${config.partialExample}</b> jeśli szukasz <b>${config.example}</b>
       |
       |lub wybierz ${config.name} z poprzednich wyszukiwań""".stripMargin

  override def staticDataIs(config: StaticDataConfig, label: String): String =
    s"<b>✅</b> ${capitalize(config.name)} jest <b>$label</b>"

  override def pleaseChooseStaticDataNameOrAny(config: StaticDataConfig): String =
    withAnyVariant(s"<b>➡</b> Wybierz ${config.name}", config.isAnyAllowed)

  override def staticNotFound(config: StaticDataConfig): String =
    withAnyVariant(
      s"""<b>➡</b> Brak wyników 😔
         |Proszę podaj nazwę ${config.name} jeszcze raz""",
      config.isAnyAllowed
    )

  override def loginAndPasswordAreOk: String =
    s"""✅ Brawo! Login i hasło są OK!
       |Teraz możesz zmienic język przez /settings
     """.stripMargin

  override def provideUsername: String =
    s"""ℹ Musisz się zalogować do <b>Luxmed</b>
       |
       |<b>➡</b> Podaj nazwę uzytkownika:""".stripMargin

  override def providePassword: String = "<b>➡</b> Podaj hasło"

  override def eventsListIsEmpty: String = "ℹ Brak wizyt"

  override def help: String =
    s"""ℹ Nieoficjalny Bot do <b>Portal Pacjenta LUX MED (v.${Lang.version})</b>.
       |Pomogę Ci w rezerwacji wizyty, stworzeniu monitoringu na termin, pokazaniu nadchodzących wizyt i przejrzeniu historii.
       |
       |<b>➡</b> Wspierane komendy
       |/book - zarezerwuj wizytę albo stwórz monitoring
       |/monitorings - lista obecnych monitoringów
       |/monitorings_history - lista przeszłych monitoringów
       |/reserved - nadchodzące wizyty
       |/history - historia wizyt
       |/accounts - zarządzanie kontami Luxmed
       |/login - ponowne logowanie
       |/settings - ustawienia, np. język
       |/help - pomoc""".stripMargin

  override def dateFromIs(dateFrom: LocalDateTime): String = s"📅 Data od ${formatDate(dateFrom, locale)}"

  override def dateToIs(dateTo: LocalDateTime): String = s"📅 Data do ${formatDate(dateTo, locale)}"

  override def termEntry(term: TermExt, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(term.term.dateTimeFrom, locale)}</b>
       |${capitalize(doctor)}: ${term.term.doctor.firstName} ${term.term.doctor.lastName}
       |${capitalize(clinic)}: ${term.term.clinic}
       |<b>➡</b> /book_$index
       |
       |""".stripMargin

  override def termsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Dostępne terminy", page, pages)

  override def historyEntry(event: Event, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(event.date, locale)}</b>
       |${capitalize(doctor)}: ${event.doctor
        .map(d => s"${capitalize(d.name)} ${capitalize(d.lastname)}")
        .getOrElse("Nie podano")}
       |${capitalize(service)}: ${event.title}
       |${capitalize(clinic)}: ${event.clinic
        .map(c => s"${capitalize(c.city)} - ${capitalize(c.address)}")
        .getOrElse("Telemedicine")}
       |
       |""".stripMargin

  override def historyHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Odbyte wizyty", page, pages)

  override def reservedVisitEntry(event: Event, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(event.date, locale)}</b>
       |${capitalize(doctor)}: ${event.doctor
        .map(d => s"${capitalize(d.name)} ${capitalize(d.lastname)}")
        .getOrElse("Nie podano")}
       |${capitalize(service)}: ${event.title}
       |${capitalize(clinic)}: ${event.clinic
        .map(c => s"${capitalize(c.city)} - ${capitalize(c.address)}")
        .getOrElse("Telemedicine")}
       |<b>➡</b> /cancel_$index
       |
       |""".stripMargin

  override def reservedVisitsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Zarezerwowane wizyty", page, pages)

  override def bugsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Zgłoszone błędy", page, pages)

  override def monitoringEntry(monitoring: Monitoring, page: Int, index: Int): String =
    s"""📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${formatTime(monitoring.timeFrom)}</b> -> <b>${formatTime(monitoring.timeTo)}</b>
       |${capitalize(doctor)}: ${monitoring.doctorName}
       |${capitalize(service)}: ${monitoring.serviceName}
       |${capitalize(clinic)}: ${monitoring.clinicName}
       |${capitalize(city)}: ${monitoring.cityName}
       |Sposób rejestracji: ${if (monitoring.autobook) "Automatyczny" else "Ręczny"}
       |Nadpisz istniejącą wizytę: ${if (monitoring.rebookIfExists) "Tak" else "Nie"}
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
       |Sposób rejestracji: ${if (monitoring.autobook) "Automatyczny" else "Ręczny"}
       |<b>➡</b> /repeat_$index
       |
       |""".stripMargin

  override def monitoringsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Aktywne monitoringi", page, pages)

  override def monitoringsHistoryHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Historia monitoringów", page, pages)

  override def invalidLoginOrPassword: String =
    """❗ Błędne dane logowania. Czy dane logowania zostały zmienione przez stronę Luxmedu?
      |Usunięto monitoringi. Zaloguj się przez /login i stwórz nowe monitoringi.
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
    s"""✅ Monitoring znalazł <b>$size</b> terminów. Pokazujemy Ci najbliszych 5.
       |
       |<b>➡</b> Wybierz jeden, by go zarezerwować""".stripMargin

  override def nothingWasFoundByMonitoring(monitoring: Monitoring): String =
    s"""❗ Monitoring nie znalazł terminów i został <b>wyłączony</b> jako przeterminowany.
       |
       |📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${formatTime(monitoring.timeFrom)}</b> -> <b>${formatTime(monitoring.timeTo)}</b>
       |${capitalize(doctor)}: ${monitoring.doctorName}
       |${capitalize(service)}: ${monitoring.serviceName}
       |${capitalize(clinic)}: ${monitoring.clinicName}
       |${capitalize(city)}: ${monitoring.cityName}
       |
       |<b>➡</b> Stwórz nowy monitoring przez /book""".stripMargin

  override def appointmentIsBooked(term: TermExt, monitoring: Monitoring, doctorDetails: Doctor): String =
    s"""👍 Zarezerwowaliśmy termin dla ${monitoring.username}!
       |
       |⏱ <b>${formatDateTime(term.term.dateTimeFrom, locale)}</b>
       |${capitalize(doctor)}: ${doctorDetails.firstName} ${doctorDetails.lastName}
       |${capitalize(service)}: ${monitoring.serviceName}
       |${capitalize(clinic)}: ${term.term.clinic}
       |${capitalize(city)}: ${monitoring.cityName}""".stripMargin

  override def maximumMonitoringsLimitExceeded: String = "Maksymalna liczba monitoringów uzytkownika to 10"

  override def termIsOutdated: String =
    s"""❗️ Wygląda na to, ze ten termin został już zarezewowany!
       |
       |Wybierz inny termin albo stwórz nowy monitoring przez /book""".stripMargin

  override def loginHasChangedOrWrong: String =
    """❗ Wprowadzono niepoprawny <b>login</b> lub <b>hasło</b> lub zostały one zmienione.
      |
      |Zaloguj się ponownie przez /login i stwórz nowy monitoring przez /book.
    """.stripMargin

  override def settingsHeader: String = "<b>➡</b> Wybierz opcję:"

  override def language: String = "🌐 Język"

  override def offset: String = "⏱ Offset"

  override def chooseLanguage: String = "<b>➡</b> Wybierz język:"

  override def configureOffset: String = "<b>➡</b> Wybierz opcje offsetu"

  override def pleaseEnterOffset(current: Int): String =
    s"<b>➡</b> Podaj domyślny offset. Obecny offset: <b>$current</b>"

  override def alwaysAskOffset(enabled: Boolean): String = s"${if (enabled) "✅ " else ""}Zawsze pytaj o offset"

  override def changeDefaultOffset(current: Int): String = s"Zmień domyślny offset ($current)"

  override def languageUpdated: String = "👍 Zmieniono język!"

  override def appointmentWasNotCancelled: String = "👍 Wizyta nie została anulowana"

  override def monitoringWasNotDeactivated: String = "👍 Nie wyłączono monitoringu"

  override def bugAction: String = "<b>➡</b> Wybierz opcję:"

  override def createNewBug: String = "🐞 Dodaj nowy"

  override def showSubmittedBugs: String = "👀 Pokaż dotychczasowe"

  override def enterIssueDetails: String = "<b>➡</b> Podaj szczegóły błędu:"

  override def noSubmittedIssuesFound: String = "ℹ Nie znaleziono dotychczasowych zgłoszeń błędów"

  override def bugHasBeenCreated(bugId: Long): String = s"✅ Dziękuję za zgłoszenie błędu<b>#$bugId</b>!"

  override def deleteAccount: String = "➖ Usuń konto"

  override def addAccount: String = "➕ Dodaj konto"

  override def accountSwitched(username: String): String =
    s"✅ Zmieniono konto na: <b>$username</b>"

  override def pleaseChooseAccount(currentAccountName: String): String =
    s"""Obecne konto to: <b>$currentAccountName</b>
       |
       |<b>➡</b> Wybierz <b>opcję</b> albo <b>konto</b>""".stripMargin

  override def moreParameters: String = "🛠 Więcej opcji"

  override def chooseTimeFrom(exampleTime: LocalTime): String =
    s"<b>➡</b> Wybierz godzinę OD albo zapisz w formacie HH:mm, np. ${formatTime(exampleTime)}"

  override def chooseTimeTo(exampleTime: LocalTime): String =
    s"<b>➡</b> Wybierz godzinę DO albo zapisz w formacie HH:mm ${formatTime(exampleTime)}"

  override def timeFromIs(timeFrom: LocalTime): String = s"⏱ Godzina OD: ${formatTime(timeFrom)}"

  override def timeToIs(timeTo: LocalTime): String = s"⏱ Godzina DO: ${formatTime(timeTo)}"

  override def canNotDetectPayer(error: String): String = s"Nie udało się ustalić płatnika. Powód: $error"

  override def pleaseChoosePayer: String = "<b>➡</b> Nie udało się ustalić domyślnego płatnika, wybierz jakiegoś."
}
