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
  academicTitle: String,
  facilityGroupIds: Option[List[Long]],
  firstName: String,
  isEnglishSpeaker: Option[Boolean],
  genderId: Option[Long],
  id: Long,
  lastName: String
) extends Identified {
  override def name: String = firstName + " " + lastName
}
