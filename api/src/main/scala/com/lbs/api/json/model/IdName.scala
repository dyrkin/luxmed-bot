
package com.lbs.api.json.model

case class IdName(id: Long, name: String) {
  def optionalId: Option[Long] = Option(id).filterNot(_ == -1L)
}
