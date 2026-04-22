package com.lbs.common

import java.util.Optional

object Implicits {
  given [T]: Conversion[Optional[T], Option[T]] =
    optional => Option(optional.orElse(null.asInstanceOf[T]))

  given [T]: Conversion[Option[T], Optional[T]] =
    option => Optional.of(option.getOrElse(null.asInstanceOf[T]))
}
