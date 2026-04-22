package com.lbs.server.conversation

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

abstract class AkkaTestKit
    extends TestKit(ActorSystem())
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
