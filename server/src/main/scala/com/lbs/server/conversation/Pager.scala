
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.common.Logger
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Pager.{Tags, _}
import com.lbs.server.conversation.base.{Conversation, Interactional}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.util.MessageExtractors

class Pager[Data](val userId: UserId, bot: Bot, makeMessage: (Data, Int, Int) => String,
                  makeHeader: (Int, Int) => String, selectionPrefix: Option[String],
                  val localization: Localization, originator: Interactional)(val actorSystem: ActorSystem)
  extends Conversation[(Registry[Data], Option[String])] with Localizable with Logger {

  private val Selection = s"/${selectionPrefix.getOrElse("")}_(\\d+)_(\\d+)".r

  entryPoint(awaitForData)

  private def awaitForData: Step =
    monologue {
      case Msg(Left(error: Throwable), _) =>
        bot.sendMessage(userId.source, error.getMessage)
        end()
      case Msg(Right(items: Seq[Data]), _) if items.isEmpty =>
        originator ! NoItemsFound
        end()
      case Msg(Right(items: Seq[Data]), _) =>
        goto(displayPage) using Registry(0, items.grouped(Pager.PageSize).toList) -> None
    }

  private def displayPage: Step =
    ask { case (registry, massageIdMaybe) =>
      sendPage(registry.page, registry.pages, massageIdMaybe)
    } onReply {
      case Msg(Command(_, msg, Some(Tags.Next)), (registry, _)) =>
        val page = registry.page + 1
        goto(displayPage) using registry.copy(page = page) -> Some(msg.messageId)
      case Msg(Command(_, msg, Some(Tags.Previous)), (registry, _)) =>
        val page = registry.page - 1
        goto(displayPage) using registry.copy(page = page) -> Some(msg.messageId)
      case Msg(MessageExtractors.TextCommand(Selection(pageStr, indexStr)), (registry, _)) if selectionPrefix.nonEmpty =>
        val page = pageStr.toInt
        val index = indexStr.toInt
        originator ! registry.pages(page)(index)
        end()
    }

  private def sendPage(page: Int, data: Seq[Seq[Data]], messageId: Option[String] = None): Unit = {
    val pages = data.length
    val message = makeHeader(page, data.length) + "\n\n" + data(page).zipWithIndex.map { case (d, index) => makeMessage(d, page, index) }.mkString

    val previousButton = if (page > 0) Some(Button(lang.previous, Tags.Previous)) else None
    val nextButton = if (page >= 0 && page < pages - 1) Some(Button(lang.next, Tags.Next)) else None
    val buttons = previousButton.toSeq ++ nextButton.toSeq

    messageId match {
      case Some(id) =>
        bot.sendEditMessage(userId.source, id, message, inlineKeyboard = createInlineKeyboard(buttons))
      case None =>
        bot.sendMessage(userId.source, message, inlineKeyboard = createInlineKeyboard(buttons))
    }
  }
}

object Pager {
  val PageSize = 5

  case class Registry[Data](page: Int, pages: Seq[Seq[Data]])

  object NoItemsFound

  object Tags {
    val Previous = "previous"
    val Next = "next"
  }

}