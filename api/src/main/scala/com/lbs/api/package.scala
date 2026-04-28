package com.lbs.api

import cats.MonadError

type ThrowableMonad[F[_]] = MonadError[F, Throwable]
