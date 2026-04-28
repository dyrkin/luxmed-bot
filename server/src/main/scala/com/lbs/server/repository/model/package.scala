package com.lbs.server.repository.model

type JLong = java.lang.Long
type JInt  = java.lang.Integer

given Conversion[JLong, Option[Long]] = jLong => Option(jLong).map(_.longValue())
given Conversion[Option[Long], JLong] = long => long.map(Long.box).orNull
