
package com.lbs.common

import scala.collection.TraversableLike
import scala.collection.generic.CanBuildFrom
import scala.language.{higherKinds, implicitConversions}

trait ModelConverters {

  trait ObjectConverter[-In, +Out] {
    def convert(any: In): Out
  }

  implicit class ObjectOps[From](anyRef: From) {
    def mapTo[To](implicit converter: ObjectConverter[From, To]): To = converter.convert(anyRef)
  }

  implicit def sequenceConverters[From, To, Col[+X] <: TraversableLike[X, Col[X]]]
  (implicit objectConverter: ObjectConverter[From, To], bf: CanBuildFrom[Col[From], To, Col[To]]): ObjectConverter[Col[From], Col[To]] = {
    col: Col[From] => col.map(objectConverter.convert)
  }

}
