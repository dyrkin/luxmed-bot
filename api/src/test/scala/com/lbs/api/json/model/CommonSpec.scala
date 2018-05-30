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
package com.lbs.api.json.model

import org.scalatest.Matchers

trait CommonSpec {
  _: Matchers =>

  private type SimpleEntity = {val id: Long; val name: String}

  protected def testSimpleEntity(simpleEntity: SimpleEntity, expectedId: Long, expectedName: String): Unit = {
    simpleEntity.id should be(expectedId)
    simpleEntity.name should be(expectedName)
  }

  protected def testSimpleEntities(simpleEntities: List[SimpleEntity], expectedSize: Int, expectedId: Long, expectedName: String): Unit = {
    simpleEntities.size should be(expectedSize)
    val simpleEntity = simpleEntities.head
    testSimpleEntity(simpleEntity, expectedId, expectedName)
  }
}
