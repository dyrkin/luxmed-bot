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
  *
{
    "AvailableVisitsTermPresentation": [
        {
            "Clinic": {
                "Id": 6,
                "Name": "LX Wrocław - Szewska 3A"
            },
            "Doctor": {
                "Id": 38275,
                "Name": "lek. med. ANNA ABRAMCZYK"
            },
            "Impediment": {
                "ImpedimentText": "",
                "IsImpediment": false
            },
            "IsFree": false,
            "PayerDetailsList": [
                {
                    "BrandId": 2,
                    "ContractId": 1111111,
                    "PayerId": 22222,
                    "PayerName": "FIRMA POLAND SP. Z O.O.",
                    "ProductElementId": 3333333,
                    "ProductId": 44444,
                    "ProductInContractId": 555555,
                    "ServaAppId": 0,
                    "ServaId": 6666
                },
                {
                    "BrandId": 2,
                    "ContractId": 1111111,
                    "PayerId": 22222,
                    "PayerName": "FIRMA POLAND SP. Z O.O.",
                    "ProductElementId": 8547135,
                    "ProductId": 44444,
                    "ProductInContractId": 555555,
                    "ServaAppId": 1,
                    "ServaId": 6666
                }
            ],
            "ReferralRequiredByProduct": false,
            "ReferralRequiredByService": false,
            "RoomId": 543,
            "ScheduleId": 3331908,
            "ServiceId": 6666,
            "VisitDate": {
                "FormattedDate": "26th April, Thu. at 12:40 pm",
                "StartDateTime": "2018-02-23T11:30:00+02:00"
            }
        }
    ]
}

  */
case class AvailableTermsResponse(availableVisitsTermPresentation: List[AvailableVisitsTermPresentation]) extends SerializableJsonObject

case class AvailableVisitsTermPresentation(clinic: IdName, doctor: IdName, payerDetailsList: List[PayerDetails],
                                           referralRequiredByProduct: Boolean, referralRequiredByService: Boolean,
                                           roomId: Long, scheduleId: Long, serviceId: Long, visitDate: VisitDate) extends SerializableJsonObject
