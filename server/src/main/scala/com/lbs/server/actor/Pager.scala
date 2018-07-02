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

import akka.actor.{ActorRef, Props}
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.common.Logger
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.Pager.{Tags, _}
import com.lbs.server.actor.conversation.Conversation
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.util.MessageExtractors

class Pager[Data](val userId: UserId, bot: Bot, makeMessage: (Data, Int, Int) => String,
                  makeHeader: (Int, Int) => String, selectionPrefix: Option[String],
                  val localization: Localization, originator: ActorRef)
  extends Conversation[(Registry[Data], Option[String])] with Localizable with Logger {

  private val Selection = s"/${selectionPrefix.getOrElse("")}_(\\d+)_(\\d+)".r

  entryPoint(awaitForData)

  private def awaitForData: EC =
    externalConfig {
      case Msg(Left(error: Throwable), _) =>
        bot.sendMessage(userId.source, error.getMessage)
        end()
      case Msg(Right(items: Seq[Data]), _) if items.isEmpty =>
        originator ! NoItemsFound
        end()
      case Msg(Right(items: Seq[Data]), _) =>
        goto(displayPage) using Registry(0, items.grouped(Pager.PageSize).toList) -> None
    }

  private def displayPage: QA =
    question { case (registry, massageIdMaybe) =>
      sendPage(registry.page, registry.pages, massageIdMaybe)
    } answer {
      case Msg(Command(_, msg, Some(Tags.Next)), (registry, _)) =>
        val page = registry.page + 1
        goto(displayPage) using registry.copy(page = page) -> Some(msg.messageId)
      case Msg(Command(_, msg, Some(Tags.Previous)), (registry, _)) =>
        val page = registry.page - 1
        goto(displayPage) using registry.copy(page = page) -> Some(msg.messageId)
      case Msg(Command(_, MessageExtractors.Text(Selection(pageStr, indexStr)), _), (registry, _)) if selectionPrefix.nonEmpty =>
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
  def props[Data](userId: UserId, bot: Bot,
                  makeMessage: (Data, Int, Int) => String, makeHeader: (Int, Int) => String, dataPrefix: Option[String], localization: Localization, originator: ActorRef): Props =
    Props(new Pager[Data](userId, bot, makeMessage, makeHeader, dataPrefix, localization, originator))

  val PageSize = 5

  case class Registry[Data](page: Int, pages: Seq[Seq[Data]])

  object NoItemsFound

  object Tags {
    val Previous = "previous"
    val Next = "next"
  }

}