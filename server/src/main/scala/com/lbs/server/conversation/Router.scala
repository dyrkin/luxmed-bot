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

import akka.actor.{ActorSystem, Cancellable}
import com.lbs.bot.model.{Command, MessageSource}
import com.lbs.common.Logger
import com.lbs.server.conversation.Account.SwitchAccount
import com.lbs.server.conversation.base.Conversation

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationLong

class Router(authFactory: MessageSourceTo[Auth])(val actorSystem: ActorSystem) extends Conversation[Unit] with Logger {

  private case class DestroyChat(source: MessageSource)

  private val chats = mutable.Map.empty[MessageSource, Auth]

  private val timers = mutable.Map.empty[MessageSource, Cancellable]

  private val idleTimeout = 1.hour

  private implicit val dispatcher: ExecutionContextExecutor = actorSystem.dispatcher

  entryPoint(routeMessage)

  private def routeMessage: Step =
    monologue {
      case Msg(cmd@Command(source, _, _), _) =>
        val chat = instantiateChatOrGet(source)
        chat ! cmd
        stay()
      case Msg(DestroyChat(source), _) =>
        info(s"Destroying chat for $source due to $idleTimeout of inactivity")
        destroyChat(source)
        stay()
      case Msg(SwitchAccount(userId), _) =>
        switchAccount(userId)
        stay()
      case msg: Msg =>
        info(s"Unknown message received: $msg")
        stay()
    }

  private def instantiateChatOrGet(source: MessageSource) = {
    scheduleIdleChatDestroyer(source)
    chats.getOrElseUpdate(source, authFactory(source))
  }

  private def destroyChat(source: MessageSource): Unit = {
    timers.remove(source)
    removeChat(source)
  }

  private def switchAccount(userId: Login.UserId): Unit = {
    removeChat(userId.source)
    chats += userId.source -> authFactory(userId.source)
  }

  private def removeChat(source: MessageSource): Unit = {
    chats.remove(source).foreach(_.destroy())
  }

  private def scheduleIdleChatDestroyer(source: MessageSource): Unit = {
    timers.remove(source).foreach(_.cancel())
    val cancellable = actorSystem.scheduler.scheduleOnce(idleTimeout) {
      self ! DestroyChat(source)
    }
    timers += source -> cancellable
  }

  beforeDestroy {
    chats.foreach(chat => destroyChat(chat._1))
  }
}