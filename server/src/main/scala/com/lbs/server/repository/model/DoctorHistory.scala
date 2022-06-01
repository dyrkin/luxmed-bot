
package com.lbs.server.repository.model

import java.time.ZonedDateTime
import javax.persistence.{Access, AccessType, Column, Entity}
import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class DoctorHistory extends History with RecordId {
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
  @Column(name = "service_id", nullable = false)
  var serviceId: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var time: ZonedDateTime = _
}

object DoctorHistory {
  def apply(accountId: Long, id: Long, name: String, cityId: Long, clinicId: Option[Long], serviceId: Long, time: ZonedDateTime): DoctorHistory = {
    val doctor = new DoctorHistory
    doctor.accountId = accountId
    doctor.id = id
    doctor.name = name
    doctor.time = time
    doctor.cityId = cityId
    doctor.clinicId = clinicId
    doctor.serviceId = serviceId
    doctor
  }
}
