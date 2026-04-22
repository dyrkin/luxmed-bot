package com.lbs.api.json.model

case class RehabFacilitiesResponse(
  locations: List[RehabLocation],
  facilities: List[RehabFacility]
) extends SerializableJsonObject

case class RehabLocation(
  id: Long,
  name: String
) extends SerializableJsonObject with Identified

case class RehabFacility(
  id: Long,
  name: String,
  locationId: Long,
  availabilityLevel: Int
) extends SerializableJsonObject with Identified

