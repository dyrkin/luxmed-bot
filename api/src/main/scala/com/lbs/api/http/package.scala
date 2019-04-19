
package com.lbs.api

import com.lbs.api.exception.{ApiException, GenericException, InvalidLoginOrPasswordException, ServiceIsAlreadyBookedException}
import com.lbs.api.json.JsonSerializer.extensions._
import com.lbs.api.json.model._
import com.lbs.common.Logger
import scalaj.http.{HttpRequest, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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

  implicit class HttpResponseWithJsonDeserializationSupport(httpResponse: HttpResponse[String]) {

    def asEntity[T <: SerializableJsonObject](implicit mf: scala.reflect.Manifest[T]): HttpResponse[T] = {
      httpResponse.copy(body = httpResponse.body.as[T])
    }

    def asEntityAsync[T <: SerializableJsonObject](implicit mf: scala.reflect.Manifest[T], ec: ExecutionContext): Future[HttpResponse[T]] = {
      Future(asEntity[T])
    }
  }

  implicit class ExtendedHttpRequest(httpRequest: HttpRequest) {

    def toEither: Either[Throwable, HttpResponse[String]] = {
      toTry.toEither
    }

    def toTry: Try[HttpResponse[String]] = {
      debug(s"Sending request:\n${hidePasswords(httpRequest)}")
      val httpResponse = Try(httpRequest.asString)
      debug(s"Received response:\n$httpResponse")
      extractLuxmedError(httpResponse) match {
        case Some(error) => Try(throw error)
        case None => httpResponse.map(_.throwError)
      }
    }

    def param(key: String, value: Option[String]): HttpRequest = {
      value.map(v => httpRequest.param(key, v)).getOrElse(httpRequest)
    }

    private def luxmedErrorToApiException[T <: LuxmedBaseError](ler: HttpResponse[T]): ApiException = {
      val message = ler.body.message
      val errorMessage = message.toLowerCase
      if (errorMessage.contains("invalid login or password"))
        new InvalidLoginOrPasswordException
      else if (errorMessage.contains("already booked this service"))
        new ServiceIsAlreadyBookedException
      else
        new GenericException(ler.code, ler.statusLine, message)
    }

    private def extractLuxmedError(httpResponse: Try[HttpResponse[String]]) = {
      httpResponse.flatMap { response =>
        Try(response.asEntity[LuxmedErrorsMap])
          .orElse(Try(response.asEntity[LuxmedErrorsList]))
          .orElse(Try(response.asEntity[LuxmedError]))
          .map(e => luxmedErrorToApiException(e.asInstanceOf[HttpResponse[LuxmedBaseError]]))
      }.toOption
    }

    private def hidePasswords(httpRequest: HttpRequest) = {
      httpRequest.copy(params = httpRequest.params.map { case (k, v) =>
        if (k.toLowerCase.contains("passw")) k -> "******" else k -> v
      })
    }
  }

}
