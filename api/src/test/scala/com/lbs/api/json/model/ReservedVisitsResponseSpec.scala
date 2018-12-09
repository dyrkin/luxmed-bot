
package com.lbs.api.json.model

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.lbs.api.json.JsonSerializer.extensions._
import org.scalatest.{FunSuiteLike, Matchers}

class ReservedVisitsResponseSpec extends FunSuiteLike with Matchers with CommonSpec {
  test("deserialization") {
    val json =
      """
        |{
        |    "ReservedVisits": [
        |        {
        |            "CanBeCanceled": true,
        |            "Clinic": {
        |                "Id": 6,
        |                "Name": "Szewska 3A"
        |            },
        |            "DoctorName": "TARAS SHEVCZENKO",
        |            "Impediment": {
        |                "ImpedimentText": "",
        |                "IsImpediment": false
        |            },
        |            "IsAdditional": false,
        |            "IsPreparationRequired": false,
        |            "Links": [
        |                {
        |                    "Href": "/PatientPortalMobileAPI/api/visits/preparations/6621",
        |                    "Method": "GET",
        |                    "Rel": "get_preparations"
        |                }
        |            ],
        |            "ReservationId": 888888888,
        |            "Service": {
        |                "Id": 6621,
        |                "Name": "stomatolog"
        |            },
        |            "VisitDate": {
        |                "FormattedDate": "21rd May, Mon. at 3:00 pm",
        |                "StartDateTime": "2018-05-21T15:00:00+02:00"
        |            }
        |        }
        |    ]
        |}
      """.stripMargin

    val response = json.as[ReservedVisitsResponse]

    response.reservedVisits.size should be(1)
    val reservedVisit = response.reservedVisits.head
    reservedVisit.canBeCanceled should be(true)
    testSimpleEntity(reservedVisit.clinic, 6L, "Szewska 3A")
    reservedVisit.doctorName should be("TARAS SHEVCZENKO")
    reservedVisit.reservationId should be(888888888L)
    testSimpleEntity(reservedVisit.service, 6621L, "stomatolog")
    reservedVisit.visitDate.formattedDate should be("21rd May, Mon. at 3:00 pm")
    reservedVisit.visitDate.startDateTime should be(ZonedDateTime.parse("2018-05-21T15:00:00+02:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  }
}
