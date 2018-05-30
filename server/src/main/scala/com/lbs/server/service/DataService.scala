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
package com.lbs.server.service

import java.time.ZonedDateTime

import com.lbs.api.json.model.IdName
import com.lbs.bot.model.MessageSource
import com.lbs.server.actor.Book.BookingData
import com.lbs.server.repository.DataRepository
import com.lbs.server.repository.model._
import com.lbs.server.util.ServerModelConverters._
import javax.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DataService {

  @Autowired
  private var dataRepository: DataRepository = _

  def getLatestCities(userId: Long): Seq[IdName] = {
    dataRepository.getCityHistory(userId).mapTo[IdName]
  }

  def getLatestClinicsByCityId(userId: Long, cityId: Long): Seq[IdName] = {
    dataRepository.getClinicHistory(userId, cityId).mapTo[IdName]
  }

  def getLatestServicesByCityIdAndClinicId(userId: Long, cityId: Long, clinicId: Option[Long]): Seq[IdName] = {
    dataRepository.getServiceHistory(userId, cityId, clinicId).mapTo[IdName]
  }

  def getLatestDoctorsByCityIdAndClinicIdAndServiceId(userId: Long, cityId: Long, clinicId: Option[Long], serviceId: Long): Seq[IdName] = {
    dataRepository.getDoctorHistory(userId, cityId, clinicId, serviceId).mapTo[IdName]
  }

  def getCredentials(userId: Long): Option[Credentials] = {
    dataRepository.findCredentials(userId)
  }

  @Transactional
  def submitBug(userId: Long, sourceSystemId: Long, details: String): Option[Long] = {
    dataRepository.saveEntity(Bug(userId, sourceSystemId, details)).recordId
  }

  def getBugs(chatId: Long): Seq[Bug] = {
    dataRepository.getBugs(chatId)
  }

  @Transactional
  def saveMonitoring(monitoring: Monitoring): Monitoring = {
    dataRepository.saveEntity(monitoring)
  }

  def getActiveMonitorings: Seq[Monitoring] = {
    dataRepository.getActiveMonitorings
  }

  def getActiveMonitoringsCount(userId: Long): Long = {
    dataRepository.getActiveMonitoringsCount(userId)
  }

  def getActiveMonitorings(userId: Long): Seq[Monitoring] = {
    dataRepository.getActiveMonitorings(userId)
  }

  def findActiveMonitoring(userId: Long, cityId: Long, serviceId: Long): Option[Monitoring] = {
    dataRepository.findActiveMonitoring(userId, cityId, serviceId)
  }

  def getActiveMonitoringsSince(since: ZonedDateTime): Seq[Monitoring] = {
    dataRepository.getActiveMonitoringsSince(since)
  }

  def findMonitoring(userId: Long, monitoringId: Long): Option[Monitoring] = {
    dataRepository.findMonitoring(userId, monitoringId)
  }

  def findSettings(userId: Long): Option[Settings] = {
    dataRepository.findSettings(userId)
  }

  def findUserIdBySource(source: MessageSource): Option[Long] = {
    dataRepository.findUserId(source.chatId, source.sourceSystem.id).map(_.toLong)
  }

  def findCredentialsByUsername(username: String): Option[Credentials] = {
    dataRepository.findCredentialsByUsername(username)
  }

  @Transactional
  def saveSettings(settings: Settings): Settings = {
    dataRepository.saveEntity(settings)
  }

  @Transactional
  def saveCredentials(source: MessageSource, username: String, password: String): Credentials = {
    val credentialsMaybe = findCredentialsByUsername(username)
    credentialsMaybe match {
      case Some(credentials) => //user already exists
        val sourceMaybe = dataRepository.findSource(source.chatId, source.sourceSystem.id, credentials.userId)
        sourceMaybe match {
          case Some(_) => //source already exists. Just update credentials
          case None => //add new source
            val src = Source(source.chatId, source.sourceSystem.id, credentials.userId)
            dataRepository.saveEntity(src)
        }
        credentials.username = username
        credentials.password = password
        dataRepository.saveEntity(credentials)
      case None => //new user
        val user = dataRepository.saveEntity(new SystemUser)
        val src = Source(source.chatId, source.sourceSystem.id, user.recordId)
        dataRepository.saveEntity(src)
        val credentials = Credentials(user.recordId, username, password)
        dataRepository.saveEntity(credentials)
    }
  }

  @Transactional
  def storeAppointment(userId: Long, bookingData: BookingData): Unit = {
    val time = ZonedDateTime.now()
    val cityId = bookingData.cityId
    val clinicId = bookingData.clinicId
    val serviceId = bookingData.serviceId
    val doctorId = bookingData.doctorId

    val city = CityHistory(userId, cityId.id, cityId.name, time)
    dataRepository.saveEntity(city)

    val clinicMaybe = clinicId.optionalId.map(id => ClinicHistory(userId, id, clinicId.name, cityId.id, time))
    clinicMaybe.foreach(dataRepository.saveEntity)

    val service = ServiceHistory(userId, serviceId.id, serviceId.name, cityId.id, clinicId.optionalId, time)
    dataRepository.saveEntity(service)

    val doctorMaybe = doctorId.optionalId.map(id => DoctorHistory(userId, id, doctorId.name, cityId.id, clinicId.optionalId, serviceId.id, time))
    doctorMaybe.foreach(dataRepository.saveEntity)
  }
}
