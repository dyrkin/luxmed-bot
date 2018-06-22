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
import com.lbs.server.service.{DataService, MonitoringService}
import com.lbs.server.util.MessageExtractors._

import scala.util.matching.Regex

class Chat(val userId: UserId, dataService: DataService, monitoringService: MonitoringService, bookingActorFactory: ByUserIdActorFactory, helpActorFactory: ByUserIdActorFactory,
           monitoringsActorFactory: ByUserIdActorFactory, historyActorFactory: ByUserIdActorFactory,
           visitsActorFactory: ByUserIdActorFactory, settingsActorFactory: ByUserIdActorFactory,
           bugActorFactory: ByUserIdActorFactory, accountActorFactory: ByUserIdActorFactory) extends SafeFSM[FSMState, FSMData] with Logger {

  private val bookingActor = bookingActorFactory(userId)
  private val helpActor = helpActorFactory(userId)
  private val monitoringsActor = monitoringsActorFactory(userId)
  private val historyActor = historyActorFactory(userId)
  private val visitsActor = visitsActorFactory(userId)
  private val settingsActor = settingsActorFactory(userId)
  private val bugActor = bugActorFactory(userId)
  private val accountActor = accountActorFactory(userId)

  startWith(HelpChat, null)

  when(HelpChat, helpActor) {
    case Event(cmd@Command(_, Text("/help"), _), _) =>
      helpActor ! cmd
      stay()
    case Event(cmd@Command(_, Text("/start"), _), _) =>
      helpActor ! cmd
      stay()
  }

  when(BookChat, bookingActor) {
    case Event(Command(_, Text("/book"), _), _) =>
      bookingActor ! Init
      stay()
  }

  when(HistoryChat, historyActor) {
    case Event(Command(_, Text("/history"), _), _) =>
      historyActor ! Init
      stay()
  }

  when(VisitsChat, visitsActor) {
    case Event(Command(_, Text("/reserved"), _), _) =>
      visitsActor ! Init
      stay()
  }

  when(BugChat, bugActor) {
    case Event(Command(_, Text("/bug"), _), _) =>
      bugActor ! Init
      goto(BugChat)
  }

  when(MonitoringsChat, monitoringsActor) {
    case Event(Command(_, Text("/monitorings"), _), _) =>
      monitoringsActor ! Init
      stay()
  }

  when(SettingsChat, settingsActor) {
    case Event(Command(_, Text("/settings"), _), _) =>
      settingsActor ! Init
      stay()
  }

  when(AccountChat, accountActor) {
    case Event(Command(_, Text("/accounts"), _), _) =>
      accountActor ! Init
      stay()
  }

  private def when(state: FSMState, actor: ActorRef)(mainStateFunction: StateFunction): Unit = {
    whenSafe(state) {
      case event: Event =>
        if (mainStateFunction.isDefinedAt(event)) mainStateFunction(event)
        else {
          val secondaryStateFunction = secondaryState(actor)
          if (secondaryStateFunction.isDefinedAt(event)) secondaryStateFunction(event)
          else eventHandler(event)
        }
    }
  }

  private def secondaryState(actor: ActorRef): StateFunction = {
    case Event(cmd@Command(_, Text("/bug"), _), _) =>
      self ! cmd
      goto(BugChat)
    case Event(cmd@Command(_, Text("/help"), _), _) =>
      self ! cmd
      goto(HelpChat)
    case Event(cmd@Command(_, Text("/start"), _), _) =>
      self ! cmd
      goto(HelpChat)
    case Event(cmd@Command(_, Text("/book"), _), _) =>
      self ! cmd
      goto(BookChat)
    case Event(cmd@Command(_, Text("/monitorings"), _), _) =>
      self ! cmd
      goto(MonitoringsChat)
    case Event(cmd@Command(_, Text("/history"), _), _) =>
      self ! cmd
      goto(HistoryChat)
    case Event(cmd@Command(_, Text("/reserved"), _), _) =>
      self ! cmd
      goto(VisitsChat)
    case Event(cmd@Command(_, Text("/settings"), _), _) =>
      self ! cmd
      goto(SettingsChat)
    case Event(cmd@Command(_, Text("/accounts"), _), _) =>
      self ! cmd
      goto(AccountChat)
    case Event(cmd@Command(_, Text(MonitoringId(monitoringIdStr, scheduleIdStr, timeStr)), _), _) =>
      val monitoringId = monitoringIdStr.toLong
      val scheduleId = scheduleIdStr.toLong
      val time = timeStr.toLong
      monitoringService.bookAppointmentByScheduleId(userId.accountId, monitoringId, scheduleId, time)
      stay()
    case Event(cmd: Command, _) =>
      actor ! cmd
      stay()
  }

  whenUnhandledSafe {
    case e: Event =>
      debug(s"Unhandled event in state:$stateName. Event: $e")
      stay()
  }

  initialize()

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

  object HelpChat extends FSMState

  object BookChat extends FSMState

  object MonitoringsChat extends FSMState

  object HistoryChat extends FSMState

  object VisitsChat extends FSMState

  object SettingsChat extends FSMState

  object BugChat extends FSMState

  object AccountChat extends FSMState

  object Init

  val MonitoringId: Regex = s"/reserve_(\\d+)_(\\d+)_(\\d+)".r

}