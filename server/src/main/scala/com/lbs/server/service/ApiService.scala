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
          (clinicId.isEmpty || clinicId.contains(term.term.clinicId)) &&
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
        .map(_.events.filter(_.status == "Realized"))
    }

  def reserved(
    accountId: Long,
    fromDate: LocalDateTime = LocalDateTime.now(),
    toDate: LocalDateTime = LocalDateTime.now().plusMonths(3)
  ): ThrowableOr[List[Event]] =
    withSession(accountId) { session =>
      luxmedApi
        .events(session, fromDate.atZone(DateTimeUtil.Zone), toDate.atZone(DateTimeUtil.Zone))
        .map(_.events.filter(_.status == "Reserved"))
    }

  def deleteReservation(accountId: Long, reservationId: Long): ThrowableOr[HttpResponse[String]] =
    withSession(accountId) { session =>
      luxmedApi.reservationDelete(session, reservationId)
    }

  override def fullLogin(username: String, encryptedPassword: String): ThrowableOr[Session] = {
    val password = textEncryptor.decrypt(encryptedPassword)
    for {
      r1 <- luxmedApi.login(username, password)
      tmpSession = Session(r1.body.accessToken, r1.body.accessToken, r1.cookies)
      r2 <- luxmedApi.loginToApp(tmpSession)
      cookies = r1.cookies ++ r2.cookies
      accessToken = r1.body.accessToken
      tokenType = r1.body.tokenType
    } yield Session(accessToken, tokenType, cookies)
  }

  def getXsrfToken(accountId: Long): ThrowableOr[XsrfToken] = {
    withSession(accountId) { session =>
      luxmedApi.getForgeryToken(session).map(ft => XsrfToken(ft.body.token, ft.cookies))
    }
  }
}
