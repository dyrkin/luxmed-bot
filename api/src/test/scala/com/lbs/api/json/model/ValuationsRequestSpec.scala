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

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.lbs.api.json.JsonSerializer.extensions._
import org.scalatest.{FunSuiteLike, Matchers}

class ValuationsRequestSpec extends FunSuiteLike with Matchers with CommonSpec {
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

    val request = ValuationsRequest(clinicId = 6L, doctorId = 38275L, payerDetailsList = List(
      PayerDetails(brandId = Some(2L), contractId = 1111111L, payerId = 22222L, payerName = "FIRMA",
        productElementId = 3333333L, productId = 44444L, productInContractId = 555555L, servaAppId = 0L, servaId = 6666L)
    ), referralRequiredByService = false, roomId = 543L, serviceId = 6666L,
      startDateTime = ZonedDateTime.parse("2018-02-23T11:30:00+02:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME))

    val requestJson = request.asJson
    val requestActual = requestJson.as[ValuationsRequest]
    val requestExpected = json.as[ValuationsRequest]

    requestActual should be (requestExpected)
  }
}
