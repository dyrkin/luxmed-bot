package com.lbs.server.conversation.base

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar

import scala.reflect.ClassTag

object ConversationTestProbe extends MockitoSugar {

  class ConversationTestProbe[T <: Interactional](actorSystem: ActorSystem, conversationMock: T) extends TestProbe(actorSystem) {
    when(conversationMock.ref).thenReturn(ref)
    when(conversationMock.!(any())(any())).thenCallRealMethod()
    when(conversationMock.start()).thenCallRealMethod()
    when(conversationMock.restart()).thenCallRealMethod()
    when(conversationMock.destroy()).thenCallRealMethod()
    when(conversationMock.continue()).thenCallRealMethod()
    when(conversationMock.init()).thenCallRealMethod()
    when(conversationMock.InitConversation).thenCallRealMethod()
    when(conversationMock.StartConversation).thenCallRealMethod()
    when(conversationMock.ContinueConversation).thenCallRealMethod()

    ignoreMsg {
      case conversationMock.InitConversation => true
      case conversationMock.StartConversation => true
      case conversationMock.ContinueConversation => true
    }

    def conversation: T = conversationMock
  }

  def apply[T <: Interactional]()(implicit classTag: ClassTag[T], system: ActorSystem): ConversationTestProbe[T] = {
    val conversationMock = mock[T]
    new ConversationTestProbe(system, conversationMock)
  }
}
