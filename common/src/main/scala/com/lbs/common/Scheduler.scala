package com.lbs.common

import com.typesafe.scalalogging.LazyLogging

import java.util.concurrent.{Executors, ScheduledFuture}
import scala.concurrent.duration.FiniteDuration

class Scheduler(poolSize: Int) extends LazyLogging {
  private val scheduledThreadPool = Executors.newScheduledThreadPool(poolSize)

  def schedule(fn: => Unit, period: FiniteDuration): ScheduledFuture[_] = {
    scheduledThreadPool.scheduleAtFixedRate(() => fn, period.length, period.length, period.unit)
  }

  def schedule(fn: => Unit, delay: FiniteDuration, period: FiniteDuration): ScheduledFuture[_] = {
    require(delay.unit == period.unit, s"Delay units must be the same as for period ${period.unit}")
    scheduledThreadPool.scheduleAtFixedRate(silentFn(fn), delay.length, period.length, period.unit)
  }

  private def silentFn(fn: => Unit): Runnable = { () =>
    try {
      fn
    } catch {
      case ex: Exception =>
        logger.error(s"Unable to execute scheduler task", ex)
    }
  }
}
