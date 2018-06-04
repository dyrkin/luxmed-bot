package com.lbs.server.actor

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import com.lbs.bot.model.MessageSource
import com.lbs.server.actor.Login.UserId

trait Mocks {

  protected implicit val system: ActorSystem

  protected val unauthorizedHelpActor = TestProbe()
  protected val loginActor = TestProbe()
  protected val chatActor = TestProbe()
  protected val unauthorizedHelpFactory: MessageSource => ActorRef = _ => unauthorizedHelpActor.ref
  protected val loginActorFactory: (MessageSource, ActorRef) => ActorRef = (_, _) => loginActor.ref
  protected val chatActorFactory: UserId => ActorRef = _ => chatActor.ref
}
