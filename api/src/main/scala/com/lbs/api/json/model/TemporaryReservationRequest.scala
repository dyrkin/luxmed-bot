
package com.lbs.api.json.model

import java.time.ZonedDateTime


/**
{
    "ClinicId": 6,
    "DoctorId": 38275,
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
    "ReferralRequiredByService": false,
    "RoomId": 543,
    "ServiceId": 6666,
    "StartDateTime": "2018-02-23T11:30:00+02:00"
}
  */
case class TemporaryReservationRequest(clinicId: Long, doctorId: Long, payerDetailsList: List[PayerDetails],
                                       referralRequiredByService: Boolean, roomId: Long, serviceId: Long,
                                       startDateTime: ZonedDateTime) extends SerializableJsonObject
