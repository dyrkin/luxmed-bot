package com.lbs.api.json.model

import org.scalatest.{FunSuiteLike, Matchers}
import com.lbs.api.json.JsonSerializer.extensions._

class LuxmedErrorsSpec extends FunSuiteLike with Matchers with CommonSpec {
  test("deserialization") {
    val json =
      """
        |{"Errors":{"ToDate.Date":["'To Date. Date' must be greater than or equal to '06/04/2018 00:00:00'."]}}
      """.stripMargin

    val response = json.as[LuxmedErrors]

    response should be(LuxmedErrors(Map("toDate.Date" -> List("'To Date. Date' must be greater than or equal to '06/04/2018 00:00:00'."))))
  }
}
