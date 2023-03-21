package com.lbs.server

import com.lbs.api.json.model._
import com.lbs.bot.model.Command
import com.lbs.common.ModelConverters
import com.lbs.server.conversation.Book.BookingData
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.repository.model.{History, Monitoring}

import java.time._
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.language.{higherKinds, implicitConversions}
import scala.util.Try

package object util {

  object ServerModelConverters extends ModelConverters {

    implicit val BookingDataToMonitoringConverter: ObjectConverter[(UserId, BookingData), Monitoring] =
      (data: (UserId, BookingData)) => {
        val (userId, bookingData) = data
        Monitoring(
          userId = userId.userId,
          username = userId.username,
          accountId = userId.accountId,
          chatId = userId.source.chatId,
          sourceSystemId = userId.source.sourceSystem.id,
          payerId = bookingData.payerId,
          cityId = bookingData.cityId.id,
          cityName = bookingData.cityId.name,
          clinicId = bookingData.clinicId.optionalId,
          clinicName = bookingData.clinicId.name,
          serviceId = bookingData.serviceId.id,
          serviceName = bookingData.serviceId.name,
          doctorId = bookingData.doctorId.optionalId,
          doctorName = bookingData.doctorId.name,
          dateFrom = bookingData.dateFrom.atZone(DateTimeUtil.Zone),
          dateTo = bookingData.dateTo.atZone(DateTimeUtil.Zone),
          timeFrom = bookingData.timeFrom,
          timeTo = bookingData.timeTo,
          autobook = bookingData.autobook,
          rebookIfExists = bookingData.rebookIfExists,
          offset = bookingData.offset
        )
      }

    implicit val ReservationLocktermResponseAndTermToReservationConfirmRequestConverter
      : ObjectConverter[(ReservationLocktermResponse, TermExt), ReservationConfirmRequest] =
      (data: (ReservationLocktermResponse, TermExt)) => {
        val (reservationLocktermResponse, termExt) = data
        val term = termExt.term
        ReservationConfirmRequest(
          date = term.dateTimeFrom.get.minusHours(2).toString + ":00.000Z",
          doctorId = term.doctor.id,
          facilityId = term.clinicId,
          roomId = term.roomId,
          scheduleId = term.scheduleId,
          serviceVariantId = term.serviceId,
          temporaryReservationId = reservationLocktermResponse.value.temporaryReservationId,
          timeFrom = term.dateTimeFrom.get.toLocalTime,
          valuation = reservationLocktermResponse.value.valuations.head
        )
      }

    implicit val ReservationLocktermResponseAndTermToReservationChangeTermRequestConverter
      : ObjectConverter[(ReservationLocktermResponse, TermExt), ReservationChangetermRequest] =
      (data: (ReservationLocktermResponse, TermExt)) => {
        val (reservationLocktermResponse, termExt) = data
        val term = termExt.term
        val existingReservationId = reservationLocktermResponse.value.relatedVisits.head.reservationId
        ReservationChangetermRequest(
          existingReservationId = existingReservationId,
          term = NewTerm(
            date = term.dateTimeFrom.get.minusHours(2).toString + ":00.000Z",
            doctorId = term.doctor.id,
            facilityId = term.clinicId,
            parentReservationId = existingReservationId,
            referralRequired = reservationLocktermResponse.value.valuations.head.isReferralRequired,
            roomId = term.roomId,
            scheduleId = term.scheduleId,
            serviceVariantId = term.serviceId,
            temporaryReservationId = reservationLocktermResponse.value.temporaryReservationId,
            timeFrom = term.dateTimeFrom.get.toLocalTime,
            valuation = reservationLocktermResponse.value.valuations.head
          )
        )
      }

    implicit val TermToReservationLocktermRequest: ObjectConverter[TermExt, ReservationLocktermRequest] =
      termExt => {
        val term = termExt.term
        val additionalData = termExt.additionalData
        ReservationLocktermRequest(
          date = term.dateTimeFrom.get.minusHours(2).toString + ":00.000Z",
          doctor = term.doctor,
          doctorId = term.doctor.id,
          facilityId = term.clinicId,
          impedimentText = term.impedimentText,
          isAdditional = term.isAdditional,
          isImpediment = term.isImpediment,
          isPreparationRequired = additionalData.isPreparationRequired,
          isTelemedicine = term.isTelemedicine,
          preparationItems = additionalData.preparationItems,
          roomId = term.roomId,
          scheduleId = term.scheduleId,
          serviceVariantId = term.serviceId,
          timeFrom = term.dateTimeFrom.get.toLocalTime.toString,
          timeTo = term.dateTimeTo.get.toLocalTime.toString
        )
      }

    implicit val HistoryToIdNameConverter: ObjectConverter[History, IdName] =
      (history: History) => IdName(history.id, history.name)
  }

  object MessageExtractors {

    object TextCommand {
      def unapply(cmd: Command): Option[String] =
        if (cmd.callbackData.isEmpty) cmd.message.text.filter(_.nonEmpty) else None
    }

    object OptionalTextCommand {
      def unapply(cmd: Command): Option[Option[String]] =
        if (cmd.callbackData.isEmpty) Some(TextCommand.unapply(cmd)) else None
    }

    object CallbackCommand {
      def unapply(cmd: Command): Option[String] = cmd.callbackData
    }

    object BooleanString {
      def unapply(string: String): Option[Boolean] = Try(string.toBoolean).toOption
    }

    object IntString {
      def unapply(string: String): Option[Int] = Try(string.toInt).toOption
    }

    object LongString {
      def unapply(string: String): Option[Long] = Try(string.toLong).toOption
    }

  }

  object DateTimeUtil {
    val Zone: ZoneId = ZoneId.of("Europe/Warsaw")

    private val DateFormat: Locale => DateTimeFormatter = locale => DateTimeFormatter.ofPattern("dd MMM yyyy", locale)

    private val DateShortFormat = DateTimeFormatter.ofPattern("dd-MM")

    private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val DateTimeFormat: Locale => DateTimeFormatter = locale =>
      DateTimeFormatter.ofPattern("EEE',' dd MMM yyyy',' HH:mm", locale)

    def formatDate(date: LocalDateTime, locale: Locale): String = date.format(DateFormat(locale))

    def formatDate(date: ZonedDateTime, locale: Locale): String = date.format(DateFormat(locale))

    def formatDateShort(date: LocalDateTime): String = date.format(DateShortFormat)

    def formatTime(time: LocalTime): String = time.format(TimeFormat)

    def formatDateTime(date: ZonedDateTime, locale: Locale): String = date.format(DateTimeFormat(locale))

    def formatDateTime(date: LuxmedFunnyDateTime, locale: Locale): String = date.get.format(DateTimeFormat(locale))

    private val EpochMinutesTillBeginOf2022: Long = epochMinutes(LocalDateTime.of(2022, 1, 1, 0, 0, 0, 0))

    def epochMinutes(time: LocalDateTime): Long = time.toInstant(ZonedDateTime.now().getOffset).getEpochSecond / 60

    def minutesSinceBeginOf2018(time: LocalDateTime): Long = epochMinutes(time) - EpochMinutesTillBeginOf2022

    def applyDayMonth(dayMonthStr: String, date: LocalDateTime): LocalDateTime = {
      val dayMonth = MonthDay.parse(dayMonthStr, DateShortFormat)
      val newDate = date.withDayOfMonth(dayMonth.getDayOfMonth).withMonth(dayMonth.getMonthValue)

      if (newDate.isBefore(date)) newDate.plusYears(1) else newDate
    }

    def applyHourMinute(hourMinuteStr: String): LocalTime = {
      LocalTime.parse(hourMinuteStr, TimeFormat)
    }
  }

}
