
package com.lbs.api.json.model

/**
  * {
  *   "access_token": "IDtjG_ECOd_ETYE2fwrCoTcC6bW935cn_nUh6d3BaEa-jvPlHfPLOY5AkF",
  *   "expires_in": 599,
  *   "refresh_token": "d251c66c-49e0-4777-b766-08326d83fa31",
  *   "token_type": "bearer"
  * }
  */
case class LoginResponse(accessToken: String, expiresIn: Int, refreshToken: String, tokenType: String) extends SerializableJsonObject
