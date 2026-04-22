package com.lbs.common

import scala.collection.IterableOps

trait ModelConverters {

  trait ObjectConverter[-In, +Out] {
    def convert(any: In): Out
  }

  extension [From](anyRef: From)
    def mapTo[To](using converter: ObjectConverter[From, To]): To = converter.convert(anyRef)

  given sequenceConverters[From, To, Col[+X] <: IterableOps[X, Col, Col[X]]](using
    objectConverter: ObjectConverter[From, To]
  ): ObjectConverter[Col[From], Col[To]] with
    def convert(col: Col[From]): Col[To] = col.map(objectConverter.convert)

}
