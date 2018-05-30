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
