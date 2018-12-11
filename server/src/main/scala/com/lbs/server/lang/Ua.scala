
package com.lbs.server.lang

import java.time.{LocalTime, ZonedDateTime}
import java.util.Locale

import com.lbs.api.json.model.{AvailableVisitsTermPresentation, HistoricVisit, ReservedVisit, ValuationsResponse}
import com.lbs.server.conversation.Book
import com.lbs.server.conversation.StaticData.StaticDataConfig
import com.lbs.server.repository.model.{Bug, Monitoring}
import com.lbs.server.util.DateTimeUtil._

object Ua extends Lang {

  override def id: Int = 1

  override def locale: Locale = new Locale("uk", "UA")

  override def label: String = "🇺🇦Українська"

  override protected def withPages(message: String, page: Int, pages: Int): String = {
    if (pages > 1) s"$message. Сторінка <b>${page + 1}</b> з <b>$pages</b>"
    else message
  }

  override def unableToCancelUpcomingVisit(reason: String): String =
    s"⚠ Не вдається скасувати візит! Причина: $reason"

  override def appointmentHasBeenCancelled: String =
    s"👍 Ваш візит було скасовано!"

  override def yes: String = "Так"

  override def no: String = "Ні"

  override def noUpcomingVisits: String =
    "ℹ Не знайдено жодного майбутнього візиту"

  override def areYouSureToCancelAppointment(visit: ReservedVisit): String =
    s"""<b>➡</b> Ви впевнені, що хочете скасувати візит?
       |
       |⏱ <b>${formatDateTime(visit.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${visit.doctorName}
       |${capitalizeFirstLetter(service)}: ${visit.service.name}
       |${capitalizeFirstLetter(clinic)}: ${visit.clinic.name}
       |""".stripMargin

  override def chooseDateFrom(exampleDate: ZonedDateTime): String = s"<b>➡</b> Будь ласка, виберіть початкову дату або введіть її, використовуючи формат dd-MM, наприклад ${formatDateShort(exampleDate)}"

  override def chooseDateTo(exampleDate: ZonedDateTime): String = s"<b>➡</b> Будь ласка, виберіть кінцеву дату або введіть її, використовуючи формат dd-MM, наприклад ${formatDateShort(exampleDate)}"

  override def findTerms: String = "🔍 Знайти терміни"

  override def modifyDate: String = "📅 Змінити дату"

  override def bookingSummary(bookingData: Book.BookingData): String =
    s"🦄 Супер! Ми збираємося зарезервувати послугу <b>${bookingData.serviceId.name}</b>" +
      s" з обраним лікарем <b>${bookingData.doctorId.name}</b>" +
      s" в <b>${bookingData.clinicId.name}</b> клініці" +
      s" міста <b>${bookingData.cityId.name}</b>." +
      s"\nБажані дати: <b>${formatDate(bookingData.dateFrom, locale)}</b> -> <b>${formatDate(bookingData.dateTo, locale)}</b>" +
      s"\nЧас: <b>${formatTime(bookingData.timeFrom)}</b> -> <b>${formatTime(bookingData.timeTo)}</b>" +
      s"\n\n<b>➡</b> Тепер оберіть наступну дію"

  override def noTermsFound: String =
    s"""ℹ Терміни відсутні
       |
       |Що ви хочете зробити далі?""".stripMargin

  override def createMonitoring: String = "👀 Створити моніторінг"

  override def cancel: String = "Відмінити"

  override def book: String = "Зарезервувати"

  override def confirmAppointment(term: AvailableVisitsTermPresentation, valuations: ValuationsResponse): String =

    s"""<b>➡</b> ${valuations.optionsQuestion.getOrElse("Ви хотіли б підтвердити резервацію візиту?")}
       |
       |⏱ <b>${formatDateTime(term.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${term.doctor.name}
       |${capitalizeFirstLetter(clinic)}: ${term.clinic.name}
       |
       |ℹ${valuations.visitTermVariants.head.infoMessage}""".stripMargin

  override def appointmentIsConfirmed: String = "👍 Ваш візит було підтверджено!"

