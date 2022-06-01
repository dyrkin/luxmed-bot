
package com.lbs.server.repository.model

import javax.persistence._
import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class Credentials extends RecordId {
  @BeanProperty
  @Column(name = "user_id", nullable = false)
  var userId: JLong = _

  @BeanProperty
  @Column(name = "account_id", nullable = false)
  var accountId: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var username: String = _

  @BeanProperty
  @Column(nullable = false)
  var password: String = _
}

object Credentials {
  def apply(userId: JLong, accountId: JLong, username: String, password: String): Credentials = {
    val credentials = new Credentials
    credentials.userId = userId
    credentials.accountId = accountId
    credentials.username = username
    credentials.password = password
    credentials
  }
}
