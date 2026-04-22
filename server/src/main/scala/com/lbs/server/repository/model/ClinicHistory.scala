package com.lbs.server.repository.model

import jakarta.persistence.{Access, AccessType, Column, Entity}

import java.time.ZonedDateTime
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Access(AccessType.FIELD)
class ClinicHistory extends History with RecordId {
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
  @Column(name = "city_id", nullable = false)
  var cityId: JLong = uninitialized

  @BeanProperty
  @Column(nullable = false)
  var time: ZonedDateTime = uninitialized
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
