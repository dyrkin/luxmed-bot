package com.lbs.server.repository.model

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.persistence.{Access, AccessType, Column, Entity}

import java.time.{DayOfWeek, LocalDate, LocalTime, ZonedDateTime}
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
  @Column(name = "clinic_ids", nullable = true)
  var clinicIds: String = uninitialized

  @BeanProperty
  @Column(name = "clinic_names", nullable = true)
  var clinicNames: String = uninitialized

  @BeanProperty
  @Column(name = "clinic_selection", nullable = true)
  var clinicSelection: String = uninitialized

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

  @BeanProperty
  @Column(name = "excluded_weekdays", nullable = true)
  var excludedWeekdays: String = uninitialized

  @BeanProperty
  @Column(name = "excluded_dates", nullable = true)
  var excludedDates: String = uninitialized

  def clinics: Seq[(Option[Long], String)] = {
    val selected = Monitoring.parseClinicSelection(clinicSelection)
    if (selected.nonEmpty) selected
    else {
      val ids = Monitoring.parseLongs(clinicIds)
      val names = Monitoring.parseStrings(clinicNames)
      val parsed = ids.zipAll(names, -1L, "").map { case (id, name) =>
        Option(id).filterNot(_ == -1L) -> name
      }
      if (parsed.nonEmpty) parsed else Seq(Option(clinicId).map(_.toLong) -> clinicName)
    }
  }

  def clinicOptions: Seq[Option[Long]] = clinics.map(_._1) match {
    case Nil => Seq(None)
    case xs  => xs
  }

  def clinicFilter: Seq[Long] = clinicOptions.flatten.distinct

  def singleClinicId: Option[Long] = {
    val selected = clinicOptions.distinct
    if (selected.size == 1) selected.head else None
  }

  def clinicDisplayName: String = clinics.map(_._2).filter(_.nonEmpty).distinct.mkString(", ")

  def slotWeight: Int = {
    val selected = clinicOptions
    if (selected.exists(_.isEmpty)) 1 else selected.distinct.size.max(1)
  }

  def excludedWeekdaysSet: Set[DayOfWeek] =
    Monitoring.parseInts(excludedWeekdays).flatMap(i => scala.util.Try(DayOfWeek.of(i)).toOption).toSet

  def excludedDatesSet: Set[LocalDate] =
    Monitoring.parseCommaStrings(excludedDates).flatMap(d => scala.util.Try(LocalDate.parse(d)).toOption).toSet

  def isDateExcluded(date: LocalDate): Boolean =
    excludedWeekdaysSet.contains(date.getDayOfWeek) || excludedDatesSet.contains(date)
}

object Monitoring {
  private case class ClinicSelection(id: Option[Long], name: String)

  private given Codec[ClinicSelection] = deriveCodec

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
    clinics: Seq[(Option[Long], String)] = Seq.empty,
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
    serviceVariantId: Option[Long] = None,
    excludedWeekdays: Set[DayOfWeek] = Set.empty,
    excludedDates: Set[LocalDate] = Set.empty
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
    val selectedClinics = if (clinics.nonEmpty) clinics else Seq(clinicId -> clinicName)
    monitoring.clinicIds = selectedClinics.map(_._1.getOrElse(-1L)).mkString(",")
    monitoring.clinicNames = selectedClinics.map(_._2).mkString("|")
    monitoring.clinicSelection = Monitoring.formatClinicSelection(selectedClinics)
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
    monitoring.excludedWeekdays = excludedWeekdays.toSeq.sortBy(_.getValue).map(_.getValue).mkString(",")
    monitoring.excludedDates = excludedDates.toSeq.sortBy(_.toString).map(_.toString).mkString(",")
    monitoring
  }

  private def parseLongs(value: String): Seq[Long] =
    parseCommaStrings(value).flatMap(v => scala.util.Try(v.toLong).toOption)

  private def parseInts(value: String): Seq[Int] =
    parseCommaStrings(value).flatMap(v => scala.util.Try(v.toInt).toOption)

  private def parseStrings(value: String): Seq[String] =
    Option(value).toSeq.flatMap(_.split("\\|").map(_.trim).filter(_.nonEmpty))

  private def parseCommaStrings(value: String): Seq[String] =
    Option(value).toSeq.flatMap(_.split(",").map(_.trim).filter(_.nonEmpty))

  private def parseClinicSelection(value: String): Seq[(Option[Long], String)] =
    Option(value)
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap(json => decode[Seq[ClinicSelection]](json).toOption)
      .toSeq
      .flatten
      .map(clinic => clinic.id -> clinic.name)

  private def formatClinicSelection(clinics: Seq[(Option[Long], String)]): String =
    clinics.map { case (id, name) => ClinicSelection(id, name) }.asJson.noSpaces
}
