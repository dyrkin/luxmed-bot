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
package com.lbs.api

import java.time.ZonedDateTime

import com.lbs.api.json.model._
import scalaj.http.HttpResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions


object LuxmedApiAsync {

  private val syncApi = LuxmedApi

  def login(username: String, password: String, clientId: String = "iPhone")(implicit ec: ExecutionContext): Future[LoginResponse] = {
    async(syncApi.login(username, password, clientId))
  }

  def refreshToken(refreshToken: String, clientId: String = "iPhone")(implicit ec: ExecutionContext): Future[LoginResponse] = {
    async(syncApi.refreshToken(refreshToken, clientId))
  }

  def reservedVisits(accessToken: String, tokenType: String, fromDate: ZonedDateTime = ZonedDateTime.now(),
                     toDate: ZonedDateTime = ZonedDateTime.now().plusMonths(3))(implicit ec: ExecutionContext): Future[ReservedVisitsResponse] = {
    async(syncApi.reservedVisits(accessToken, tokenType, fromDate, toDate))
  }

  def visitsHistory(accessToken: String, tokenType: String, fromDate: ZonedDateTime = ZonedDateTime.now().minusYears(1),
                    toDate: ZonedDateTime, page: Int = 1, pageSize: Int = 100)(implicit ec: ExecutionContext): Future[VisitsHistoryResponse] = {
    async(syncApi.visitsHistory(accessToken, tokenType, fromDate, toDate, page, pageSize))
  }

  def reservationFilter(accessToken: String, tokenType: String, fromDate: ZonedDateTime = ZonedDateTime.now(),
                        toDate: Option[ZonedDateTime] = None, cityId: Option[Long] = None,
                        serviceId: Option[Long] = None)(implicit ec: ExecutionContext): Future[ReservationFilterResponse] = {
    async(syncApi.reservationFilter(accessToken, tokenType, fromDate, toDate, cityId, serviceId))
  }

  def availableTerms(accessToken: String, tokenType: String, payerId: Long, cityId: Long, clinicId: Option[Long], serviceId: Long, doctorId: Option[Long],
                     fromDate: ZonedDateTime = ZonedDateTime.now(), toDate: Option[ZonedDateTime] = None, timeOfDay: Int = 0,
                     languageId: Long = 10, findFirstFreeTerm: Boolean = true)(implicit ec: ExecutionContext): Future[AvailableTermsResponse] = {
    async(syncApi.availableTerms(accessToken, tokenType, cityId, payerId, clinicId, serviceId, doctorId, fromDate, toDate, timeOfDay, languageId, findFirstFreeTerm))
  }

  def temporaryReservation(accessToken: String, tokenType: String, temporaryReservationRequest: TemporaryReservationRequest)(implicit ec: ExecutionContext): Future[TemporaryReservationResponse] = {
    async(syncApi.temporaryReservation(accessToken, tokenType, temporaryReservationRequest))
  }

  def deleteTemporaryReservation(accessToken: String, tokenType: String, temporaryReservationId: Long)(implicit ec: ExecutionContext): Future[HttpResponse[String]] = {
    async(syncApi.deleteTemporaryReservation(accessToken, tokenType, temporaryReservationId))
  }

  def valuations(accessToken: String, tokenType: String, valuationsRequest: ValuationsRequest)(implicit ec: ExecutionContext): Future[ValuationsResponse] = {
    async(syncApi.valuations(accessToken, tokenType, valuationsRequest))
  }

  def reservation(accessToken: String, tokenType: String, reservationRequest: ReservationRequest)(implicit ec: ExecutionContext): Future[ReservationResponse] = {
    async(syncApi.reservation(accessToken, tokenType, reservationRequest))
  }

  def deleteReservation(accessToken: String, tokenType: String, reservationId: Long)(implicit ec: ExecutionContext): Future[HttpResponse[String]] = {
    async(syncApi.deleteReservation(accessToken, tokenType, reservationId))
  }

  private def async[T](f: => Either[Throwable, T])(implicit ec: ExecutionContext) = {
    Future(f).flatMap {
      case Right(r) => Future.successful(r)
      case Left(ex) => Future.failed(ex)
    }
  }
}