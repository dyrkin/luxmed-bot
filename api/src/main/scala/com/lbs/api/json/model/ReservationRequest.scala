
package com.lbs.api.json.model

import java.time.ZonedDateTime


/**
{
    "ClinicId": 6,
    "DoctorId": 38509,
    "PayerData": {
        "ContractId": 1111111,
        "PayerId": 22222,
        "PayerName": "FIRMA POLAND SP. Z O.O.",
        "ProductElementId": 8547100,
        "ProductId": 44444,
        "ProductInContractId": 555555,
        "ServaAppId": 0,
        "ServaId": 6621
    },
    "RoomId": 159,
    "ServiceId": 6621,
    "StartDateTime": "2018-06-04T11:00:00+02:00",
    "TemporaryReservationId": 250303839
}
  */
case class ReservationRequest(clinicId: Long, doctorId: Long, payerData: PayerDetails, roomId: Long, serviceId: Long,
                              startDateTime: ZonedDateTime, temporaryReservationId: Long) extends SerializableJsonObject
