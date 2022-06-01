
package com.lbs.api.json

import com.lbs.api.json.model.SerializableJsonObject
import com.lbs.common.Logger
import org.json4s._
import org.json4s.jackson.JsonMethods._

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, LocalTime, ZonedDateTime}


object JsonSerializer extends Logger {

  private val zonedDateTimeSerializer = new CustomSerializer[ZonedDateTime](_ => ( {
    case JString(str) => ZonedDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }, {
    case zonedDateTime: ZonedDateTime => JString(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  }
  ))

  private val localTimeSerializer = new CustomSerializer[LocalTime](_ => ( {
    case JString(str) => LocalTime.parse(str)
  }, {
    case localTime: LocalTime => JString(localTime.toString)
  }
  ))

  private val localDateTimeSerializer = new CustomSerializer[LocalDateTime](_ => ( {
    case JString(str) => LocalDateTime.parse(str)
  }, {
    case localTime: LocalDateTime => JString(localTime.toString)
  }
  ))

  private implicit val formats: Formats = DefaultFormats.withStrictArrayExtraction + zonedDateTimeSerializer + localTimeSerializer + localDateTimeSerializer

  def extract[T](jsonString: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    parse(jsonString).camelizeKeys.extract[T]
  }

  def write[T](jsonObject: T): String = {
    val json = pretty(render(Extraction.decompose(jsonObject)))
    info(json)
    json
  }

  object extensions {

    implicit class JsonStringToObject(jsonString: String) {
      def as[T <: SerializableJsonObject](implicit mf: scala.reflect.Manifest[T]): T = {
        extract[T](jsonString)
      }

      def asList[T <: SerializableJsonObject](implicit mf: scala.reflect.Manifest[T]): List[T] = {
        extract[List[T]](jsonString)
      }
    }

    implicit class JsonObjectToString[T <: SerializableJsonObject](jsonObject: T) {
      def asJson: String = {
        write(jsonObject)
      }
    }

  }

}
