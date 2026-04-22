package com.lbs.api

import com.lbs.api.http.Session
import com.lbs.api.http.headers.*
import sttp.client3.*
import sttp.model.Uri

import java.net.HttpCookie

trait ApiBase {
  protected def oldApiBaseUrl: String = "https://portalpacjenta.luxmed.pl/PatientPortalMobileAPI/api"
  protected def newApiBaseUrl: String = "https://portalpacjenta.luxmed.pl/PatientPortal"

  private val CommonHeaders =
    Map(
      Accept           -> "application/json, text/plain, */*",
      `Accept-Encoding` -> "gzip, deflate, br",
      `Accept-Language` -> "pl;q=1.0, pl;q=0.9, en;q=0.8"
    )

  private val OldApiHeaders =
    Map(
      `X-Api-Client-Identifier` -> "Android",
      `Custom-User-Agent`       -> "Patient Portal; 4.42.0; 12345678-54b1-4c07-ba09-a3db8daea24b; Android; 33; Samsung Galaxy S23",
      `User-Agent`              -> "okhttp/4.9.0"
    )

  private val NewApiHeaders =
    Map(
      `Custom-User-Agent` -> "Patient Portal; 4.42.0; 12345678-54b1-4c07-ba09-a3db8daea24b; Android; 33; Samsung Galaxy S23",
      `User-Agent`        -> "Mozilla/5.0 (Linux; Android 13; Galaxy S23 Build/TQ2B.230505.005.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/101.0.4951.61 Safari/537.36"
    )

  private def baseGet(url: String): Request[String, Any] =
    basicRequest.get(Uri.unsafeParse(url)).response(asStringAlways).followRedirects(false)

  protected def httpUnauthorized(url: String): Request[String, Any] =
    baseGet(s"$oldApiBaseUrl/$url")
      .headers(CommonHeaders ++ OldApiHeaders)

  protected def http(url: String, session: Session): Request[String, Any] =
    baseGet(s"$oldApiBaseUrl/$url")
      .headers(CommonHeaders ++ OldApiHeaders)
      .cookies(session.cookies.map(c => (c.getName, c.getValue))*)
      .header(Authorization, s"${session.tokenType} ${session.accessToken}")

  protected def httpNewApi(url: String, session: Session, cookiesMaybe: Option[Seq[HttpCookie]] = None): Request[String, Any] = {
    val cookies = cookiesMaybe.getOrElse(session.cookies)
    baseGet(s"$newApiBaseUrl/$url")
      .headers(CommonHeaders ++ NewApiHeaders)
      .cookies(cookies.map(c => (c.getName, c.getValue))*)
      .header(AuthorizationToken, s"Bearer ${session.jwtToken}")
  }

  protected def httpNewApiWithOldToken(url: String, session: Session, cookiesMaybe: Option[Seq[HttpCookie]] = None): Request[String, Any] = {
    val cookies = cookiesMaybe.getOrElse(session.cookies)
    baseGet(s"$newApiBaseUrl/$url")
      .headers(CommonHeaders ++ NewApiHeaders)
      .header(`X-Requested-With`, "pl.luxmed.pp")
      .header(Authorization, session.accessToken)
      .cookies(cookies.map(c => (c.getName, c.getValue))*)
  }
}
