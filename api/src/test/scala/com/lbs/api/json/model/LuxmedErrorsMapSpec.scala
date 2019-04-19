package com.lbs.api.json.model

import com.lbs.api.json.JsonSerializer.extensions._
import org.scalatest.{FunSuiteLike, Matchers}

class LuxmedErrorsMapSpec extends FunSuiteLike with Matchers with CommonSpec {
  test("deserialization") {
    val json =
      """
        |{"Errors":{"ToDate.Date":["'To Date. Date' must be greater than or equal to '06/04/2018 00:00:00'."]}}
      """.stripMargin

    val response = json.as[LuxmedErrorsMap]

    response should be(LuxmedErrorsMap(Map("toDate.Date" -> List("'To Date. Date' must be greater than or equal to '06/04/2018 00:00:00'."))))
  }
}
