package com.lbs.api

import com.lbs.api.http.Session
import com.lbs.api.http.headers._
import scalaj.http.{BaseHttp, HttpRequest}

import java.net.HttpCookie

object ApiHttp
    extends BaseHttp(
      userAgent =
        "okhttp/4.9.0"
    )

trait ApiBase {
  private val CommonHeaders =
    Map(
      Host -> "portalpacjenta.luxmed.pl",
      Origin -> "https://portalpacjenta.luxmed.pl",
      Accept -> "application/json, text/plain, */*",
      `Accept-Encoding` -> "gzip, deflate, br",
      `Accept-Language` -> "pl;q=1.0, pl;q=0.9, en;q=0.8"
    )

  private val OldApiHeaders =
    Map(
      `X-Api-Client-Identifier` -> "Android",
      `Custom-User-Agent` -> "Patient Portal; 4.31.0; 12345678-54b1-4c07-ba09-a3db8daea24b; Android; 33; Samsung Galaxy S23",
      `User-Agent` -> "okhttp/4.9.0"
    )

  private val NewApiHeaders =
    Map(
      `Custom-User-Agent` -> "Patient Portal; 4.31.0; 12345678-54b1-4c07-ba09-a3db8daea24b; Android; 33; Samsung Galaxy S23",
      `User-Agent` -> "Mozilla/5.0 (Linux; Android 13; Galaxy S23 Build/TQ2B.230505.005.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/101.0.4951.61 Safari/537.36"
    )

  protected def httpUnauthorized(url: String): HttpRequest = {
    ApiHttp(s"https://portalpacjenta.luxmed.pl/PatientPortalMobileAPI/api/$url")
      .headers(CommonHeaders ++ OldApiHeaders)
  }

  protected def http(url: String, session: Session): HttpRequest = {
    ApiHttp(s"https://portalpacjenta.luxmed.pl/PatientPortalMobileAPI/api/$url")
      .headers(CommonHeaders ++ OldApiHeaders)
      .cookies(session.cookies)
      .header(Authorization, s"${session.tokenType} ${session.accessToken}")
  }

  protected def httpNewApi(url: String, session: Session, cookiesMaybe: Option[Seq[HttpCookie]] = None): HttpRequest = {
    val req = ApiHttp(s"https://portalpacjenta.luxmed.pl/PatientPortal/$url")
      .headers(CommonHeaders ++ NewApiHeaders)
      .header(Authorization, s"Bearer ${session.jwtToken}")
    cookiesMaybe.map(cookies => req.cookies(cookies)).getOrElse(req.cookies(session.cookies))
  }

  protected def httpNewApiWithOldToken(url: String, session: Session, cookiesMaybe: Option[Seq[HttpCookie]] = None): HttpRequest = {
    val req = ApiHttp(s"https://portalpacjenta.luxmed.pl/PatientPortal/$url")
      .headers(CommonHeaders ++ NewApiHeaders)
      .header(`X-Requested-With`, "pl.luxmed.pp")
      .header(Authorization, session.accessToken)
    cookiesMaybe.map(cookies => req.cookies(cookies)).getOrElse(req.cookies(session.cookies))
  }
}
