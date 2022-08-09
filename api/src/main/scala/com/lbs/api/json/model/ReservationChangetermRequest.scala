package com.lbs.api.json.model

import java.time.LocalTime

/**
  * {
  *   "existingReservationId": 987654321,
  *   "term": {
  *     "date": "2021-07-03T05:30:00.000Z",
  *     "doctorId": 22222,
  *     "eReferralId": null,
  *     "facilityId": 33333,
  *     "parentReservationId": 987654321,
  *     "referralId": null,
  *     "referralRequired": false,
  *     "roomId": 55555,
  *     "scheduleId": 666666,
  *     "serviceVariantId": 777777,
  *     "temporaryReservationId": 8888888,
  *     "timeFrom": "08:30",
  *     "valuation": {
  *     "alternativePrice": null,
  *     "contractId": 99999,
  *     "isExternalReferralAllowed": false,
  *     "isReferralRequired": false,
  *     "payerId": 9111111,
  *     "price": 0,
  *     "productElementId": 9222222,
  *     "productId": 93333333,
  *     "productInContractId": 9444444,
  *     "requireReferralForPP": false,
  *     "valuationType": 1
  *   },
  *   "valuationId": null
  * }
  */
case class ReservationChangetermRequest(existingReservationId: Long, term: NewTerm) extends SerializableJsonObject

case class NewTerm(
  date: String,
  doctorId: Long,
  facilityId: Long,
  parentReservationId: Long,
  referralRequired: Boolean,
  roomId: Long,
  scheduleId: Long,
  serviceVariantId: Long,
  temporaryReservationId: Long,
  timeFrom: LocalTime,
  valuation: Valuation
) extends SerializableJsonObject
