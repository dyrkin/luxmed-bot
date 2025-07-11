package com.lbs.server.service

import cats.instances.either._
import com.lbs.api.LuxmedApi
import com.lbs.api.http.Session
import com.lbs.api.json.model._
import com.lbs.server.ThrowableOr
import com.lbs.server.util.DateTimeUtil
import org.jasypt.util.text.TextEncryptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import scalaj.http.HttpResponse

import java.net.HttpCookie
import java.time.{LocalDateTime, LocalTime}

@Service
class ApiService extends SessionSupport {

  @Autowired
  protected var dataService: DataService = _
  @Autowired
  private var textEncryptor: TextEncryptor = _

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

  def deleteReservation(accountId: Long, reservationId: Long): ThrowableOr[HttpResponse[String]] =
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
        tmpSession = Session(r1.body.accessToken, r1.body.accessToken, "", r1.cookies)
        r2 <- luxmedApi.loginToApp(tmpSession)
        jwtToken = extractAuthorizationTokenFromCookies(r2)
        cookies = joinCookies(r1.cookies, r2.cookies, Seq(new HttpCookie("GlobalLang", "pl")))
        accessToken = r1.body.accessToken
        tokenType = r1.body.tokenType
        r3 <- luxmedApi.getReservationPage(tmpSession, cookies)
      } yield Session(accessToken, tokenType, jwtToken, joinCookies(cookies, r3.cookies))
    } catch {
      case e: Exception if !secondAttempt => {
        logger.warn("couldn't login from the first attempt. trying one more time after a short pause", e)
        Thread.sleep(3000)
        fullLogin(username, encryptedPassword, secondAttempt = true)
      }
      case e: Exception => Left(e)
    }
  }

  def getXsrfToken(accountId: Long): ThrowableOr[XsrfToken] = {
    withSession(accountId) { session =>
      luxmedApi.getForgeryToken(session).map(ft => XsrfToken(ft.body.token, ft.cookies))
    }
  }

  private def extractAccessTokenFromReservationPage(responsePage: String): String = {
    val accessTokenRegex = """(?s).*'Authorization', '(.+?)'.*""".r
    responsePage match {
      case accessTokenRegex(token) => token
      case _ => throw new java.lang.RuntimeException(s"Unable to extract authorization token from reservation page")
    }
  }

  private def extractAuthorizationTokenFromCookies(response: HttpResponse[_]): String = {
    response.headers.get("Set-Cookie") match {
      case Some(cookieHeaders) =>
        cookieHeaders
          .find(_.startsWith("Authorization-Token="))
          .flatMap { header =>
            header.split(";").headOption.flatMap {
              _.split("=", 2) match {
                case Array(_, value) => Some(value)
                case _ => None
              }
            }
          }
          .getOrElse(throw new RuntimeException("Authorization-Token cookie not found in response headers"))
      case None =>
        throw new RuntimeException("No Set-Cookie headers found in response")
    }
  }
}
