package com.lbs.api.json.model

/**
  * [
  *     {
  *         "id": 70,
  *         "name": "Białystok"
  *     },
  *     {
  *         "id": 12,
  *         "name": "Bielsk Podlaski"
  *     },
  *     {
  *         "id": 100,
  *         "name": "Bielsko-Biała"
  *     }
  * ]
  */
case class DictionaryCity(override val id: Long, override val name: String)
    extends Identified
    with SerializableJsonObject