  override def monitoringHasBeenCreated: String = "👍 Моніторинг був створений! Список активних /monitorings"

  override def unableToCreateMonitoring(reason: String): String = s"👎 Не вдається створити моніторинг. Причина: $reason."

  override def chooseTypeOfMonitoring: String = "<b>➡</b> Будь ласка, виберіть тип моніторингу"

  override def bookByApplication: String = "👾 Автоматична резервація"

  override def bookManually: String = "👤 Ручна резервація"

  override def rebookIfExists: String = "<b>➡</b> Чи хотіли би ви змінити термін в разі, якщо резервація вже існує?"

  override def visitAlreadyExists: String = "<b>➡</b> Резервація для такого сервісу вже існує. Чі хотіли би ви змінити термін?"

  override def city: String = "місто"

  override def clinic: String = "клініка"

  override def service: String = "послуга"

  override def doctor: String = "лікар"

  override def previous: String = "Попередня"

  override def next: String = "Наступна"

  override def noActiveMonitorings: String = "ℹ У вас немає активних моніторингів. Створити новий /book"

  override def deactivateMonitoring(monitoring: Monitoring): String =
    s"""<b>➡</b> Ви впевнені, що хочете вимкнути моніторинг?
       |
       |📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${formatTime(monitoring.timeFrom)}</b> -> <b>${formatTime(monitoring.timeTo)}</b>
       |${capitalizeFirstLetter(doctor)}: ${monitoring.doctorName}
       |${capitalizeFirstLetter(service)}: ${monitoring.serviceName}
       |${capitalizeFirstLetter(clinic)}: ${monitoring.clinicName}""".stripMargin

  override def deactivated: String = "👍 Деактивовано! Список активних /monitorings"

  override def any: String = "Будь-який"

  override def pressAny: String = s"або натисніть кнопку <b>$any</b>"

  override def pleaseEnterStaticDataNameOrAny(config: StaticDataConfig): String =
    withAnyVariant(
      s"""<b>➡</b> Будь ласка, введіть ${config.name}
         |Наприклад: <b>${config.example}</b>""".stripMargin,
      config.isAnyAllowed)

  override def pleaseEnterStaticDataNameOrPrevious(config: StaticDataConfig): String =
    s"""<b>➡</b> Будь ласка, введіть ${config.name}
       |Наприклад: <b>${config.example}</b>
       |
       |або оберіть ${config.name} з попередніх пошуків""".stripMargin

  override def staticDataIs(config: StaticDataConfig, label: String): String =
    s"<b>✅</b> ${capitalizeFirstLetter(config.name)} <b>$label</b>"

  override def pleaseChooseStaticDataNameOrAny(config: StaticDataConfig): String =
    withAnyVariant(s"<b>➡</b> Будь ласка, виберіть ${config.name}", config.isAnyAllowed)

  override def staticNotFound(config: StaticDataConfig): String =
    withAnyVariant(
      s"""<b>➡</b> Нічого не знайдено 😔
         |Будь ласка, введіть ${config.name} знову""".stripMargin, config.isAnyAllowed)

  override def loginAndPasswordAreOk: String =
    s"""✅ Супер! Логін і пароль збережено
       |Тепер ви можете змінити мову /settings""".stripMargin

  override def provideUsername: String =
    s"""ℹ Ви повинні увійти в систему, використовуючи облікові дані <b>Luxmed</b>
       |
       |<b>➡</b> Будь ласка, введіть ім'я користувача""".stripMargin

  override def providePassword: String = "<b>➡</b> Будь ласка, введіть пароль"

  override def visitsHistoryIsEmpty: String = "ℹ Немає візитів в вашій історії"

  override def help: String =
    s"""ℹ Це неофіційний бот для <b>Порталу Пацієнта LUX MED</b>.
       |Завдяки йому ви можете зарезервувати візит до лікаря, створити моніторинг доступних термінів, переглянути історію та майбутні візити.
       |
       |Пінгани @dyrkin_me у разі питань.
       |
       |<b>➡</b> Підтримувані команди
       |/book - зарезервувати візит або створити моніторинг
       |/monitorings - моніторінг доступних термінів
       |/reserved - зарезеровані візити
       |/history - історія візитів
       |/accounts - управління акаунтами Luxmed
       |/settings - налаштування
       |/bug - відправити баг""".stripMargin

