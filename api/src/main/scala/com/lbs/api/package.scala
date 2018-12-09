
package com.lbs

import com.lbs.api.json.model.{AvailableTermsResponse, ReservationFilterResponse, ReservedVisitsResponse, VisitsHistoryResponse}

import scala.util.matching.Regex

package object api {

  object ApiResponseMutators {
    private val DoctorPrefixes: Regex = """\s*(dr\s*n.\s*med.|dr\s*hab.\s*n.\s*med|lek.\s*med.|lek.\s*stom.)\s*""".r

    private def cleanupDoctorName(name: String) = DoctorPrefixes.replaceFirstIn(name, "")

    trait ResponseMutator[T] {
      def mutate(response: T): T
    }

    implicit class ResponseOps[T: ResponseMutator](response: Either[Throwable, T]) {
      def mutate: Either[Throwable, T] = {
        val mutator = implicitly[ResponseMutator[T]]
        response.map(mutator.mutate)
      }
    }

    implicit val ReservedVisitsResponseMutator: ResponseMutator[ReservedVisitsResponse] = (response: ReservedVisitsResponse) => {
      response.copy(reservedVisits = response.reservedVisits.map(rv => rv.copy(doctorName = cleanupDoctorName(rv.doctorName))))
    }

    implicit val VisitsHistoryResponseMutator: ResponseMutator[VisitsHistoryResponse] = (response: VisitsHistoryResponse) => {
      response.copy(historicVisits = response.historicVisits.map(hv => hv.copy(doctorName = cleanupDoctorName(hv.doctorName))))
    }

    implicit val ReservationFilterResponseMutator: ResponseMutator[ReservationFilterResponse] = (response: ReservationFilterResponse) => {
      response.copy(doctors = response.doctors.map(d => d.copy(name = cleanupDoctorName(d.name))))
    }

    implicit val AvailableTermsResponseMutator: ResponseMutator[AvailableTermsResponse] = (response: AvailableTermsResponse) => {
      response.copy(availableVisitsTermPresentation =
        response.availableVisitsTermPresentation.map(atp => atp.copy(doctor = atp.doctor.copy(name = cleanupDoctorName(atp.doctor.name)))))
    }
  }

}
