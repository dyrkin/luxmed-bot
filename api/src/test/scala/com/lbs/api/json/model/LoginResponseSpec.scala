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
