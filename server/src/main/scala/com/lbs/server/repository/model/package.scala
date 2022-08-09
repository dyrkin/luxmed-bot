package com.lbs.server.repository

import scala.language.implicitConversions

package object model {
  type JLong = java.lang.Long
  type JInt = java.lang.Integer

  implicit def JLongToOptionLong(jLong: JLong): Option[Long] = Option(jLong).map(_.longValue())

  implicit def OptionLongToJLong(long: Option[Long]): JLong = long.map(Long.box).orNull
}
