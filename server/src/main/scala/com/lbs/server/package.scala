package com.lbs

package object server {
  type ThrowableOr[T] = Either[Throwable, T]
}
