
package com.lbs.server.repository.model

import java.time.{LocalTime, ZonedDateTime}
import javax.persistence.{Access, AccessType, Column, Entity}
import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class Monitoring extends RecordId {
  @BeanProperty
  @Column(name = "user_id", nullable = false)
  var userId: JLong = _

  @BeanProperty
  @Column(name = "account_id", nullable = false)
  var accountId: JLong = _

  @BeanProperty
  @Column(name = "chat_id", nullable = false)
  var chatId: String = _

  @BeanProperty
  @Column(name = "source_system_id", nullable = false)
  var sourceSystemId: JLong = _

  @BeanProperty
  @Column(name = "payer_id", nullable = false)
  var payerId: JLong = _

  @BeanProperty
  @Column(name = "city_id", nullable = false)
  var cityId: JLong = _

  @BeanProperty
  @Column(name = "city_name", nullable = false)
  var cityName: String = _

  @BeanProperty
  @Column(name = "clinic_id", nullable = true)
  var clinicId: JLong = _

  @BeanProperty
  @Column(name = "clinic_name", nullable = false)
  var clinicName: String = _

  @BeanProperty
  @Column(name = "service_id", nullable = false)
  var serviceId: JLong = _

  @BeanProperty
  @Column(name = "service_name", nullable = false)
  var serviceName: String = _

  @BeanProperty
  @Column(name = "doctor_id", nullable = true)
  var doctorId: JLong = _

  @BeanProperty
  @Column(name = "doctor_name", nullable = false)
  var doctorName: String = _

  @BeanProperty
  @Column(name = "date_from", nullable = false)
  var dateFrom: ZonedDateTime = _

  @BeanProperty
  @Column(name = "date_to", nullable = false)
  var dateTo: ZonedDateTime = _

  @BeanProperty
  @Column(name = "time_from", nullable = false)
  var timeFrom: LocalTime = _

  @BeanProperty
  @Column(name = "time_to", nullable = false)
  var timeTo: LocalTime = _

  @BeanProperty
  @Column(nullable = false)
  var autobook: Boolean = false

  @BeanProperty
  @Column(name = "rebook_if_exists", nullable = false)
  var rebookIfExists: Boolean = false

  @BeanProperty
  @Column(nullable = false)
  var created: ZonedDateTime = _

  @BeanProperty
  @Column(nullable = false)
  var active: Boolean = true

  @BeanProperty
  @Column(name = "time_offset", nullable = false)
  var offset: Int = 0
}

object Monitoring {
  def apply(userId: Long, accountId: Long, chatId: String, sourceSystemId: Long, payerId: Long, cityId: Long, cityName: String, clinicId: Option[Long], clinicName: String,
            serviceId: Long, serviceName: String, doctorId: Option[Long], doctorName: String, dateFrom: ZonedDateTime,
            dateTo: ZonedDateTime, autobook: Boolean = false, rebookIfExists: Boolean = false, created: ZonedDateTime = ZonedDateTime.now(), timeFrom: LocalTime, timeTo: LocalTime,
            active: Boolean = true, offset: Int): Monitoring = {
    val monitoring = new Monitoring
    monitoring.userId = userId
    monitoring.accountId = accountId
    monitoring.chatId = chatId
    monitoring.sourceSystemId = sourceSystemId
    monitoring.payerId = payerId
    monitoring.cityId = cityId
    monitoring.cityName = cityName
    monitoring.clinicId = clinicId
    monitoring.clinicName = clinicName
    monitoring.serviceId = serviceId
    monitoring.serviceName = serviceName
    monitoring.doctorId = doctorId
    monitoring.doctorName = doctorName
    monitoring.dateFrom = dateFrom
    monitoring.dateTo = dateTo
    monitoring.timeFrom = timeFrom
    monitoring.timeTo = timeTo
    monitoring.autobook = autobook
    monitoring.rebookIfExists = rebookIfExists
    monitoring.created = created
    monitoring.active = active
    monitoring.offset = offset
    monitoring
  }
}