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

import akka.actor.{ActorRef, PoisonPill, Props}
import com.lbs.bot.model.Command
import com.lbs.common.Logger
import com.lbs.server.actor.Chat._
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.conversation.Conversation
import com.lbs.server.actor.conversation.Conversation.{InitConversation, StartConversation}
import com.lbs.server.service.{DataService, MonitoringService}
import com.lbs.server.util.MessageExtractors._

import scala.util.matching.Regex

class Chat(val userId: UserId, dataService: DataService, monitoringService: MonitoringService, bookingActorFactory: ByUserIdActorFactory, helpActorFactory: ByUserIdActorFactory,
           monitoringsActorFactory: ByUserIdActorFactory, historyActorFactory: ByUserIdActorFactory,
           visitsActorFactory: ByUserIdActorFactory, settingsActorFactory: ByUserIdActorFactory,
           bugActorFactory: ByUserIdActorFactory, accountActorFactory: ByUserIdActorFactory) extends Conversation[Unit] with Logger {

  private val bookingActor = bookingActorFactory(userId)
  private val helpActor = helpActorFactory(userId)
  private val monitoringsActor = monitoringsActorFactory(userId)
  private val historyActor = historyActorFactory(userId)
  private val visitsActor = visitsActorFactory(userId)
  private val settingsActor = settingsActorFactory(userId)
  private val bugActor = bugActorFactory(userId)
  private val accountActor = accountActorFactory(userId)

  entryPoint(helpChat)

  private def helpChat: Step = actorDialogue(helpActor) {
    case Msg(cmd@Command(_, Text("/help"), _), _) =>
      helpActor ! cmd
      stay()
    case Msg(cmd@Command(_, Text("/start"), _), _) =>
      helpActor ! cmd
      stay()
  }

  private def bookChat: Step = actorDialogue(bookingActor) {
    case Msg(Command(_, Text("/book"), _), _) =>
      bookingActor ! InitConversation
      bookingActor ! StartConversation
      stay()
  }

  private def historyChat: Step = actorDialogue(historyActor) {
    case Msg(Command(_, Text("/history"), _), _) =>
      historyActor ! InitConversation
      historyActor ! StartConversation
      stay()
  }

  private def visitsChat: Step = actorDialogue(visitsActor) {
    case Msg(Command(_, Text("/reserved"), _), _) =>
      visitsActor ! InitConversation
      visitsActor ! StartConversation
      stay()
  }

  private def bugChat: Step = actorDialogue(bugActor) {
    case Msg(Command(_, Text("/bug"), _), _) =>
      bugActor ! InitConversation
      bugActor ! StartConversation
      stay()
  }

  private def monitoringsChat: Step = actorDialogue(monitoringsActor) {
    case Msg(Command(_, Text("/monitorings"), _), _) =>
      monitoringsActor ! InitConversation
      monitoringsActor ! StartConversation
      stay()
  }

  private def settingsChat: Step = actorDialogue(settingsActor) {
    case Msg(Command(_, Text("/settings"), _), _) =>
      settingsActor ! InitConversation
      settingsActor ! StartConversation
      stay()
  }

  private def accountChat: Step = actorDialogue(accountActor) {
    case Msg(Command(_, Text("/accounts"), _), _) =>
      accountActor ! InitConversation
      accountActor ! StartConversation
      stay()
  }

  private def actorDialogue(actor: ActorRef)(mainStateFunction: AnswerFn): Step =
    monologue {
      case event: Msg =>
        if (mainStateFunction.isDefinedAt(event)) mainStateFunction(event)
        else {
          val secondaryStateFunction = secondaryState(actor)
          secondaryStateFunction(event)
        }
    }

  private def secondaryState(actor: ActorRef): AnswerFn = {
    case Msg(cmd@Command(_, Text("/bug"), _), _) =>
      self ! cmd
      goto(bugChat)
    case Msg(cmd@Command(_, Text("/help"), _), _) =>
      self ! cmd
      goto(helpChat)
    case Msg(cmd@Command(_, Text("/start"), _), _) =>
      self ! cmd
      goto(helpChat)
    case Msg(cmd@Command(_, Text("/book"), _), _) =>
      self ! cmd
      goto(bookChat)
    case Msg(cmd@Command(_, Text("/monitorings"), _), _) =>
      self ! cmd
      goto(monitoringsChat)
    case Msg(cmd@Command(_, Text("/history"), _), _) =>
      self ! cmd
      goto(historyChat)
    case Msg(cmd@Command(_, Text("/reserved"), _), _) =>
      self ! cmd
      goto(visitsChat)
    case Msg(cmd@Command(_, Text("/settings"), _), _) =>
      self ! cmd
      goto(settingsChat)
    case Msg(cmd@Command(_, Text("/accounts"), _), _) =>
      self ! cmd
      goto(accountChat)
    case Msg(cmd@Command(_, Text(MonitoringId(monitoringIdStr, scheduleIdStr, timeStr)), _), _) =>
      val monitoringId = monitoringIdStr.toLong
      val scheduleId = scheduleIdStr.toLong
      val time = timeStr.toLong
      monitoringService.bookAppointmentByScheduleId(userId.accountId, monitoringId, scheduleId, time)
      stay()
    case Msg(cmd: Command, _) =>
      actor ! cmd
      stay()
  }

  override def postStop(): Unit = {
    bookingActor ! PoisonPill
    helpActor ! PoisonPill
    monitoringsActor ! PoisonPill
    historyActor ! PoisonPill
    visitsActor ! PoisonPill
    settingsActor ! PoisonPill
    bugActor ! PoisonPill
    accountActor ! PoisonPill
    super.postStop()
  }
}

object Chat {
  def props(userId: UserId, dataService: DataService, monitoringService: MonitoringService, bookingActorFactory: ByUserIdActorFactory, helpActorFactory: ByUserIdActorFactory,
            monitoringsActorFactory: ByUserIdActorFactory, historyActorFactory: ByUserIdActorFactory,
            visitsActorFactory: ByUserIdActorFactory, settingsActorFactory: ByUserIdActorFactory, bugActorFactory: ByUserIdActorFactory,
            accountActorFactory: ByUserIdActorFactory): Props =
    Props(new Chat(userId, dataService, monitoringService, bookingActorFactory, helpActorFactory, monitoringsActorFactory,
      historyActorFactory, visitsActorFactory, settingsActorFactory, bugActorFactory, accountActorFactory))

  val MonitoringId: Regex = s"/reserve_(\\d+)_(\\d+)_(\\d+)".r

}