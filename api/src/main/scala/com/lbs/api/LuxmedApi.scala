package com.lbs.api

import cats.implicits.toFunctorOps
import com.lbs.api.http.*
import com.lbs.api.http.headers.*
import com.lbs.api.json.JsonSerializer.extensions.*
import com.lbs.api.json.model.*
import com.lbs.api.json.model.JsonCodecs.given
import io.circe.Decoder
import sttp.client3.{HttpClientSyncBackend, *}
import sttp.model.{Method, Uri}

import java.net.HttpCookie
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZonedDateTime}

class LuxmedApi[F[_]: ThrowableMonad] extends ApiBase {

  private given backend: SttpBackend[Identity, Any] = HttpClientSyncBackend()

  private val dateFormatNewPortal = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  private val dateFormatEvents    = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")

  def login(username: String, password: String, clientId: String = "Android"): F[LuxmedResponse[LoginResponse]] = {
    val request = httpUnauthorized("token")
      .method(Method.POST, Uri.unsafeParse(s"$oldApiBaseUrl/token"))
      .body(Map(
        "client_id"  -> clientId,
        "grant_type" -> "password",
        "password"   -> password,
        "username"   -> username
      ))
    request.invoke.map(r => r.copy(body = r.body.as[LoginResponse]))
  }

  def loginToApp(session: Session): F[LuxmedResponse[String]] = {
    val request = httpNewApiWithOldToken("Account/LogInToApp?app=search&client=3&lang=pl", session)
    getString(request)
  }

  def getForgeryToken(session: Session): F[LuxmedResponse[ForgeryTokenResponse]] = {
    val request = httpNewApi("security/getforgerytoken", session)
    get[ForgeryTokenResponse](request)
  }

  def getReservationPage(session: Session, cookies: Seq[HttpCookie]): F[LuxmedResponse[String]] = {
    val request = httpNewApiWithOldToken("NewPortal/Page/Reservation", session, Some(cookies))
    getString(request)
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
      .param("searchPlace.id", cityId.toString)
      .param("searchPlace.type", 0.toString)
      .param("serviceVariantId", serviceId.toString)
      .param("languageId", languageId.toString)
      .param("searchDateFrom", dateFormatNewPortal.format(fromDate))
      .param("searchDateTo", dateFormatNewPortal.format(toDate))
      .param("searchDatePreset", 14.toString)
      .param("processId", java.util.UUID.randomUUID.toString)
      .param("serviceVariantSource", 0.toString)
      .param("facilitiesIds", clinicId.map(_.toString))
      .param("doctorsIds", doctorId.map(_.toString))
      .param("nextSearch", false.toString)
      .param("searchByMedicalSpecialist", false.toString)
      .param("delocalized", false.toString)
    get[TermsIndexResponse](request).map(_.body)
  }

  def getReferrals(session: Session): F[ReferralsResponse] = {
    val request = http("events/referrals", session)
      .header(`Content-Type`, "application/json")
    get[ReferralsResponse](request).map(_.body)
  }

  def getServiceReferral(session: Session, serviceInstanceId: Long): F[ServiceReferralResponse] = {
    val request = httpNewApi(
      s"NewPortal/Rehabilitation/GetServiceReferral?serviceInstanceId=$serviceInstanceId", session)
    get[ServiceReferralResponse](request).map(_.body)
  }

  def getRehabFacilities(session: Session, serviceVariantId: Long): F[RehabFacilitiesResponse] = {
    val request = httpNewApi(
      s"NewPortal/Rehabilitation/GetFacilitiesForService?serviceVariantId=$serviceVariantId", session)
    get[RehabFacilitiesResponse](request).map(_.body)
  }

