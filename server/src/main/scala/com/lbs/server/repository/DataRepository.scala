package com.lbs.server.repository

import com.lbs.server.repository.model.{CityHistory, ClinicHistory, Credentials, DoctorHistory, JLong, Monitoring, Reminder, ServiceHistory, Settings, Source, SystemUser}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

import java.time.ZonedDateTime
import javax.persistence.EntityManager
import scala.jdk.CollectionConverters._

@Repository
class DataRepository(@Autowired em: EntityManager) {

  private val maxHistory = 4

  def getCityHistory(accountId: Long): Seq[CityHistory] = {
    em.createQuery(
      """select city from CityHistory city where city.recordId in
        | (select max(c.recordId) from CityHistory c where c.accountId = :accountId group by c.name order by MAX(c.time) desc)
        | order by city.time desc""".stripMargin,
      classOf[CityHistory]
    ).setParameter("accountId", accountId)
      .setMaxResults(maxHistory)
      .getResultList
      .asScala
      .toSeq
  }

  def getClinicHistory(accountId: Long, cityId: Long): Seq[ClinicHistory] = {
    em.createQuery(
      """select clinic from ClinicHistory clinic where clinic.recordId in
        | (select max(c.recordId) from ClinicHistory c where c.accountId = :accountId and c.cityId = :cityId group by c.name order by MAX(c.time) desc)
        | order by clinic.time desc""".stripMargin,
      classOf[ClinicHistory]
    ).setParameter("accountId", accountId)
      .setParameter("cityId", cityId)
      .setMaxResults(maxHistory)
      .getResultList
      .asScala
      .toSeq
  }

  def getServiceHistory(accountId: Long, cityId: Long, clinicId: Option[Long]): Seq[ServiceHistory] = {
    val query = em
      .createQuery(
        s"""select service from ServiceHistory service where service.recordId in
         | (select max(s.recordId) from ServiceHistory s where s.accountId = :accountId and s.cityId = :cityId
         | and s.clinicId ${clinicId
            .map(_ => "= :clinicId")
            .getOrElse("IS NULL")} group by s.name order by MAX(s.time) desc)
         | order by service.time desc""".stripMargin,
        classOf[ServiceHistory]
      )
      .setParameter("accountId", accountId)
      .setParameter("cityId", cityId)
      .setMaxResults(maxHistory)

    clinicId.map(id => query.setParameter("clinicId", id)).getOrElse(query).getResultList.asScala.toSeq
  }

  def getDoctorHistory(accountId: Long, cityId: Long, clinicId: Option[Long], serviceId: Long): Seq[DoctorHistory] = {
    val query = em
      .createQuery(
        s"""select doctor from DoctorHistory doctor where doctor.recordId in
         | (select max(d.recordId) from DoctorHistory d where d.accountId = :accountId
         | and d.cityId = :cityId and d.clinicId ${clinicId.map(_ => "= :clinicId").getOrElse("IS NULL")}
         | and d.serviceId = :serviceId group by d.name order by MAX(d.time) desc)
         | order by doctor.time desc""".stripMargin,
        classOf[DoctorHistory]
      )
      .setParameter("accountId", accountId)
      .setParameter("cityId", cityId)
      .setParameter("serviceId", serviceId)
      .setMaxResults(maxHistory)

    clinicId.map(id => query.setParameter("clinicId", id)).getOrElse(query).getResultList.asScala.toSeq
  }

  def findCredentials(accountId: Long): Option[Credentials] = {
    em.createQuery(
      "select credentials from Credentials credentials where credentials.accountId = :accountId",
      classOf[Credentials]
    ).setParameter("accountId", accountId)
      .getResultList
      .asScala
      .headOption
  }

