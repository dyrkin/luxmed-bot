
package com.lbs.api.json.model

case class LuxmedCompositeError(errors: List[LuxmedCompositeMessage]) extends SerializableJsonObject with LuxmedBaseError

case class LuxmedCompositeMessage(errorCode: Int, message: String) extends SerializableJsonObject