
package com.lbs.api.json.model

case class LuxmedErrorsList(errors: List[LuxmedErrorsListElement]) extends SerializableJsonObject with LuxmedBaseError {
  override def message: String = errors.map(_.message).mkString("; ")
}

case class LuxmedErrorsListElement(errorCode: Int, message: String, additionalData: Map[String, String])  extends SerializableJsonObject