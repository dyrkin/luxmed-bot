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
package com.lbs.server.repository

import java.time.ZonedDateTime

import com.lbs.server.repository.model.{Bug, CityHistory, ClinicHistory, Credentials, DoctorHistory, JLong, Monitoring, ServiceHistory, Settings, Source}
import javax.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

import scala.collection.JavaConverters._

@Repository
class DataRepository(@Autowired em: EntityManager) {

  private val maxHistory = 2

  def getCityHistory(userId: Long): Seq[CityHistory] = {
    em.createQuery(
      """select city from CityHistory city where city.recordId in
        | (select max(c.recordId) from CityHistory c where c.userId = :userId group by c.name order by MAX(c.time) desc)
        | order by city.time desc""".stripMargin, classOf[CityHistory])
      .setParameter("userId", userId)
      .setMaxResults(maxHistory)
      .getResultList.asScala
  }

  def getClinicHistory(userId: Long, cityId: Long): Seq[ClinicHistory] = {
    em.createQuery(
      """select clinic from ClinicHistory clinic where clinic.recordId in
        | (select max(c.recordId) from ClinicHistory c where c.userId = :userId and c.cityId = :cityId group by c.name order by MAX(c.time) desc)
        | order by clinic.time desc""".stripMargin, classOf[ClinicHistory])
      .setParameter("userId", userId)
      .setParameter("cityId", cityId)
      .setMaxResults(maxHistory)
      .getResultList.asScala
  }

  def getServiceHistory(userId: Long, cityId: Long, clinicId: Option[Long]): Seq[ServiceHistory] = {
    val query = em.createQuery(
      s"""select service from ServiceHistory service where service.recordId in
         | (select max(s.recordId) from ServiceHistory s where s.userId = :userId and s.cityId = :cityId
         | and s.clinicId ${clinicId.map(_ => "= :clinicId").getOrElse("IS NULL")} group by s.name order by MAX(s.time) desc)
         | order by service.time desc""".stripMargin, classOf[ServiceHistory])
      .setParameter("userId", userId)
      .setParameter("cityId", cityId)
      .setMaxResults(maxHistory)

    clinicId.map(id => query.setParameter("clinicId", id)).getOrElse(query).getResultList.asScala
  }

  def getDoctorHistory(userId: Long, cityId: Long, clinicId: Option[Long], serviceId: Long): Seq[DoctorHistory] = {
    val query = em.createQuery(
      s"""select doctor from DoctorHistory doctor where doctor.recordId in
         | (select max(d.recordId) from DoctorHistory d where d.userId = :userId
         | and d.cityId = :cityId and d.clinicId ${clinicId.map(_ => "= :clinicId").getOrElse("IS NULL")}
         | and d.serviceId = :serviceId group by d.name order by MAX(d.time) desc)
         | order by doctor.time desc""".stripMargin, classOf[DoctorHistory])
      .setParameter("userId", userId)
      .setParameter("cityId", cityId)
      .setParameter("serviceId", serviceId)
      .setMaxResults(maxHistory)

    clinicId.map(id => query.setParameter("clinicId", id)).getOrElse(query).getResultList.asScala
  }

  def findCredentials(userId: Long): Option[Credentials] = {
    em.createQuery(
      "select credentials from Credentials credentials where credentials.userId = :userId", classOf[Credentials])
      .setParameter("userId", userId)
      .getResultList.asScala.headOption
  }

  def getBugs(userId: Long): Seq[Bug] = {
    em.createQuery(
      """select bug from Bug bug where bug.userId = :userId order by bug.submitted desc""".stripMargin, classOf[Bug])
      .setParameter("userId", userId)
      .setMaxResults(50)
      .getResultList.asScala
  }

  def getActiveMonitorings: Seq[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.active = true""".stripMargin, classOf[Monitoring])
      .getResultList.asScala
  }

  def getActiveMonitoringsCount(userId: Long): JLong = {
    em.createQuery(
      """select count(monitoring) from Monitoring monitoring where monitoring.active = true
        | and monitoring.userId = :userId""".stripMargin, classOf[JLong])
      .setParameter("userId", userId)
      .getSingleResult
  }

  def getActiveMonitorings(userId: Long): Seq[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.active = true
        | and monitoring.userId = :userId order by monitoring.dateTo asc""".stripMargin, classOf[Monitoring])
      .setParameter("userId", userId)
      .getResultList.asScala
  }

  def findActiveMonitoring(userId: Long, cityId: Long, serviceId: Long): Option[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.active = true
        | and monitoring.userId = :userId
        | and monitoring.cityId = :cityId
        | and monitoring.serviceId = :serviceId""".stripMargin, classOf[Monitoring])
      .setParameter("userId", userId)
      .setParameter("cityId", cityId)
      .setParameter("serviceId", serviceId)
      .getResultList.asScala.headOption
  }

  def getActiveMonitoringsSince(since: ZonedDateTime): Seq[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.active = true
        | and monitoring.created > :since""".stripMargin, classOf[Monitoring])
      .setParameter("since", since)
      .getResultList.asScala
  }

  def findMonitoring(userId: Long, monitoringId: Long): Option[Monitoring] = {
    em.createQuery(
      """select monitoring from Monitoring monitoring where monitoring.userId = :userId
        | and monitoring.recordId = :monitoringId""".stripMargin, classOf[Monitoring])
      .setParameter("userId", userId)
      .setParameter("monitoringId", monitoringId)
      .getResultList.asScala.headOption
  }

  def findSettings(userId: Long): Option[Settings] = {
    em.createQuery(
      "select settings from Settings settings where settings.userId = :userId", classOf[Settings])
      .setParameter("userId", userId)
      .getResultList.asScala.headOption
  }

  def findUserId(chatId: String, sourceSystemId: Long): Option[JLong] = {
    em.createQuery(
      "select source.userId from Source source where source.chatId = :chatId" +
        " and source.sourceSystemId = :sourceSystemId", classOf[JLong])
      .setParameter("chatId", chatId)
      .setParameter("sourceSystemId", sourceSystemId)
      .getResultList.asScala.headOption
  }

  def findCredentialsByUsername(username: String): Option[Credentials] = {
    em.createQuery(
      "select credentials from Credentials credentials where credentials.username = :username", classOf[Credentials])
      .setParameter("username", username)
      .getResultList.asScala.headOption
  }

  def findSource(chatId: String, sourceSystemId: Long, userId: Long): Option[Source] = {
    em.createQuery(
      "select source from Source source where source.chatId = :chatId" +
        " and source.sourceSystemId = :sourceSystemId" +
        " and userId = :userId", classOf[Source])
      .setParameter("chatId", chatId)
      .setParameter("sourceSystemId", sourceSystemId)
      .setParameter("userId", userId)
      .getResultList.asScala.headOption
  }

  def saveEntity[T](entity: T): T = {
    em.merge(entity)
  }
}