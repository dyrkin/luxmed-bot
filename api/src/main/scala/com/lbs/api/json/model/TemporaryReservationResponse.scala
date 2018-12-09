
package com.lbs.api.json.model

case class TemporaryReservationResponse(hasReferralRequired: Boolean, id: Long,
                                        informationMessages: List[String],
                                        mustTermOfReservedVisitBeChanged: Boolean) extends SerializableJsonObject
