
package com.lbs.api.exception

class GenericException(val code: Int, val status: String, val message: String) extends ApiException(message) {
  override def toString: String = s"Code: $code, status: $status, message: $message"
}
