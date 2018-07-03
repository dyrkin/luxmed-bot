package com.lbs.server.actor.conversation

import akka.actor.{ActorRef, Props}
import akka.testkit.TestProbe
import com.lbs.server.actor.AkkaTestKit
import com.lbs.server.actor.conversation.Conversation.{InitConversation, StartConversation}

class ConversationSpec extends AkkaTestKit {

  "Actor must send complete data" when {
    "conversation is done" in {

      case class Data(configured: Boolean = false, hello: String = null, world: String = null, people: String = null)

      object Hello

      object World

      object Dialogue

      class TestActor(originator: ActorRef) extends Conversation[Data] {

        private var conf: String = _

        def configure: EC =
          externalConfig {
            case Msg(confStr: String, data) =>
              conf = confStr
              goto(askHello) using data.copy(configured = true)
          }

        def askHello: QA =
          question { data =>
            self ! Hello
          } answer {
            case Msg(Hello, data) =>
              goto(askWorld) using data.copy(hello = "hello")
          }

        def askWorld: QA =
          question { data =>
            self ! World
          } answer {
            case Msg(World, data) =>
              goto(askDialogue) using data.copy(world = "world")
          }

        def askDialogue: QA =
          question { data =>
            self ! Dialogue
          } answer {
            case Msg(Dialogue, data) =>
              originator ! data.copy(people = "dialogue") -> conf
              end()
          }

        entryPoint(configure, Data())
      }

      val expected = Data(configured = true, "hello", "world", "dialogue") -> "myconf"

      val originator = TestProbe()
      val actor = system.actorOf(Props(new TestActor(originator.ref)))
      actor ! StartConversation
      actor ! expected._2
      originator.expectMsg(expected)

      //reinit
      actor ! InitConversation
      actor ! StartConversation
      actor ! expected._2
      originator.expectMsg(expected)
    }

    "configuration is done" in {

      case class Data(configured: Boolean = false, message1: String = null, message2: String = null)

      object InvokeEnrichMessage

      class TestActor(originator: ActorRef) extends Conversation[Data] {

        def configure1: IC =
          internalConfig { _ =>
            goto(configure2) using Data(configured = true)
          }

        def configure2: IC =
          internalConfig { data =>
            goto(askMessage2) using data.copy(message1 = "hello")
          }

        def askMessage2: QA =
          question { _ =>
            self ! InvokeEnrichMessage
          } answer {
            case Msg(InvokeEnrichMessage, data) =>
              originator ! data.copy(message2 = "world")
              end()
          }

        entryPoint(configure1, Data())
      }

      val expected = Data(configured = true, message1 = "hello", message2 = "world")

      val originator = TestProbe()
      val actor = system.actorOf(Props(new TestActor(originator.ref)))
      actor ! StartConversation
      originator.expectMsg(expected)

      //reinit
      actor ! InitConversation
      actor ! StartConversation
      originator.expectMsg(expected)
    }
  }

}
