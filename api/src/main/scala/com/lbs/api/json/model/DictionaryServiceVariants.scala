
package com.lbs.api.json.model

/**
[
  {
    "actionCode": "",
    "children": [
      {
        "actionCode": null,
        "children": [],
        "expanded": false,
        "id": 4502,
        "isTelemedicine": false,
        "name": "Consultation with a general practitioner",
        "paymentType": 2,
        "type": 0
      },
      {
        "actionCode": null,
        "children": [],
        "expanded": false,
        "id": 4480,
        "isTelemedicine": false,
        "name": "Gynaecological consultation",
        "paymentType": 2,
        "type": 0
      }
    ],
    "expanded": true,
    "id": 2,
    "isTelemedicine": false,
    "name": "Most popular",
    "paymentType": 0,
    "type": 2
  },
  {
    "actionCode": "",
    "children": [
      {
        "actionCode": null,
        "children": [],
        "expanded": false,
        "id": 4387,
        "isTelemedicine": false,
        "name": "Allergologist consultation",
        "paymentType": 2,
        "type": 0
      }
    ],
    "expanded": true,
    "id": 1,
    "isTelemedicine": false,
    "name": "On-site consultations",
    "paymentType": 0,
    "type": 1
  },
  {
    "actionCode": "",
    "children": [
      {
        "actionCode": null,
        "children": [],
        "expanded": false,
        "id": 13764,
        "isTelemedicine": true,
        "name": "Telephone consultation - Allergist",
        "paymentType": 2,
        "type": 0
      },
      {
        "actionCode": null,
        "children": [],
        "expanded": false,
        "id": 13775,
        "isTelemedicine": true,
        "name": "Telephone consultation - Cardiologist",
        "paymentType": 2,
        "type": 0
      },
      {
        "actionCode": null,
        "children": [],
        "expanded": false,
        "id": 13800,
        "isTelemedicine": true,
        "name": "Telephone consultation - Dentist",
        "paymentType": 2,
        "type": 0
      },
      {
        "actionCode": null,
        "children": [],
        "expanded": false,
        "id": 13766,
        "isTelemedicine": true,
        "name": "Telephone consultation - Dermatologist",
        "paymentType": 2,
        "type": 0
      }
    ],
    "expanded": false,
    "id": 13,
    "isTelemedicine": false,
    "name": "Telephone consultations",
    "paymentType": 0,
    "type": 1
  },
  {
    "actionCode": "",
    "children": [
      {
        "actionCode": null,
        "children": [],
        "expanded": false,
        "id": 8904,
        "isTelemedicine": false,
        "name": "Arranging an appointment with a dental surgeon",
        "paymentType": 2,
        "type": 0
      },
      {
        "actionCode": null,
        "children": [],
        "expanded": false,
        "id": 6817,
        "isTelemedicine": false,
        "name": "Arranging an appointment with a dental hygienist",
        "paymentType": 2,
        "type": 0
      },
      {
        "actionCode": null,
        "children": [],
        "expanded": false,
        "id": 6621,
        "isTelemedicine": false,
        "name": "Arranging an appointment with a dentist",
        "paymentType": 2,
        "type": 0
      }
    ],
    "expanded": false,
    "id": 4,
    "isTelemedicine": false,
    "name": "Dentist",
    "paymentType": 0,
    "type": 1
  },
  {
    "actionCode": "WIZYTY_DOMOWE",
    "children": [],
    "expanded": false,
    "id": 14,
    "isTelemedicine": false,
    "name": "Home visits",
    "paymentType": 0,
    "type": 0
  },
  {
    "actionCode": "NFZ",
    "children": [],
    "expanded": false,
    "id": 12,
    "isTelemedicine": false,
    "name": "NHF visits",
    "paymentType": 0,
    "type": 0
  }
]
 *
 */
case class DictionaryServiceVariants(override val id: Long, override val name: String, expanded: Boolean, children: List[DictionaryServiceVariants], isTelemedicine: Boolean, paymentType: Long) extends Identified with SerializableJsonObject {
    def flatten: List[DictionaryServiceVariants] = List(this) ::: children.flatMap(_.flatten)
}
