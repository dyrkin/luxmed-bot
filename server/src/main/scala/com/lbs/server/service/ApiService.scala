package com.lbs.server.service

import cats.instances.either.*
import com.lbs.api.LuxmedApi
import com.lbs.api.http.{LuxmedResponse, Session}
import com.lbs.api.json.model.*
import com.lbs.server.ThrowableOr
import com.lbs.server.util.DateTimeUtil
import org.jasypt.util.text.TextEncryptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.net.HttpCookie
import java.time.{LocalDateTime, LocalTime}
import scala.compiletime.uninitialized

@Service
class ApiService extends SessionSupport {

  @Autowired
  protected var dataService: DataService = uninitialized
  @Autowired
  private var textEncryptor: TextEncryptor = uninitialized

  private val luxmedApi = new LuxmedApi[ThrowableOr]

  def getAllCities(accountId: Long): ThrowableOr[List[DictionaryCity]] =
    withSession(accountId) { session =>
      luxmedApi.dictionaryCities(session)

    }

  def getAllFacilities(accountId: Long, cityId: Long, serviceVariantId: Long): ThrowableOr[List[IdName]] =
    withSession(accountId) { session =>
      luxmedApi
        .dictionaryFacilitiesAndDoctors(session, cityId = Some(cityId), serviceVariantId = Some(serviceVariantId))
        .map(_.facilities)
    }

  def getAllServices(accountId: Long): ThrowableOr[List[DictionaryServiceVariants]] =
    withSession(accountId) { session =>
      luxmedApi.dictionaryServiceVariants(session).map(s => s.flatMap(_.flatten.filterNot(_.children.nonEmpty)))
    }

  def getAllDoctors(accountId: Long, cityId: Long, serviceVariantId: Long): ThrowableOr[List[Doctor]] =
    withSession(accountId) { session =>
      luxmedApi
        .dictionaryFacilitiesAndDoctors(session, cityId = Some(cityId), serviceVariantId = Some(serviceVariantId))
        .map(_.doctors)
    }

  def getAvailableTerms(
    accountId: Long,
    cityId: Long,
    clinicId: Option[Long],
    serviceId: Long,
    doctorId: Option[Long],
    fromDate: LocalDateTime,
    toDate: LocalDateTime,
    timeFrom: LocalTime,
    timeTo: LocalTime,
    languageId: Long = 10
  ): ThrowableOr[List[TermExt]] =
    withSession(accountId) { session =>
      val termsEither = luxmedApi
        .termsIndex(session, cityId, clinicId, serviceId, doctorId, fromDate, toDate, languageId = languageId)
        .map(termsIndexResponse =>
          termsIndexResponse.termsForService.termsForDays
            .flatMap(_.terms.map(term => TermExt(termsIndexResponse.termsForService.additionalData, term)))
        )
      termsEither.map { terms =>
        terms.filter { term =>
          val time = term.term.dateTimeFrom.get.toLocalTime
          val date = term.term.dateTimeFrom.get
          (doctorId.isEmpty || doctorId.contains(term.term.doctor.id)) &&
          (clinicId.isEmpty || clinicId.contains(term.term.clinicGroupId)) &&
          (time == timeFrom || time == timeTo || (time.isAfter(timeFrom) && time.isBefore(timeTo))) &&
          (date == fromDate || date == toDate || (date.isAfter(fromDate) && date.isBefore(toDate)))
        }
      }
    }

  def getRehabReferrals(accountId: Long): ThrowableOr[List[Referral]] =
    withSession(accountId) { session =>
      luxmedApi.getReferrals(session).map(r =>
        (r.planned ++ r.unplanned).filter(ref =>
          ref.serviceVariant.name == "Rehabilitacja" && ref.referralStatus == "ToBook"
        )
      )
    }

  def getServiceReferral(accountId: Long, serviceInstanceId: Long): ThrowableOr[ServiceReferralResponse] =
    withSession(accountId) { session =>
      luxmedApi.getServiceReferral(session, serviceInstanceId)
    }

