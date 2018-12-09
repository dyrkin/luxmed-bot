
package com.lbs.api.json.model

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.lbs.api.json.JsonSerializer.extensions._
import org.scalatest.{FunSuiteLike, Matchers}

class TemporaryReservationRequestSpec extends FunSuiteLike with Matchers with CommonSpec {
  test("serialization") {
    val json =
      """
        |{
        |    "ClinicId": 6,
        |    "DoctorId": 38275,
        |    "PayerDetailsList": [
        |        {
        |            "BrandId": 2,
        |            "ContractId": 1111111,
        |            "PayerId": 22222,
        |            "PayerName": "FIRMA",
        |            "ProductElementId": 3333333,
        |            "ProductId": 44444,
        |            "ProductInContractId": 555555,
        |            "ServaAppId": 0,
        |            "ServaId": 6666
        |        }
        |    ],
        |    "ReferralRequiredByService": false,
        |    "RoomId": 543,
        |    "ServiceId": 6666,
        |    "StartDateTime": "2018-02-23T11:30:00+02:00"
        |}
      """.stripMargin

    val request = TemporaryReservationRequest(clinicId = 6L, doctorId = 38275L, payerDetailsList = List(
      PayerDetails(brandId = Some(2L), contractId = 1111111L, payerId = 22222L, payerName = "FIRMA",
        productElementId = 3333333L, productId = 44444L, productInContractId = 555555L, servaAppId = 0L, servaId = 6666L)
    ), referralRequiredByService = false, roomId = 543L, serviceId = 6666L,
      startDateTime = ZonedDateTime.parse("2018-02-23T11:30:00+02:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME))

    val requestJson = request.asJson
    val requestActual = requestJson.as[TemporaryReservationRequest]
    val requestExpected = json.as[TemporaryReservationRequest]

    requestActual should be(requestExpected)
  }
}
