package com.lbs.server.conversation

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
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
