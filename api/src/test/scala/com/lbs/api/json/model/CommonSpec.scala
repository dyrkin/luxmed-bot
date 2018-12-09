
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