  def rehabTermsIndex(
    session: Session,
    cityId: Long,
    serviceVariantId: Long,
    referralId: Long,
    referralTypeId: Int = 1,
    fromDate: LocalDateTime,
    toDate: LocalDateTime,
    facilitiesIds: Option[Long] = None,
    doctorId: Option[Long] = None,
    languageId: Long = 10,
    isNextSearch: Boolean = false
  ): F[TermsIndexResponse] = {
    val request = httpNewApi("NewPortal/terms/index", session)
      .param("searchPlace.id", cityId.toString)
      .param("searchPlace.type", "0")
      .param("serviceVariantId", serviceVariantId.toString)
      .param("languageId", languageId.toString)
      .param("searchDateFrom", dateFormatNewPortal.format(fromDate))
      .param("searchDateTo", dateFormatNewPortal.format(toDate))
      .param("searchDatePreset", "6")
      .param("referralId", referralId.toString)
      .param("referralTypeId", referralTypeId.toString)
      .param("processId", java.util.UUID.randomUUID.toString)
      .param("serviceVariantSource", "3")
      .param("facilitiesIds", facilitiesIds.map(_.toString))
      .param("doctorsIds", doctorId.map(_.toString))
      .param("nextSearch", isNextSearch.toString)
      .param("searchByMedicalSpecialist", "false")
      .param("delocalized", "false")
      .param("locationReplaced", "false")
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
    post[ReservationLocktermResponse](request, bodyOpt = Some(reservationLocktermRequest.asJson)).map(_.body)
  }

  def deleteTemporaryReservation(session: Session, xsrfToken: XsrfToken, temporaryReservationId: Long): F[Unit] = {
    val request = httpNewApi(
      s"NewPortal/reservation/releaseterm?reservationId=$temporaryReservationId",
      session,
      Some(session.cookies ++ xsrfToken.cookies)
    )
      .header(`Content-Type`, "application/json")
      .header(`xsrf-token`, xsrfToken.token)
    postVoid(request, bodyOpt = Some(Empty().asJson))
  }

  def reservationConfirm(
    session: Session,
    xsrfToken: XsrfToken,
    reservationConfirmRequest: ReservationConfirmRequest
  ): F[ReservationConfirmResponse] = {
    val request = httpNewApi("NewPortal/reservation/confirm", session, Some(session.cookies ++ xsrfToken.cookies))
      .header(`Content-Type`, "application/json")
      .header(`xsrf-token`, xsrfToken.token)
    post[ReservationConfirmResponse](request, bodyOpt = Some(reservationConfirmRequest.asJson)).map(_.body)
  }

  def reservationChangeTerm(
    session: Session,
    xsrfToken: XsrfToken,
    reservationChangetermRequest: ReservationChangetermRequest
  ): F[ReservationConfirmResponse] = {
    val request = httpNewApi("NewPortal/reservation/changeterm", session, Some(session.cookies ++ xsrfToken.cookies))
      .header(`Content-Type`, "application/json")
      .header(`xsrf-token`, xsrfToken.token)
    post[ReservationConfirmResponse](request, bodyOpt = Some(reservationChangetermRequest.asJson)).map(_.body)
  }

  def reservationDelete(session: Session, reservationId: Long): F[LuxmedResponse[String]] = {
    val request = http(s"events/Visit/$reservationId", session).header(`Content-Type`, "application/json")
    delete(request)
  }

  private def get[T <: SerializableJsonObject: Decoder](
    request: Request[String, Any]
  ): F[LuxmedResponse[T]] =
    request.invoke.map(r => r.copy(body = r.body.as[T]))

  private def getList[T <: SerializableJsonObject: Decoder](
    request: Request[String, Any]
  ): F[LuxmedResponse[List[T]]] =
    request.invoke.map(r => r.copy(body = r.body.asList[T]))

  private def post[T <: SerializableJsonObject: Decoder](
    request: Request[String, Any],
    bodyOpt: Option[String] = None
  ): F[LuxmedResponse[T]] = {
    val postRequest = bodyOpt match {
      case Some(body) => request.method(Method.POST, request.uri).body(body)
      case None       => request.method(Method.POST, request.uri)
    }
    postRequest.invoke.map(r => r.copy(body = r.body.as[T]))
  }

  private def postVoid(request: Request[String, Any], bodyOpt: Option[String] = None): F[Unit] = {
    val postRequest = bodyOpt match {
      case Some(body) => request.method(Method.POST, request.uri).body(body)
      case None       => request.method(Method.POST, request.uri)
    }
    postRequest.invoke.void
  }

  private def delete(request: Request[String, Any]): F[LuxmedResponse[String]] =
    request.method(Method.DELETE, request.uri).invoke

  private def getString(request: Request[String, Any]): F[LuxmedResponse[String]] =
    request.invoke
}
