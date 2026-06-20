package com.lbs.server.repository.model

import org.scalatest.wordspec.AnyWordSpec

class MonitoringSpec extends AnyWordSpec {

  "Monitoring clinic selection" should {

    "read clinic selection from JSON before legacy fields" in {
      val monitoring = new Monitoring
      monitoring.clinicSelection = """[{"id":2764,"name":"A | B"},{"id":null,"name":"Any"}]"""
      monitoring.clinicIds = "1"
      monitoring.clinicNames = "Legacy"

      assert(monitoring.clinics == Seq(Some(2764L) -> "A | B", None -> "Any"))
    }

    "fall back to legacy clinic ids and names" in {
      val monitoring = new Monitoring
      monitoring.clinicIds = "2764,2769"
      monitoring.clinicNames = "Modlinska|Mysliborska"

      assert(monitoring.clinics == Seq(Some(2764L) -> "Modlinska", Some(2769L) -> "Mysliborska"))
    }

    "write JSON clinic selection for new monitorings" in {
      val monitoring = Monitoring(
        userId = 1L,
        username = "user",
        accountId = 2L,
        chatId = "3",
        sourceSystemId = 4L,
        payerId = 5L,
        cityId = 6L,
        cityName = "Warszawa",
        clinicId = None,
        clinicName = "Any",
        clinics = Seq(None -> "Any"),
        serviceId = 7L,
        serviceName = "Consultation",
        doctorId = None,
        doctorName = "Any",
        dateFrom = java.time.ZonedDateTime.now(),
        dateTo = java.time.ZonedDateTime.now().plusDays(1),
        timeFrom = java.time.LocalTime.of(7, 0),
        timeTo = java.time.LocalTime.of(21, 0),
        offset = 0
      )

      assert(monitoring.clinicSelection == """[{"id":null,"name":"Any"}]""")
      assert(monitoring.clinics == Seq(None -> "Any"))
    }
  }
}
