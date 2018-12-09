
package com.lbs.api.exception

class GenericException(code: Int, status: String, message: String) extends ApiException(message) {
  override def toString: String = s"Code: $code, status: $status, message: $message"
}
