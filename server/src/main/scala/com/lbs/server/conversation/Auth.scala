/**
  * MIT License
  *
  * Copyright (c) 2018 Yevhen Zadyra
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.model.{Command, MessageSource}
import com.lbs.common.Logger
import com.lbs.server.conversation.Login.{LoggedIn, UserId}
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.service.DataService
import com.lbs.server.util.MessageExtractors._

class Auth(val source: MessageSource, dataService: DataService, unauthorizedHelpFactory: MessageSourceTo[UnauthorizedHelp],
           loginFactory: MessageSourceWithOriginatorTo[Login], chatFactory: UserIdTo[Chat])(val actorSystem: ActorSystem) extends Conversation[Unit] with Logger {

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