package com.lbs.api.json.model

/**
  * {
  *            "academicTitle": "dr n. med.",
  *            "facilityGroupIds": [
  *                78
  *            ],
  *            "firstName": "TARAS",
  *            "id": 11111,
  *            "isEnglishSpeaker": true,
  *            "lastName": "SHEVCHENKO"
  *        }
  */

case class Doctor(
  academicTitle: Option[String],
  facilityGroupIds: Option[List[Long]],
  firstName: Option[String],
  isEnglishSpeaker: Option[Boolean],
  genderId: Option[Long],
  id: Long,
  lastName: Option[String]
) extends Identified {
  override def name: String = (firstName, lastName) match {
    case (Some(first), Some(last)) => s"$first $last"
    case (None, Some(last)) => last
    case (Some(first), None) => first
    case (None, None) => "N/A"
  }
}
