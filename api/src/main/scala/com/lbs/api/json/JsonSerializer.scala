
package com.lbs.api.json

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.lbs.api.json.model.SerializableJsonObject
import org.json4s._
import org.json4s.jackson.JsonMethods._


object JsonSerializer {

  private val localDateTimeSerializer = new CustomSerializer[ZonedDateTime](_ => ( {
    case JString(str) => ZonedDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }, {
    case zonedDateTime: ZonedDateTime => JString(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  }
  ))

  private implicit val formats: Formats = DefaultFormats.withStrictArrayExtraction + localDateTimeSerializer

  def extract[T <: SerializableJsonObject](jsonString: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    parse(jsonString).camelizeKeys.extract[T]
  }

  def write[T <: SerializableJsonObject](jsonObject: T): String = {
    pretty(render(Extraction.decompose(jsonObject).pascalizeKeys))
  }

  object extensions {

    implicit class JsonStringToObject(jsonString: String) {
      def as[T <: SerializableJsonObject](implicit mf: scala.reflect.Manifest[T]): T = {
        extract[T](jsonString)
      }
    }

    implicit class JsonObjectToString[T <: SerializableJsonObject](jsonObject: T) {
      def asJson: String = {
        write(jsonObject)
      }
    }

  }

}
