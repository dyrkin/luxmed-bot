
package com.lbs

import cats.MonadError

import scala.language.higherKinds

package object api {

  type ThrowableMonad[F[_]] = MonadError[F, Throwable]

}
