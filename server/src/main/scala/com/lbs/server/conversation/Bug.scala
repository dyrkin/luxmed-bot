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
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.server.conversation.Bug._
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model
import com.lbs.server.service.DataService
import com.lbs.server.util.MessageExtractors

class Bug(val userId: UserId, bot: Bot, dataService: DataService, bugPagerFactory: UserIdWithOriginatorTo[Pager[model.Bug]],
          val localization: Localization)(val actorSystem: ActorSystem) extends Conversation[Unit] with Localizable {

  private val bugPager = bugPagerFactory(userId, self)

  entryPoint(askAction)

  def askAction: Step =
    ask { _ =>
      bot.sendMessage(userId.source, lang.bugAction, inlineKeyboard =
        createInlineKeyboard(Seq(Button(lang.createNewBug, Tags.SubmitNew), Button(lang.showSubmittedBugs, Tags.ListSubmitted))))
    } onReply {
      case Msg(Command(_, _, Some(Tags.SubmitNew)), _) =>
        goto(askBugDescription)
      case Msg(Command(_, _, Some(Tags.ListSubmitted)), _) =>
        goto(displaySubmittedBugs)
    }

  def displaySubmittedBugs: Step =
    process { _ =>
      val bugs = dataService.getBugs(userId.userId)
      bugPager.restart()
      bugPager ! Right[Throwable, Seq[model.Bug]](bugs)
      goto(processResponseFromPager)
    }

  def processResponseFromPager: Step =
    monologue {
      case Msg(cmd: Command, _) =>
        bugPager ! cmd
        stay()
      case Msg(Pager.NoItemsFound, _) =>
        bot.sendMessage(userId.source, lang.noSubmittedIssuesFound)
        end()
    }

  def askBugDescription: Step =
    ask { _ =>
      bot.sendMessage(userId.source, lang.enterIssueDetails)
    } onReply {
      case Msg(MessageExtractors.TextCommand(details), _) =>
        val bugId = dataService.submitBug(userId.userId, userId.source.sourceSystem.id, details)
        bot.sendMessage(userId.source, lang.bugHasBeenCreated(bugId.getOrElse(-1L)))
        end()
    }

  beforeDestroy {
    bugPager.destroy()
  }
}

object Bug {

  object Tags {
    val SubmitNew = "submit"
    val ListSubmitted = "list"
  }

}