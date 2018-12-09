
package com.lbs.api.json.model

import com.lbs.api.json.JsonSerializer.extensions._
import org.scalatest.{FunSuiteLike, Matchers}

class LoginResponseSpec extends FunSuiteLike with Matchers {
  test("deserialization") {
    val json =
      """
        |{
        |    "access_token": "RmC6qccJMJ1uVhqJZ-6sBYdfT_LznEoGuH2di0",
        |    "expires_in": 599,
        |    "refresh_token": "7854cd0b-8545-483e-88d7-d07eda90995d",
        |    "token_type": "bearer"
        |}
      """.stripMargin

    val response = json.as[LoginResponse]

    response.accessToken should be("RmC6qccJMJ1uVhqJZ-6sBYdfT_LznEoGuH2di0")
    response.expiresIn should be(599)
    response.refreshToken should be("7854cd0b-8545-483e-88d7-d07eda90995d")
    response.tokenType should be("bearer")
  }
}
