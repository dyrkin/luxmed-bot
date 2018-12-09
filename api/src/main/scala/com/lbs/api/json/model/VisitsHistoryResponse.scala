
package com.lbs.api.json.model

/**
{
    "AreMoreVisits": false,
    "HistoricVisits": [
        {
            "ClinicName": "LX Wrocław - Szewska 3A",
            "DoctorName": "lek. stom. TARAS SHEVCZENKO",
            "HasRecommendations": false,
            "HasReferrals": false,
            "IsAdditional": false,
            "Links": [
                {
                    "Href": "/PatientPortalMobileAPI/api/visits/recommendations/222222222",
                    "Method": "GET",
                    "Rel": "get_recommendations"
                }
            ],
            "QuestionToVisit": {
                "IsAnswered": false,
                "IsAsked": false,
                "IsQuestionToVisitAvailable": false
            },
            "RateVisit": {
                "IsRatingAvailable": false,
                "IsVisitRated": false
            },
            "ReservationId": 222222222,
            "Service": {
                "Id": 6621,
                "Name": "Umówienie wizyty u stomatologa"
            },
            "VisitDate": {
                "FormattedDate": "17th Jan 2018, at 1:00 pm",
                "StartDateTime": "2018-01-17T13:00:00+02:00"
            }
        },
        {
            "ClinicName": "LX Wrocław - Szewska 3A",
            "DoctorName": "lek. stom. TARAS SHEVCZENKO",
            "HasRecommendations": false,
            "HasReferrals": false,
            "IsAdditional": false,
            "Links": [
                {
                    "Href": "/PatientPortalMobileAPI/api/visits/recommendations/999999999",
                    "Method": "GET",
                    "Rel": "get_recommendations"
                }
            ],
            "QuestionToVisit": {
                "IsAnswered": false,
                "IsAsked": false,
                "IsQuestionToVisitAvailable": false
            },
            "RateVisit": {
                "IsRatingAvailable": false,
                "IsVisitRated": false
            },
            "ReservationId": 999999999,
            "Service": {
                "Id": 3589,
                "Name": "Wypełnienie ubytku korony zęba na 2 powierzchniach"
            },
            "VisitDate": {
                "FormattedDate": "17th Jan 2018, at 1:00 pm",
                "StartDateTime": "2018-01-17T13:00:00+02:00"
            }
        }
    ]
}
  */
case class VisitsHistoryResponse(areMoreVisits: Boolean, historicVisits: List[HistoricVisit]) extends SerializableJsonObject

case class HistoricVisit(clinicName: String, doctorName: String, reservationId: Long, service: IdName, visitDate: VisitDate)
