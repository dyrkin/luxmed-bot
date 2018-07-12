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

import akka.actor.{PoisonPill, Props}
import com.lbs.bot._
import com.lbs.bot.model.{Button, Command}
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.Monitorings.Tags
import com.lbs.server.actor.conversation.Conversation
import com.lbs.server.actor.conversation.Conversation.{InitConversation, StartConversation}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.service.MonitoringService

class Monitorings(val userId: UserId, bot: Bot, monitoringService: MonitoringService, val localization: Localization, monitoringsPagerActorFactory: ByUserIdWithOriginatorActorFactory) extends Conversation[Monitoring] with Localizable {

  private val monitoringsPager = monitoringsPagerActorFactory(userId, self)

  entryPoint(prepareData)

  def prepareData: Step =
    process { _ =>
      val monitorings = monitoringService.getActiveMonitorings(userId.accountId)
      monitoringsPager ! InitConversation
      monitoringsPager ! StartConversation
      monitoringsPager ! Right[Throwable, Seq[Monitoring]](monitorings)
      goto(processResponseFromPager)
    }

  def processResponseFromPager: Step =
    monologue {
      case Msg(cmd: Command, _) =>
        monitoringsPager ! cmd
        stay()
      case Msg(Pager.NoItemsFound, _) =>
        bot.sendMessage(userId.source, lang.noActiveMonitorings)
        end()
      case Msg(monitoring: Monitoring, _) =>
        goto(askToDeactivateMonitoring) using monitoring
    }

  def askToDeactivateMonitoring: Step =
    ask { monitoring =>
      bot.sendMessage(userId.source, lang.deactivateMonitoring(monitoring), inlineKeyboard =
        createInlineKeyboard(Seq(Button(lang.no, Tags.No), Button(lang.yes, Tags.Yes))))
    } onReply {
      case Msg(Command(_, _, Some(Tags.No)), _) =>
        bot.sendMessage(userId.source, lang.monitoringWasNotDeactivated)
        end()
      case Msg(Command(_, _, Some(Tags.Yes)), monitoring: Monitoring) =>
        monitoringService.deactivateMonitoring(monitoring.recordId)
        bot.sendMessage(userId.source, lang.deactivated)
        end()
    }

  override def postStop(): Unit = {
    monitoringsPager ! PoisonPill
    super.postStop()
  }
}

object Monitorings {
  def props(userId: UserId, bot: Bot, monitoringService: MonitoringService, localization: Localization, monitoringsPagerActorFactory: ByUserIdWithOriginatorActorFactory): Props =
    Props(new Monitorings(userId, bot, monitoringService, localization, monitoringsPagerActorFactory))

  object Tags {
    val Yes = "yes"
    val No = "no"
  }

}