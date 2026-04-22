package com.lbs.api.json.model

import com.lbs.api.json.JsonSerializer
import com.lbs.api.json.JsonSerializer.extensions.*
import com.lbs.api.json.model.JsonCodecs.given
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.LocalTime

class JsonSerializerSpec extends AnyFunSuite with Matchers {

  test("deserialize LoginResponse") {
    val json =
      """{
        |  "access_token": "IDtjG_ECOd_ETYE2fwrCoTcC6bW935cn",
        |  "expires_in": 599,
        |  "refresh_token": "d251c66c-49e0-4777-b766-08326d83fa31",
        |  "token_type": "bearer"
        |}""".stripMargin

    val result = json.as[LoginResponse]

    result.accessToken  shouldBe "IDtjG_ECOd_ETYE2fwrCoTcC6bW935cn"
    result.expiresIn    shouldBe 599
    result.refreshToken shouldBe "d251c66c-49e0-4777-b766-08326d83fa31"
    result.tokenType    shouldBe "bearer"
  }

  test("serialize and deserialize LoginResponse round-trip") {
    val original = LoginResponse("myToken", 300, "refresh123", "bearer")
    val json     = original.asJson
    val result   = json.as[LoginResponse]

    result shouldBe original
  }

  test("deserialize Doctor with optional fields") {
    val json =
      """{
        |  "academic_title": "dr n. med.",
        |  "facility_group_ids": [78],
        |  "first_name": "TARAS",
        |  "id": 11111,
        |  "is_english_speaker": true,
        |  "last_name": "SHEVCHENKO"
        |}""".stripMargin

    val result = JsonSerializer.extract[Doctor](json)

    result.firstName              shouldBe "TARAS"
    result.lastName               shouldBe "SHEVCHENKO"
    result.id                     shouldBe 11111L
    result.academicTitle          shouldBe Some("dr n. med.")
    result.isEnglishSpeaker       shouldBe Some(true)
    result.facilityGroupIds       shouldBe Some(List(78L))
    result.name                   shouldBe "TARAS SHEVCHENKO"
  }

  test("deserialize Doctor with missing optional fields") {
    val json =
      """{
        |  "academic_title": "lek. med.",
        |  "first_name": "LESYA",
        |  "id": 22222,
        |  "last_name": "UKRAINKA"
        |}""".stripMargin

    val result = JsonSerializer.extract[Doctor](json)

    result.isEnglishSpeaker shouldBe None
    result.facilityGroupIds shouldBe None
    result.genderId         shouldBe None
  }

  test("deserialize list of DictionaryCity") {
    val json =
      """[
        |  {"id": 70,  "name": "Białystok"},
        |  {"id": 12,  "name": "Bielsk Podlaski"},
        |  {"id": 100, "name": "Bielsko-Biała"}
        |]""".stripMargin

    val result = json.asList[DictionaryCity]

    result should have size 3
    result.head.id   shouldBe 70L
    result.head.name shouldBe "Białystok"
    result.last.name shouldBe "Bielsko-Biała"
  }

  test("deserialize FacilitiesAndDoctors") {
    val json =
      """{
        |  "doctors": [
        |    {"academic_title":"dr n. med.","facility_group_ids":[78],"first_name":"TARAS","id":111111,"is_english_speaker":true,"last_name":"SHEVCHENKO"},
        |    {"academic_title":"lek. med.","facility_group_ids":[78,127],"first_name":"VLADIMIR","id":22222,"is_english_speaker":false,"last_name":"ZELENSKIY"}
        |  ],
        |  "facilities": [
        |    {"id": 78,  "name": "ul. Fabryczna 6"},
        |    {"id": 127, "name": "ul. Kwidzyńska 6"}
        |  ]
        |}""".stripMargin

    val result = json.as[FacilitiesAndDoctors]

    result.doctors    should have size 2
    result.facilities should have size 2
    result.doctors.head.lastName   shouldBe "SHEVCHENKO"
    result.facilities.last.name    shouldBe "ul. Kwidzyńska 6"
  }

  test("deserialize ReservationLocktermResponse without errors") {
    val json =
      """{
        |  "errors": [],
        |  "has_errors": false,
        |  "has_warnings": false,
        |  "warnings": [],
        |  "value": {
        |    "change_term_available": false,
        |    "conflicted_visit": null,
        |    "doctor_details": {
        |      "academic_title": "lek. med.",
        |      "first_name": "TARAS",
        |      "gender_id": 1,
        |      "id": 11111,
        |      "last_name": "SHEVCHENKO"
        |    },
        |    "related_visits": [],
        |    "temporary_reservation_id": 222222,
        |    "valuations": [
        |      {
        |        "alternative_price": null,
        |        "contract_id": 333333,
        |        "is_external_referral_allowed": false,
        |        "is_referral_required": false,
        |        "payer_id": 44444,
        |        "price": 0.0,
        |        "product_element_id": 555555,
        |        "product_id": 666666,
        |        "product_in_contract_id": 777777,
        |        "require_referral_for_p_p": false,
        |        "valuation_type": 1
        |      }
        |    ]
        |  }
        |}""".stripMargin

    val result = json.as[ReservationLocktermResponse]

    result.hasErrors                               shouldBe false
    result.hasWarnings                             shouldBe false
    result.errors                                  shouldBe empty
    result.value.temporaryReservationId            shouldBe 222222L
    result.value.changeTermAvailable               shouldBe false
    result.value.conflictedVisit                   shouldBe None
    result.value.doctorDetails.firstName           shouldBe "TARAS"
    result.value.valuations                        should have size 1
    result.value.valuations.head.contractId        shouldBe Some(333333L)
  }

