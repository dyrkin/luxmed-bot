
package com.lbs.api.json.model

import java.time.LocalTime


/**
 * {
 * "errors": [],
 * "hasErrors": false,
 * "hasWarnings": false,
    *"value": {
        *"askForReferral": false,
        *"changeTermAvailable": true,
        *"conflictedVisit": null,
        *"doctorDetails": {
            *"academicTitle": "lek. med.",
            *"firstName": "TARAS",
            *"genderId": 1,
            *"id": 11111,
            *"lastName": "SHEVCHENKO"
        *},
        *"isBloodExamination": false,
        *"isStomatology": false,
        *"relatedVisits": [
            *{
                *"date": "2021-06-03T05:20:00",
                *"doctor": {
                    *"academicTitle": "lek. med.",
                    *"firstName": "LESYA",
                    *"genderId": 2,
                    *"id": 0,
                    *"lastName": "UKRAINKA"
                *},
                *"facilityName": "LX Wrocław - Szewska 3A",
                *"isAsdk": false,
                *"isTelemedicine": false,
                *"payerName": null,
                *"reservationId": 333333,
                *"serviceInstanceId": 9999999,
                *"serviceVariantId": 111111,
                *"serviceVariantName": "Consultation with a general practitioner",
                *"timeFrom": "07:30:00",
                *"timeTo": "07:45:00"
            *}
        *],
        *"temporaryReservationId": 222222,
        *"valuations": [
            *{
                *"alternativePrice": null,
                *"contractId": 333333,
                *"isExternalReferralAllowed": false,
                *"isReferralRequired": false,
                *"payerId": 44444,
                *"price": 0.0,
                *"productElementId": 555555,
                *"productId": 666666,
                *"productInContractId": 777777,
                *"requireReferralForPP": false,
                *"valuationType": 1
            *}
        *]
    *},
    *"warnings": []
*}
  */
case class ReservationLocktermResponse(errors: List[String], warnings: List[String], hasErrors: Boolean, hasWarnings: Boolean,
                                       value: ReservationLocktermResponseValue) extends SerializableJsonObject

case class ReservationLocktermResponseValue(changeTermAvailable: Boolean, conflictedVisit: Option[String], doctorDetails: Doctor,
                                            relatedVisits: List[RelatedVisit], temporaryReservationId: Long, valuations: List[Valuation]) extends SerializableJsonObject

case class RelatedVisit(doctor: Doctor, facilityName: String, isTelemedicine: Boolean, reservationId: Long,
                        timeFrom: LocalTime, timeTo: LocalTime)
