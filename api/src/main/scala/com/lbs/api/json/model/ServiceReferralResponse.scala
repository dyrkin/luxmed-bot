package com.lbs.api.json.model

case class ServiceReferralResponse(
  serviceReferrals: List[ServiceReferralItem]
) extends SerializableJsonObject {
  def primaryReferral: Option[ServiceReferralItem] =
    serviceReferrals.sortBy(_.priority).headOption
}

case class ServiceReferralItem(
  id: Long,
  serviceVariantId: Long,
  serviceName: String,
  priority: Int = 1,
  requiresPreparation: Boolean = false
) extends SerializableJsonObject
