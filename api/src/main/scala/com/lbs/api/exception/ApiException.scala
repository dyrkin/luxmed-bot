
package com.lbs.api.exception

abstract class ApiException(message: String) extends Exception(s"Luxmed API exception: $message")