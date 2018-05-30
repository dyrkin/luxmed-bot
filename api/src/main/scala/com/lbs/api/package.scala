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
