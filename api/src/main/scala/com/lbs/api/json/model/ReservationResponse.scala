
package com.lbs.api.json.model


/**
{
    "PreparationInfo": {
        "IsPreparationRequired": true
    },
    "ReservedVisitsLimitInfo": {
        "CanReserve": true,
        "HasPatientLimit": false,
        "MaxReservedVisitsCount": null,
        "Message": "",
        "ReservedVisitsCount": null
    }
}
  */
case class ReservationResponse(preparationInfo: PreparationInfo, reservedVisitsLimitInfo: ReservedVisitsLimitInfo) extends SerializableJsonObject

case class PreparationInfo(isPreparationRequired: Boolean) extends SerializableJsonObject

case class ReservedVisitsLimitInfo(canReserve: Boolean, hasPatientLimit: Boolean, maxReservedVisitsCount: Option[Int],
                                   message: String, reservedVisitsCount: Option[Int]) extends SerializableJsonObject
