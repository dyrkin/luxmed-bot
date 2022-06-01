package com.lbs.api.json.model

trait Identified {
  def id: Long
  def name: String

  def toIdName: IdName = IdName(id, name)
}

case class IdName(id: Long, name: String) extends Identified {
  def optionalId: Option[Long] = Option(id).filterNot(_ == -1L)
}

object IdName {
  def from(id: java.lang.Long, name: String): IdName = new IdName(if (id != null) id else -1, name)
}
