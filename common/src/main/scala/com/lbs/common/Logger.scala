
package com.lbs.common

import org.slf4j
import org.slf4j.LoggerFactory

trait Logger {
  private val log: slf4j.Logger = LoggerFactory.getLogger(this.getClass)

  protected def debug(msg: => String): Unit = {
    if (log.isDebugEnabled)
      log.debug(msg)
  }

  protected def warn(msg: => String): Unit = {
    if (log.isWarnEnabled)
      log.warn(msg)
  }

  protected def warn(msg: => String, throwable: Throwable): Unit = {
    if (log.isWarnEnabled)
      log.warn(msg, throwable)
  }

  protected def error(msg: => String): Unit = {
    if (log.isErrorEnabled)
      log.error(msg)
  }

  protected def error(msg: => String, throwable: Throwable): Unit = {
    if (log.isErrorEnabled)
      log.error(msg, throwable)
  }

  protected def info(msg: => String): Unit = {
    if (log.isInfoEnabled)
      log.info(msg)
  }

  protected def trace(msg: => String): Unit = {
    if (log.isTraceEnabled)
      log.trace(msg)
  }

}
