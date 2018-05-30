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
import com.lbs.server.actor.Chat.Init
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.StaticData._
import com.lbs.server.lang.{Localizable, Localization}

class StaticData(val userId: UserId, bot: Bot, val localization: Localization, originator: ActorRef) extends SafeFSM[FSMState, IdName] with Localizable {

  private def anySelectOption: List[TaggedButton] = if (config.isAnyAllowed) List(Button(lang.any, -1L)) else List()

  startWith(AwaitConfig, null)

  private var config: StaticDataConfig = _

  private var callbackTags: List[TaggedButton] = List()

  whenSafe(AwaitConfig) {
    case Event(newConfig: StaticDataConfig, _) =>
      config = newConfig
      invokeNext()
      goto(RequestStaticData)
  }

  whenSafe(RequestStaticData) {
    case Event(Next, _) =>
      originator ! LatestOptions
      stay()
    case Event(LatestOptions(options), _) if options.isEmpty =>
      callbackTags = anySelectOption
      bot.sendMessage(userId.source, lang.pleaseEnterStaticDataNameOrAny(config),
        inlineKeyboard = createInlineKeyboard(callbackTags))
      goto(AwaitStaticData)
    case Event(LatestOptions(options), _) if options.nonEmpty =>
      callbackTags = anySelectOption ++ options.map(data => Button(data.name, data.id))
      bot.sendMessage(userId.source, lang.pleaseEnterStaticDataNameOrPrevious(config),
        inlineKeyboard = createInlineKeyboard(callbackTags, columns = 1))
      goto(AwaitStaticData)
  }

  whenSafe(AwaitStaticData) {
    case Event(Command(_, msg, Some(tag)), _) =>
      val id = tag.toLong
      val label = callbackTags.find(_.tag == tag).map(_.label).getOrElse(sys.error("Unable to get callback tag label"))
      bot.sendEditMessage(userId.source, msg.messageId, lang.staticDataIs(config, label))
      originator ! IdName(id, label)
      goto(AwaitConfig)

    case Event(Command(_, msg, _), _) =>
      val searchText = msg.text.get.toLowerCase
      originator ! FindOptions(searchText)
      stay()

    case Event(FoundOptions(Right(options)), _) if options.nonEmpty =>
      callbackTags = anySelectOption ::: options.map(c => Button(c.name, c.id))
      bot.sendMessage(userId.source, lang.pleaseChooseStaticDataNameOrAny(config),
        inlineKeyboard = createInlineKeyboard(callbackTags, columns = 1))
      stay()

    case Event(FoundOptions(Right(options)), _) if options.isEmpty =>
      callbackTags = anySelectOption
      bot.sendMessage(userId.source, lang.staticNotFound(config), inlineKeyboard = createInlineKeyboard(callbackTags))
      stay()

    case Event(FoundOptions(Left(ex)), _) =>
      bot.sendMessage(userId.source, ex.getMessage)
      stay()
  }

  whenUnhandledSafe {
    case Event(Init, _) =>
      goto(AwaitConfig) using null
    case e: Event =>
      LOG.error(s"Unhandled event in state:$stateName. Event: $e")
      stay()
  }

  initialize()
}

object StaticData {
  def props(userId: UserId, bot: Bot, localization: Localization, originator: ActorRef): Props =
    Props(classOf[StaticData], userId, bot, localization, originator)

  object AwaitConfig extends FSMState

  object RequestStaticData extends FSMState

  object AwaitStaticData extends FSMState

  case class StaticDataConfig(name: String, example: String, isAnyAllowed: Boolean)

  object LatestOptions

  case class LatestOptions(options: Seq[IdName])

  case class FindOptions(searchText: String)

  case class FoundOptions(option: Either[Throwable, List[IdName]])

}





