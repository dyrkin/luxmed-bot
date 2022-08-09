package com.lbs.api.json.model

import java.time.ZonedDateTime

/**
  * {
  *   "DataAvailableFrom": "2016-09-01T00:00:00+02:00",
  *   "Events": [
  *     {
  *       "AutoConfirmationInfo": {
  *           "Message": null,
  *           "Type": "None"
  *       },
  *       "Clinic": {
  *         "Address": "WOŁOWSKA 20",
  *         "City": "WROCŁAW",
  *         "Id": 42,
  *         "Name": "LX Wrocław - Wołowska 20"
  *       },
  *       "ConfirmationInfo": null,
  *       "Date": "2021-06-02T07:45:00+02:00",
  *       "DateTo": "2021-06-02T08:15:00+02:00",
  *       "Doctor": {
  *         "Id": 111111,
  *         "Lastname": "SHEVCHENKO",
  *         "Name": "TARAS",
  *         "Sex": "Male",
  *         "Title": "lek. med."
  *       },
  *       "DownloadLinks": [],
  *       "EventId": 2222222,
  *       "EventType": "Visit",
  *       "FromEreferral": false,
  *       "HasImpediments": false,
  *       "HasQuestionnaireBeforeService": false,
  *       "IsOverbooked": false,
  *       "IsPaymentRequired": false,
  *       "IsPreparationRequired": false,
  *       "IsServiceWithOverbookingRegularDistribution": true,
  *       "Links": [
  *         {
  *           "ApiVersion": 1,
  *           "Href": "/PatientPortalMobileAPI/api/events/reservation/Visit/2222222/detail",
  *           "Method": "GET",
  *           "Rel": "events_detail"
  *         },
  *         {
  *           "ApiVersion": 1,
  *           "Href": "/PatientPortalMobileAPI/api/events/Visit/2222222",
  *           "Method": "DELETE",
  *           "Rel": "delete_reservation"
  *         },
  *         {
  *           "ApiVersion": 1,
  *           "Href": "/PatientPortalMobileAPI/api/visits/reserved/2222222/can-term-be-changed",
  *           "Method": "GET",
  *           "Rel": "get_can_term_be_changed"
  *         }
  *         ],
  *       "OnlinePaymentType": "Possible",
  *       "PaymentState": "None",
  *       "ReferralType": "None",
  *       "Status": "Reserved",
  *       "Title": "Internista",
  *       "Type": "Timeline_Visit_ReservedVisit"
  *     },
  *     {
  *       "Date": "2021-03-27T16:45:00+02:00",
  *       "DateTo": "2021-03-27T17:15:00+02:00",
  *       "Doctor": {
  *         "Id": 11111,
  *         "Lastname": "LESJA",
  *         "Name": "UKRAINKA",
  *         "Sex": "Female",
  *         "Title": "lek. med."
  *       },
  *       "DownloadLinks": [],
  *       "EventId": 3333333,
  *       "EventType": "Telemedicine",
  *       "HasPrescription": false,
  *       "HasRecommendations": true,
  *       "HasReferrals": false,
  *       "IsServiceWithOverbookingRegularDistribution": true,
  *       "Links": [
  *         {
  *           "ApiVersion": 1,
  *           "Href": "/PatientPortalMobileAPI/api/events/reservation/Telemedicine/3333333/detail",
  *           "Method": "GET",
  *           "Rel": "events_detail"
  *         }
  *       ],
  *       "ReferralType": "None",
  *       "Status": "Realized",
  *       "Title": "Centrum Leczenia Infekcji - konsultacja telefoniczna",
  *       "Type": "Timeline_Telemedicine_RealizedTelemedicineVisit"
  *       }
  *     ],
  *   "IsEndOfList": false,
  *   "ServerDateTime": "2021-07-01T14:32:00+02:00"
  * }
  */
case class EventsResponse(events: List[Event]) extends SerializableJsonObject

case class Event(
  date: ZonedDateTime,
  clinic: Option[EventClinic],
  doctor: EventDoctor,
  eventId: Long,
  status: String,
  title: String
)

case class EventClinic(address: String, city: String)

case class EventDoctor(lastname: String, name: String)
