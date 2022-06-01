
package com.lbs.api.json.model


/**
 * {
 * "correlationId": "00000000-0000-0000-0000-000000000000",
    *"date": "2021-05-19T08:00:00.000Z",
    *"doctor": {
        *"academicTitle": "dr n.med.",
        *"firstName": "TARAS",
        *"id": 11111,
        *"lastName": "SHEVCHENKO"
    *},
    *"doctorId": 22222,
    *"eReferralId": null,
    *"facilityId": 33,
    *"facilityName": "Telephone consultation",
    *"impedimentText": "",
    *"isAdditional": false,
    *"isImpediment": false,
    *"isPreparationRequired": false,
    *"isTelemedicine": true,
    *"parentReservationId": null,
    *"preparationItems": [],
    *"referralId": null,
    *"referralTypeId": null,
    *"roomId": 3333,
    *"scheduleId": 444444,
    *"serviceVariantId": 555555,
    *"serviceVariantName": "Telephone consultation - General practitioner",
    *"timeFrom": "12:00",
    *"timeTo": "12:15"
*}
  */
case class ReservationLocktermRequest(date: String, doctor: Doctor, doctorId: Long, eReferralId: String = null,
                                      facilityId: Long,
                                       impedimentText:String, isAdditional: Boolean, isImpediment: Boolean,
                                      isPreparationRequired: Boolean, isTelemedicine: Boolean, parentReservationId: String = null,
                                      preparationItems: List[PreparationItem], referralId: String = null, referralTypeId: String = null,
                                      roomId: Long, scheduleId: Long, serviceVariantId: Long,
                                      timeFrom: String, timeTo: String) extends SerializableJsonObject
