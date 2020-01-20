
package com.lbs.api.json.model


object IdName {
  def from(id: java.lang.Long, name: String): IdName = new IdName(if (id != null) id else -1, name)
}

case class IdName(id: Long, name: String) {
  def optionalId: Option[Long] = Option(id).filterNot(_ == -1L)
}
