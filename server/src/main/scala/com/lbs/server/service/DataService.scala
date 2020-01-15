
package com.lbs.server.service

import java.time.ZonedDateTime

import com.lbs.api.json.model.IdName
import com.lbs.bot.model.MessageSource
import com.lbs.server.conversation.Book.BookingData
import com.lbs.server.repository.DataRepository
import com.lbs.server.repository.model._
import com.lbs.server.util.ServerModelConverters._
import javax.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DataService {

  @Autowired
  private[service] var dataRepository: DataRepository = _

  def getLatestCities(accountId: Long): Seq[IdName] = {
    dataRepository.getCityHistory(accountId).mapTo[Seq[IdName]]
  }

  def getLatestClinicsByCityId(userId: Long, cityId: Long): Seq[IdName] = {
    dataRepository.getClinicHistory(userId, cityId).mapTo[Seq[IdName]]
  }

  def getLatestServicesByCityIdAndClinicId(userId: Long, cityId: Long, clinicId: Option[Long]): Seq[IdName] = {
    dataRepository.getServiceHistory(userId, cityId, clinicId).mapTo[Seq[IdName]]
  }

  def getLatestDoctorsByCityIdAndClinicIdAndServiceId(userId: Long, cityId: Long, clinicId: Option[Long], serviceId: Long): Seq[IdName] = {
    dataRepository.getDoctorHistory(userId, cityId, clinicId, serviceId).mapTo[Seq[IdName]]
  }

  def getCredentials(accountId: Long): Option[Credentials] = {
    dataRepository.findCredentials(accountId)
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

  def getActiveMonitoringsCount(accountId: Long): Long = {
    dataRepository.getActiveMonitoringsCount(accountId)
  }

  def getActiveMonitorings(accountId: Long): Seq[Monitoring] = {
    dataRepository.getActiveMonitorings(accountId)
  }

  def getActiveMonitoringsSince(since: ZonedDateTime): Seq[Monitoring] = {
    dataRepository.getActiveMonitoringsSince(since)
  }

  def findMonitoring(accountId: Long, monitoringId: Long): Option[Monitoring] = {
    dataRepository.findMonitoring(accountId, monitoringId)
  }

  def findSettings(userId: Long): Option[Settings] = {
    dataRepository.findSettings(userId)
  }

  def findUserAndAccountIdBySource(source: MessageSource): Option[(Long, Long)] = {
    val userIdMaybe = dataRepository.findUserId(source.chatId, source.sourceSystem.id).map(_.toLong)
    userIdMaybe.flatMap(userId => dataRepository.findAccountId(userId).map(_.toLong).map(accountId => userId -> accountId))
  }

  def findCredentialsByUsername(username: String, userId: Long): Option[Credentials] = {
    dataRepository.findCredentialsByUsername(username, userId)
  }

  def getUserCredentials(userId: Long): Seq[Credentials] = {
    dataRepository.getUserCredentials(userId)
  }

  def findUserCredentialsByAccountId(userId: Long, accountId: Long): Option[Credentials] = {
    dataRepository.findUserCredentialsByUserIdAndAccountId(userId, accountId)
  }

  def findUser(userId: Long): Option[SystemUser] = {
    dataRepository.findUser(userId)
  }

  @Transactional
  def saveUser(user: SystemUser): SystemUser = {
    dataRepository.saveEntity(user)
  }

  @Transactional
  def saveSettings(settings: Settings): Settings = {
    dataRepository.saveEntity(settings)
  }

  @Transactional
  def saveCredentials(source: MessageSource, username: String, password: String): Credentials = {
    val userMaybe = dataRepository.findUserIdBySource(source.chatId, source.sourceSystem.id).flatMap {
      userId => dataRepository.findUser(userId).map(_ -> userId)
    }
    userMaybe match {
      case Some((user, userId)) =>
        val credentialsMaybe = findCredentialsByUsername(username, userId)
        credentialsMaybe match {
          case Some(credentials) => //user already exists
            val sourceMaybe = dataRepository.findSource(source.chatId, source.sourceSystem.id, credentials.userId)
            sourceMaybe match {
              case Some(_) => //source already exists. Just update credentials
              case None => //add new source
                val src = Source(source.chatId, source.sourceSystem.id, credentials.userId)
                dataRepository.saveEntity(src)
            }
            user.activeAccountId = credentials.accountId
            dataRepository.saveEntity(user)
            credentials.username = username
            credentials.password = password
            dataRepository.saveEntity(credentials)
          case None =>
            val account = dataRepository.saveEntity(new Account)
            user.activeAccountId = account.recordId
            dataRepository.saveEntity(user)
            val sourceMaybe = dataRepository.findSource(source.chatId, source.sourceSystem.id, user.recordId)
            sourceMaybe match {
              case Some(_) => //source already exists. Just save credentials
              case None => //add new source
                val src = Source(source.chatId, source.sourceSystem.id, user.recordId)
                dataRepository.saveEntity(src)
            }
            val credentials = Credentials(user.recordId, account.recordId, username, password)
            dataRepository.saveEntity(credentials)
        }

      case None => //everything is new
        val account = dataRepository.saveEntity(new Account)
        val user = dataRepository.saveEntity(SystemUser(account.recordId))
        val src = Source(source.chatId, source.sourceSystem.id, user.recordId)
        dataRepository.saveEntity(src)
        val credentials = Credentials(user.recordId, account.recordId, username, password)
        dataRepository.saveEntity(credentials)
    }
  }

  @Transactional
  def storeAppointment(accountId: Long, bookingData: BookingData): Unit = {
    val time = ZonedDateTime.now()
    val cityId = bookingData.cityId
    val clinicId = bookingData.clinicId
    val serviceId = bookingData.serviceId
    val doctorId = bookingData.doctorId

    val city = CityHistory(accountId, cityId.id, cityId.name, time)
    dataRepository.saveEntity(city)

    val clinicMaybe = clinicId.optionalId.map(id => ClinicHistory(accountId, id, clinicId.name, cityId.id, time))
    clinicMaybe.foreach(dataRepository.saveEntity)

    val service = ServiceHistory(accountId, serviceId.id, serviceId.name, cityId.id, clinicId.optionalId, time)
    dataRepository.saveEntity(service)

    val doctorMaybe = doctorId.optionalId.map(id => DoctorHistory(accountId, id, doctorId.name, cityId.id, clinicId.optionalId, serviceId.id, time))
    doctorMaybe.foreach(dataRepository.saveEntity)
  }
}