  override def dateFromIs(dateFrom: ZonedDateTime): String = s"📅 Початкова дата ${formatDate(dateFrom, locale)}"

  override def dateToIs(dateTo: ZonedDateTime): String = s"📅 Кінцева дата ${formatDate(dateTo, locale)}"

  override def termEntry(term: AvailableVisitsTermPresentation, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(term.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${term.doctor.name}
       |${capitalizeFirstLetter(clinic)}: ${term.clinic.name}
       |<b>➡</b> /book_${page}_$index
       |
       |""".stripMargin

  override def termsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Доступні терміни", page, pages)

  override def historyEntry(visit: HistoricVisit, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(visit.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${visit.doctorName}
       |${capitalizeFirstLetter(service)}: ${visit.service.name}
       |${capitalizeFirstLetter(clinic)}: ${visit.clinicName}
       |<b>➡</b> /repeat_${page}_$index
       |
       |""".stripMargin

  override def historyHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Завершені візити", page, pages)

  override def upcomingVisitEntry(visit: ReservedVisit, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(visit.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${visit.doctorName}
       |${capitalizeFirstLetter(service)}: ${visit.service.name}
       |${capitalizeFirstLetter(clinic)}: ${visit.clinic.name}
       |<b>➡</b> /cancel_${page}_$index
       |
       |""".stripMargin

  override def upcomingVisitsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Зарезервовані візити", page, pages)

  override def bugEntry(bug: Bug, page: Int, index: Int): String =
    s"""⏱ <b>${formatDateTime(bug.submitted, locale)}</b>
       |Опис: ${bug.details}
       |Статус: <b>${if (bug.resolved) "✅ Вирішено" else "🚫 Невирішено"}</b>
       |
       |""".stripMargin

  override def bugsHeader(page: Int, pages: Int): String =
    withPages("<b>➡</b> Створені баги", page, pages)

  override def monitoringEntry(monitoring: Monitoring, page: Int, index: Int): String =
    s"""📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${formatTime(monitoring.timeFrom)}</b> -> <b>${formatTime(monitoring.timeTo)}</b>
       |${capitalizeFirstLetter(doctor)}: ${monitoring.doctorName}
       |${capitalizeFirstLetter(service)}: ${monitoring.serviceName}
       |${capitalizeFirstLetter(clinic)}: ${monitoring.clinicName}
       |${capitalizeFirstLetter(city)}: ${monitoring.cityName}
       |Тип: ${if (monitoring.autobook) "Автоматичний" else "Ручний"}
       |<b>➡</b> /cancel_${page}_$index
       |
       |""".stripMargin

  override def monitoringsHeader(page: Int, pages: Int): String =
    s"<b>➡</b> Активні моніторінги"

  override def invalidLoginOrPassword: String =
    """❗ Ви ввели невірний логін або пароль, або змінили його через сайт.
      |Ваші моніторинги були видалені. Будь ласка, /login знову і створіть нові моніторинги.
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
    s"""✅ <b>$size</b> термінів було знайдено за допомогою моніторінгу. Ми показали вам найближчі 5.
       |
       |<b>➡</b> Будь ласка, оберіть один щоб зарезервувати""".stripMargin

  override def nothingWasFoundByMonitoring(monitoring: Monitoring): String =
    s"""❗ Нічого не знайдено за вашим моніторингом. Моніторинг був <b>вимкнений</b> як застарілий.
       |
       |📅 <b>${formatDate(monitoring.dateFrom, locale)}</b> -> <b>${formatDate(monitoring.dateTo, locale)}</b>
       |⏱ <b>${formatTime(monitoring.timeFrom)}</b> -> <b>${formatTime(monitoring.timeTo)}</b>
       |${capitalizeFirstLetter(doctor)}: ${monitoring.doctorName}
       |${capitalizeFirstLetter(service)}: ${monitoring.serviceName}
       |${capitalizeFirstLetter(clinic)}: ${monitoring.clinicName}
       |${capitalizeFirstLetter(city)}: ${monitoring.cityName}
       |
       |<b>➡</b> Створити новий моніторінг /book""".stripMargin

  override def appointmentIsBooked(term: AvailableVisitsTermPresentation, monitoring: Monitoring): String =
    s"""👍 Ми зерезевували візит для вас!
       |
       |⏱ <b>${formatDateTime(term.visitDate.startDateTime, locale)}</b>
       |${capitalizeFirstLetter(doctor)}: ${term.doctor.name}
       |${capitalizeFirstLetter(service)}: ${monitoring.serviceName}
       |${capitalizeFirstLetter(clinic)}: ${term.clinic.name}
       |${capitalizeFirstLetter(city)}: ${monitoring.cityName}""".stripMargin

  override def maximumMonitoringsLimitExceeded: String = "Максимальна кількість моніторінгів 10"

  override def monitoringOfTheSameTypeExists: String = "У вас вже є активний моніторинг на таку ж саму послугу i лiкаря /monitorings"

  override def termIsOutdated: String =
    s"""❗️ Схоже, що термін вже не є доступним
       |
       |Будь ласка, спробуйте інший або створіть новий моніторинг /book""".stripMargin

  override def loginHasChangedOrWrong: String =
    """❗ Ви ввели невірний і <b>логін</b> або <b>пароль</b> або змінили його через сайт.
      |
      |Будь ласка, /login знову і створіть новий моніторинг/book.
    """.stripMargin

  override def settingsHeader: String = "<b>➡</b> Оберіть дію"

  override def language: String = "🌐 Змінити мову"

  override def chooseLanguage: String = "<b>➡</b> Будь ласка, оберіть мову"

  override def languageUpdated: String = "👍 Мову успішно змінено!"

  override def appointmentWasNotCancelled: String = "👍 Візит не було скасовано"

  override def monitoringWasNotDeactivated: String = "👍 Моніторінг не було деактивовано"

  override def bugAction: String = "<b>➡</b> Оберіть дію"

  override def createNewBug: String = "🐞 Створити новий"

  override def showSubmittedBugs: String = "👀 Показати створені"

  override def enterIssueDetails: String = "<b>➡</b> Будь ласка, введіть деталі проблеми"

  override def noSubmittedIssuesFound: String = "ℹ Створених вами багів не знайдено"

  override def bugHasBeenCreated(bugId: Long): String = s"✅ Дякуємо за відправлений баг <b>#$bugId</b>!"

  override def afterFive: String = "Після 17:00"

  override def nineToFive: String = "Від 09:00 до 17:00"

  override def beforeNine: String = "До 09:00"

  override def allDay: String = "Весь день"

  override def deleteAccount: String = "➖ Видалити акаунт"

  override def addAccount: String = "➕ Додати акаунт"

  override def accountSwitched(username: String): String =
    s"✅ Аккаунт було переключено на <b>$username</b>"

  override def pleaseChooseAccount(currentAccountName: String): String =
    s"""Поточний акаунт <b>$currentAccountName</b>
       |
       |<b>➡</b> Будь ласка, оберіть <b>дію</b> або виберіть <b>акаунт</b>""".stripMargin

  override def moreParameters: String = "🛠 Більше налаштувань"

  override def chooseTimeFrom(exampleTime: LocalTime): String = s"<b>➡</b> Будь ласка, виберіть початковий час або введіть час, використовуючи формат HH:mm, наприклад ${formatTime(exampleTime)}"

  override def chooseTimeTo(exampleTime: LocalTime): String = s"<b>➡</b> Будь ласка, виберіть кінцевий час або введіть час, використовуючи формат HH:mm, наприклад ${formatTime(exampleTime)}"

  override def timeFromIs(timeFrom: LocalTime): String = s"⏱ Початковий час  ${formatTime(timeFrom)}"

  override def timeToIs(timeTo: LocalTime): String = s"⏱ Кінцевий час ${formatTime(timeTo)}"
}