  test("deserialize ReservationConfirmResponse") {
    val json =
      """{
        |  "errors": [],
        |  "has_errors": false,
        |  "has_warnings": false,
        |  "warnings": [],
        |  "value": {
        |    "can_self_confirm": false,
        |    "nps_token": "babababa-9282-1662-a525-ababbabaa",
        |    "reservation_id": 2222222,
        |    "service_instance_id": 33333333
        |  }
        |}""".stripMargin

    val result = json.as[ReservationConfirmResponse]

    result.hasErrors              shouldBe false
    result.value.reservationId    shouldBe 2222222L
    result.value.serviceInstanceId shouldBe 33333333L
    result.value.npsToken         shouldBe "babababa-9282-1662-a525-ababbabaa"
    result.value.canSelfConfirm   shouldBe false
  }

  // ── TermsIndexResponse ─────────────────────────────────────────────────────

  test("deserialize TermsIndexResponse with local datetime") {
    val json =
      """{
        |  "correlation_id": "00000000-0000-0000-0000-000000000000",
        |  "terms_for_service": {
        |    "additional_data": {
        |      "is_preparation_required": false,
        |      "preparation_items": []
        |    },
        |    "terms_for_days": [
        |      {
        |        "day": "2022-05-31T00:00:00",
        |        "terms": [
        |          {
        |            "clinic": "LX Wrocław - Fabryczna 6",
        |            "clinic_id": 2222,
        |            "clinic_group_id": 11,
        |            "date_time_from": "2021-05-21T18:45:00",
        |            "date_time_to": "2021-05-21T19:00:00",
        |            "doctor": {
        |              "academic_title": "lek. med.",
        |              "first_name": "TARAS",
        |              "gender_id": 0,
        |              "id": 33333,
        |              "last_name": "GRYGORYCH"
        |            },
        |            "impediment_text": "",
        |            "is_additional": false,
        |            "is_impediment": false,
        |            "is_telemedicine": true,
        |            "room_id": 4444,
        |            "schedule_id": 555555,
        |            "service_id": 66666
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |}""".stripMargin

    val result = json.as[TermsIndexResponse]

    result.correlationId shouldBe "00000000-0000-0000-0000-000000000000"
    val terms = result.termsForService.termsForDays
    terms                          should have size 1
    terms.head.terms               should have size 1
    val term = terms.head.terms.head
    term.clinic                    shouldBe Some("LX Wrocław - Fabryczna 6")
    term.clinicId                  shouldBe 2222L
    term.isTelemedicine            shouldBe true
    term.scheduleId                shouldBe 555555L
    term.doctor.lastName           shouldBe "GRYGORYCH"
    term.dateTimeFrom.get.toLocalTime shouldBe LocalTime.of(18, 45)
  }

  test("deserialize TermsIndexResponse with zoned datetime") {
    val json =
      """{
        |  "correlation_id": "abc",
        |  "terms_for_service": {
        |    "additional_data": {"is_preparation_required": false, "preparation_items": []},
        |    "terms_for_days": [
        |      {
        |        "day": "2022-05-31T00:00:00",
        |        "terms": [
        |          {
        |            "clinic": "Clinic A",
        |            "clinic_id": 1,
        |            "clinic_group_id": 1,
        |            "date_time_from": "2021-05-21T18:45:00+02:00",
        |            "date_time_to": "2021-05-21T19:00:00+02:00",
        |            "doctor": {"academic_title":"dr","first_name":"X","id":1,"last_name":"Y"},
        |            "impediment_text": "",
        |            "is_additional": false,
        |            "is_impediment": false,
        |            "is_telemedicine": false,
        |            "room_id": 1,
        |            "schedule_id": 1,
        |            "service_id": 1
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |}""".stripMargin

    val result = json.as[TermsIndexResponse]
    val term   = result.termsForService.termsForDays.head.terms.head

    term.dateTimeFrom.dateTimeTz shouldBe defined
    term.dateTimeFrom.get.toLocalTime shouldBe LocalTime.of(18, 45)
  }

  // ── Valuation ──────────────────────────────────────────────────────────────

  test("deserialize Valuation with null optional fields") {
    val json =
      """{
        |  "alternative_price": null,
        |  "contract_id": 555555,
        |  "is_external_referral_allowed": false,
        |  "is_referral_required": false,
        |  "payer_id": 66666,
        |  "price": 0.0,
        |  "product_element_id": 7777777,
        |  "product_id": 888888,
        |  "product_in_contract_id": 9999999,
        |  "require_referral_for_p_p": false,
        |  "valuation_type": 1
        |}""".stripMargin

    val result = JsonSerializer.extract[Valuation](json)

    result.contractId         shouldBe Some(555555L)
    result.alternativePrice   shouldBe None
    result.price              shouldBe Some(0.0)
    result.valuationType      shouldBe 1L
    result.isReferralRequired shouldBe false
  }

  // ── LuxmedFunnyDateTime ────────────────────────────────────────────────────

  test("LuxmedFunnyDateTime parses local datetime") {
    val json  = """"2021-05-21T18:45:00""""
    val result = JsonSerializer.extract[LuxmedFunnyDateTime](json)

    result.dateTimeLocal shouldBe defined
    result.dateTimeTz    shouldBe None
    result.get.toLocalTime shouldBe LocalTime.of(18, 45)
  }

  test("LuxmedFunnyDateTime parses zoned datetime") {
    val json   = """"2021-05-21T18:45:00+02:00""""
    val result = JsonSerializer.extract[LuxmedFunnyDateTime](json)

    result.dateTimeTz    shouldBe defined
    result.dateTimeLocal shouldBe None
    result.get.toLocalTime shouldBe LocalTime.of(18, 45)
  }
}



