package com.lbs.api.http

import cats.instances.either.*
import com.lbs.api.ThrowableMonad
import com.lbs.api.exception.GenericException
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub

class ExtendedHttpRequestSpec extends AnyFunSuite with Matchers {

  private type ThrowableOr[T] = Either[Throwable, T]

  private val baseRequest: Request[String, Any] =
    basicRequest.get(uri"https://example.com/api/test").response(asStringAlways)

  test("ok response") {
    given backend: SttpBackend[Identity, Any] = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespond("ok")

    val result: ThrowableOr[LuxmedResponse[String]] = baseRequest.invoke
    result shouldBe a[Right[?, ?]]
    result.toOption.get.body shouldBe "ok"
  }

  test("error response with luxmed error json") {
    given backend: SttpBackend[Identity, Any] = SttpBackendStub.synchronous
      .whenAnyRequest
      .thenRespond(
        """{"Errors":{"ToDate.Date":["'To Date. Date' must be greater than or equal to '06/04/2018 00:00:00'."]}}"""
      )

    val result: ThrowableOr[LuxmedResponse[String]] = baseRequest.invoke
    result shouldBe Left(
      GenericException(200, "'To Date. Date' must be greater than or equal to '06/04/2018 00:00:00'.")
    )
  }
}
