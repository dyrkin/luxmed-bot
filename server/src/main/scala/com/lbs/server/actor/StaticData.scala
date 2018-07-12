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
import com.lbs.api.json.model.IdName
import com.lbs.bot.model.{Button, Command, TaggedButton}
import com.lbs.bot.{Bot, _}
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.StaticData._
import com.lbs.server.actor.conversation.Conversation
import com.lbs.server.lang.{Localizable, Localization}

class StaticData(val userId: UserId, bot: Bot, val localization: Localization, originator: ActorRef) extends Conversation[List[TaggedButton]] with Localizable {

  private def anySelectOption: List[TaggedButton] = if (config.isAnyAllowed) List(Button(lang.any, -1L)) else List()

  private var config: StaticDataConfig = _

  entryPoint(AwaitConfig)

  def AwaitConfig: Step =
    monologue {
      case Msg(newConfig: StaticDataConfig, _) =>
        config = newConfig
        goto(askForLatestOption)
    }

  def askForLatestOption: Step =
    ask { _ =>
      originator ! LatestOptions
    } onReply {
      case Msg(LatestOptions(options), _) if options.isEmpty =>
        val callbackTags = anySelectOption
        goto(askForUserInput) using callbackTags
      case Msg(LatestOptions(options), _) if options.nonEmpty =>
        val callbackTags = anySelectOption ++ options.map(data => Button(data.name, data.id))
        goto(askForUserInput) using callbackTags
    }

  def askForUserInput: Step =
    ask { callbackTags =>
      bot.sendMessage(userId.source, lang.pleaseEnterStaticDataNameOrPrevious(config),
        inlineKeyboard = createInlineKeyboard(callbackTags, columns = 1))
    } onReply {
      case Msg(Command(_, msg, Some(tag)), callbackTags) =>
        val id = tag.toLong
        val label = callbackTags.find(_.tag == tag).map(_.label).getOrElse(sys.error("Unable to get callback tag label"))
        bot.sendEditMessage(userId.source, msg.messageId, lang.staticDataIs(config, label))
        originator ! IdName(id, label)
        end()

      case Msg(Command(_, msg, _), _) =>
        val searchText = msg.text.get.toLowerCase
        originator ! FindOptions(searchText)
        stay()

      case Msg(FoundOptions(Right(options)), _) if options.nonEmpty =>
        val callbackTags = anySelectOption ::: options.map(c => Button(c.name, c.id))
        goto(askForUserInput) using callbackTags

      case Msg(FoundOptions(Right(options)), _) if options.isEmpty =>
        val callbackTags = anySelectOption
        goto(askForUserInput) using callbackTags

      case Msg(FoundOptions(Left(ex)), _) =>
        bot.sendMessage(userId.source, ex.getMessage)
        end()
    }
}

object StaticData {
  def props(userId: UserId, bot: Bot, localization: Localization, originator: ActorRef): Props =
    Props(new StaticData(userId, bot, localization, originator))

  case class StaticDataConfig(name: String, example: String, isAnyAllowed: Boolean)

  object LatestOptions

  case class LatestOptions(options: Seq[IdName])

  case class FindOptions(searchText: String)

  case class FoundOptions(option: Either[Throwable, List[IdName]])

}