
package com.lbs.server.repository.model

import javax.persistence._

import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class SystemUser extends RecordId {
  @BeanProperty
  @Column(name = "active_account_id", nullable = false)
  var activeAccountId: JLong = _
}

object SystemUser {
  def apply(activeAccountId: Long): SystemUser = {
    val user = new SystemUser
    user.activeAccountId = activeAccountId
    user
  }
}