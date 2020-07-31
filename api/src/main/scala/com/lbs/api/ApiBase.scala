
package com.lbs.api

import com.lbs.api.http.headers._
import scalaj.http.{BaseHttp, HttpRequest}

object ApiHttp extends BaseHttp(
  userAgent = "PatientPortal/3.20.5 (pl.luxmed.pp.LUX-MED; build:853; iOS 13.5.1) Alamofire/4.9.1"
)

trait ApiBase {
  private val CommonHeaders =
    Map(
      Host -> "portalpacjenta.luxmed.pl",
      `Custom-User-Agent` -> "PatientPortal; 3.20.5; 4380E6AC-D291-4895-8B1B-F774C318BD7D; iOS; 13.5.1; iPhone8,1",
      Accept -> "*/*",
      Connection -> "keep-alive",
      `Accept-Encoding` -> "gzip;q=1.0, compress;q=0.5",
      `Accept-Language` -> "en;q=1.0, en-PL;q=0.9, pl-PL;q=0.8, ru-PL;q=0.7, uk-PL;q=0.6"
    )


  protected def http(url: String): HttpRequest = {
    ApiHttp(s"https://portalpacjenta.luxmed.pl/PatientPortalMobileAPI/api/$url").headers(CommonHeaders)
  }
}
