package com.lbs.server.repository.model

import java.time.ZonedDateTime
import javax.persistence.{Access, AccessType, Column, Entity}
import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class CityHistory extends History with RecordId {
  @BeanProperty
  @Column(nullable = false)
  var id: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var name: String = _

  @BeanProperty
  @Column(name = "account_id", nullable = false)
  var accountId: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var time: ZonedDateTime = _
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
