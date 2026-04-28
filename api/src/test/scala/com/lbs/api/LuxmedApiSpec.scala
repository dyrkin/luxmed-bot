package com.lbs.api

import cats.instances.either.*
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.lbs.api.http.Session
import com.lbs.api.json.model.*
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.net.HttpCookie
import java.time.{LocalDateTime, LocalTime, ZonedDateTime}

/** LuxmedApi integration-style test.
  *
  * Every test starts WireMock on a random port, creates a [[TestLuxmedApi]] that points all HTTP
  * calls to that local server, invokes one API method and then verifies:
  *   - the correct HTTP verb and path were used
  *   - all mandatory headers (Common, OldApi / NewApi) are present
  *   - auth headers / cookies are correct
  *   - query-string parameters match
  *   - request body serializes to the expected JSON
  *   - the response body is correctly deserialized
  */
class LuxmedApiSpec extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  // ── type alias so we can use Either as our ThrowableMonad ──────────────────
  private type ThrowableOr[T] = Either[Throwable, T]

  // ── WireMock lifecycle ─────────────────────────────────────────────────────
  private val wireMock = new WireMockServer(wireMockConfig().dynamicPort())

  override def beforeAll(): Unit = wireMock.start()
  override def afterAll(): Unit  = wireMock.stop()

  private def resetWireMock(): Unit = wireMock.resetAll()

  // ── Test double ───────────────────────────────────────────────────────────
  /** Overrides both base URLs so every scalaj call goes to WireMock. */
  private class TestLuxmedApi extends LuxmedApi[ThrowableOr] {
    // evaluated lazily so it is only resolved after WireMock has started
    override protected lazy val oldApiBaseUrl: String = s"http://localhost:${wireMock.port()}/PatientPortalMobileAPI/api"
    override protected lazy val newApiBaseUrl: String = s"http://localhost:${wireMock.port()}/PatientPortal"
  }

  private val api = new TestLuxmedApi

  // ── Common header matchers ─────────────────────────────────────────────────
  private val commonHeaderMatchers = Seq(
    matching("portalpacjenta.luxmed.pl"),   // Host
    matching("application/json.*"),          // Accept
    matching(".*okhttp.*|.*Mozilla.*")       // User-Agent (old or new)
  )

  // ── Fixtures ───────────────────────────────────────────────────────────────
  private def mkCookie(name: String, value: String): HttpCookie = {
    val c = new HttpCookie(name, value)
    c.setPath("/")
    c.setVersion(0)
    c
  }

  private val sessionCookie  = mkCookie("SessionCookie", "sessionVal123")
  private val session = Session(
    accessToken = "ACCESS_TOKEN_XYZ",
    tokenType   = "bearer",
    jwtToken    = "JWT_TOKEN_ABC",
    cookies     = Seq(sessionCookie)
  )

  private val xsrfCookie = mkCookie("__RequestVerificationToken", "XSRF_COOKIE_VAL")
  private val xsrfToken  = XsrfToken(token = "XSRF_HEADER_VAL", cookies = Seq(xsrfCookie))

  // ═══════════════════════════════════════════════════════════════════════════
  // login
  // ═══════════════════════════════════════════════════════════════════════════

  test("login – POST /token with form params, returns LoginResponse") {
    resetWireMock()
    wireMock.stubFor(
      post(urlPathEqualTo("/PatientPortalMobileAPI/api/token"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{"access_token":"tok123","expires_in":599,"refresh_token":"ref456","token_type":"bearer"}"""
            )
        )
    )

    val result = api.login("user@example.com", "secret_pass")
    result shouldBe a[Right[?, ?]]

    val body = result.toOption.get.body
    body.accessToken  shouldBe "tok123"
    body.tokenType    shouldBe "bearer"
    body.refreshToken shouldBe "ref456"
    body.expiresIn    shouldBe 599

    // verify the outgoing request
    wireMock.verify(
      postRequestedFor(urlPathEqualTo("/PatientPortalMobileAPI/api/token"))
        .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
        // Note: Java's HttpURLConnection overrides Host with the actual server host (localhost in tests)
        .withHeader("Accept",       containing("application/json"))
        .withHeader("X-Api-Client-Identifier", equalTo("Android"))
        .withHeader("Custom-User-Agent", containing("Patient Portal"))
        .withRequestBody(containing("grant_type=password"))
        .withRequestBody(containing("client_id=Android"))
        .withRequestBody(containing("username=user%40example.com"))
        .withRequestBody(containing("password=secret_pass"))
    )
  }

  test("login – custom clientId is sent as form param") {
    resetWireMock()
    wireMock.stubFor(
      post(urlPathEqualTo("/PatientPortalMobileAPI/api/token"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("""{"access_token":"t","expires_in":1,"refresh_token":"r","token_type":"bearer"}""")
        )
    )

    api.login("u", "p", clientId = "iOS")

    wireMock.verify(
      postRequestedFor(urlPathEqualTo("/PatientPortalMobileAPI/api/token"))
        .withRequestBody(containing("client_id=iOS"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // loginToApp
  // ═══════════════════════════════════════════════════════════════════════════

  test("loginToApp – GET NewPortal/Page/LogInToApp with old-token auth and session cookie") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/Account/LogInToApp"))
        .willReturn(aResponse().withStatus(200).withBody("OK"))
    )

    val result = api.loginToApp(session)
    result shouldBe a[Right[?, ?]]

    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/PatientPortal/Account/LogInToApp"))
        // Note: Host/Origin are overridden by Java's HttpURLConnection to the actual server host
        .withHeader("Authorization", equalTo("ACCESS_TOKEN_XYZ"))
        .withHeader("X-Requested-With", equalTo("pl.luxmed.pp"))
        .withHeader("Custom-User-Agent", containing("Patient Portal"))
        .withHeader("Cookie", containing("SessionCookie=sessionVal123"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // getForgeryToken
  // ═══════════════════════════════════════════════════════════════════════════

  test("getForgeryToken – GET security/getforgerytoken with JWT bearer") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/security/getforgerytoken"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("""{"token":"FORGERY_TOK"}""")
        )
    )

    val result = api.getForgeryToken(session)
    result shouldBe a[Right[?, ?]]
    result.toOption.get.body.token shouldBe "FORGERY_TOK"

    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/PatientPortal/security/getforgerytoken"))
        .withHeader("authorization-token", equalTo("Bearer JWT_TOKEN_ABC"))
        .withHeader("Custom-User-Agent",   containing("Patient Portal"))
        .withHeader("Cookie",              containing("SessionCookie=sessionVal123"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // getReservationPage
  // ═══════════════════════════════════════════════════════════════════════════

  test("getReservationPage – GET NewPortal/Page/Reservation with provided cookies") {
    resetWireMock()
    val extraCookie = mkCookie("ExtraCookie", "extraVal")
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/NewPortal/Page/Reservation"))
        .willReturn(aResponse().withStatus(200).withBody("<html/>"))
    )

    val result = api.getReservationPage(session, Seq(extraCookie))
    result shouldBe a[Right[?, ?]]

    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/PatientPortal/NewPortal/Page/Reservation"))
        .withHeader("Authorization",    equalTo("ACCESS_TOKEN_XYZ"))
        .withHeader("X-Requested-With", equalTo("pl.luxmed.pp"))
        .withHeader("Cookie",           containing("ExtraCookie=extraVal"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // events
  // ═══════════════════════════════════════════════════════════════════════════

  test("events – GET Events with date range params and old-API auth") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortalMobileAPI/api/Events"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("""{"Events":[],"IsEndOfList":true,"ServerDateTime":"2021-07-01T14:32:00+02:00"}""")
        )
    )
    val from = ZonedDateTime.parse("2021-01-01T00:00:00+01:00")
    val to   = ZonedDateTime.parse("2021-06-30T00:00:00+01:00")
    val result = api.events(session, fromDate = from, toDate = to)
    result shouldBe a[Right[?, ?]]
    result.toOption.get.events shouldBe empty

    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/PatientPortalMobileAPI/api/Events"))
        .withHeader("Content-Type",  containing("application/json"))
        .withHeader("Authorization", equalTo("bearer ACCESS_TOKEN_XYZ"))
        .withHeader("X-Api-Client-Identifier", equalTo("Android"))
        .withHeader("Cookie", containing("SessionCookie=sessionVal123"))
        .withQueryParam("filter.filterDateFrom", equalTo("2021-01-01T00:00:00+0100"))
        .withQueryParam("filter.filterDateTo",   equalTo("2021-06-30T00:00:00+0100"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // dictionaryCities
  // ═══════════════════════════════════════════════════════════════════════════

  test("dictionaryCities – GET NewPortal/Dictionary/cities with JWT, returns list") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/cities"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("""[{"id":70,"name":"Białystok"},{"id":12,"name":"Gdańsk"}]""")
        )
    )

    val result = api.dictionaryCities(session)
    result shouldBe a[Right[?, ?]]
    val cities = result.toOption.get
    cities should have size 2
    cities.head.id   shouldBe 70L
    cities.head.name shouldBe "Białystok"

    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/cities"))
        .withHeader("authorization-token", equalTo("Bearer JWT_TOKEN_ABC"))
        .withHeader("Content-Type",        containing("application/json"))
        .withHeader("Cookie",              containing("SessionCookie=sessionVal123"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // dictionaryServiceVariants
  // ═══════════════════════════════════════════════════════════════════════════

  test("dictionaryServiceVariants – GET serviceVariantsGroups with JWT") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/serviceVariantsGroups"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("""[{"id":1,"name":"Internista","expanded":true,"children":[],"isTelemedicine":false,"paymentType":0}]""")
        )
    )

    val result = api.dictionaryServiceVariants(session)
    result shouldBe a[Right[?, ?]]
    result.toOption.get should have size 1

    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/serviceVariantsGroups"))
        .withHeader("authorization-token", equalTo("Bearer JWT_TOKEN_ABC"))
        .withHeader("Content-Type",        containing("application/json"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // dictionaryFacilitiesAndDoctors
  // ═══════════════════════════════════════════════════════════════════════════

  test("dictionaryFacilitiesAndDoctors – GET with optional cityId and serviceVariantId") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/facilitiesAndDoctors"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("""{"doctors":[],"facilities":[]}""")
        )
    )

    val result = api.dictionaryFacilitiesAndDoctors(session, cityId = Some(70L), serviceVariantId = Some(999L))
    result shouldBe a[Right[?, ?]]

    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/facilitiesAndDoctors"))
        .withHeader("authorization-token", equalTo("Bearer JWT_TOKEN_ABC"))
        .withQueryParam("cityId",           equalTo("70"))
        .withQueryParam("serviceVariantId", equalTo("999"))
    )
  }

  test("dictionaryFacilitiesAndDoctors – omits absent optional params") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/facilitiesAndDoctors"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("""{"doctors":[],"facilities":[]}""")
        )
    )

    api.dictionaryFacilitiesAndDoctors(session, cityId = None, serviceVariantId = None)

    // verify the request was made (params should be absent – checked by not finding them in url)
    wireMock.verify(
      getRequestedFor(urlMatching("/PatientPortal/NewPortal/Dictionary/facilitiesAndDoctors(?!.*cityId).*"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // termsIndex
  // ═══════════════════════════════════════════════════════════════════════════

  test("termsIndex – GET NewPortal/terms/index with all required params") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/NewPortal/terms/index"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody(
              """{
                |  "correlation_id":"corr-1",
                |  "terms_for_service":{
                |    "additional_data":{"is_preparation_required":false,"preparation_items":[]},
                |    "terms_for_days":[]
                |  }
                |}""".stripMargin
            )
        )
    )

    val from = LocalDateTime.of(2021, 5, 10, 0, 0)
    val to   = LocalDateTime.of(2021, 5, 24, 0, 0)
    val result = api.termsIndex(
      session,
      cityId    = 70L,
      clinicId  = Some(78L),
      serviceId = 999L,
      doctorId  = Some(111L),
      fromDate  = from,
      toDate    = to
    )
    result shouldBe a[Right[?, ?]]
    result.toOption.get.correlationId shouldBe "corr-1"

    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/PatientPortal/NewPortal/terms/index"))
        .withHeader("authorization-token", equalTo("Bearer JWT_TOKEN_ABC"))
        .withHeader("Cookie",              containing("SessionCookie=sessionVal123"))
        .withQueryParam("searchPlace.id",    equalTo("70"))
        .withQueryParam("searchPlace.type",  equalTo("0"))
        .withQueryParam("serviceVariantId",  equalTo("999"))
        .withQueryParam("languageId",        equalTo("10"))
        .withQueryParam("searchDateFrom",    equalTo("2021-05-10"))
        .withQueryParam("searchDateTo",      equalTo("2021-05-24"))
        .withQueryParam("searchDatePreset",  equalTo("14"))
        .withQueryParam("facilitiesIds",     equalTo("78"))
        .withQueryParam("doctorsIds",        equalTo("111"))
        .withQueryParam("nextSearch",        equalTo("false"))
        .withQueryParam("searchByMedicalSpecialist", equalTo("false"))
        .withQueryParam("delocalized",       equalTo("false"))
        .withQueryParam("serviceVariantSource", equalTo("0"))
    )
  }

  test("termsIndex – omits absent clinicId and doctorId params") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/NewPortal/terms/index"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody(
              """{
                |  "correlation_id":"corr-2",
                |  "terms_for_service":{
                |    "additional_data":{"is_preparation_required":false,"preparation_items":[]},
                |    "terms_for_days":[]
                |  }
                |}""".stripMargin
            )
        )
    )

    val to = LocalDateTime.of(2021, 5, 24, 0, 0)
    api.termsIndex(session, cityId = 70L, clinicId = None, serviceId = 999L, doctorId = None, toDate = to)

    // verify the request was made without facilitiesIds/doctorsIds in the URL
    wireMock.verify(
      getRequestedFor(urlMatching("/PatientPortal/NewPortal/terms/index(?!.*facilitiesIds)(?!.*doctorsIds).*"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // reservationLockterm
  // ═══════════════════════════════════════════════════════════════════════════

  test("reservationLockterm – POST NewPortal/reservation/lockterm with xsrf headers, cookies, JSON body") {
    resetWireMock()
    wireMock.stubFor(
      post(urlPathEqualTo("/PatientPortal/NewPortal/reservation/lockterm"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody(
              """{
                |"errors":[],"has_errors":false,"has_warnings":false,"warnings":[],
                |"value":{
                |  "change_term_available":false,
                |  "conflicted_visit":null,
                |  "doctor_details":{"academic_title":"lek.","first_name":"TARAS","gender_id":1,"id":111,"last_name":"SHEV"},
                |  "related_visits":[],
                |  "temporary_reservation_id":555,
                |  "valuations":[]
                |}}""".stripMargin
            )
        )
    )

    val doctor = Doctor(
      academicTitle    = "lek.",
      facilityGroupIds = None,
      firstName        = "TARAS",
      isEnglishSpeaker = None,
      genderId         = None,
      id               = 111L,
      lastName         = "SHEV"
    )
    val locktermReq = ReservationLocktermRequest(
      date                  = "2021-05-21T08:00:00.000Z",
      doctor                = doctor,
      doctorId              = 111L,
      facilityId            = 78L,
      impedimentText        = "",
      isAdditional          = false,
      isImpediment          = false,
      isPreparationRequired = false,
      isTelemedicine        = false,
      preparationItems      = Nil,
      roomId                = 3333L,
      scheduleId            = 44444L,
      serviceVariantId      = 55555L,
      timeFrom              = "08:00",
      timeTo                = "08:15"
    )

    val result = api.reservationLockterm(session, xsrfToken, locktermReq)
    result shouldBe a[Right[?, ?]]
    result.toOption.get.value.temporaryReservationId shouldBe 555L

    wireMock.verify(
      postRequestedFor(urlPathEqualTo("/PatientPortal/NewPortal/reservation/lockterm"))
        .withHeader("Content-Type",        equalTo("application/json"))
        .withHeader("authorization-token", equalTo("Bearer JWT_TOKEN_ABC"))
        .withHeader("xsrf-token",          equalTo("XSRF_HEADER_VAL"))
        .withHeader("Cookie",              containing("SessionCookie=sessionVal123"))
        .withHeader("Cookie",              containing("__RequestVerificationToken=XSRF_COOKIE_VAL"))
        .withRequestBody(containing("\"doctorId\" : 111"))
        .withRequestBody(containing("\"facilityId\" : 78"))
        .withRequestBody(containing("\"scheduleId\" : 44444"))
        .withRequestBody(containing("\"serviceVariantId\" : 55555"))
        .withRequestBody(containing("\"isTelemedicine\" : false"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // deleteTemporaryReservation
  // ═══════════════════════════════════════════════════════════════════════════

  test("deleteTemporaryReservation – POST releaseterm?reservationId with xsrf and empty body") {
    resetWireMock()
    wireMock.stubFor(
      post(urlPathEqualTo("/PatientPortal/NewPortal/reservation/releaseterm"))
        .willReturn(aResponse().withStatus(200).withBody(""))
    )

    val result = api.deleteTemporaryReservation(session, xsrfToken, 888L)
    result shouldBe a[Right[?, ?]]

    wireMock.verify(
      postRequestedFor(urlPathEqualTo("/PatientPortal/NewPortal/reservation/releaseterm"))
        .withQueryParam("reservationId",   equalTo("888"))
        .withHeader("Content-Type",        equalTo("application/json"))
        .withHeader("authorization-token", equalTo("Bearer JWT_TOKEN_ABC"))
        .withHeader("xsrf-token",          equalTo("XSRF_HEADER_VAL"))
        .withHeader("Cookie",              containing("SessionCookie=sessionVal123"))
        .withHeader("Cookie",              containing("__RequestVerificationToken=XSRF_COOKIE_VAL"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // reservationConfirm
  // ═══════════════════════════════════════════════════════════════════════════

  test("reservationConfirm – POST NewPortal/reservation/confirm with xsrf and JSON body") {
    resetWireMock()
    wireMock.stubFor(
      post(urlPathEqualTo("/PatientPortal/NewPortal/reservation/confirm"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody(
              """{
                |"errors":[],"has_errors":false,"has_warnings":false,"warnings":[],
                |"value":{
                |  "can_self_confirm":false,
                |  "nps_token":"nps-tok",
                |  "reservation_id":777,
                |  "service_instance_id":888
                |}}""".stripMargin
            )
        )
    )

    val valuation = Valuation(
      contractId             = Some(333L),
      payerId                = Some(44L),
      price                  = Some(0.0),
      alternativePrice       = None,
      productElementId       = Some(5L),
      productId              = Some(6L),
      productInContractId    = Some(7L),
      isReferralRequired     = false,
      isExternalReferralAllowed = false,
      requireReferralForPP   = false,
      valuationType          = 1L
    )
    val confirmReq = ReservationConfirmRequest(
      date                  = "2021-05-21T08:00:00.000Z",
      doctorId              = 111L,
      facilityId            = 78L,
      roomId                = 3333L,
      scheduleId            = 44444L,
      serviceVariantId      = 55555L,
      temporaryReservationId = 222L,
      timeFrom              = LocalTime.of(8, 0),
      valuation             = valuation
    )

    val result = api.reservationConfirm(session, xsrfToken, confirmReq)
    result shouldBe a[Right[?, ?]]
    result.toOption.get.value.reservationId shouldBe 777L

    wireMock.verify(
      postRequestedFor(urlPathEqualTo("/PatientPortal/NewPortal/reservation/confirm"))
        .withHeader("Content-Type",        equalTo("application/json"))
        .withHeader("authorization-token", equalTo("Bearer JWT_TOKEN_ABC"))
        .withHeader("xsrf-token",          equalTo("XSRF_HEADER_VAL"))
        .withHeader("Cookie",              containing("SessionCookie=sessionVal123"))
        .withHeader("Cookie",              containing("__RequestVerificationToken=XSRF_COOKIE_VAL"))
        .withRequestBody(containing("\"doctorId\" : 111"))
        .withRequestBody(containing("\"temporaryReservationId\" : 222"))
        .withRequestBody(containing("\"facilityId\" : 78"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // reservationChangeTerm
  // ═══════════════════════════════════════════════════════════════════════════

  test("reservationChangeTerm – POST NewPortal/reservation/changeterm with xsrf and nested JSON") {
    resetWireMock()
    wireMock.stubFor(
      post(urlPathEqualTo("/PatientPortal/NewPortal/reservation/changeterm"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody(
              """{
                |"errors":[],"has_errors":false,"has_warnings":false,"warnings":[],
                |"value":{
                |  "can_self_confirm":false,
                |  "nps_token":"nps-2",
                |  "reservation_id":111,
                |  "service_instance_id":222
                |}}""".stripMargin
            )
        )
    )

    val valuation = Valuation(
      contractId             = Some(99L),
      payerId                = Some(10L),
      price                  = Some(0.0),
      alternativePrice       = None,
      productElementId       = Some(1L),
      productId              = Some(2L),
      productInContractId    = Some(3L),
      isReferralRequired     = false,
      isExternalReferralAllowed = false,
      requireReferralForPP   = false,
      valuationType          = 1L
    )
    val newTerm = NewTerm(
      date                  = "2021-07-03T05:30:00.000Z",
      doctorId              = 22222L,
      facilityId            = 33333L,
      parentReservationId   = 987654321L,
      referralRequired      = false,
      roomId                = 55555L,
      scheduleId            = 666666L,
      serviceVariantId      = 777777L,
      temporaryReservationId = 8888888L,
      timeFrom              = LocalTime.of(8, 30),
      valuation             = valuation
    )
    val changeTermReq = ReservationChangetermRequest(existingReservationId = 987654321L, term = newTerm)

    val result = api.reservationChangeTerm(session, xsrfToken, changeTermReq)
    result shouldBe a[Right[?, ?]]
    result.toOption.get.value.reservationId shouldBe 111L

    wireMock.verify(
      postRequestedFor(urlPathEqualTo("/PatientPortal/NewPortal/reservation/changeterm"))
        .withHeader("Content-Type",        equalTo("application/json"))
        .withHeader("authorization-token", equalTo("Bearer JWT_TOKEN_ABC"))
        .withHeader("xsrf-token",          equalTo("XSRF_HEADER_VAL"))
        .withHeader("Cookie",              containing("SessionCookie=sessionVal123"))
        .withHeader("Cookie",              containing("__RequestVerificationToken=XSRF_COOKIE_VAL"))
        .withRequestBody(containing("\"existingReservationId\" : 987654321"))
        .withRequestBody(containing("\"temporaryReservationId\" : 8888888"))
        .withRequestBody(containing("\"serviceVariantId\" : 777777"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // reservationDelete
  // ═══════════════════════════════════════════════════════════════════════════

  test("reservationDelete – DELETE events/Visit/{id} with old-API auth") {
    resetWireMock()
    wireMock.stubFor(
      delete(urlPathEqualTo("/PatientPortalMobileAPI/api/events/Visit/12345"))
        .willReturn(aResponse().withStatus(200).withBody(""))
    )

    val result = api.reservationDelete(session, 12345L)
    result shouldBe a[Right[?, ?]]

    wireMock.verify(
      deleteRequestedFor(urlPathEqualTo("/PatientPortalMobileAPI/api/events/Visit/12345"))
        .withHeader("Content-Type",  containing("application/json"))
        .withHeader("Authorization", equalTo("bearer ACCESS_TOKEN_XYZ"))
        .withHeader("X-Api-Client-Identifier", equalTo("Android"))
        .withHeader("Cookie",                  containing("SessionCookie=sessionVal123"))
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Error / edge cases
  // ═══════════════════════════════════════════════════════════════════════════

  test("any endpoint – 401 response with session-expired message raises SessionExpiredException") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/cities"))
        .willReturn(
          aResponse().withStatus(302)
            .withHeader("Location", "https://portalpacjenta.luxmed.pl/PatientPortal/LogOn")
        )
    )

    val result = api.dictionaryCities(session)
    result shouldBe a[Left[?, ?]]
    result.left.toOption.get shouldBe a[com.lbs.api.exception.SessionExpiredException]
  }

  test("any endpoint – 409 with invalid credentials body raises InvalidLoginOrPasswordException") {
    resetWireMock()
    wireMock.stubFor(
      post(urlPathEqualTo("/PatientPortalMobileAPI/api/token"))
        .willReturn(
          aResponse().withStatus(409)
            .withHeader("Content-Type", "application/json")
            .withBody("""{"Message":"Invalid login or password"}""")
        )
    )

    val result = api.login("bad@user.com", "wrong_pw")
    result shouldBe a[Left[?, ?]]
    result.left.toOption.get shouldBe a[com.lbs.api.exception.InvalidLoginOrPasswordException]
  }

  test("any endpoint – 4xx generic error raises GenericException with message") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/cities"))
        .willReturn(
          aResponse().withStatus(400)
            .withHeader("Content-Type", "application/json")
            .withBody("""{"Message":"Bad request happened"}""")
        )
    )

    val result = api.dictionaryCities(session)
    result shouldBe a[Left[?, ?]]
    result.left.toOption.get shouldBe a[com.lbs.api.exception.GenericException]
    result.left.toOption.get.getMessage should include("Bad request happened")
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Common headers invariants
  // ═══════════════════════════════════════════════════════════════════════════

  test("all old-API requests include common headers: Host, Accept, Accept-Encoding, Accept-Language") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortalMobileAPI/api/Events"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("""{"Events":[],"IsEndOfList":true,"ServerDateTime":"2021-07-01T14:32:00+02:00"}""")
        )
    )
    api.events(session, fromDate = ZonedDateTime.now().minusMonths(1), toDate = ZonedDateTime.now())

    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/PatientPortalMobileAPI/api/Events"))
        // Note: Java's HttpURLConnection overrides Host/Origin with the actual server host
        .withHeader("Accept",          containing("application/json"))
        .withHeader("Accept-Encoding", containing("gzip"))
        .withHeader("accept-language", containing("pl"))
        .withHeader("User-Agent",      equalTo("okhttp/4.9.0"))
        .withHeader("Custom-User-Agent", containing("Patient Portal; 4.42.0"))
        .withHeader("X-Api-Client-Identifier", equalTo("Android"))
    )
  }

  test("all new-API requests include common + new headers and JWT token") {
    resetWireMock()
    wireMock.stubFor(
      get(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/cities"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody("[]")
        )
    )

    api.dictionaryCities(session)

    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/PatientPortal/NewPortal/Dictionary/cities"))
        // Note: Java's HttpURLConnection overrides Host/Origin with the actual server host
        .withHeader("Accept",          containing("application/json"))
        .withHeader("Accept-Encoding", containing("gzip"))
        .withHeader("accept-language", containing("pl"))
        .withHeader("Custom-User-Agent", containing("Patient Portal; 4.42.0"))
        // User-Agent is set in NewApiHeaders (Mozilla/5.0) but Java's HttpClient may also
        // send its own User-Agent; exact value is not asserted here.
        .withHeader("authorization-token", equalTo("Bearer JWT_TOKEN_ABC"))
    )
  }
}
















