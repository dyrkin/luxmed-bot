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
import com.lbs.api.json.model.HistoricVisit
import com.lbs.bot.Bot
import com.lbs.bot.model.Command
import com.lbs.server.actor.Chat.Init
import com.lbs.server.actor.History.{AwaitPage, RequestData}
import com.lbs.server.actor.Login.UserId
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.service.ApiService

class History(val userId: UserId, bot: Bot, apiService: ApiService, val localization: Localization, historyPagerActorFactory: (UserId, ActorRef) => ActorRef) extends SafeFSM[FSMState, FSMData] with Localizable {

  private val historyPager = historyPagerActorFactory(userId, self)

  startWith(RequestData, null)

  whenSafe(RequestData) {
    case Event(Next, _) =>
      val visits = apiService.visitsHistory(userId.userId)
      historyPager ! visits
      goto(AwaitPage)
  }

  whenSafe(AwaitPage) {
    case Event(cmd: Command, _) =>
      historyPager ! cmd
      stay()
    case Event(Pager.NoItemsFound, _) =>
      bot.sendMessage(userId.source, lang.visitsHistoryIsEmpty)
      goto(RequestData)
    case Event(_: HistoricVisit, _) =>
      goto(RequestData) using null
  }

  whenUnhandledSafe {
    case Event(Init, _) =>
      invokeNext()
      historyPager ! Init
      goto(RequestData)
  }

  initialize()

  override def postStop(): Unit = {
    historyPager ! PoisonPill
    super.postStop()
  }
}

object History {
  def props(userId: UserId, bot: Bot, apiService: ApiService, localization: Localization, historyPagerActorFactory: (UserId, ActorRef) => ActorRef): Props =
    Props(new History(userId, bot, apiService, localization, historyPagerActorFactory))

  object RequestData extends FSMState

  object AwaitPage extends FSMState


}













