/**
  * MIT License
  *
  * Copyright (c) 2018 Yevhen Zadyra
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
package com.lbs.api

import com.lbs.api.exception.{ApiException, GenericException, InvalidLoginOrPasswordException, ServiceIsAlreadyBookedException}
import com.lbs.api.json.JsonSerializer.extensions._
import com.lbs.api.json.model.{LuxmedBaseError, LuxmedCompositeError, LuxmedError, SerializableJsonObject}
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
      ler.body match {
        case e: LuxmedCompositeError =>
          new GenericException(ler.code, ler.statusLine, e.errors.map(_.message).mkString("; "))
        case e: LuxmedError =>
          val errorMessage = e.message.toLowerCase
          if (errorMessage.contains("invalid login or password"))
            new InvalidLoginOrPasswordException
          else if (errorMessage.contains("have already booked this service"))
            new ServiceIsAlreadyBookedException
          else
            new GenericException(ler.code, ler.statusLine, e.message)
      }
    }

    private def extractLuxmedError(httpResponse: Try[HttpResponse[String]]) = {
      httpResponse.flatMap(response => Try(response.asEntity[LuxmedCompositeError]).map(luxmedErrorToApiException).
        orElse(Try(response.asEntity[LuxmedError]).map(luxmedErrorToApiException))).toOption
    }

    private def hidePasswords(httpRequest: HttpRequest) = {
      httpRequest.copy(params = httpRequest.params.map { case (k, v) =>
        if (k.toLowerCase.contains("passw")) k -> "******" else k -> v
      })
    }
  }

}
