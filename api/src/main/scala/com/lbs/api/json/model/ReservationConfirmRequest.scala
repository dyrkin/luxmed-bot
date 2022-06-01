
package com.lbs.api.json.model

import java.time.LocalTime


/**
 * {
 * "date": "2021-05-19T08:00:00.000Z",
    *"doctorId": 111111,
    *"eReferralId": null,
    *"facilityId": 2222,
    *"parentReservationId": null,
    *"referralId": null,
    *"referralRequired": false,
    *"roomId": 1248,
    *"scheduleId": 333333,
    *"serviceVariantId": 444444,
    *"temporaryReservationId": 4111111,
    *"timeFrom": "18:45",
    *"valuation": {
        *"alternativePrice": null,
        *"contractId": 555555,
        *"isExternalReferralAllowed": false,
        *"isReferralRequired": false,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  false,
        *"payerId": 66666,
        *"price": 0.0,
        *"productElementId": 7777777,
        *"productId": 888888,
        *"productInContractId": 9999999,
        *"requireReferralForPP": false,
        *"valuationType": 1
    *},
    *"valuationId": null
*}
  */
case class ReservationConfirmRequest(date: String, doctorId: Long, facilityId: Long, roomId: Long, scheduleId: Long, serviceVariantId: Long,
                                     temporaryReservationId: Long, timeFrom: LocalTime, valuation: Valuation) extends SerializableJsonObject