  def getActiveMonitorings: Seq[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.active = true""".stripMargin,
      classOf[Monitoring]
    ).getResultList
      .asScala
      .toSeq
  }

  def getActiveReminders: Seq[Reminder] = {
    em.createQuery(
      """select reminder from Reminder reminder where reminder.active = true""".stripMargin,
      classOf[Reminder]
    ).getResultList
      .asScala
      .toSeq
  }

  def getActiveMonitoringsCount(accountId: Long): JLong = {
    em.createQuery(
      """select count(monitoring) from Monitoring monitoring where monitoring.active = true
        | and monitoring.accountId = :accountId""".stripMargin,
      classOf[JLong]
    ).setParameter("accountId", accountId)
      .getSingleResult
  }

  def getActiveMonitorings(accountId: Long): Seq[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.active = true
        | and monitoring.accountId = :accountId order by monitoring.dateTo asc""".stripMargin,
      classOf[Monitoring]
    ).setParameter("accountId", accountId)
      .getResultList
      .asScala
      .toSeq
  }

  def getAllMonitoringsCount(accountId: Long): JLong = {
    em.createQuery(
      """select count(monitoring) from Monitoring monitoring where
        | monitoring.accountId = :accountId""".stripMargin,
      classOf[JLong]
    ).setParameter("accountId", accountId)
      .getSingleResult
  }

  def getMonitoringsPage(accountId: Long, start: Int, count: Int): Seq[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.accountId = :accountId
        | order by monitoring.created desc""".stripMargin,
      classOf[Monitoring]
    ).setParameter("accountId", accountId)
      .setFirstResult(start)
      .setMaxResults(count)
      .getResultList
      .asScala
      .toSeq
  }

  def findActiveMonitoring(accountId: Long, cityId: Long, serviceId: Long, doctorId: Long): Option[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.active = true
        | and monitoring.accountId = :accountId
        | and monitoring.cityId = :cityId
        | and monitoring.serviceId = :serviceId
        | and monitoring.doctorId = :doctorId""".stripMargin,
      classOf[Monitoring]
    ).setParameter("accountId", accountId)
      .setParameter("cityId", cityId)
      .setParameter("serviceId", serviceId)
      .setParameter("doctorId", doctorId)
      .getResultList
      .asScala
      .headOption
  }

  def getActiveMonitoringsSince(since: ZonedDateTime): Seq[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.active = true
        | and monitoring.created > :since""".stripMargin,
      classOf[Monitoring]
    ).setParameter("since", since)
      .getResultList
      .asScala
      .toSeq
  }

  def findMonitoring(accountId: Long, monitoringId: Long): Option[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.accountId = :accountId
        | and monitoring.recordId = :monitoringId""".stripMargin,
      classOf[Monitoring]
    ).setParameter("accountId", accountId)
      .setParameter("monitoringId", monitoringId)
      .getResultList
      .asScala
      .headOption
  }

  def findReminder(accountId: Long, reminderId: Long): Option[Reminder] = {
    em.createQuery(
      """select reminder from Reminder reminder where reminder.accountId = :accountId
        | and reminder.recordId = :reminderId""".stripMargin,
      classOf[Reminder]
    ).setParameter("accountId", accountId)
      .setParameter("reminderId", reminderId)
      .getResultList
      .asScala
      .headOption
  }

  def findSettings(userId: Long): Option[Settings] = {
    em.createQuery("select settings from Settings settings where settings.userId = :userId", classOf[Settings])
      .setParameter("userId", userId)
      .getResultList
      .asScala
      .headOption
  }

  def findUserId(chatId: String, sourceSystemId: Long): Option[JLong] = {
    em.createQuery(
      "select source.userId from Source source where source.chatId = :chatId" +
        " and source.sourceSystemId = :sourceSystemId",
      classOf[JLong]
    ).setParameter("chatId", chatId)
      .setParameter("sourceSystemId", sourceSystemId)
      .getResultList
      .asScala
      .headOption
  }

  def findCredentialsByUsername(username: String, userId: Long): Option[Credentials] = {
    em.createQuery(
      "select credentials from Credentials credentials where credentials.username = :username" +
        " and credentials.userId = :userId",
      classOf[Credentials]
    ).setParameter("username", username)
      .setParameter("userId", userId)
      .getResultList
      .asScala
      .headOption
  }

  def findSource(chatId: String, sourceSystemId: Long, userId: Long): Option[Source] = {
    em.createQuery(
      "select source from Source source where source.chatId = :chatId" +
        " and source.sourceSystemId = :sourceSystemId" +
        " and userId = :userId",
      classOf[Source]
    ).setParameter("chatId", chatId)
      .setParameter("sourceSystemId", sourceSystemId)
      .setParameter("userId", userId)
      .getResultList
      .asScala
      .headOption
  }

  def findUserIdBySource(chatId: String, sourceSystemId: Long): Option[JLong] = {
    em.createQuery(
      "select source.userId from Source source where source.chatId = :chatId" +
        " and source.sourceSystemId = :sourceSystemId",
      classOf[JLong]
    ).setParameter("chatId", chatId)
      .setParameter("sourceSystemId", sourceSystemId)
      .getResultList
      .asScala
      .headOption
  }

  def findAccountId(userId: Long): Option[JLong] = {
    em.createQuery(
      "select systemUser.activeAccountId from SystemUser systemUser where systemUser.recordId = :recordId",
      classOf[JLong]
    ).setParameter("recordId", userId)
      .getResultList
      .asScala
      .headOption
  }

  def findUser(userId: Long): Option[SystemUser] = {
    em.createQuery(
      "select systemUser from SystemUser systemUser where systemUser.recordId = :recordId",
      classOf[SystemUser]
    ).setParameter("recordId", userId)
      .getResultList
      .asScala
      .headOption
  }

  def getUserCredentials(userId: Long): Seq[Credentials] = {
    em.createQuery(
      "select credentials from Credentials credentials where credentials.userId = :userId",
      classOf[Credentials]
    ).setParameter("userId", userId)
      .getResultList
      .asScala
      .toSeq
  }

  def findUserCredentialsByUserIdAndAccountId(userId: Long, accountId: Long): Option[Credentials] = {
    em.createQuery(
      """select credentials from Credentials credentials where credentials.userId = :userId
        | and credentials.accountId = :accountId
      """.stripMargin,
      classOf[Credentials]
    ).setParameter("userId", userId)
      .setParameter("accountId", accountId)
      .getResultList
      .asScala
      .headOption
  }

  def saveEntity[T](entity: T): T = {
    em.merge(entity)
  }
}
