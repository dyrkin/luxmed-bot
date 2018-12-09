
package com.lbs.api.json.model


/**
{
    "OptionsQuestion": "Would you like to confirm your appointment booking?",
    "VisitTermVariants": [
        {
            "CanBeReserve": true,
            "InfoMessage": "During the appointment, the physician will indicate the services to be provided and will inform you of the relevant fee, if any. The services will be provided in accordance with the scope of the agreement.",
            "IsStomatology": true,
            "OptionMessage": "I do not have the required referral",
            "PaymentMessage": "",
            "ReferralRequired": false,
            "ValuationDetail": {
                "PayerData": {
                    "BrandId": null,
                    "ContractId": 1111111,
                    "PayerId": 22222,
                    "PayerName": "FIRMA POLAND SP. Z O.O.",
                    "ProductElementId": 8547100,
                    "ProductId": 44444,
                    "ProductInContractId": 555555,
                    "ServaAppId": 0,
                    "ServaId": 6621
                },
                "Price": 0.0,
                "ValuationType": 1
            },
            "WarningMessage": ""
        }
    ]
}
  */
case class ValuationsResponse(optionsQuestion: Option[String], visitTermVariants: List[VisitTermVariant]) extends SerializableJsonObject

case class VisitTermVariant(canBeReserve: Boolean, infoMessage: String, isStomatology: Boolean, optionMessage: String, paymentMessage: String,
                            referralRequired: Boolean, valuationDetail: ValuationDetail, warningMessage: String) extends SerializableJsonObject

case class ValuationDetail(payerData: PayerDetails, price: BigDecimal, valuationType: Int) extends SerializableJsonObject
