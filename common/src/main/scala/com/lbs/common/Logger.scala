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

import org.slf4j
import org.slf4j.LoggerFactory

trait Logger {
  private val log: slf4j.Logger = LoggerFactory.getLogger(this.getClass)

  protected val LOG = new LoggerWrapper

  class LoggerWrapper {
    def debug(msg: => String): Unit = {
      if (log.isDebugEnabled)
        log.debug(msg)
    }

    def warn(msg: => String): Unit = {
      if (log.isWarnEnabled)
        log.warn(msg)
    }

    def warn(msg: => String, throwable: Throwable): Unit = {
      if (log.isWarnEnabled)
        log.warn(msg, throwable)
    }

    def error(msg: => String): Unit = {
      if (log.isErrorEnabled)
        log.error(msg)
    }

    def error(msg: => String, throwable: Throwable): Unit = {
      if (log.isErrorEnabled)
        log.error(msg, throwable)
    }

    def info(msg: => String): Unit = {
      if (log.isInfoEnabled)
        log.info(msg)
    }
  }

}
