
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
      `User-Agent` -> "PatientPortal/3.10.0 (pl.luxmed.pp.LUX-MED; build:401; iOS 12.1.0) Alamofire/4.5.1",
      `Accept-Language` -> "en-PL;q=1.0, ru-PL;q=0.9, pl-PL;q=0.8, uk-PL;q=0.7"
    )


  protected def http(url: String): HttpRequest = {
    Http(s"https://portalpacjenta.luxmed.pl/PatientPortalMobileAPI/api/$url").headers(CommonHeaders)
  }
}
