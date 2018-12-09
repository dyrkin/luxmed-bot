
package com.lbs.api.json.model

/**
{
    "Cities": [
        {
            "Id": 5,
            "Name": "Wrocław"
        }
    ],
    "Clinics": [
        {
            "Id": 1405,
            "Name": "Konsylium Wrocław - Legnicka 40"
        },
        {
            "Id": 7,
            "Name": "LX Wrocław - Kwidzyńska 6"
        }
    ],
    "DefaultPayer": {
        "Id": 22222,
        "Name": "FIRMA POLAND SP. Z O.O."
    },
    "Doctors": [
        {
            "Id": 38275,
            "Name": "ANNA ABRAMCZYK lek. med."
        },
        {
            "Id": 15565,
            "Name": "ANDRZEJ ANDEWSKI dr n. med."
        }
    ],
    "Languages": [
        {
            "Id": 11,
            "Name": "english"
        },
        {
            "Id": 10,
            "Name": "polish"
        }
    ],
    "Payers": [
        {
            "Id": 22222,
            "Name": "FIRMA POLAND SP. Z O.O."
        }
    ],
    "Services": [
        {
            "Id": 5857,
            "Name": "Audiometr standardowy"
        },
        {
            "Id": 7976,
            "Name": "Audiometr standardowy - audiometria nadprogowa"
        }
    ]
}
  */
case class ReservationFilterResponse(cities: List[IdName], clinics: List[IdName], defaultPayer: Option[IdName],
                                     doctors: List[IdName], languages: List[IdName], payers: List[IdName],
                                     services: List[IdName]) extends SerializableJsonObject

