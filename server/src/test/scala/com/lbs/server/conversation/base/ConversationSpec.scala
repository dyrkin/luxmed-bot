package com.lbs.server.conversation.base

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import com.lbs.server.conversation.AkkaTestKit

class ConversationSpec extends AkkaTestKit {

  "Actor must send complete data" when {
    "conversation is done" in {

      case class Data(configured: Boolean = false, hello: String = null, world: String = null, people: String = null)

      object Hello

      object World

      object Dialogue

      class TestConversation(originator: ActorRef)(implicit val actorSystem: ActorSystem) extends Conversation[Data] {

        private var conf: String = _

        def configure: Step =
          monologue { case Msg(confStr: String, data) =>
            conf = confStr
            goto(askHello) using data.copy(configured = true)
          }

        def askHello: Step =
          ask { data =>
            self ! Hello
          } onReply { case Msg(Hello, data) =>
            goto(askWorld) using data.copy(hello = "hello")
          }

        def askWorld: Step =
          ask { data =>
            self ! World
          } onReply { case Msg(World, data) =>
            goto(askDialogue) using data.copy(world = "world")
          }

        def askDialogue: Step =
          ask { data =>
            self ! Dialogue
          } onReply { case Msg(Dialogue, data) =>
            originator ! data.copy(people = "dialogue") -> conf
            end()
          }

        entryPoint(configure, Data())
      }

      val expected = Data(configured = true, "hello", "world", "dialogue") -> "myconf"

      val originator = TestProbe()
      val testConversation = new TestConversation(originator.ref)(system)
      testConversation.start()
      testConversation ! expected._2
      originator.expectMsg(expected)

      // reinit
      testConversation.restart()
      testConversation ! expected._2
      originator.expectMsg(expected)
    }

    "configuration is done" in {

      case class Data(configured: Boolean = false, message1: String = null, message2: String = null)

      object InvokeEnrichMessage

      class TestConversation(originator: ActorRef)(implicit val actorSystem: ActorSystem) extends Conversation[Data] {

        def configure1: Step =
          process { _ =>
            goto(configure2) using Data(configured = true)
          }

        def configure2: Step =
          process { data =>
            goto(askMessage2) using data.copy(message1 = "hello")
          }

        def askMessage2: Step =
          ask { _ =>
            self ! InvokeEnrichMessage
          } onReply { case Msg(InvokeEnrichMessage, data) =>
            originator ! data.copy(message2 = "world")
            end()
          }

        entryPoint(configure1, Data())
      }

      val expected = Data(configured = true, message1 = "hello", message2 = "world")

      val originator = TestProbe()
      val actor = new TestConversation(originator.ref)(system)
      actor.start()
      originator.expectMsg(expected)

      // reinit
      actor.restart()
      originator.expectMsg(expected)
    }
  }

}
