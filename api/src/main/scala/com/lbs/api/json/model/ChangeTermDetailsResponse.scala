
package com.lbs.api.json.model

/**
{
    "AvailableNewPayers": [
        {
            "Id": 45185,
            "IsFeeForService": false,
            "Name": "Moja firma SP. Z O.O."
        }
    ],
    "CityId": 5,
    "Payer": {
        "Id": 45185,
        "IsFeeForService": false,
        "Name": "Moja firma SP. Z O.O."
    }
}
  */
case class ChangeTermDetailsResponse(availableNewPayers: ShortPayerDetails, cityId: Long, payer: ShortPayerDetails) extends SerializableJsonObject

case class ShortPayerDetails(id: Long, isFeeForService: Boolean, name: String) extends SerializableJsonObject