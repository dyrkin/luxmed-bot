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
package com.lbs.server

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.Locale

import com.lbs.api.json.model._
import com.lbs.bot.model.Message
import com.lbs.common.ModelConverters
import com.lbs.server.actor.Book.BookingData
import com.lbs.server.actor.Login.UserId
import com.lbs.server.repository.model.{History, Monitoring}

import scala.collection.generic.CanBuildFrom
import scala.language.{higherKinds, implicitConversions}


package object util {

  object ServerModelConverters extends ModelConverters {

    implicit val BookingDataToMonitoringConverter:
      ObjectConverter[(UserId, BookingData), Monitoring] =
      new ObjectConverter[(UserId, BookingData), Monitoring] {
        override def convert[Z <: (UserId, BookingData)](data: Z): Monitoring = {
          val (userId, bookingData) = data.asInstanceOf[(UserId, BookingData)]
          Monitoring(
            userId = userId.userId,
            chatId = userId.source.chatId,
            sourceSystemId = userId.source.sourceSystem.id,
            cityId = bookingData.cityId.id,
            cityName = bookingData.cityId.name,
            clinicId = bookingData.clinicId.optionalId,
            clinicName = bookingData.clinicId.name,
            serviceId = bookingData.serviceId.id,
            serviceName = bookingData.serviceId.name,
            doctorId = bookingData.doctorId.optionalId,
            doctorName = bookingData.doctorId.name,
            dateFrom = bookingData.dateFrom,
            dateTo = bookingData.dateTo,
            timeOfDay = bookingData.timeOfDay,
            autobook = bookingData.autobook
          )
        }
      }

    implicit val AvailableVisitsTermPresentationToTemporaryReservationRequestConverter:
      ObjectConverter[AvailableVisitsTermPresentation, TemporaryReservationRequest] =
      new ObjectConverter[AvailableVisitsTermPresentation, TemporaryReservationRequest] {
        override def convert[Z <: AvailableVisitsTermPresentation](term: Z): TemporaryReservationRequest = {
          TemporaryReservationRequest(
            clinicId = term.clinic.id,
            doctorId = term.doctor.id,
            payerDetailsList = term.payerDetailsList,
            referralRequiredByService = term.referralRequiredByService,
            roomId = term.roomId,
            serviceId = term.serviceId,
            startDateTime = term.visitDate.startDateTime
          )
        }
      }

    implicit val TmpReservationIdWithValuationsToReservationRequestConverter:
      ObjectConverter[(Long, VisitTermVariant, AvailableVisitsTermPresentation), ReservationRequest] =
      new ObjectConverter[(Long, VisitTermVariant, AvailableVisitsTermPresentation), ReservationRequest] {
        override def convert[Z <: (Long, VisitTermVariant, AvailableVisitsTermPresentation)](any: Z): ReservationRequest = {
          val (tmpReservationId, valuations, term) = any.asInstanceOf[(Long, VisitTermVariant, AvailableVisitsTermPresentation)]
          ReservationRequest(
            clinicId = term.clinic.id,
            doctorId = term.doctor.id,
            payerData = valuations.valuationDetail.payerData,
            roomId = term.roomId,
            serviceId = term.serviceId,
            startDateTime = term.visitDate.startDateTime,
            temporaryReservationId = tmpReservationId
          )
        }
      }

    implicit val AvailableVisitsTermPresentationToValuationRequestConverter:
      ObjectConverter[AvailableVisitsTermPresentation, ValuationsRequest] =
      new ObjectConverter[AvailableVisitsTermPresentation, ValuationsRequest] {
        override def convert[Z <: AvailableVisitsTermPresentation](term: Z): ValuationsRequest = {
          ValuationsRequest(
            clinicId = term.clinic.id,
            doctorId = term.doctor.id,
            payerDetailsList = term.payerDetailsList,
            referralRequiredByService = term.referralRequiredByService,
            roomId = term.roomId,
            serviceId = term.serviceId,
            startDateTime = term.visitDate.startDateTime
          )
        }
      }

    implicit val HistoryToIdNameConverter: CollectionConverter[History, IdName] = new CollectionConverter[History, IdName] {
      override def convert[Z <: History, Col[X] <: Iterable[X]](col: Col[Z])(implicit bf: CanBuildFrom[Col[Z], IdName, Col[IdName]]): Col[IdName] = {
        col.map(history => IdName(history.id, history.name))(collection.breakOut)
      }
    }
  }

  object MessageExtractors {

    object Text {
      def unapply(msg: Message): Option[String] = msg.text
    }

    object TextOpt {
      def unapply(msg: Message): Option[Option[String]] = Some(msg.text)
    }

  }

  object DateTimeUtil {
    private val DateFormat: Locale => DateTimeFormatter = locale => DateTimeFormatter.ofPattern("dd MMM yyyy", locale)

    private val DateTimeFormat: Locale => DateTimeFormatter = locale => DateTimeFormatter.ofPattern("EEE',' dd MMM yyyy',' HH:mm", locale)

    def formatDate(date: ZonedDateTime, locale: Locale): String = date.format(DateFormat(locale))

    def formatDateTime(date: ZonedDateTime, locale: Locale): String = date.format(DateTimeFormat(locale))

    private val EpochMinutesTillBeginOf2018: Long = epochMinutes(ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))

    def epochMinutes(time: ZonedDateTime): Long = time.toInstant.getEpochSecond / 60

    def minutesSinceBeginOf2018(time: ZonedDateTime): Long = epochMinutes(time) - EpochMinutesTillBeginOf2018
  }

}