package com.lbs.api.json.model

import scala.collection.Seq

/**
  * [
  *     {
  *         "id": 10,
  *         "name": "Polish"
  *     },
  *     {
  *         "id": 11,
  *         "name": "English"
  *     }
  * ]
  */
case class VisitLanguage(override val id: Long, override val name: String)
    extends Identified
    with SerializableJsonObject