  def getRehabFacilities(accountId: Long, serviceVariantId: Long): ThrowableOr[RehabFacilitiesResponse] =
    withSession(accountId) { session =>
      luxmedApi.getRehabFacilities(session, serviceVariantId)
    }

  def getAvailableRehabTerms(
    accountId: Long,
    cityId: Long,
    serviceVariantId: Long,
    referralId: Long,
    referralTypeId: Int,
    fromDate: LocalDateTime,
    toDate: LocalDateTime,
    timeFrom: LocalTime,
    timeTo: LocalTime,
    facilityId: Option[Long] = None,
    doctorId: Option[Long] = None
  ): ThrowableOr[List[TermExt]] =
    withSession(accountId) { session =>
      luxmedApi.rehabTermsIndex(session, cityId, serviceVariantId, referralId, referralTypeId,
        fromDate, toDate, facilityId, doctorId).map { response =>
        response.termsForService.termsForDays
          .flatMap(_.terms.map(term => TermExt(response.termsForService.additionalData, term)))
          .filter { term =>
            val time = term.term.dateTimeFrom.get.toLocalTime
            (doctorId.isEmpty || doctorId.contains(term.term.doctor.id)) &&
            (time == timeFrom || time == timeTo || (time.isAfter(timeFrom) && time.isBefore(timeTo)))
          }
      }
    }

  def reservationLockterm(
    accountId: Long,
    xsrfToken: XsrfToken,
    reservationLocktermRequest: ReservationLocktermRequest
  ): ThrowableOr[ReservationLocktermResponse] =
    withSession(accountId) { session =>
      luxmedApi.reservationLockterm(session, xsrfToken, reservationLocktermRequest)
    }

  def deleteTemporaryReservation(
    accountId: Long,
    xsrfToken: XsrfToken,
    temporaryReservationId: Long
  ): ThrowableOr[Unit] =
    withSession(accountId) { session =>
      luxmedApi.deleteTemporaryReservation(session, xsrfToken, temporaryReservationId)
    }

  def reservationConfirm(
    accountId: Long,
    xsrfToken: XsrfToken,
    reservationConfirmRequest: ReservationConfirmRequest
  ): ThrowableOr[ReservationConfirmResponse] =
    withSession(accountId) { session =>
      luxmedApi.reservationConfirm(session, xsrfToken, reservationConfirmRequest)
    }

  def reservationChangeTerm(
    accountId: Long,
    xsrfToken: XsrfToken,
    reservationChangetermRequest: ReservationChangetermRequest
  ): ThrowableOr[ReservationConfirmResponse] =
    withSession(accountId) { session =>
      luxmedApi.reservationChangeTerm(session, xsrfToken, reservationChangetermRequest)
    }

  def history(
    accountId: Long,
    fromDate: LocalDateTime = LocalDateTime.now().minusYears(1),
    toDate: LocalDateTime = LocalDateTime.now()
  ): ThrowableOr[List[Event]] =
    withSession(accountId) { session =>
      luxmedApi
        .events(session, fromDate.atZone(DateTimeUtil.Zone), toDate.atZone(DateTimeUtil.Zone))
        .map(_.events.filter(_.status == "Realized").sortBy(_.date).reverse)
    }

  def reserved(
    accountId: Long,
    fromDate: LocalDateTime = LocalDateTime.now(),
    toDate: LocalDateTime = LocalDateTime.now().plusMonths(3)
  ): ThrowableOr[List[Event]] =
    withSession(accountId) { session =>
      luxmedApi
        .events(session, fromDate.atZone(DateTimeUtil.Zone), toDate.atZone(DateTimeUtil.Zone))
        .map(_.events.filter(_.status == "Reserved").sortBy(_.date))
    }

  def deleteReservation(accountId: Long, reservationId: Long): ThrowableOr[LuxmedResponse[String]] =
    withSession(accountId) { session =>
      luxmedApi.reservationDelete(session, reservationId)
    }

  private def joinCookies(cookies: Seq[HttpCookie]*): Seq[HttpCookie] = {
    cookies.map(_.map(v => v.getName -> v).toMap).reduce(_ ++ _).values.toSeq
  }

