package com.lbs.api.json.model

import com.lbs.api.json.JsonSerializer.extensions._
import org.scalatest.{FunSuiteLike, Matchers}

class ValuationsResponseSpec extends FunSuiteLike with Matchers with CommonSpec {
  test("deserialization") {
    val json =
      """
        |{
        |  "VisitTermVariants": [
        |    {
        |      "ValuationDetail": {
        |        "PayerData": {
        |          "PayerId": 12345,
        |          "PayerName": "ZBIGNEW",
        |          "ContractId": 123456,
        |          "ProductInContractId": 234567,
        |          "ProductId": 34567,
        |          "BrandId": null,
        |          "ProductElementId": 2536352,
        |          "ServaId": 12345,
        |          "ServaAppId": 0
        |        },
        |        "ValuationType": 1,
        |        "Price": 0.000000
        |      },
        |      "InfoMessage": "Package-covered service",
        |      "PaymentMessage": "Price with a valid referral: 0.00 zł \r\nPrice without a valid referral: 10.30 zł",
        |      "OptionMessage": "Yes, from a LUX MED Group or a subcontractor facility physician",
        |      "WarningMessage": "In accordance with your medical package terms and conditions, you must have a valid referral from a LUX MED Group or a subcontractor facility physician",
        |      "CanBeReserve": true,
        |      "ReferralRequired": true,
        |      "IsStomatology": false
        |    },
        |    {
        |      "ValuationDetail": {
        |        "PayerData": {
        |          "PayerId": 123456,
        |          "PayerName": "ZBIGNEW",
        |          "ContractId": 12345,
        |          "ProductInContractId": 345678,
        |          "ProductId": 23467,
        |          "BrandId": null,
        |          "ProductElementId": null,
        |          "ServaId": 1234,
        |          "ServaAppId": 0
        |        },
        |        "ValuationType": 4,
        |        "Price": 10.30
        |      },
        |      "InfoMessage": "Out-of-package service",
        |      "PaymentMessage": "Price: 10.30 zł",
        |      "OptionMessage": "I do not have the required referral",
        |      "WarningMessage": "",
        |      "CanBeReserve": true,
        |      "ReferralRequired": false,
        |      "IsStomatology": false
        |    }
        |  ],
        |  "OptionsQuestion": "Do you have a valid referral for this service?",
        |  "IsReferralRequired": false
        |}
      """.stripMargin

    val responseActual = json.as[ValuationsResponse]

    val responseExpected = ValuationsResponse(
      Some("Do you have a valid referral for this service?"),
      List(
        VisitTermVariant(
          canBeReserve = true,
          "Package-covered service",
          isStomatology = false,
          "Yes, from a LUX MED Group or a subcontractor facility physician", "Price with a valid referral: 0.00 zł \r\nPrice without a valid referral: 10.30 zł",
          referralRequired = true,
          ValuationDetail(PayerDetails(None, 123456, 12345, "ZBIGNEW", Some(2536352), 34567, 234567, 0, 12345), 0.0, 1),
          "In accordance with your medical package terms and conditions, you must have a valid referral from a LUX MED Group or a subcontractor facility physician"
        ),
        VisitTermVariant(
          canBeReserve = true,
          "Out-of-package service",
          isStomatology = false,
          "I do not have the required referral",
          "Price: 10.30 zł",
          referralRequired = false,
          ValuationDetail(PayerDetails(None, 12345, 123456, "ZBIGNEW", None, 23467, 345678, 0, 1234), 10.3, 4),
          "")
      )
    )

    responseActual should be(responseExpected)
  }
}
