
package com.lbs.server.repository.model

import java.time.ZonedDateTime

import javax.persistence.{Access, AccessType, Column, Entity}

import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class ServiceHistory extends History with RecordId {
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
  @Column(name = "clinic_id", nullable = true)
  var clinicId: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var time: ZonedDateTime = _
}

object ServiceHistory {
  def apply(accountId: Long, id: Long, name: String, cityId: Long, clinicId: Option[Long], time: ZonedDateTime): ServiceHistory = {
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