  override def fullLogin(username: String, encryptedPassword: String, secondAttempt: Boolean = false): ThrowableOr[Session] = {
    val password = textEncryptor.decrypt(encryptedPassword)
    val clientId = java.util.UUID.randomUUID.toString
    try {
      for {
        r1 <- luxmedApi.login(username, password, clientId)
        tmpSession = Session(r1.body.accessToken, r1.body.tokenType, "", r1.cookies)
        r2 <- luxmedApi.loginToApp(tmpSession)
        cookies = joinCookies(r1.cookies, r2.cookies, Seq(new HttpCookie("GlobalLang", "pl")))
        accessToken = r1.body.accessToken
        tokenType = r1.body.tokenType
        r3 <- luxmedApi.getReservationPage(tmpSession, cookies)
        allCookies = joinCookies(cookies, r3.cookies)
        jwtToken = allCookies
          .find(_.getName == "Authorization-Token")
          .map(_.getValue)
          .orElse(r2.header("authorization-token"))
          .orElse(r3.header("authorization-token"))
          .orElse(scala.util.Try(extractAccessTokenFromLoginToApp(r2.body)).toOption)
          .orElse(scala.util.Try(extractAccessTokenFromReservationPage(r3.body)).toOption)
          .orElse {
            logger.warn(s"Authorization-Token not found in cookies, headers, or page body; falling back to OAuth accessToken")
            logger.warn(s"loginToApp body (first 500): ${r2.body.take(500)}")
            logger.warn(s"reservation page body (first 500): ${r3.body.take(500)}")
            Some(accessToken)
          }
          .get
        _ = logger.info(s"Login successful, JWT token obtained (length=${jwtToken.length})")
      } yield Session(accessToken, tokenType, jwtToken, allCookies)
    } catch {
      case e: Exception if !secondAttempt =>
        logger.warn("couldn't login from the first attempt. trying one more time after a short pause", e)
        Thread.sleep(3000)
        fullLogin(username, encryptedPassword, secondAttempt = true)
      case e: Exception => Left(e)
    }
  }

  def getXsrfToken(accountId: Long): ThrowableOr[XsrfToken] = {
    withSession(accountId) { session =>
      luxmedApi.getForgeryToken(session).map(ft => XsrfToken(ft.body.token, ft.cookies))
    }
  }

  private def extractAccessTokenFromLoginToApp(body: String): String = {
    // Try to extract token from loginToApp response body (JSON or other formats)
    val patterns = Seq(
      """"[Aa]uthorization[_-]?[Tt]oken"\s*:\s*"([^"]+)"""".r,
      """"access_token"\s*:\s*"([^"]+)"""".r,
      """"token"\s*:\s*"([^"]+)"""".r,
      """'Authorization',\s*'([^']+)'""".r,
      """[Aa]uthorization[_-]?[Tt]oken=([A-Za-z0-9._\-]+)""".r
    )
    patterns.flatMap(_.findFirstMatchIn(body).map(_.group(1))).headOption
      .getOrElse(throw new java.lang.RuntimeException(s"Unable to extract authorization token from loginToApp response"))
  }

  private def extractAccessTokenFromReservationPage(responsePage: String): String = {
    val patterns = Seq(
      """'Authorization',\s*'([^']+)'""".r,
      """"Authorization",\s*"([^"]+)"""".r,
      """"[Aa]uthorization[_-]?[Tt]oken"\s*:\s*"([^"]+)"""".r,
      """[Aa]uthorization[_-]?[Tt]oken=([A-Za-z0-9._\-]+)""".r
    )
    patterns.flatMap(_.findFirstMatchIn(responsePage).map(_.group(1))).headOption
      .getOrElse(throw new java.lang.RuntimeException(s"Unable to extract authorization token from reservation page"))
  }

  private def extractAuthorizationTokenFromCookies(response: LuxmedResponse[?]): String = {
    response.cookies
      .find(_.getName == "Authorization-Token")
      .map(_.getValue)
      .getOrElse(throw new RuntimeException("Authorization-Token cookie not found in response"))
  }

}
