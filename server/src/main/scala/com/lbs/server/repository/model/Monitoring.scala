/**
  * MIT License
  *
  * Copyright (c) 2018 Yevhen Zadyra
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
package com.lbs.server.repository.model

import java.time.ZonedDateTime

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
  @Column(name = "time_of_day", nullable = true)
  var timeOfDay: JInt = 0

  @BeanProperty
  @Column(nullable = false)
  var autobook: Boolean = false

  @BeanProperty
  @Column(nullable = false)
  var created: ZonedDateTime = _

  @BeanProperty
  @Column(nullable = false)
  var active: Boolean = true
}

object Monitoring {
  def apply(userId: Long, accountId: Long, chatId: String, sourceSystemId: Long, cityId: Long, cityName: String, clinicId: Option[Long], clinicName: String,
            serviceId: Long, serviceName: String, doctorId: Option[Long], doctorName: String, dateFrom: ZonedDateTime,
            dateTo: ZonedDateTime, autobook: Boolean = false, created: ZonedDateTime = ZonedDateTime.now(), timeOfDay: Int,
            active: Boolean = true): Monitoring = {
    val monitoring = new Monitoring
    monitoring.userId = userId
    monitoring.accountId = accountId
    monitoring.chatId = chatId
    monitoring.sourceSystemId = sourceSystemId
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
    monitoring.timeOfDay = timeOfDay
    monitoring.autobook = autobook
    monitoring.created = created
    monitoring.active = active
    monitoring
  }
}