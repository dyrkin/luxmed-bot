package com.lbs.api

import cats.implicits.toFunctorOps
import com.lbs.api.http._
import com.lbs.api.http.headers._
import com.lbs.api.json.JsonSerializer.extensions._
import com.lbs.api.json.model._
import scalaj.http.{HttpRequest, HttpResponse}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZonedDateTime}

class LuxmedApi[F[_]: ThrowableMonad] extends ApiBase {

  private val dateFormatNewPortal = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  private val dateFormatEvents = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")

  def login(username: String, password: String, clientId: String = "iPhone"): F[HttpResponse[LoginResponse]] = {
    val request = httpUnauthorized("token")
      .header(`Content-Type`, "application/x-www-form-urlencoded")
      .header(`x-api-client-identifier`, clientId)
      .param("client_id", clientId)
      .param("grant_type", "password")
      .param("password", password)
      .param("username", username)
    post[LoginResponse](request)
  }

  def loginToApp(session: Session): F[HttpResponse[Unit]] = {
    val request = httpNewApi("Account/LogInToApp?app=search&lang=pl&client=2&paymentSupported=true", session)
      .header(Authorization, session.accessToken)
    getVoid(request)
  }

  def getForgeryToken(session: Session): F[HttpResponse[ForgeryTokenResponse]] = {
    val request = httpNewApi("security/getforgerytoken", session)
    get[ForgeryTokenResponse](request)
  }

  def events(
    session: Session,
    fromDate: ZonedDateTime = ZonedDateTime.now().minusYears(1),
    toDate: ZonedDateTime = ZonedDateTime.now()
  ): F[EventsResponse] = {
    val request = http("Events", session)
      .header(`Content-Type`, "application/json")
      .param("filter.filterDateFrom", dateFormatEvents.format(fromDate))
      .param("filter.filterDateTo", dateFormatEvents.format(toDate))
    get[EventsResponse](request).map(_.body)
  }

  def dictionaryCities(session: Session): F[List[DictionaryCity]] = {
    val request = httpNewApi("NewPortal/Dictionary/cities", session).header(`Content-Type`, "application/json")
    getList[DictionaryCity](request).map(_.body)
  }

  def dictionaryServiceVariants(session: Session): F[List[DictionaryServiceVariants]] = {
    val request =
      httpNewApi("NewPortal/Dictionary/serviceVariantsGroups", session).header(`Content-Type`, "application/json")
    getList[DictionaryServiceVariants](request).map(_.body)
  }

  def dictionaryFacilitiesAndDoctors(
    session: Session,
    cityId: Option[Long],
    serviceVariantId: Option[Long]
  ): F[FacilitiesAndDoctors] = {
    val request = httpNewApi("NewPortal/Dictionary/facilitiesAndDoctors", session)
      .header(`Content-Type`, "application/json")
      .param("cityId", cityId.map(_.toString))
      .param("serviceVariantId", serviceVariantId.map(_.toString))
    get[FacilitiesAndDoctors](request).map(_.body)
  }

  def termsIndex(
    session: Session,
    cityId: Long,
    clinicId: Option[Long],
    serviceId: Long,
    doctorId: Option[Long],
    fromDate: LocalDateTime = LocalDateTime.now(),
    toDate: LocalDateTime,
    languageId: Long = 10
  ): F[TermsIndexResponse] = {
    val request = httpNewApi("NewPortal/terms/index", session)
      .header(`Content-Type`, "application/json")
      .param("searchPlace.id", cityId.toString)
      .param("searchPlace.type", 0.toString)
      .param("serviceVariantId", serviceId.toString)
      .param("languageId", languageId.toString)
      .param("searchDateFrom", dateFormatNewPortal.format(fromDate))
      .param("searchDateTo", dateFormatNewPortal.format(toDate))
      .param("searchDatePreset", 14.toString)
      .param("facilitiesIds", clinicId.map(_.toString))
      .param("doctorsIds", doctorId.map(_.toString))
      .param("nextSearch", false.toString)
      .param("searchByMedicalSpecialist", false.toString)
      .param("delocalized", false.toString)
    get[TermsIndexResponse](request).map(_.body)
  }

