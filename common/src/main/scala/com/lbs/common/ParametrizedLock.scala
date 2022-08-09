package com.lbs.common

import java.util.concurrent.ConcurrentHashMap

class ParametrizedLock[K] {
  private val locks = new ConcurrentHashMap[K, AnyRef]

  def obtainLock(key: K): AnyRef = locks.computeIfAbsent(key, k => new AnyRef)
}
