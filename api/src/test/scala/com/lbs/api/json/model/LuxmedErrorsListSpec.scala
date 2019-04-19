package com.lbs.api.json.model

import com.lbs.api.json.JsonSerializer.extensions._
import org.scalatest.{FunSuiteLike, Matchers}

class LuxmedErrorsListSpec extends FunSuiteLike with Matchers with CommonSpec {

  test("deserialization") {
    val json =
      """
        |{"Errors":[{"ErrorCode":16000006,"Message":"You have already booked this service","AdditionalData":{}}]}
      """.stripMargin

    val response = json.as[LuxmedErrorsList]

    response should be(LuxmedErrorsList(List(LuxmedErrorsListElement(16000006, "You have already booked this service", Map.empty))))
  }
}
