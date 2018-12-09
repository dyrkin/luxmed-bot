
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
    response.defaultPayer should be(_: Some[IdName])
    testSimpleEntity(response.defaultPayer.get, 22222L, "FIRMA")
    testSimpleEntities(response.doctors, 2, 38275L, "ANNA ABRAMCZYK")
    testSimpleEntities(response.languages, 2, 11L, "english")
    testSimpleEntities(response.payers, 1, 22222L, "FIRMA")
    testSimpleEntities(response.services, 2, 5857L, "Audiometr standardowy")
  }


}
