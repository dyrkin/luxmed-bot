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
class Bug extends RecordId {
  @BeanProperty
  @Column(name = "user_id", nullable = false)
  var userId: JLong = _

  @BeanProperty
  @Column(name = "source_system_id", nullable = false)
  var sourceSystemId: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var details: String = _

  @BeanProperty
  @Column(nullable = false)
  var resolved: Boolean = false

  @BeanProperty
  @Column(nullable = false)
  var submitted: ZonedDateTime = ZonedDateTime.now()
}

object Bug {
  def apply(userId: Long, sourceSystemId: Long, details: String, resolved: Boolean = false, submitted: ZonedDateTime = ZonedDateTime.now()): Bug = {
    val bug = new Bug
    bug.userId = userId
    bug.sourceSystemId = sourceSystemId
    bug.details = details
    bug.resolved = resolved
    bug.submitted = submitted
    bug
  }
}