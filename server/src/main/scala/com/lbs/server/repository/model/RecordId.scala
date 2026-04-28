package com.lbs.server.repository.model

import jakarta.persistence.*

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Access(AccessType.FIELD)
trait RecordId extends Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @BeanProperty
  var recordId: JLong = uninitialized
}
