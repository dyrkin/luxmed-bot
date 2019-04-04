
package com.lbs.api.json.model

case class LuxmedErrors(errors: Map[String, List[String]]) extends SerializableJsonObject with LuxmedBaseError