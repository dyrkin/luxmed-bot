package com.lbs.server.repository.model

import jakarta.persistence.*

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Access(AccessType.FIELD)
class Credentials extends RecordId {
  @BeanProperty
  @Column(name = "user_id", nullable = false)
  var userId: JLong = uninitialized

  @BeanProperty
  @Column(name = "account_id", nullable = false)
  var accountId: JLong = uninitialized

  @BeanProperty
  @Column(nullable = false)
  var username: String = uninitialized

  @BeanProperty
  @Column(nullable = false)
  var password: String = uninitialized
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
