package com.lbs.api.json.model

/**
  * {
  *    "doctors": [
  *        {
  *            "academicTitle": "dr n. med.",
  *            "facilityGroupIds": [
  *                78
  *            ],
  *            "firstName": "TARAS",
  *            "id": 111111,
  *            "isEnglishSpeaker": true,
  *            "lastName": "SHEVCHENKO"
  *        },
  *        {
  *            "academicTitle": "lek. med.",
  *            "facilityGroupIds": [
  *                78,
  *                127
  *            ],
  *            "firstName": "VLADIMIR",
  *            "id": 22222,
  *            "isEnglishSpeaker": false,
  *            "lastName": "ZELENSKIY"
  *        }
  *    ],
  *    "facilities": [
  *        {
  *            "id": 78,
  *            "name": "ul. Fabryczna 6"
  *        },
  *        {
  *            "id": 127,
  *            "name": "ul. Kwidzyńska 6"
  *        }
  *    ]
  * }
  */
case class FacilitiesAndDoctors(doctors: List[Doctor], facilities: List[IdName]) extends SerializableJsonObject
