package com.lbs.api.json.model

import java.net.HttpCookie

case class XsrfToken(token: String, cookies: Seq[HttpCookie])
