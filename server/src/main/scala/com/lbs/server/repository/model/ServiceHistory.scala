package com.lbs.server.repository.model

import jakarta.persistence.{Access, AccessType, Column, Entity}

import java.time.ZonedDateTime
import scala.beans.BeanProperty
import scala.compiletime.uninitialized
import scala.language.implicitConversions

@Entity
@Access(AccessType.FIELD)
class ServiceHistory extends History with RecordId {
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
  @Column(name = "clinic_id", nullable = true)
  var clinicId: JLong = uninitialized

  @BeanProperty
  @Column(nullable = false)
  var time: ZonedDateTime = uninitialized
}

object ServiceHistory {
  def apply(
    accountId: Long,
    id: Long,
    name: String,
    cityId: Long,
    clinicId: Option[Long],
    time: ZonedDateTime
  ): ServiceHistory = {
    val service = new ServiceHistory
    service.accountId = accountId
    service.id = id
    service.name = name
    service.time = time
    service.cityId = cityId
    service.clinicId = clinicId
    service
  }
}
