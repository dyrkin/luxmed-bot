
package com.lbs.api.json.model

case class LuxmedErrorsMap(errors: Map[String, List[String]]) extends SerializableJsonObject with LuxmedBaseError {
  override def message: String = errors.values.mkString("; ")
}