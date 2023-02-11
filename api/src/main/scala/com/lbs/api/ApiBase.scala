package com.lbs.api

import com.lbs.api.http.Session
import com.lbs.api.http.headers._
import scalaj.http.{BaseHttp, HttpRequest}

import java.net.HttpCookie

object ApiHttp
    extends BaseHttp(
      userAgent =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"
    )

trait ApiBase {
  private val CommonHeaders =
    Map(
      Host -> "portalpacjenta.luxmed.pl",
      Origin -> "https://portalpacjenta.luxmed.pl",
      `Custom-User-Agent` -> "PatientPortal; 4.21.0; 12345678-3F3C-2C63-375B-BBA12392F44C; iOS; 16.3.0; iPhone14,5",
      `User-Agent` -> "Mozilla/5.0 (iPhone; CPU iPhone OS 16_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148",
      Accept -> "application/json, text/plain, */*",
      `Accept-Encoding` -> "gzip, deflate, br",
      `Accept-Language` -> "pl;q=1.0, pl;q=0.9, en;q=0.8"
    )

  protected def httpUnauthorized(url: String): HttpRequest = {
    ApiHttp(s"https://portalpacjenta.luxmed.pl/PatientPortalMobileAPI/api/$url")
      .headers(CommonHeaders)
  }

  protected def http(url: String, session: Session): HttpRequest = {
    ApiHttp(s"https://portalpacjenta.luxmed.pl/PatientPortalMobileAPI/api/$url")
      .headers(CommonHeaders)
      .cookies(session.cookies)
      .header(Authorization, s"${session.tokenType} ${session.accessToken}")
  }

  protected def httpNewApi(url: String, session: Session, cookiesMaybe: Option[Seq[HttpCookie]] = None): HttpRequest = {
    val req = ApiHttp(s"https://portalpacjenta.luxmed.pl/PatientPortal/$url")
      .headers(CommonHeaders)
      .header(Authorization, session.accessToken)
    cookiesMaybe.map(cookies => req.cookies(cookies)).getOrElse(req.cookies(session.cookies))
  }

  protected def httpNewApi(url: String, cookies: IndexedSeq[HttpCookie]): HttpRequest = {
    ApiHttp(s"https://portalpacjenta.luxmed.pl/PatientPortal/$url")
      .headers(CommonHeaders)
      .cookies(cookies)
  }
}
