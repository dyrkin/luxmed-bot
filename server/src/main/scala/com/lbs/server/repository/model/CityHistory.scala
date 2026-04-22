package com.lbs.server.repository.model

import jakarta.persistence.{Access, AccessType, Column, Entity}

import java.time.ZonedDateTime
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Access(AccessType.FIELD)
class CityHistory extends History with RecordId {
  @BeanProperty
  @Column(nullable = false)
  var id: JLong = uninitialized

  @BeanProperty
  @Column(nullable = false)
  var name: String = uninitialized

  @BeanProperty
  @Column(name = "account_id", nullable = false)
  var accountId: JLong = uninitialized

  @BeanProperty
  @Column(nullable = false)
  var time: ZonedDateTime = uninitialized
}

object CityHistory {
  def apply(accountId: Long, id: Long, name: String, time: ZonedDateTime): CityHistory = {
    val city = new CityHistory
    city.accountId = accountId
    city.id = id
    city.name = name
    city.time = time
    city
  }
}
