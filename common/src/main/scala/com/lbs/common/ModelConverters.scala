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
