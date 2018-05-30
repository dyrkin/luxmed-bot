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

/**
{
    "Cities": [
        {
            "Id": 5,
            "Name": "Wrocław"
        }
    ],
    "Clinics": [
        {
            "Id": 1405,
            "Name": "Konsylium Wrocław - Legnicka 40"
        },
        {
            "Id": 7,
            "Name": "LX Wrocław - Kwidzyńska 6"
        }
    ],
    "DefaultPayer": {
        "Id": 22222,
        "Name": "FIRMA POLAND SP. Z O.O."
    },
    "Doctors": [
        {
            "Id": 38275,
            "Name": "ANNA ABRAMCZYK lek. med."
        },
        {
            "Id": 15565,
            "Name": "ANDRZEJ ANDEWSKI dr n. med."
        }
    ],
    "Languages": [
        {
            "Id": 11,
            "Name": "english"
        },
        {
            "Id": 10,
            "Name": "polish"
        }
    ],
    "Payers": [
        {
            "Id": 22222,
            "Name": "FIRMA POLAND SP. Z O.O."
        }
    ],
    "Services": [
        {
            "Id": 5857,
            "Name": "Audiometr standardowy"
        },
        {
            "Id": 7976,
            "Name": "Audiometr standardowy - audiometria nadprogowa"
        }
    ]
}
  */
case class ReservationFilterResponse(cities: List[IdName], clinics: List[IdName], defaultPayer: Option[IdName],
                                     doctors: List[IdName], languages: List[IdName], payers: List[IdName],
                                     services: List[IdName]) extends SerializableJsonObject

