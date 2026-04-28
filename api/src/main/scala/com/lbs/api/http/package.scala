package com.lbs.api.http

import cats.MonadError
import cats.implicits.*
import com.lbs.api.ThrowableMonad
import com.lbs.api.exception.*
import com.lbs.api.json.JsonSerializer.extensions.*
import com.lbs.api.json.model.*
import com.lbs.api.json.model.JsonCodecs.given
import sttp.client3.*

import java.net.{HttpCookie, HttpURLConnection}
import scala.util.{Failure, Success, Try}

case class Session(accessToken: String, tokenType: String, jwtToken: String, cookies: Seq[HttpCookie])

case class LuxmedResponse[T](body: T, code: Int, cookies: Seq[HttpCookie], headers: Map[String, String] = Map.empty) {
  def copy[U](body: U): LuxmedResponse[U] = LuxmedResponse(body, code, cookies, headers)
  def header(name: String): Option[String] = headers.get(name.toLowerCase)
}

object headers {
  val `Content-Type`            = "Content-Type"
  val `xsrf-token`              = "xsrf-token"
  val Host                      = "Host"
  val Origin                    = "Origin"
  val Accept                    = "Accept"
  val `Accept-Encoding`         = "Accept-Encoding"
  val `User-Agent`              = "User-Agent"
  val `Custom-User-Agent`       = "Custom-User-Agent"
  val `X-Api-Client-Identifier` = "X-Api-Client-Identifier"
  val `Accept-Language`         = "accept-language"
  val Authorization             = "Authorization"
  val AuthorizationToken        = "authorization-token"
  val `X-Requested-With`       = "X-Requested-With"
}

private val SensitiveParams = List("passw", "access_token", "refresh_token", "authorization")

extension [F[_]: ThrowableMonad](request: Request[String, Any])
  def invoke(using backend: SttpBackend[Identity, Any]): F[LuxmedResponse[String]] =
    val me = MonadError[F, Throwable]
    Try(backend.send(request)) match
      case Failure(ex) => me.raiseError(ex)
      case Success(resp) =>
        val cookies    = Try(resp.history.flatMap(_.unsafeCookies) ++ resp.unsafeCookies).getOrElse(Nil).map(c => new HttpCookie(c.name, c.value))
        val allHeaders = (resp.history.flatMap(_.headers) ++ resp.headers).map(h => h.name.toLowerCase -> h.value).toMap
        val luxmedResp = LuxmedResponse(resp.body, resp.code.code, cookies, allHeaders)
        extractLuxmedError(luxmedResp) match
          case Some(error) => me.raiseError(error)
          case None =>
            if (resp.code.isSuccess || resp.code.isRedirect) me.pure(luxmedResp)
            else me.raiseError(new RuntimeException(s"HTTP error ${resp.code.code}: ${resp.body}"))

extension (request: Request[String, Any])
  def param(key: String, value: String): Request[String, Any] =
    request.get(request.uri.addParam(key, value))
  def param(key: String, value: Option[String]): Request[String, Any] =
    value.fold(request)(v => request.get(request.uri.addParam(key, v)))

private def luxmedErrorToApiException[T <: LuxmedBaseError](code: Int, error: T): ApiException =
  val message      = error.message
  val errorMessage = message.toLowerCase
  if (errorMessage.contains("invalid login or password"))
    new InvalidLoginOrPasswordException
  else if (errorMessage.contains("session has expired"))
    new SessionExpiredException
  else
    GenericException(code, message)

private def extractLuxmedError(response: LuxmedResponse[String]): Option[ApiException] =
  val body           = response.body
  val lowercasedBody = body.toLowerCase
  val code           = response.code
  val location       = response.header("location").map(_.toLowerCase).getOrElse("")
  code match
    case HttpURLConnection.HTTP_MOVED_TEMP
        if lowercasedBody.contains("/logon") || lowercasedBody.contains("/universallink")
          || location.contains("/logon") || location.contains("/universallink") =>
      Some(new SessionExpiredException)
    case HttpURLConnection.HTTP_CONFLICT
        if lowercasedBody.contains("nieprawidłowy login lub hasło") || lowercasedBody.contains("invalid login or password") =>
      Some(new InvalidLoginOrPasswordException)
    case c if c >= HttpURLConnection.HTTP_BAD_REQUEST =>
      Try(body.as[LuxmedErrorsList])
        .orElse(Try(body.as[LuxmedErrorsMap]))
        .orElse(Try(body.as[LuxmedError]))
        .map(error => luxmedErrorToApiException(code, error))
        .toOption
    case _ =>
      Try(body.as[LuxmedErrorsMap])
        .map(error => luxmedErrorToApiException(code, error))
        .toOption
