
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.model.{Command, MessageSource}
import com.lbs.server.conversation.Login.{LoggedIn, UserId}
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.service.DataService
import com.lbs.server.util.MessageExtractors._
import com.typesafe.scalalogging.StrictLogging

class Auth(val source: MessageSource, dataService: DataService, unauthorizedHelpFactory: MessageSourceTo[UnauthorizedHelp],
           loginFactory: MessageSourceWithOriginatorTo[Login], chatFactory: UserIdTo[Chat])(val actorSystem: ActorSystem) extends Conversation[Unit] with StrictLogging {

  private val login = loginFactory(source, self)
  private val unauthorizedHelp = unauthorizedHelpFactory(source)

  private var userId: Option[UserId] = getUserId
  private var chat: Chat = _

  entryPoint(processIncoming)

  private def processIncoming =
    monologue {
      case Msg(cmd@TextCommand("/help"), _) if userId.isEmpty =>
        unauthorizedHelp ! cmd
        stay()
      case Msg(cmd@TextCommand("/start"), _) if userId.isEmpty =>
        unauthorizedHelp ! cmd
        stay()
      case Msg(cmd@TextCommand("/login"), _) =>
        userId = None
        login.restart()
        login ! cmd
        stay()
      case Msg(cmd: Command, _) if userId.isEmpty =>
        login ! cmd
        stay()
      case Msg(cmd: Command, _) if userId.nonEmpty =>
        chat = getChat(userId.get)
        chat ! cmd
        stay()
      case Msg(LoggedIn(forwardCommand, uId, aId), _) =>
        val id = UserId(uId, aId, source)
        val cmd = forwardCommand.cmd
        userId = Some(id)
        chat = getChat(id, reInit = true)
        if (!cmd.message.text.contains("/login"))
          chat ! cmd
        stay()
      case Msg(cmd: Command, _) =>
        chat ! cmd
        stay()
    }

  private def getChat(userId: UserId, reInit: Boolean = false): Chat = {
    if (chat == null) {
      chatFactory(userId)
    } else {
      if (reInit) {
        chat.destroy()
        chatFactory(userId)
      } else chat
    }
  }

  def getUserId: Option[UserId] = {
    val userIdMaybe = dataService.findUserAndAccountIdBySource(source)
    userIdMaybe.map { case (uId, aId) => UserId(uId, aId, source) }
  }

  beforeDestroy {
    login.destroy()
    unauthorizedHelp.destroy()
    if (chat != null) chat.destroy()
  }
}