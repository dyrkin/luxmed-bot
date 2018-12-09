
package com.lbs.common

import scala.collection.generic.CanBuildFrom
import scala.language.{higherKinds, implicitConversions}

trait ModelConverters {

  trait CollectionConverter[-In, Out] {
    def convert[Z <: In, Col[X] <: Iterable[X]](col: Col[Z])(implicit bf: CanBuildFrom[Col[Z], Out, Col[Out]]): Col[Out]
  }

  trait ObjectConverter[-In, Out] {
    def convert[Z <: In](any: Z): Out
  }

  implicit class CollectionOps[From, Col[X] <: Iterable[X]](col: Col[From]) {
    def mapTo[To](implicit converter: CollectionConverter[From, To], bf: CanBuildFrom[Col[From], To, Col[To]]): Col[To] = converter.convert(col)
  }

  implicit class ObjectOps[From](anyRef: From) {
    def mapTo[To](implicit converter: ObjectConverter[From, To]): To = converter.convert(anyRef)
  }

}
