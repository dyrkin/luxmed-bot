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
package com.lbs.server.actor

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import com.lbs.bot.model.{Command, MessageSource}
import com.lbs.common.Logger
import com.lbs.server.actor.Chat.Init
import com.lbs.server.actor.Login.{LoggedIn, UserId}
import com.lbs.server.service.DataService
import com.lbs.server.util.MessageExtractors._

class Auth(val source: MessageSource, dataService: DataService, unauthorizedHelpActorFactory: ByMessageSourceActorFactory,
           loginActorFactory: ByMessageSourceWithOriginatorActorFactory, chatActorFactory: ByUserIdActorFactory) extends Actor with Logger {

  private val loginActor = loginActorFactory(source, self)
  private val unauthorizedHelpActor: ActorRef = unauthorizedHelpActorFactory(source)

  private var userId: Option[UserId] = getUserId
  private var chatActor: ActorRef = _

  override def receive: Receive = {
    case cmd@Command(_, Text("/help"), _) if userId.isEmpty =>
      unauthorizedHelpActor ! cmd
    case cmd@Command(_, Text("/start"), _) if userId.isEmpty =>
      unauthorizedHelpActor ! cmd
    case cmd@Command(_, Text("/login"), _) =>
      userId = None
      loginActor ! Init
      loginActor ! cmd
    case cmd: Command if userId.isEmpty =>
      loginActor ! cmd
    case cmd: Command if userId.nonEmpty =>
      chatActor = getChatActor(userId.get)
      chatActor ! cmd
    case LoggedIn(forwardCommand, uId, aId) =>
      val id = UserId(uId, aId, source)
      val cmd = forwardCommand.cmd
      userId = Some(id)
      chatActor = getChatActor(id, reInit = true)
      if (!cmd.message.text.contains("/login"))
        chatActor ! cmd
    case cmd: Command =>
      chatActor ! cmd
  }

  private def getChatActor(userId: UserId, reInit: Boolean = false): ActorRef = {
    if (chatActor == null) {
      chatActorFactory(userId)
    } else {
      if (reInit) {
        chatActor ! PoisonPill
        chatActorFactory(userId)
      } else chatActor
    }
  }

  def getUserId: Option[UserId] = {
    val userIdMaybe = dataService.findUserAndAccountIdBySource(source)
    userIdMaybe.map { case (uId, aId) => UserId(uId, aId, source) }
  }

  override def postStop(): Unit = {
    loginActor ! PoisonPill
    unauthorizedHelpActor ! PoisonPill
    if (chatActor != null) chatActor ! PoisonPill
  }
}

object Auth {
  def props(source: MessageSource, dataService: DataService, unauthorizedHelpActorFactory: ByMessageSourceActorFactory,
            loginActorFactory: ByMessageSourceWithOriginatorActorFactory, chatActorFactory: ByUserIdActorFactory): Props =
    Props(new Auth(source, dataService, unauthorizedHelpActorFactory, loginActorFactory, chatActorFactory))
}