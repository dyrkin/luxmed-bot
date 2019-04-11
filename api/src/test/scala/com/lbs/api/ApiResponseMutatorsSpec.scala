package com.lbs.api

import com.lbs.api.json.model.{AvailableTermsResponse, AvailableVisitsTermPresentation, HistoricVisit, IdName, ReservationFilterResponse, ReservedVisit, ReservedVisitsResponse, VisitsHistoryResponse}
import org.scalatest.{FunSuiteLike, Matchers}

class ApiResponseMutatorsSpec extends FunSuiteLike with Matchers {
  test("ReservationFilterResponseMutator") {
    val mutated =
      ApiResponseMutators.ReservationFilterResponseMutator.mutate(
        ReservationFilterResponse(
          cities = Nil,
          clinics = Nil,
          defaultPayer = None,
          doctors = List(IdName(1, "AGNIESZKA dr n. med.")),
          languages = Nil,
          payers = Nil,
          services = Nil
        )
      )

    assert(mutated.doctors === List(IdName(1, "AGNIESZKA")))
  }

  test("ReservedVisitsResponseMutator") {
    val mutated =
      ApiResponseMutators.ReservedVisitsResponseMutator.mutate(
        ReservedVisitsResponse(
          List(
            ReservedVisit(
              canBeCanceled = false,
              clinic = null,
              doctorName = "AGNIESZKA dr n. med.",
              reservationId = 1L,
              service = null,
              visitDate = null
            )
          )
        )
      )

    assert(mutated.reservedVisits.head.doctorName === "AGNIESZKA")
  }

  test("VisitsHistoryResponseMutator") {
    val mutated =
      ApiResponseMutators.VisitsHistoryResponseMutator.mutate(
        VisitsHistoryResponse(
          areMoreVisits = false,
          historicVisits = List(
            HistoricVisit(
              clinicName = null,
              doctorName = "AGNIESZKA dr n. med.",
              reservationId = 1L,
              service = null,
              visitDate = null
            )
          )
        )
      )

    assert(mutated.historicVisits.head.doctorName === "AGNIESZKA")
  }

  test("AvailableTermsResponseMutator") {
    val mutated =
      ApiResponseMutators.AvailableTermsResponseMutator.mutate(
        AvailableTermsResponse(
          availableVisitsTermPresentation = List(
            AvailableVisitsTermPresentation(
              clinic = null,
              doctor = IdName(1, "AGNIESZKA dr n. med."),
              payerDetailsList = Nil,
              referralRequiredByProduct = false,
              referralRequiredByService = false,
              roomId = 1L,
              scheduleId = 1L,
              serviceId = 1L,
              visitDate = null
            )
          )
        )
      )

    assert(mutated.availableVisitsTermPresentation.head.doctor === IdName(1, "AGNIESZKA"))
  }
}
