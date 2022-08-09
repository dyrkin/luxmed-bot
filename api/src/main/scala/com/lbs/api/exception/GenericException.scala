package com.lbs.api.exception

case class GenericException(code: Int, message: String) extends ApiException(message) {
  override def toString: String = s"Code: $code, message: $message"
}
