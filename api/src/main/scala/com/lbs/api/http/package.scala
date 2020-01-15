
package com.lbs.api

import cats.MonadError
import cats.implicits._
import com.lbs.api.exception._
import com.lbs.api.json.JsonSerializer.extensions._
import com.lbs.api.json.model._
import com.lbs.common.Logger
import scalaj.http.{HttpRequest, HttpResponse}

import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

package object http extends Logger {

  object headers {
    val `Content-Type` = "Content-Type"
    val Host = "Host"
    val Accept = "Accept"
    val Connection = "Connection"
    val `Accept-Encoding` = "Accept-Encoding"
    val `User-Agent` = "User-Agent"
    val `x-api-client-identifier` = "x-api-client-identifier"
    val `Accept-Language` = "Accept-Language"
    val Authorization = "Authorization"
  }

  private val SensitiveHeaders = List("passw", "access_token", "refresh_token", "authorization")

  implicit class HttpResponseWithJsonDeserializationSupport(httpResponse: HttpResponse[String]) {

    def asEntity[T <: SerializableJsonObject](implicit mf: scala.reflect.Manifest[T]): HttpResponse[T] = {
      httpResponse.copy(body = httpResponse.body.as[T])
    }
  }

  implicit class ExtendedHttpRequest[F[_] : ThrowableMonad](httpRequest: HttpRequest) {
    def invoke: F[HttpResponse[String]] = {
      val me = MonadError[F, Throwable]
      debug(s"Sending request:\n${hideSensitive(httpRequest)}")
      val httpResponse = me.pure(httpRequest.asString)
      debug(s"Received response:\n${hideSensitive(httpResponse)}")

      httpResponse.flatMap { response =>
        val errorMaybe = extractLuxmedError(response)
        errorMaybe match {
          case Some(error) => me.raiseError(error)
          case None =>
            Try(response.throwError) match {
              case Failure(error) => me.raiseError(error)
              case Success(value) => me.pure(value)
            }
        }
      }
    }

    def param(key: String, value: Option[String]): HttpRequest = {
      value.map(v => httpRequest.param(key, v)).getOrElse(httpRequest)
    }

    private def luxmedErrorToApiException[T <: LuxmedBaseError](code: Int, error: T): ApiException = {
      val message = error.message
      val errorMessage = message.toLowerCase
      if (errorMessage.contains("invalid login or password"))
        new InvalidLoginOrPasswordException
      else if (errorMessage.contains("already booked this service"))
        new ServiceIsAlreadyBookedException
      else if (errorMessage.contains("session has expired"))
        new SessionExpiredException
      else
        GenericException(code, message)
    }

    private def extractLuxmedError(httpResponse: HttpResponse[String]) = {
      val body = httpResponse.body
      val code = httpResponse.code
      Try(body.as[LuxmedErrorsMap])
        .orElse(Try(body.as[LuxmedErrorsList]))
        .orElse(Try(body.as[LuxmedError]))
        .map(error => luxmedErrorToApiException(code, error))
        .toOption
    }

    private def hideSensitive(httpRequest: HttpRequest) = {
      httpRequest.copy(params = httpRequest.params.map { case (k, v) =>
        if (hide(k)) k -> "******" else k -> v
      })
    }

    private def hideSensitive(httpResponse: F[HttpResponse[String]]) = {
      httpResponse.map { response =>
        response.copy(headers = response.headers.map { case (k, v) =>
          if (hide(k)) k -> IndexedSeq("******") else k -> v
        })
      }
    }

    private def hide(key: String) = {
      val lowerCaseKey = key.toLowerCase
      SensitiveHeaders.exists(h => lowerCaseKey.contains(h))
    }
  }

}
