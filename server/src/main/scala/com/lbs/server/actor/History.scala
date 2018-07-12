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
import com.lbs.api.json.model.HistoricVisit
import com.lbs.bot.Bot
import com.lbs.bot.model.Command
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.conversation.Conversation
import com.lbs.server.actor.conversation.Conversation.{InitConversation, StartConversation}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.service.ApiService

class History(val userId: UserId, bot: Bot, apiService: ApiService, val localization: Localization, historyPagerActorFactory: ByUserIdWithOriginatorActorFactory) extends Conversation[Unit] with Localizable {

  private val historyPager = historyPagerActorFactory(userId, self)

  entryPoint(prepareData)

  def prepareData: Step =
    process { _ =>
      val visits = apiService.visitsHistory(userId.accountId)
      historyPager ! InitConversation
      historyPager ! StartConversation
      historyPager ! visits
      goto(processResponseFromPager)
    }

  def processResponseFromPager: Step =
    monologue {
      case Msg(cmd: Command, _) =>
        historyPager ! cmd
        stay()
      case Msg(Pager.NoItemsFound, _) =>
        bot.sendMessage(userId.source, lang.visitsHistoryIsEmpty)
        end()
      case Msg(_: HistoricVisit, _) =>
        end()
    }

  override def postStop(): Unit = {
    historyPager ! PoisonPill
    super.postStop()
  }
}

object History {
  def props(userId: UserId, bot: Bot, apiService: ApiService, localization: Localization, historyPagerActorFactory: ByUserIdWithOriginatorActorFactory): Props =
    Props(new History(userId, bot, apiService, localization, historyPagerActorFactory))
}