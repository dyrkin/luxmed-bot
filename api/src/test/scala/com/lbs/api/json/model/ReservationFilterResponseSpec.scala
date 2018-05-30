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

class ReservationFilterResponseSpec extends FunSuiteLike with Matchers with CommonSpec {
  test("deserialization") {
    val json =
      """
        |{
        |    "Cities": [
        |        {
        |            "Id": 5,
        |            "Name": "Wrocław"
        |        }
        |    ],
        |    "Clinics": [
        |        {
        |            "Id": 1405,
        |            "Name": "Legnicka 40"
        |        },
        |        {
        |            "Id": 7,
        |            "Name": "Kwidzyńska 6"
        |        }
        |    ],
        |    "DefaultPayer": {
        |        "Id": 22222,
        |        "Name": "FIRMA"
        |    },
        |    "Doctors": [
        |        {
        |            "Id": 38275,
        |            "Name": "ANNA ABRAMCZYK"
        |        },
        |        {
        |            "Id": 15565,
        |            "Name": "ANDRZEJ ANDEWSKI"
        |        }
        |    ],
        |    "Languages": [
        |        {
        |            "Id": 11,
        |            "Name": "english"
        |        },
        |        {
        |            "Id": 10,
        |            "Name": "polish"
        |        }
        |    ],
        |    "Payers": [
        |        {
        |            "Id": 22222,
        |            "Name": "FIRMA"
        |        }
        |    ],
        |    "Services": [
        |        {
        |            "Id": 5857,
        |            "Name": "Audiometr standardowy"
        |        },
        |        {
        |            "Id": 7976,
        |            "Name": "Audiometr standardowy - audiometria nadprogowa"
        |        }
        |    ]
        |}
      """.stripMargin

    val response = json.as[ReservationFilterResponse]

    testSimpleEntities(response.cities, 1, 5L, "Wrocław")
    testSimpleEntities(response.clinics, 2, 1405L, "Legnicka 40")
    response.defaultPayer should be (_: Some[IdName])
    testSimpleEntity(response.defaultPayer.get, 22222L, "FIRMA")
    testSimpleEntities(response.doctors, 2, 38275L, "ANNA ABRAMCZYK")
    testSimpleEntities(response.languages, 2, 11L, "english")
    testSimpleEntities(response.payers, 1, 22222L, "FIRMA")
    testSimpleEntities(response.services, 2, 5857L, "Audiometr standardowy")
  }


}
