package com.lbs.server.conversation

import com.lbs.bot.model.{Command, Message, MessageSource, TelegramMessageSourceSystem}
import com.lbs.server.conversation.Login.{ForwardCommand, LoggedIn, UserId}
import com.lbs.server.conversation.base.ConversationTestProbe
import com.lbs.server.service.DataService
import org.mockito.Mockito._

class AuthSpec extends AkkaTestKit {

  "An Auth actor " when {

    val source = MessageSource(TelegramMessageSourceSystem, "1")
    val userId = UserId(1L, "", 1L, source)

    "user is unauthorized" must {
      val unauthorizedHelpActor = ConversationTestProbe[UnauthorizedHelp]()
      val loginActor = ConversationTestProbe[Login]()
      val chatActor = ConversationTestProbe[Chat]()
      val unauthorizedHelpFactory: MessageSourceTo[UnauthorizedHelp] = _ => unauthorizedHelpActor.conversation
      val loginActorFactory: MessageSourceWithOriginatorTo[Login] = (_, _) => loginActor.conversation
      val chatActorFactory: UserIdTo[Chat] = _ => chatActor.conversation
      val dataService = mock(classOf[DataService])
      when(dataService.findUserAndAccountIdBySource(source)).thenReturn(None)
      val auth = new Auth(source, dataService, unauthorizedHelpFactory, loginActorFactory, chatActorFactory)(system)

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
        val msg = LoggedIn(ForwardCommand(cmd), 1L, "", 1L)
        auth ! msg
        chatActor.expectMsg(cmd)
      }

      "forward all commands to chat actor" in {
        val cmd = Command(source, Message("1", Some("any")))
        auth ! cmd
        chatActor.expectMsg(cmd)
        unauthorizedHelpActor.expectNoMessage()
        loginActor.expectNoMessage()
      }
    }

    "user is authorized" must {
      val unauthorizedHelpActor = ConversationTestProbe[UnauthorizedHelp]()
      val loginActor = ConversationTestProbe[Login]()
      val chatActor = ConversationTestProbe[Chat]()
      val unauthorizedHelpFactory: MessageSourceTo[UnauthorizedHelp] = _ => unauthorizedHelpActor.conversation
      val loginActorFactory: MessageSourceWithOriginatorTo[Login] = (_, _) => loginActor.conversation
      val chatActorFactory: UserIdTo[Chat] = _ => chatActor.conversation
      val dataService = mock(classOf[DataService])
      when(dataService.findUserAndAccountIdBySource(source)).thenReturn(Some(userId.userId, "", userId.accountId))

      val auth = new Auth(source, dataService, unauthorizedHelpFactory, loginActorFactory, chatActorFactory)(system)

      "forward all commands to chat actor" in {
        val cmd = Command(source, Message("1", Some("any")))
        auth ! cmd
        chatActor.expectMsg(cmd)
        unauthorizedHelpActor.expectNoMessage()
        loginActor.expectNoMessage()
      }

      "initialize dialogue with login actor on /login command" in {
        val cmd = Command(source, Message("1", Some("/login")))
        auth ! cmd
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
