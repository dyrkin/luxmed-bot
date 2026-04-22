package com.lbs.server.repository.model

import jakarta.persistence.*

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Table(name = "\"system_user\"")
@Access(AccessType.FIELD)
class SystemUser extends RecordId {
  @BeanProperty
  @Column(name = "active_account_id", nullable = false)
  var activeAccountId: JLong = uninitialized
}

object SystemUser {
  def apply(activeAccountId: Long): SystemUser = {
    val user = new SystemUser
    user.activeAccountId = activeAccountId
    user
  }
}
