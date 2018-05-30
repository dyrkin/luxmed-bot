/**
 * MIT License
 *
 * Copyright (c) 2018 Yevhen Zadyra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
