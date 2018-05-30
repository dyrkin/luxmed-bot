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
package com.lbs.api

import com.lbs.api.http.headers._
import scalaj.http.{Http, HttpRequest}

trait ApiBase {
  private val CommonHeaders =
    Map(
      Host -> "portalpacjenta.luxmed.pl",
      Accept -> "*/*",
      Connection -> "keep-alive",
      `Accept-Encoding` -> "gzip;q=1.0, compress;q=0.5",
      `User-Agent` -> "PatientPortal/3.3.0 (pl.luxmed.pp.LUX-MED; build:166; iOS 11.3.0) Alamofire/4.5.1",
      `Accept-Language` -> "en-PL;q=1.0, ru-PL;q=0.9, pl-PL;q=0.8, uk-PL;q=0.7"
    )


  protected def http(url: String): HttpRequest = {
    Http(s"https://portalpacjenta.luxmed.pl/PatientPortalMobileAPI/api/$url").headers(CommonHeaders)
  }
}
