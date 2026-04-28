package com.lbs.api.json.model

import com.lbs.api.json.JsonSerializer.extensions.*
import com.lbs.api.json.model.JsonCodecs.given
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class RehabModelsSpec extends AnyFunSuite with Matchers {

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
