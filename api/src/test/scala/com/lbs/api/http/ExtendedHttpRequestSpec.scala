package com.lbs.api.http

import cats.instances.either._
import com.lbs.api.exception.GenericException
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import scalaj.http.{HttpRequest, HttpResponse}

class ExtendedHttpRequestSpec extends AnyFunSuite with Matchers with MockitoSugar with BeforeAndAfterEach {

  private val request = mock[HttpRequest]
  private type ThrowableOr[T] = Either[Throwable, T]

  override protected def beforeEach(): Unit = {
    reset(request)
    when(request.params).thenReturn(Seq())
  }

  test("ok response") {
    val okResponse = HttpResponse("ok", 200, Map())
    when(request.asString).thenReturn(okResponse)

    assert(invoke(request) == Right(okResponse))
  }

  test("error response") {
    val errorResponse = HttpResponse("""{"Errors":{"ToDate.Date":["'To Date. Date' must be greater than or equal to '06/04/2018 00:00:00'."]}}""", 200, Map())
    when(request.asString).thenReturn(errorResponse)
    val result = invoke(request)

    assert(result == Left(GenericException(200, "'To Date. Date' must be greater than or equal to '06/04/2018 00:00:00'.")))
  }

  private def invoke(request: HttpRequest) = {
    ExtendedHttpRequest[ThrowableOr](request).invoke
  }
}
