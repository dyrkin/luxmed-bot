
package com.lbs.api.json.model

import java.time.{LocalDateTime, ZonedDateTime}

/**
 *
 * {
 * "correlationId": "00000000-0000-0000-0000-000000000000",
 * "pMode": 500,
 * "termsForService": {
        *"additionalData": {
            *"anyTermForFacilityVisit": false,
            *"anyTermForTelemedicine": true,
            *"isPreparationRequired": false,
            *"nextTermsAvailable": false,
            *"preparationItems": [],
            *"previousTermsAvailable": false
        *},
        *"serviceVariantId": 111111,
        *"termsForDays": [
            *{
                *"correlationId": "00000000-0000-0000-0000-000000000000",
                *"day": "2022-05-31T00:00:00",
                *"terms": [
                    *{
                        *"clinic": "LX Wrocław - Fabryczna 6",
                        *"clinicGroup": "ul. Fabryczna 6",
                        *"clinicGroupId": 11,
                        *"clinicId": 2222,
                        *"dateTimeFrom": "2021-05-21T18:45:00", or 2021-05-21T18:45:00+02:00 sometimes!!!!
                        *"dateTimeTo": "2021-05-21T19:00:00",
                        *"doctor": {
                            *"academicTitle": "lek. med.",
                            *"firstName": "TARAS",
                            *"genderId": 0,
                            *"id": 33333,
                            *"lastName": "GRYGORYCH"
                        *},
                        *"impedimentText": "",
                        *"isAdditional": false,
                        *"isImpediment": false,
                        *"isInfectionTreatmentCenter": false,
                        *"isTelemedicine": true,
                        *"partOfDay": 3,
                        *"roomId": 4444,
                        *"scheduleId": 555555,
                        *"serviceId": 66666
                    *},
                    *{
                        *"clinic": "LX Wrocław - Fabryczna 6",
                        *"clinicGroup": "ul. Fabryczna 6",
                        *"clinicGroupId": 77,
                        *"clinicId": 88888,
                        *"dateTimeFrom": "2021-05-21T18:45:00",
                        *"dateTimeTo": "2021-05-21T19:10:00",
                        *"doctor": {
                            *"academicTitle": "lek. med.",
                            *"firstName": "VASYL",
                            *"genderId": 0,
                            *"id": 99999,
                            *"lastName": "STUS"
                        *},
                        *"impedimentText": "",
                        *"isAdditional": false,
                        *"isImpediment": false,
                        *"isInfectionTreatmentCenter": false,
                        *"isTelemedicine": true,
                        *"partOfDay": 3,
                        *"roomId": 11111,
                        *"scheduleId": 1222222,
                        *"serviceId": 133333
                    *}
                *]
            *}
        *],
        *"termsInfoForDays": [
            *{
                *"day": "2021-05-22T00:00:00",
                *"isLastDayWithLoadedTerms": true,
                *"isLimitedDay": false,
                *"isMoreTermsThanCounter": null,
                *"message": "We can propose visits on the searched day but in other locations, at other doctors or another hour range.",
                *"termsCounter": {
                    *"partialTermsCounters": [],
                    *"termsNumber": 41
                *},
                *"termsStatus": 0
            *},
            *{
                *"day": "2021-05-23T00:00:00",
                *"isLastDayWithLoadedTerms": false,
                *"isLimitedDay": false,
                *"isMoreTermsThanCounter": null,
                *"message": "Available visits have been already booked. Check later, additonal visits appear regularly.",
                *"termsCounter": {
                    *"partialTermsCounters": [],
                    *"termsNumber": 0
                *},
                *"termsStatus": 5
            *},
            *{
                *"day": "2021-05-24T00:00:00",
                *"isLastDayWithLoadedTerms": false,
                *"isLimitedDay": false,
                *"isMoreTermsThanCounter": null,
                *"message": "Schedules for that day are not available yet. Check in 1 day.",
                *"termsCounter": {
                    *"partialTermsCounters": [],
                    *"termsNumber": 0
                *},
                *"termsStatus": 4
            *}
        *]
    *}
*}
 *
 */
case class TermsIndexResponse(correlationId: String, termsForService: TermsForService) extends SerializableJsonObject

case class TermsForService(additionalData: AdditionalData, termsForDays: List[TermsForDay]) extends SerializableJsonObject

case class PreparationItem(header: Option[String], text: Option[String])

case class AdditionalData(isPreparationRequired: Boolean, preparationItems: List[PreparationItem])

case class TermsForDay(day: LocalDateTime, terms: List[Term]) extends SerializableJsonObject

case class Term(clinic: String, clinicId: Long, dateTimeFrom: LuxmedFunnyDateTime, dateTimeTo: LuxmedFunnyDateTime, doctor: Doctor,
                impedimentText: String, isAdditional: Boolean, isImpediment: Boolean, isTelemedicine: Boolean, roomId: Long,
                scheduleId: Long, serviceId: Long) extends SerializableJsonObject

case class TermExt(additionalData: AdditionalData, term: Term)  extends SerializableJsonObject

case class LuxmedFunnyDateTime(dateTimeTz: Option[ZonedDateTime] = None, dateTimeLocal: Option[LocalDateTime] = None) {
  def get: LocalDateTime = dateTimeLocal.getOrElse(dateTimeTz.map(_.toLocalDateTime).get)
}

