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
