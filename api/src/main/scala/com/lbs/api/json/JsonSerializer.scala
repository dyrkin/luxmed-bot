package com.lbs.api.json

import com.lbs.api.json.model.*
import com.lbs.api.json.model.JsonCodecs.given
import com.typesafe.scalalogging.StrictLogging
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

object JsonSerializer extends StrictLogging {

  /** Convert snake_case / PascalCase JSON keys to camelCase before decoding. */
  private def normalizeKey(key: String): String = {
    val camelized = "_([a-zA-Z0-9])".r.replaceAllIn(key, m => m.group(1).toUpperCase)
    if (camelized.nonEmpty) camelized.head.toLower +: camelized.tail else camelized
  }

  private def transformKeys(json: Json): Json =
    json
      .mapObject { obj =>
        JsonObject.fromIterable(obj.toIterable.map { case (k, v) =>
          normalizeKey(k) -> transformKeys(v)
        })
      }
      .mapArray(_.map(transformKeys))

  def extract[T: Decoder](jsonString: String): T =
    parse(jsonString)
      .map(transformKeys)
      .flatMap(_.as[T]) match {
      case Right(value) => value
      case Left(err)    => throw new RuntimeException(s"JSON decode error: $err\nInput: $jsonString")
    }

  def write[T: Encoder](jsonObject: T): String = {
    val json = jsonObject.asJson.spaces2
    logger.info(json)
    json
  }

  object extensions {

    extension (jsonString: String)
      def as[T <: SerializableJsonObject: Decoder]: T = extract[T](jsonString)

      def asList[T <: SerializableJsonObject: Decoder]: List[T] =
        parse(jsonString)
          .map(transformKeys)
          .flatMap(_.as[List[T]]) match {
          case Right(value) => value
          case Left(err)    => throw new RuntimeException(s"JSON decode error: $err\nInput: $jsonString")
        }

    extension [T <: SerializableJsonObject: Encoder](jsonObject: T)
      def asJson: String = write(jsonObject)

  }
}
