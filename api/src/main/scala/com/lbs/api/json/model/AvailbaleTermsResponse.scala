
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
