
package com.lbs.api.json.model

/**
{
    "PreparationInfo": {
        "IsPreparationRequired": true
    }
}
  */
case class ChangeTermResponse(preparationInfo: PreparationInfo) extends SerializableJsonObject
