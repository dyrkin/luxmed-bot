
package com.lbs.server.repository.model

import java.time.ZonedDateTime

import javax.persistence.{Access, AccessType, Column, Entity}

import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class Bug extends RecordId {
  @BeanProperty
  @Column(name = "user_id", nullable = false)
  var userId: JLong = _

  @BeanProperty
  @Column(name = "source_system_id", nullable = false)
  var sourceSystemId: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var details: String = _

  @BeanProperty
  @Column(nullable = false)
  var resolved: Boolean = false

  @BeanProperty
  @Column(nullable = false)
  var submitted: ZonedDateTime = ZonedDateTime.now()
}

object Bug {
  def apply(userId: Long, sourceSystemId: Long, details: String, resolved: Boolean = false, submitted: ZonedDateTime = ZonedDateTime.now()): Bug = {
    val bug = new Bug
    bug.userId = userId
    bug.sourceSystemId = sourceSystemId
    bug.details = details
    bug.resolved = resolved
    bug.submitted = submitted
    bug
  }
}