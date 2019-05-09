
package com.lbs

import cats.MonadError
import cats.implicits._
import com.lbs.api.json.model.{AvailableTermsResponse, ReservationFilterResponse, ReservedVisitsResponse, VisitsHistoryResponse}
import com.softwaremill.quicklens._

import scala.language.higherKinds
import scala.util.matching.Regex

package object api {

  type ThrowableMonad[F[_]] = MonadError[F, Throwable]

  object ApiResponseMutators {
    private val DoctorPrefixes: Regex = """\s*(dr\s*n.\s*med.|dr\s*hab.\s*n.\s*med|lek.\s*med.|lek.\s*stom.)\s*""".r

    private def cleanupDoctorName(name: String) = DoctorPrefixes.replaceFirstIn(name, "")

    trait ResponseMutator[T] {
      def mutate(response: T): T
    }

    implicit class ResponseOps[T: ResponseMutator, F[_] : ThrowableMonad](response: F[T]) {
      def mutate: F[T] = {
        val mutator = implicitly[ResponseMutator[T]]
        response.map(mutator.mutate)
      }
    }

    implicit val ReservedVisitsResponseMutator: ResponseMutator[ReservedVisitsResponse] = (response: ReservedVisitsResponse) => {
      response.modify(_.reservedVisits.each.doctorName).using(cleanupDoctorName)
    }

    implicit val VisitsHistoryResponseMutator: ResponseMutator[VisitsHistoryResponse] = (response: VisitsHistoryResponse) => {
      response.modify(_.historicVisits.each.doctorName).using(cleanupDoctorName)
    }

    implicit val ReservationFilterResponseMutator: ResponseMutator[ReservationFilterResponse] = (response: ReservationFilterResponse) => {
      response.modify(_.doctors.each.name).using(cleanupDoctorName)
    }

    implicit val AvailableTermsResponseMutator: ResponseMutator[AvailableTermsResponse] = (response: AvailableTermsResponse) => {
      response.modify(_.availableVisitsTermPresentation.each.doctor.name).using(cleanupDoctorName)
    }
  }

}
