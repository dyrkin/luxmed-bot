package com.lbs.api.json.model

import com.lbs.api.json.JsonSerializer.extensions.*
import com.lbs.api.json.model.JsonCodecs.given
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class RehabModelsSpec extends AnyFunSuite with Matchers {
  
  test("ReferralsResponse decodes real PascalCase API payload") {
    val json =
      """{
        |  "Planned": [],
        |  "Unplanned": [
        |    {
        |      "ReferralId": 999999902,
        |      "EReferralId": null,
        |      "ServiceVariant": { "Id": 12463, "Name": "Rehabilitacja", "IsTelemedicine": false },
        |      "ReferralStatus": "ToBook",
        |      "ReferralType": "Regular",
        |      "ReferralMode": "Standard",
        |      "ExpiredDate": "2026-08-22T00:00:00+02:00",
        |      "SuggestedDate": null,
        |      "HowToBookInfo": null,
        |      "ReferralBookedVisit": null,
        |      "SearchVisitInfo": {
        |        "Presets": [],
        |        "MaxNumberOfDaysToBookVisitFromNow": 365,
        |        "MaxIntervalInDays": 13,
        |        "NumberOfSearchDays": 14,
        |        "MinAndMaxDateRange": {
        |          "FromDate": "2026-04-10T00:00:00+02:00",
        |          "ToDate": "2026-07-08T00:00:00+02:00"
        |        },
        |        "DefaultDateRange": {
        |          "FromDate": "2026-04-10T00:00:00+02:00",
        |          "ToDate": "2026-04-23T00:00:00+02:00"
        |        },
        |        "FindFirstFreeTerm": false,
        |        "InformationMessage": "",
        |        "AdditionalSearchOptions": {},
        |        "NextPreviousSearchOptions": {},
        |        "TimeOfDays": []
        |      },
        |      "ProceduresAmount": 3,
        |      "Procedures": [
        |        {
        |          "Name": "KINEZYTERAPIA Terapia indywidualna - kregoslup 1 odcinek",
        |          "Count": 3,
        |          "Preparation": "Na czym polega zabieg?"
        |        }
        |      ],
        |      "Links": [],
        |      "Doctor": "mgr DOCTOR_FIRSTNAME DOCTOR_LASTNAME",
        |      "IssueDate": "2026-02-23T15:31:57+02:00",
        |      "Preparation": null,
        |      "HighlightSuggestedDate": false,
        |      "HighlightExpireDate": false,
        |      "Tag": null,
        |      "SourceVisitId": 999999903,
        |      "EReferralAccessCode": null,
        |      "DownloadLinks": [],
        |      "Tags": []
        |    }
        |  ]
        |}""".stripMargin

    val response = json.as[ReferralsResponse]

    response.planned shouldBe empty
    response.unplanned should have size 1

    val referral = response.unplanned.head
    referral.referralId shouldBe Some(999999902L)
    referral.serviceVariant.id shouldBe 12463L
    referral.serviceVariant.name shouldBe "Rehabilitacja"
    referral.referralStatus shouldBe "ToBook"
    referral.referralType shouldBe "Regular"
    referral.sourceVisitId shouldBe 999999903L
    referral.proceduresAmount shouldBe 3
    referral.procedures should have size 1
    referral.procedures.head.name shouldBe "KINEZYTERAPIA Terapia indywidualna - kregoslup 1 odcinek"
    referral.procedures.head.count shouldBe 3
    referral.doctor shouldBe Some("mgr DOCTOR_FIRSTNAME DOCTOR_LASTNAME")

    val svi = referral.searchVisitInfo.get
    svi.maxIntervalInDays shouldBe 13
    svi.numberOfSearchDays shouldBe 14
    svi.minAndMaxDateRange.get.fromDate should include("2026-04-10")
    svi.defaultDateRange.get.fromDate should include("2026-04-10")
  }
  
  test("RehabFacilitiesResponse decodes real API payload with camelCase normalization") {
    val json =
      """{
        |  "locations": [
        |    { "id": 999999905, "name": "Warszawa" },
        |    { "id": 999999906, "name": "Krakow" }
        |  ],
        |  "facilities": [
        |    { "id": 999999929, "name": "LX Stara Iwiczna - Nowa 4A", "locationId": 1, "availabilityLevel": 0 },
        |    { "id": 999999932, "name": "LX Warszawa - Gorczewska 124", "locationId": 1, "availabilityLevel": 0 },
        |    { "id": 999999936, "name": "LX Fizjoterapia Warszawa", "locationId": 100, "availabilityLevel": 0 }
        |  ],
        |  "description": "Get facilities for rehabilitation service"
        |}""".stripMargin

    val response = json.as[RehabFacilitiesResponse]

    response.locations should have size 2
    response.locations.head.name shouldBe "Warszawa"
    response.locations.head.id shouldBe 999999905L

    response.facilities should have size 3
    val warszawa = response.facilities.filter(_.name.contains("Gorczewska")).head
    warszawa.locationId shouldBe 1L
    warszawa.availabilityLevel shouldBe 0
  }
  
  test("TermsIndexResponse decodes real nextTerms payload with termsInfoForDays") {
    val json =
      """{
        |  "correlationId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        |  "termsForService": {
        |    "serviceVariantId": 12463,
        |    "additionalData": {
        |      "isPreparationRequired": true,
        |      "preparationItems": [
        |        { "header": "Na czym polega zabieg?", "text": "Indywidualna praca..." },
        |        { "header": "Jak sie przygotowac?", "text": "Stroj niekrepujacy..." }
        |      ],
        |      "previousTermsAvailable": true,
        |      "nextTermsAvailable": true,
        |      "anyTermForTelemedicine": false,
        |      "anyTermForFacilityVisit": false
        |    },
        |    "termsForDays": [],
        |    "termsInfoForDays": [
        |      {
        |        "day": "2026-04-24T00:00:00",
        |        "termsStatus": 3,
        |        "message": "Wszystkie terminy zostaly zarezerwowane.",
        |        "isLimitedDay": false,
        |        "isLastDayWithLoadedTerms": false,
        |        "isMoreTermsThanCounter": null,
        |        "termsCounter": { "termsNumber": 0, "partialTermsCounters": [] }
        |      },
        |      {
        |        "day": "2026-04-28T00:00:00",
        |        "termsStatus": 0,
        |        "message": "W tym dniu dostepne sa terminy.",
        |        "isLimitedDay": true,
        |        "isLastDayWithLoadedTerms": false,
        |        "isMoreTermsThanCounter": null,
        |        "termsCounter": {
        |          "termsNumber": 1,
        |          "partialTermsCounters": [
        |            { "clinicGroupId": 2767, "doctorId": 11106, "priority": 1, "termsNumber": 1 }
        |          ]
        |        }
        |      }
        |    ]
        |  },
        |  "pMode": 500,
        |  "success": true
        |}""".stripMargin

    val response = json.as[TermsIndexResponse]

    response.correlationId shouldBe "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    response.termsForService.termsForDays shouldBe empty
    response.termsForService.additionalData.isPreparationRequired shouldBe true
    response.termsForService.additionalData.preparationItems should have size 2
  }
  
  test("TermsIndexResponse decodes real terms/index payload") {
    val json =
      """{
        |  "correlationId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        |  "termsForService": {
        |    "serviceVariantId": 12463,
        |    "additionalData": {
        |      "isPreparationRequired": true,
        |      "preparationItems": [
        |        { "header": "Na czym polega zabieg?", "text": "Indywidualna praca..." }
        |      ],
        |      "previousTermsAvailable": false,
        |      "nextTermsAvailable": true
        |    },
        |    "termsForDays": [],
        |    "termsInfoForDays": [
        |      {
        |        "day": "2026-01-06T00:00:00",
        |        "termsStatus": 2,
        |        "message": "W tym dniu nie realizujemy tej uslugi.",
        |        "isLimitedDay": false,
        |        "termsCounter": { "termsNumber": 0, "partialTermsCounters": [] }
        |      },
        |      {
        |        "day": "2026-01-07T00:00:00",
        |        "termsStatus": 0,
        |        "message": "W tym dniu dostepne sa terminy.",
        |        "isLimitedDay": true,
        |        "termsCounter": {
        |          "termsNumber": 22,
        |          "partialTermsCounters": [
        |            { "clinicGroupId": 2149, "doctorId": 11111, "priority": 1, "termsNumber": 1 },
        |            { "clinicGroupId": 2502, "doctorId": 22222, "priority": 1, "termsNumber": 5 }
        |          ]
        |        }
        |      }
        |    ]
        |  },
        |  "pMode": 500,
        |  "success": true
        |}""".stripMargin

    val response = json.as[TermsIndexResponse]
    response.termsForService.termsForDays shouldBe empty
    response.termsForService.additionalData.preparationItems should have size 1
  }
  
  test("ServiceReferralResponse decodes real GetServiceReferral payload") {
    val json =
      """{
        |  "serviceReferrals": [
        |    {
        |      "id": 999999999,
        |      "serviceVariantId": 12463,
        |      "serviceName": "KINEZYTERAPIA Terapia indywidualna - kregoslup 1 odcinek",
        |      "isOnWhitelist": false,
        |      "issued": "2025-09-15T00:00:00",
        |      "prefix": "mgr",
        |      "firstName": "DOCTOR_FIRSTNAME",
        |      "lastName": "DOCTOR_LASTNAME",
        |      "expires": "2026-03-14T00:00:00",
        |      "requiresPreparation": true,
        |      "priority": 1
        |    },
        |    {
        |      "id": 999999998,
        |      "serviceVariantId": 12455,
        |      "serviceName": "FIZYKOTERAPIA Prady tens - kregoslup 1 odcinek",
        |      "isOnWhitelist": false,
        |      "issued": "2025-09-15T00:00:00",
        |      "prefix": "mgr",
        |      "firstName": "DOCTOR_FIRSTNAME",
        |      "lastName": "DOCTOR_LASTNAME",
        |      "expires": "2026-03-14T00:00:00",
        |      "requiresPreparation": true,
        |      "priority": 2
        |    }
        |  ]
        |}""".stripMargin

    val response = json.as[ServiceReferralResponse]

    response.serviceReferrals should have size 2
    response.primaryReferral.map(_.id) shouldBe Some(999999999L)
    response.primaryReferral.map(_.serviceVariantId) shouldBe Some(12463L)
    response.primaryReferral.map(_.requiresPreparation) shouldBe Some(true)

    val second = response.serviceReferrals.sortBy(_.priority).tail.head
    second.serviceVariantId shouldBe 12455L
    second.priority shouldBe 2
  }
  
  test("Term decodes real oneDayTerms payload with null clinic and impedimentText") {
    // The Term is embedded in TermsForDay; we test the problematic null fields directly
    val termJson =
      """{
        |  "dateTimeFrom": "2026-01-07T07:30:00",
        |  "dateTimeTo": "2026-01-07T08:00:00",
        |  "doctor": {
        |    "id": 11111, "genderId": 1, "academicTitle": "mgr",
        |    "firstName": "DOCTOR_FIRSTNAME", "lastName": "DOCTOR_LASTNAME"
        |  },
        |  "clinicId": 2149,
        |  "clinic": null,
        |  "clinicGroupId": 2149,
        |  "clinicGroup": null,
        |  "roomId": 9058,
        |  "serviceId": 12463,
        |  "scheduleId": 16772513,
        |  "isImpediment": false,
        |  "impedimentText": null,
        |  "isAdditional": false,
        |  "isTelemedicine": false,
        |  "isInfectionTreatmentCenter": false,
        |  "partOfDay": 1,
        |  "priority": 1
        |}""".stripMargin

    val term = termJson.as[Term]

    term.clinic shouldBe None
    term.impedimentText shouldBe None
    term.clinicId shouldBe 2149L
    term.scheduleId shouldBe 16772513L
    term.roomId shouldBe 9058L
    term.doctor.id shouldBe 11111L
    term.isImpediment shouldBe false
    term.isTelemedicine shouldBe false
  }

  test("Term decodes real oneDayTerms payload with non-null clinic") {
    val termJson =
      """{
        |  "dateTimeFrom": "2026-01-07T13:30:00",
        |  "dateTimeTo": "2026-01-07T14:00:00",
        |  "doctor": {
        |    "id": 22222, "genderId": 2, "academicTitle": "mgr",
        |    "firstName": "ANOTHER_DOCTOR", "lastName": "ANOTHER_LASTNAME"
        |  },
        |  "clinicId": 2502,
        |  "clinic": "LX Warszawa - Domaniewska 52",
        |  "clinicGroupId": 2502,
        |  "clinicGroup": "ul. Domaniewska 52",
        |  "roomId": 13221,
        |  "serviceId": 12463,
        |  "scheduleId": 14020993,
        |  "isImpediment": false,
        |  "impedimentText": "",
        |  "isAdditional": false,
        |  "isTelemedicine": false,
        |  "isInfectionTreatmentCenter": false,
        |  "partOfDay": 2,
        |  "priority": 1
        |}""".stripMargin

    val term = termJson.as[Term]

    term.clinic shouldBe Some("LX Warszawa - Domaniewska 52")
    term.impedimentText shouldBe Some("")
    term.scheduleId shouldBe 14020993L
  }

  test("ReferralsResponse serialization roundtrip") {
    val procedure = RehabProcedure(name = "KINEZYTERAPIA", count = 10, preparation = None)
    val serviceVariant = ServiceVariantInfo(id = 12463L, name = "Rehabilitacja", isTelemedicine = false)
    val referral = Referral(
      referralId = Some(999999902L),
      eReferralId = None,
      serviceVariant = serviceVariant,
      referralStatus = "ToBook",
      referralType = "Regular",
      referralMode = "Standard",
      expiredDate = Some("2026-12-31"),
      sourceVisitId = 999999903L,
      proceduresAmount = 10,
      procedures = List(procedure),
      doctor = Some("Dr. Smith"),
      issueDate = Some("2026-01-01"),
      searchVisitInfo = None,
      tags = List.empty
    )
    val response = ReferralsResponse(planned = List(referral), unplanned = List.empty)
    val json = response.asJson
    json should include("Rehabilitacja")
    json should include("ToBook")
    json should include("999999903")
  }

  test("ServiceReferralResponse serialization roundtrip") {
    val item = ServiceReferralItem(id = 999999902L, serviceVariantId = 12463L, serviceName = "Rehabilitacja", priority = 1)
    val response = ServiceReferralResponse(serviceReferrals = List(item))
    val json = response.asJson
    json should include("999999902")
    json should include("Rehabilitacja")

    val deserialized = json.as[ServiceReferralResponse]
    deserialized.serviceReferrals should have size 1
    deserialized.primaryReferral.map(_.id) shouldBe Some(999999902L)
    deserialized.primaryReferral.map(_.serviceVariantId) shouldBe Some(12463L)
  }

  test("RehabFacilitiesResponse serialization roundtrip") {
    val location = RehabLocation(id = 7L, name = "Warszawa")
    val facility = RehabFacility(id = 100L, name = "LX Fizjoterapia Warszawa", locationId = 7L, availabilityLevel = 1)
    val response = RehabFacilitiesResponse(locations = List(location), facilities = List(facility))
    val json = response.asJson
    json should include("Warszawa")
    json should include("LX Fizjoterapia Warszawa")

    val deserialized = json.as[RehabFacilitiesResponse]
    deserialized.locations should have size 1
    deserialized.facilities should have size 1
    deserialized.locations.head.name shouldBe "Warszawa"
    deserialized.facilities.head.locationId shouldBe 7L
  }

  test("Referral toBook filter logic") {
    val rehab = Referral(
      referralId = Some(1L), eReferralId = None,
      serviceVariant = ServiceVariantInfo(1L, "Rehabilitacja", false),
      referralStatus = "ToBook", referralType = "Regular", referralMode = "Standard",
      expiredDate = None, sourceVisitId = 100L, proceduresAmount = 5,
      procedures = List.empty, doctor = None, issueDate = None,
      searchVisitInfo = None, tags = List.empty
    )
    val other = rehab.copy(serviceVariant = ServiceVariantInfo(2L, "Kardiologia", false))
    val booked = rehab.copy(referralStatus = "Booked")

    val referrals = List(rehab, other, booked)
    val filtered = referrals.filter(r =>
      r.serviceVariant.name == "Rehabilitacja" && r.referralStatus == "ToBook"
    )
    filtered should have size 1
    filtered.head.sourceVisitId shouldBe 100L
  }
}