  def reservationLockterm(
    session: Session,
    xsrfToken: XsrfToken,
    reservationLocktermRequest: ReservationLocktermRequest
  ): F[ReservationLocktermResponse] = {
    val request = httpNewApi("NewPortal/reservation/lockterm", session, Some(session.cookies ++ xsrfToken.cookies))
      .header(`Content-Type`, "application/json")
      .header(`xsrf-token`, xsrfToken.token)
    post[ReservationLocktermResponse](request, bodyOpt = Some(reservationLocktermRequest)).map(_.body)
  }

  def deleteTemporaryReservation(session: Session, xsrfToken: XsrfToken, temporaryReservationId: Long): F[Unit] = {
    val request = httpNewApi(
      s"NewPortal/reservation/releaseterm?reservationId=$temporaryReservationId",
      session,
      Some(session.cookies ++ xsrfToken.cookies)
    )
      .header(`Content-Type`, "application/json")
      .header(`xsrf-token`, xsrfToken.token)
    postVoid(request, bodyOpt = Some(Empty()))
  }

  def reservationConfirm(
    session: Session,
    xsrfToken: XsrfToken,
    reservationConfirmRequest: ReservationConfirmRequest
  ): F[ReservationConfirmResponse] = {
    val request = httpNewApi("NewPortal/reservation/confirm", session, Some(session.cookies ++ xsrfToken.cookies))
      .header(`Content-Type`, "application/json")
      .header(`xsrf-token`, xsrfToken.token)
    post[ReservationConfirmResponse](request, bodyOpt = Some(reservationConfirmRequest)).map(_.body)
  }

  def reservationChangeTerm(
    session: Session,
    xsrfToken: XsrfToken,
    reservationChangetermRequest: ReservationChangetermRequest
  ): F[ReservationConfirmResponse] = {
    val request = httpNewApi("NewPortal/reservation/changeterm", session, Some(session.cookies ++ xsrfToken.cookies))
      .header(`Content-Type`, "application/json")
      .header(`xsrf-token`, xsrfToken.token)
    post[ReservationConfirmResponse](request, bodyOpt = Some(reservationChangetermRequest)).map(_.body)
  }

  def reservationDelete(session: Session, reservationId: Long): F[HttpResponse[String]] = {
    val request = http(s"events/Visit/$reservationId", session).header(`Content-Type`, "application/json")
    delete(request)
  }

  private def get[T <: SerializableJsonObject](
    request: HttpRequest
  )(implicit mf: scala.reflect.Manifest[T]): F[HttpResponse[T]] = {
    request.invoke.map(r => r.copy(body = r.body.as[T]))
  }

  private def getList[T <: SerializableJsonObject](
    request: HttpRequest
  )(implicit mf: scala.reflect.Manifest[T]): F[HttpResponse[List[T]]] = {
    request.invoke.map(r => r.copy(body = r.body.asList[T]))
  }

  private def getVoid[T <: SerializableJsonObject](
    request: HttpRequest
  )(implicit mf: scala.reflect.Manifest[T]): F[HttpResponse[Unit]] = {
    request.invoke.map(r => r.copy(body = {}))
  }

  private def post[T <: SerializableJsonObject](request: HttpRequest, bodyOpt: Option[SerializableJsonObject] = None)(
    implicit mf: scala.reflect.Manifest[T]
  ): F[HttpResponse[T]] = {
    val postRequest = bodyOpt match {
      case Some(body) => request.postData(body.asJson)
      case None       => request.postForm
    }
    postRequest.invoke.map(r => r.copy(body = r.body.as[T]))
  }

  private def postVoid(request: HttpRequest, bodyOpt: Option[SerializableJsonObject] = None): F[Unit] = {
    val postRequest = bodyOpt match {
      case Some(body) => request.postData(body.asJson)
      case None       => request.postForm
    }
    postRequest.invoke.void
  }

  private def delete(request: HttpRequest): F[HttpResponse[String]] = {
    request.postForm.method("DELETE").invoke
  }

}
