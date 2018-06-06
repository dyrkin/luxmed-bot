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
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.server.actor.Bug._
import com.lbs.server.actor.Chat.Init
import com.lbs.server.actor.Login.UserId
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model
import com.lbs.server.service.DataService
import com.lbs.server.util.MessageExtractors

class Bug(val userId: UserId, bot: Bot, dataService: DataService, bugPagerActorFactory: ByUserIdWithOriginatorActorFactory,
          val localization: Localization) extends SafeFSM[FSMState, FSMData] with Localizable {

  private val bugPager = bugPagerActorFactory(userId, self)

  startWith(RequestAction, null)

  whenSafe(RequestAction) {
    case Event(Next, _) =>
      bot.sendMessage(userId.source, lang.bugAction, inlineKeyboard =
        createInlineKeyboard(Seq(Button(lang.createNewBug, Tags.SubmitNew), Button(lang.showSubmittedBugs, Tags.ListSubmitted))))
      goto(AwaitAction)
  }

  whenSafe(AwaitAction) {
    case Event(Command(_, _, Some(Tags.SubmitNew)), _) =>
      bot.sendMessage(userId.source, lang.enterIssueDetails)
      goto(AwaitBugDescription)
    case Event(Command(_, _, Some(Tags.ListSubmitted)), _) =>
      invokeNext()
      goto(RequestData)
  }

  whenSafe(RequestData) {
    case Event(Next, _) =>
      val bugs = dataService.getBugs(userId.userId)
      bugPager ! Init
      bugPager ! Right[Throwable, Seq[model.Bug]](bugs)
      goto(AwaitPage)
  }

  whenSafe(AwaitPage) {
    case Event(cmd: Command, _) =>
      bugPager ! cmd
      stay()
    case Event(Pager.NoItemsFound, _) =>
      bot.sendMessage(userId.source, lang.noSubmittedIssuesFound)
      goto(RequestData)
  }

  whenSafe(AwaitBugDescription) {
    case Event(Command(_, MessageExtractors.Text(details), _), _) =>
      val bugId = dataService.submitBug(userId.userId, userId.source.sourceSystem.id, details)
      bot.sendMessage(userId.source, lang.bugHasBeenCreated(bugId.getOrElse(-1L)))
      goto(RequestAction) using null
  }

  whenUnhandledSafe {
    case Event(Init, _) =>
      invokeNext()
      bugPager ! Init
      goto(RequestAction)
  }

  initialize()

  override def postStop(): Unit = {
    bugPager ! PoisonPill
    super.postStop()
  }
}

object Bug {
  def props(userId: UserId, bot: Bot, dataService: DataService, bugPagerActorFactory: ByUserIdWithOriginatorActorFactory, localization: Localization): Props =
    Props(new Bug(userId, bot, dataService, bugPagerActorFactory, localization))

  object RequestBugDetails extends FSMState

  object AwaitBugDescription extends FSMState

  object RequestAction extends FSMState

  object AwaitAction extends FSMState

  object RequestData extends FSMState

  object AwaitPage extends FSMState

  object Tags {
    val SubmitNew = "submit"
    val ListSubmitted = "list"
  }

}















