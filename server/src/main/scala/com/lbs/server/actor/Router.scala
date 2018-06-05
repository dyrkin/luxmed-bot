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

import akka.actor.{Actor, ActorRef, Cancellable, PoisonPill, Props}
import com.lbs.bot.model.{Command, MessageSource}
import com.lbs.common.Logger
import com.lbs.server.actor.Router.DestroyChat

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationLong

class Router(authActorFactory: MessageSource => ActorRef) extends Actor with Logger {

  private val chats = mutable.Map.empty[MessageSource, ActorRef]

  private val timers = mutable.Map.empty[MessageSource, Cancellable]

  private val idleTimeout = 1.hour

  private implicit val dispatcher: ExecutionContextExecutor = context.system.dispatcher

  override def receive: Receive = {
    case cmd@Command(source, _, _) =>
      scheduleIdleChatDestroyer(source)
      val chat = chats.get(source) match {
        case Some(actor) => actor
        case None =>
          val actor = authActorFactory(source)
          chats += source -> actor
          actor
      }
      chat ! cmd
    case DestroyChat(source) =>
      destroyChat(source)
    case what => LOG.info(s"Unknown message: $what")
  }

  private def destroyChat(source: MessageSource): Unit = {
    LOG.info(s"Destroying chat for $source due to $idleTimeout inactivity")
    timers.remove(source)
    chats.remove(source).foreach(_ ! PoisonPill)
  }

  private def scheduleIdleChatDestroyer(source: MessageSource): Unit = {
    timers.remove(source).foreach(_.cancel())
    val cancellable = context.system.scheduler.scheduleOnce(idleTimeout) {
      self ! DestroyChat(source)
    }
    timers += source -> cancellable
  }

  override def postStop(): Unit = {
    chats.foreach(_._2 ! PoisonPill)
  }
}

object Router {
  def props(authActorFactory: MessageSource => ActorRef) = Props(new Router(authActorFactory))

  case class DestroyChat(source: MessageSource)

}
