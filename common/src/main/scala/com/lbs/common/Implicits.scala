package com.lbs.common

import java.util.Optional
import scala.language.implicitConversions

object Implicits {
  implicit def optionalToOption[T](optional: Optional[T]): Option[T] = {
    Option(optional.orElse(null.asInstanceOf[T]))
  }

  implicit def optionToOptional[T](option: Option[T]): Optional[T] = {
    Optional.of(option.getOrElse(null.asInstanceOf[T]))
  }
}
