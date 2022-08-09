
package com.lbs.common

import scala.language.implicitConversions

trait ModelConverters {

  trait ObjectConverter[-In, +Out] {
    def convert(any: In): Out
  }

  implicit class ObjectOps[From](anyRef: From) {
    def mapTo[To](implicit converter: ObjectConverter[From, To]): To = converter.convert(anyRef)
  }

  implicit def sequenceConverters[From, To]
  (implicit objectConverter: ObjectConverter[From, To]): ObjectConverter[Iterable[From], Iterable[To]] = {
    col: Iterable[From] => col.map(objectConverter.convert)
  }

}
