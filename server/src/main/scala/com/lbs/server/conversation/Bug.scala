
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