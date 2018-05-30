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

import java.util.concurrent.{Executors, ScheduledFuture}

import scala.concurrent.duration.FiniteDuration

class Scheduler(poolSize: Int) extends Logger {
  private val scheduledThreadPool = Executors.newScheduledThreadPool(poolSize)

  def schedule(fn: => Unit, period: FiniteDuration): ScheduledFuture[_] = {
    scheduledThreadPool.scheduleAtFixedRate(() => fn, period.length, period.length, period.unit)
  }

  def schedule(fn: => Unit, delay: FiniteDuration, period: FiniteDuration): ScheduledFuture[_] = {
    require(delay.unit == period.unit, s"Delay units must be the same as for period ${period.unit}")
    scheduledThreadPool.scheduleAtFixedRate(silentFn(fn), delay.length, period.length, period.unit)
  }

  private def silentFn(fn: => Unit): Runnable = {
    () =>
      try {
        fn
      } catch {
        case ex: Exception =>
          LOG.error(s"Unable to execute scheduler task", ex)
      }
  }
}
