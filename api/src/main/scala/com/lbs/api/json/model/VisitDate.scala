
package com.lbs.api.json.model

import java.time.ZonedDateTime

case class VisitDate(formattedDate: String, startDateTime: ZonedDateTime) extends SerializableJsonObject
