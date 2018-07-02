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

import akka.actor.Props
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.server.actor.Bug._
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.conversation.Conversation
import com.lbs.server.actor.conversation.Conversation.{InitConversation, StartConversation}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model
import com.lbs.server.service.DataService
import com.lbs.server.util.MessageExtractors

class Bug(val userId: UserId, bot: Bot, dataService: DataService, bugPagerActorFactory: ByUserIdWithOriginatorActorFactory,
          val localization: Localization) extends Conversation[Unit] with Localizable {

  private val bugPager = bugPagerActorFactory(userId, self)

  def askAction: Step =
    question { _ =>
      bot.sendMessage(userId.source, lang.bugAction, inlineKeyboard =
        createInlineKeyboard(Seq(Button(lang.createNewBug, Tags.SubmitNew), Button(lang.showSubmittedBugs, Tags.ListSubmitted))))
    } answer {
      case Msg(Command(_, _, Some(Tags.SubmitNew)), _) =>
        goto(askBugDescription)
      case Msg(Command(_, _, Some(Tags.ListSubmitted)), _) =>
        goto(displaySubmittedBugs)
    }

  def displaySubmittedBugs: IC =
    internalConfig { _ =>
      val bugs = dataService.getBugs(userId.userId)
      bugPager ! InitConversation
      bugPager ! StartConversation
      bugPager ! Right[Throwable, Seq[model.Bug]](bugs)
      goto(processResponseFromPager)
    }

  def processResponseFromPager: M =
    monologue {
      case Msg(cmd: Command, _) =>
        bugPager ! cmd
        stay()
      case Msg(Pager.NoItemsFound, _) =>
        bot.sendMessage(userId.source, lang.noSubmittedIssuesFound)
        end()
    }

  def askBugDescription: Step =
    question { _ =>
      bot.sendMessage(userId.source, lang.enterIssueDetails)
    } answer {
      case Msg(Command(_, MessageExtractors.Text(details), _), _) =>
        val bugId = dataService.submitBug(userId.userId, userId.source.sourceSystem.id, details)
        bot.sendMessage(userId.source, lang.bugHasBeenCreated(bugId.getOrElse(-1L)))
        end()
    }

  entryPoint(askAction)
}

object Bug {
  def props(userId: UserId, bot: Bot, dataService: DataService, bugPagerActorFactory: ByUserIdWithOriginatorActorFactory, localization: Localization): Props =
    Props(new Bug(userId, bot, dataService, bugPagerActorFactory, localization))

  object Tags {
    val SubmitNew = "submit"
    val ListSubmitted = "list"
  }

}