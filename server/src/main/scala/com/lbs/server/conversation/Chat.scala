
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.model.Command
import com.lbs.common.Logger
import com.lbs.server.conversation.Chat._
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.base.{Conversation, Interactional}
import com.lbs.server.service.{DataService, MonitoringService}
import com.lbs.server.util.MessageExtractors._

import scala.util.matching.Regex

class Chat(val userId: UserId, dataService: DataService, monitoringService: MonitoringService, bookingFactory: UserIdTo[Book],
           helpFactory: UserIdTo[Help], monitoringsFactory: UserIdTo[Monitorings], monitoringsHistoryFactory: UserIdTo[MonitoringsHistory], historyFactory: UserIdTo[History],
           visitsFactory: UserIdTo[Visits], settingsFactory: UserIdTo[Settings],
           bugFactory: UserIdTo[Bug], accountFactory: UserIdTo[Account])(val actorSystem: ActorSystem) extends Conversation[Unit] with Logger {

  private val book = bookingFactory(userId)
  private val help = helpFactory(userId)
  private val monitorings = monitoringsFactory(userId)
  private val monitoringsHistory = monitoringsHistoryFactory(userId)
  private val history = historyFactory(userId)
  private val visits = visitsFactory(userId)
  private val settings = settingsFactory(userId)
  private val bug = bugFactory(userId)
  private val account = accountFactory(userId)

  entryPoint(helpChat)

  private def helpChat: Step =
    dialogue(help) {
      case Msg(cmd@TextCommand("/help"), _) =>
        help ! cmd
        stay()
      case Msg(cmd@TextCommand("/start"), _) =>
        help ! cmd
        stay()
    }

  private def bookChat: Step =
    dialogue(book) {
      case Msg(TextCommand("/book"), _) =>
        book.restart()
        stay()
    }

  private def historyChat: Step =
    dialogue(history) {
      case Msg(TextCommand("/history"), _) =>
        history.restart()
        stay()
    }

  private def visitsChat: Step =
    dialogue(visits) {
      case Msg(TextCommand("/reserved"), _) =>
        visits.restart()
        stay()
    }

  private def bugChat: Step =
    dialogue(bug) {
      case Msg(TextCommand("/bug"), _) =>
        bug.restart()
        stay()
    }

  private def monitoringsChat: Step =
    dialogue(monitorings) {
      case Msg(TextCommand("/monitorings"), _) =>
        monitorings.restart()
        stay()
    }

  private def monitoringsHistoryChat: Step =
    dialogue(monitoringsHistory) {
      case Msg(TextCommand("/monitorings_history"), _) =>
        monitoringsHistory.restart()
        stay()
    }

  private def settingsChat: Step =
    dialogue(settings) {
      case Msg(TextCommand("/settings"), _) =>
        settings.restart()
        stay()
    }

  private def accountChat: Step =
    dialogue(account) {
      case Msg(TextCommand("/accounts"), _) =>
        account.restart()
        stay()
    }

  private def dialogue(interactional: Interactional)(mainMessageProcessor: MessageProcessorFn): Step =
    monologue {
      case event: Msg =>
        if (mainMessageProcessor.isDefinedAt(event)) mainMessageProcessor(event)
        else {
          val defaultMessageProcessor = secondaryState(interactional)
          defaultMessageProcessor(event)
        }
    }

  private def secondaryState(interactional: Interactional): MessageProcessorFn = {
    case Msg(cmd@TextCommand("/bug"), _) =>
      this ! cmd
      goto(bugChat)
    case Msg(cmd@TextCommand("/help"), _) =>
      self ! cmd
      goto(helpChat)
    case Msg(cmd@TextCommand("/start"), _) =>
      self ! cmd
      goto(helpChat)
    case Msg(cmd@TextCommand("/book"), _) =>
      self ! cmd
      goto(bookChat)
    case Msg(cmd@TextCommand("/monitorings"), _) =>
      self ! cmd
      goto(monitoringsChat)
    case Msg(cmd@TextCommand("/monitorings_history"), _) =>
      self ! cmd
      goto(monitoringsHistoryChat)
    case Msg(cmd@TextCommand("/history"), _) =>
      self ! cmd
      goto(historyChat)
    case Msg(cmd@TextCommand("/reserved"), _) =>
      self ! cmd
      goto(visitsChat)
    case Msg(cmd@TextCommand("/settings"), _) =>
      self ! cmd
      goto(settingsChat)
    case Msg(cmd@TextCommand("/accounts"), _) =>
      self ! cmd
      goto(accountChat)
    case Msg(TextCommand(ReserveTerm(monitoringIdStr, scheduleIdStr, timeStr)), _) =>
      val monitoringId = monitoringIdStr.toLong
      val scheduleId = scheduleIdStr.toLong
      val time = timeStr.toLong
      monitoringService.bookAppointmentByScheduleId(userId.accountId, monitoringId, scheduleId, time)
      stay()
    case Msg(cmd: Command, _) =>
      interactional ! cmd
      stay()
  }

  beforeDestroy {
    book.destroy()
    help.destroy()
    monitorings.destroy()
    monitoringsHistory.destroy()
    history.destroy()
    visits.destroy()
    settings.destroy()
    bug.destroy()
    account.destroy()
  }
}

object Chat {
  val ReserveTerm: Regex = s"/reserve_(\\d+)_(\\d+)_(\\d+)".r
}