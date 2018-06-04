package com.lbs.server.actor

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.lbs.bot.model.{Command, Message, MessageSource, TelegramMessageSourceSystem}
import com.lbs.server.actor.Chat.Init
import com.lbs.server.actor.Login.{ForwardCommand, LoggedIn, UserId}
import com.lbs.server.service.DataService
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class AuthSpec extends TestKit(ActorSystem()) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An Auth actor " when {
    val source = MessageSource(TelegramMessageSourceSystem, "1")
    val userId = UserId(1L, source)

    "user is unauthorized" must {
      val unauthorizedHelpActor = TestProbe()
      val loginActor = TestProbe()
      val chatActor = TestProbe()
      val unauthorizedHelpFactory: MessageSource => ActorRef = _ => unauthorizedHelpActor.ref
      val loginActorFactory: (MessageSource, ActorRef) => ActorRef = (_, _) => loginActor.ref
      val chatActorFactory: UserId => ActorRef = _ => chatActor.ref
      val dataService = mock(classOf[DataService])
      val auth = system.actorOf(Auth.props(source, dataService, unauthorizedHelpFactory, loginActorFactory, chatActorFactory))

      when(dataService.findUserIdBySource(source)).thenReturn(None)

      "send english help on /start command" in {
        val cmd = Command(source, Message("1", Some("/start")))
        auth ! cmd
        unauthorizedHelpActor.expectMsg(cmd)
      }

      "send english help on /help command" in {
        val cmd = Command(source, Message("1", Some("/help")))
        auth ! cmd
        unauthorizedHelpActor.expectMsg(cmd)
      }

      "initialize dialogue with login actor on /login command" in {
        val cmd = Command(source, Message("1", Some("/login")))
        auth ! cmd
        loginActor.expectMsg(Init)
        loginActor.expectMsg(cmd)
      }

      "have a dialogue with login actor on any message" in {
        val cmd1 = Command(source, Message("1", Some("any1")))
        val cmd2 = Command(source, Message("2", Some("any2")))
        auth ! cmd1
        loginActor.expectMsg(cmd1)
        auth ! cmd2
        loginActor.expectMsg(cmd2)
      }

      "forward initial message to chat actor after the user has logged in" in {
        val cmd = Command(source, Message("1", Some("any")))
        val msg = LoggedIn(ForwardCommand(cmd), 1L)
        auth ! msg
        chatActor.expectMsg(cmd)
      }

      "forward all commands to chat actor" in {
        val cmd = Command(source, Message("1", Some("any")))
        auth ! cmd
        chatActor.expectMsg(cmd)
        unauthorizedHelpActor.expectNoMsg()
        loginActor.expectNoMsg()
      }
    }

    "user is authorized" must {
      val unauthorizedHelpActor = TestProbe()
      val loginActor = TestProbe()
      val chatActor = TestProbe()
      val unauthorizedHelpFactory: MessageSource => ActorRef = _ => unauthorizedHelpActor.ref
      val loginActorFactory: (MessageSource, ActorRef) => ActorRef = (_, _) => loginActor.ref
      val chatActorFactory: UserId => ActorRef = _ => chatActor.ref
      val dataService = mock(classOf[DataService])
      val auth = system.actorOf(Auth.props(source, dataService, unauthorizedHelpFactory, loginActorFactory, chatActorFactory))

      when(dataService.findUserIdBySource(source)).thenReturn(Some(userId.userId))

      "forward all commands to chat actor" in {
        val cmd = Command(source, Message("1", Some("any")))
        auth ! cmd
        chatActor.expectMsg(cmd)
        unauthorizedHelpActor.expectNoMsg()
        loginActor.expectNoMsg()
      }

      "initialize dialogue with login actor on /login command" in {
        val cmd = Command(source, Message("1", Some("/login")))
        auth ! cmd
        loginActor.expectMsg(Init)
        loginActor.expectMsg(cmd)
      }

      "have a dialogue with login actor on any message" in {
        val cmd1 = Command(source, Message("1", Some("any1")))
        val cmd2 = Command(source, Message("2", Some("any2")))
        auth ! cmd1
        loginActor.expectMsg(cmd1)
        auth ! cmd2
        loginActor.expectMsg(cmd2)
      }
    }
  }
}
