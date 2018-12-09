
package com.lbs.api.json.model

/**
  *
  * {
  * "ReservedVisits": [
        *{
            *"CanBeCanceled": true,
            *"Clinic": {
                *"Id": 6,
                *"Name": "LX Wrocław - Szewska 3A"
            *},
            *"DoctorName": "lek. stom. TARAS SHEVCZENKO",
            *"Impediment": {
                *"ImpedimentText": "",
                *"IsImpediment": false
            *},
            *"IsAdditional": false,
            *"IsPreparationRequired": false,
            *"Links": [
                *{
                    *"Href": "/PatientPortalMobileAPI/api/visits/preparations/6621",
                    *"Method": "GET",
                    *"Rel": "get_preparations"
                *}
            *],
            *"ReservationId": 888888888,
            *"Service": {
                *"Id": 6621,
                *"Name": "Umówienie wizyty u stomatologa"
            *},
            *"VisitDate": {
                *"FormattedDate": "21rd May, Mon. at 3:00 pm",
                *"StartDateTime": "2018-05-21T15:00:00+02:00"
            *}
        *}
    *]
*}
  */
case class ReservedVisitsResponse(reservedVisits: List[ReservedVisit]) extends SerializableJsonObject

case class ReservedVisit(canBeCanceled: Boolean, clinic: IdName, doctorName: String,
                         reservationId: Long, service: IdName, visitDate: VisitDate) extends SerializableJsonObject
