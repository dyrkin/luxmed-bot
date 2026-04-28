package com.lbs.server.repository.model

import jakarta.persistence.{Access, AccessType, Column, Entity}

import java.time.{LocalTime, ZonedDateTime}
import scala.beans.BeanProperty
import scala.compiletime.uninitialized
import scala.language.implicitConversions

@Entity
@Access(AccessType.FIELD)
class Monitoring extends RecordId {
  @BeanProperty
  @Column(name = "user_id", nullable = false)
  var userId: JLong = uninitialized

  @BeanProperty
  @Column(name = "username", nullable = false)
  var username: String = uninitialized

  @BeanProperty
  @Column(name = "account_id", nullable = false)
  var accountId: JLong = uninitialized

  @BeanProperty
  @Column(name = "chat_id", nullable = false)
  var chatId: String = uninitialized

  @BeanProperty
  @Column(name = "source_system_id", nullable = false)
  var sourceSystemId: JLong = uninitialized

  @BeanProperty
  @Column(name = "payer_id", nullable = false)
  var payerId: JLong = uninitialized

  @BeanProperty
  @Column(name = "city_id", nullable = false)
  var cityId: JLong = uninitialized

  @BeanProperty
  @Column(name = "city_name", nullable = false)
  var cityName: String = uninitialized

  @BeanProperty
  @Column(name = "clinic_id", nullable = true)
  var clinicId: JLong = uninitialized

  @BeanProperty
  @Column(name = "clinic_name", nullable = false)
  var clinicName: String = uninitialized

  @BeanProperty
  @Column(name = "service_id", nullable = false)
  var serviceId: JLong = uninitialized

  @BeanProperty
  @Column(name = "service_name", nullable = false)
  var serviceName: String = uninitialized

  @BeanProperty
  @Column(name = "doctor_id", nullable = true)
  var doctorId: JLong = uninitialized

  @BeanProperty
  @Column(name = "doctor_name", nullable = false)
  var doctorName: String = uninitialized

  @BeanProperty
  @Column(name = "date_from", nullable = false)
  var dateFrom: ZonedDateTime = uninitialized

  @BeanProperty
  @Column(name = "date_to", nullable = false)
  var dateTo: ZonedDateTime = uninitialized

  @BeanProperty
  @Column(name = "time_from", nullable = false)
  var timeFrom: LocalTime = uninitialized

  @BeanProperty
  @Column(name = "time_to", nullable = false)
  var timeTo: LocalTime = uninitialized

  @BeanProperty
  @Column(nullable = false)
  var autobook: Boolean = false

  @BeanProperty
  @Column(name = "rebook_if_exists", nullable = false)
  var rebookIfExists: Boolean = false

  @BeanProperty
  @Column(nullable = false)
  var created: ZonedDateTime = uninitialized

  @BeanProperty
  @Column(nullable = false)
  var active: Boolean = true

  @BeanProperty
  @Column(name = "time_offset", nullable = false)
  var offset: Int = uninitialized

  @BeanProperty
  @Column(name = "is_rehab", nullable = false)
  var isRehab: Boolean = false

  @BeanProperty
  @Column(name = "referral_id", nullable = true)
  var referralId: JLong = uninitialized

  @BeanProperty
  @Column(name = "referral_type_id", nullable = true)
  var referralTypeId: java.lang.Integer = uninitialized

  @BeanProperty
  @Column(name = "service_variant_id", nullable = true)
  var serviceVariantId: JLong = uninitialized
}

object Monitoring {
  def apply(
    userId: Long,
    username: String,
    accountId: Long,
    chatId: String,
    sourceSystemId: Long,
    payerId: Long,
    cityId: Long,
    cityName: String,
    clinicId: Option[Long],
    clinicName: String,
    serviceId: Long,
    serviceName: String,
    doctorId: Option[Long],
    doctorName: String,
    dateFrom: ZonedDateTime,
    dateTo: ZonedDateTime,
    autobook: Boolean = false,
    rebookIfExists: Boolean = false,
    created: ZonedDateTime = ZonedDateTime.now(),
    timeFrom: LocalTime,
    timeTo: LocalTime,
    active: Boolean = true,
    offset: Int,
    isRehab: Boolean = false,
    referralId: Option[Long] = None,
    referralTypeId: Option[Int] = None,
    serviceVariantId: Option[Long] = None
  ): Monitoring = {
    val monitoring = new Monitoring
    monitoring.userId = userId
    monitoring.username = username
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
    monitoring.isRehab = isRehab
    monitoring.referralId = referralId.map(Long.box).orNull
    monitoring.referralTypeId = referralTypeId.map(Int.box).orNull
    monitoring.serviceVariantId = serviceVariantId.map(Long.box).orNull
    monitoring
  }
}
