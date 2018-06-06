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
package com.lbs.server.repository.model

import java.time.ZonedDateTime

import javax.persistence.{Access, AccessType, Column, Entity}

import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class CityHistory extends History with RecordId {
  @BeanProperty
  @Column(nullable = false)
  var id: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var name: String = _

  @BeanProperty
  @Column(name = "user_id", nullable = false)
  var userId: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var time: ZonedDateTime = _
}

object CityHistory {
  def apply(userId: Long, id: Long, name: String, time: ZonedDateTime): CityHistory = {
    val city = new CityHistory
    city.userId = userId
    city.id = id
    city.name = name
    city.time = time
    city
  }
}