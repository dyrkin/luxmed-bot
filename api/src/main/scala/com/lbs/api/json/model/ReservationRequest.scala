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
package com.lbs.api.json.model

import java.time.ZonedDateTime


/**
{
    "ClinicId": 6,
    "DoctorId": 38509,
    "PayerData": {
        "ContractId": 1111111,
        "PayerId": 22222,
        "PayerName": "FIRMA POLAND SP. Z O.O.",
        "ProductElementId": 8547100,
        "ProductId": 44444,
        "ProductInContractId": 555555,
        "ServaAppId": 0,
        "ServaId": 6621
    },
    "RoomId": 159,
    "ServiceId": 6621,
    "StartDateTime": "2018-06-04T11:00:00+02:00",
    "TemporaryReservationId": 250303839
}
  */
case class ReservationRequest(clinicId: Long, doctorId: Long, payerData: PayerDetails, roomId: Long, serviceId: Long,
                                       startDateTime: ZonedDateTime, temporaryReservationId: Long) extends SerializableJsonObject
