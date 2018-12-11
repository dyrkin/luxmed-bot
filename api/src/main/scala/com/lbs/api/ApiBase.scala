
package com.lbs.api

import com.lbs.api.http.headers._
import scalaj.http.{BaseHttp, HttpRequest}

object ApiHttp extends BaseHttp(
  userAgent = "PatientPortal/3.10.0 (pl.luxmed.pp.LUX-MED; build:401; iOS 12.1.0) Alamofire/4.5.1"
)

trait ApiBase {
  private val CommonHeaders =
    Map(
      Host -> "portalpacjenta.luxmed.pl",
      Accept -> "*/*",
      Connection -> "keep-alive",
      `Accept-Encoding` -> "gzip;q=1.0, compress;q=0.5",
      `Accept-Language` -> "en-PL;q=1.0, ru-PL;q=0.9, pl-PL;q=0.8, uk-PL;q=0.7"
    )


  protected def http(url: String): HttpRequest = {
    ApiHttp(s"https://portalpacjenta.luxmed.pl/PatientPortalMobileAPI/api/$url").headers(CommonHeaders)
  }
}
