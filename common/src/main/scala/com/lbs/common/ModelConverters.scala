package com.lbs.common

import scala.collection.IterableOps
import scala.language.implicitConversions

trait ModelConverters {

  trait ObjectConverter[-In, +Out] {
    def convert(any: In): Out
  }

  implicit class ObjectOps[From](anyRef: From) {
    def mapTo[To](implicit converter: ObjectConverter[From, To]): To = converter.convert(anyRef)
  }

  implicit def sequenceConverters[From, To, Col[+X] <: IterableOps[X, Col, Col[X]]](implicit
    objectConverter: ObjectConverter[From, To]
  ): ObjectConverter[Col[From], Col[To]] =
    (col: Col[From]) => col.map(objectConverter.convert)

}
