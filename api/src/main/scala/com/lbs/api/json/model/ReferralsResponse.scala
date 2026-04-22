package com.lbs.api.json.model

case class ReferralsResponse(
  planned: List[Referral],
  unplanned: List[Referral]
) extends SerializableJsonObject

case class Referral(
  referralId: Option[Long],
  eReferralId: Option[Long],
  serviceVariant: ServiceVariantInfo,
  referralStatus: String,
  referralType: String,
  referralMode: String,
  expiredDate: Option[String],
  sourceVisitId: Long,
  proceduresAmount: Int,
  procedures: List[RehabProcedure],
  doctor: Option[String],
  issueDate: Option[String],
  searchVisitInfo: Option[SearchVisitInfo],
  tags: List[String]
) extends SerializableJsonObject

case class ServiceVariantInfo(
  id: Long,
  name: String,
  isTelemedicine: Boolean
) extends SerializableJsonObject

case class RehabProcedure(
  name: String,
  count: Int,
  preparation: Option[String]
) extends SerializableJsonObject

case class SearchVisitInfo(
  maxIntervalInDays: Int,
  numberOfSearchDays: Int,
  minAndMaxDateRange: Option[DateRange],
  defaultDateRange: Option[DateRange]
) extends SerializableJsonObject

case class DateRange(
  fromDate: String,
  toDate: String
) extends SerializableJsonObject
