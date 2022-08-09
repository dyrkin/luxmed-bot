package com.lbs.server.repository.model

import javax.persistence._
import scala.beans.BeanProperty

@Access(AccessType.FIELD)
trait RecordId extends Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @BeanProperty
  var recordId: JLong = _
}
