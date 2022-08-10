package com.lbs.server.repository.model

import java.time.LocalDateTime
import javax.persistence.{Access, AccessType, Column, Entity}
import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class Reminder extends RecordId {
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
  @Column(name = "city_name", nullable = false)
  var cityName: String = _

  @BeanProperty
  @Column(name = "clinic_name", nullable = false)
  var clinicName: String = _

  @BeanProperty
  @Column(name = "service_name", nullable = false)
  var serviceName: String = _

  @BeanProperty
  @Column(name = "doctor_name", nullable = false)
  var doctorName: String = _

  @BeanProperty
  @Column(name = "appointment_time", nullable = false)
  var appointmentTime: LocalDateTime = _

  @BeanProperty
  @Column(name = "remind_at_time", nullable = true)
  var remindAt: LocalDateTime = _

  @BeanProperty
  @Column(nullable = false)
  var active: Boolean = false
}

object Reminder {
  def apply(
    userId: Long,
    accountId: Long,
    chatId: String,
    sourceSystemId: Long,
    cityName: String,
    clinicName: String,
    serviceName: String,
    doctorName: String,
    appointmentTime: LocalDateTime,
    remindAt: LocalDateTime,
    active: Boolean
  ): Reminder = {
    val reminder = new Reminder
    reminder.userId = userId
    reminder.accountId = accountId
    reminder.chatId = chatId
    reminder.sourceSystemId = sourceSystemId
    reminder.cityName = cityName
    reminder.clinicName = clinicName
    reminder.serviceName = serviceName
    reminder.doctorName = doctorName
    reminder.appointmentTime = appointmentTime
    reminder.remindAt = remindAt
    reminder.active = active
    reminder
  }
}
