
package com.lbs.server.repository.model

import java.time.ZonedDateTime

import javax.persistence.{Access, AccessType, Column, Entity}

import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class ClinicHistory extends History with RecordId {
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
  @Column(name = "city_id", nullable = false)
  var cityId: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var time: ZonedDateTime = _
}

object ClinicHistory {
  def apply(accountId: Long, id: Long, name: String, cityId: Long, time: ZonedDateTime): ClinicHistory = {
    val clinic = new ClinicHistory
    clinic.accountId = accountId
    clinic.id = id
    clinic.name = name
    clinic.time = time
    clinic.cityId = cityId
    clinic
  }
}
