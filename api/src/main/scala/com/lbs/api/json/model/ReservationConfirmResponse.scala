package com.lbs.api.json.model

/**
  * {
  *    "errors": [],
  *    "hasErrors": false,
  *    "hasWarnings": false,
  *    "value": {
  *        "canSelfConfirm": false,
  *        "npsToken": "babababa-9282-1662-a525-ababbabaa",
  *        "reservationId": 2222222,
  *        "serviceInstanceId": 33333333
  *    },
  *    "warnings": []
  * }
  */
case class ReservationConfirmResponse(
  errors: List[String],
  warnings: List[String],
  hasErrors: Boolean,
  hasWarnings: Boolean,
  value: ReservationConfirmValue
) extends SerializableJsonObject

case class ReservationConfirmValue(
  canSelfConfirm: Boolean,
  npsToken: String,
  reservationId: Long,
  serviceInstanceId: Long
) extends SerializableJsonObject
