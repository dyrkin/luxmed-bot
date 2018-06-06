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
import com.lbs.api.json.model.ReservedVisit
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.server.actor.Chat.Init
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.Visits.{AwaitDecision, AwaitPage, RequestData, Tags}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.service.ApiService

class Visits(val userId: UserId, bot: Bot, apiService: ApiService, val localization: Localization,
             visitsPagerActorFactory: ByUserIdWithOriginatorActorFactory) extends SafeFSM[FSMState, ReservedVisit] with Localizable {

  private val reservedVisitsPager = visitsPagerActorFactory(userId, self)

  startWith(RequestData, null)

  whenSafe(RequestData) {
    case Event(Next, _) =>
      val visits = apiService.reservedVisits(userId.userId)
      reservedVisitsPager ! visits
      goto(AwaitPage)
  }

  whenSafe(AwaitPage) {
    case Event(cmd: Command, _) =>
      reservedVisitsPager ! cmd
      stay()
    case Event(Pager.NoItemsFound, _) =>
      bot.sendMessage(userId.source, lang.noUpcomingVisits)
      goto(RequestData)
    case Event(visit: ReservedVisit, _) =>
      bot.sendMessage(userId.source, lang.areYouSureToCancelAppointment(visit),
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No), Button(lang.yes, Tags.Yes))))
      goto(AwaitDecision) using visit
  }

  whenSafe(AwaitDecision) {
    case Event(Command(_, _, Some(Tags.No)), _) =>
      bot.sendMessage(userId.source, lang.appointmentWasNotCancelled)
      goto(RequestData)
    case Event(Command(_, _, Some(Tags.Yes)), visit: ReservedVisit) =>
      apiService.deleteReservation(userId.userId, visit.reservationId) match {
        case Left(ex) => bot.sendMessage(userId.source, lang.unableToCancelUpcomingVisit(ex.getMessage))
        case Right(r) => bot.sendMessage(userId.source, lang.appointmentHasBeenCancelled)
      }
      goto(RequestData)
  }

  whenUnhandledSafe {
    case Event(Init, _) =>
      invokeNext()
      reservedVisitsPager ! Init
      goto(RequestData)
  }

  initialize()

  override def postStop(): Unit = {
    reservedVisitsPager ! PoisonPill
    super.postStop()
  }
}

object Visits {
  def props(userId: UserId, bot: Bot, apiService: ApiService, localization: Localization,
            visitsPagerActorFactory: ByUserIdWithOriginatorActorFactory): Props =
    Props(new Visits(userId, bot, apiService, localization, visitsPagerActorFactory))

  object RequestData extends FSMState

  object AwaitPage extends FSMState

  object AwaitDecision extends FSMState

  object Tags {
    val Yes = "yes"
    val No = "no"
  }

}













