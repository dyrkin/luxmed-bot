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
import com.lbs.server.actor.Chat.Init
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.Settings._
import com.lbs.server.lang.{Lang, Localizable, Localization}
import com.lbs.server.service.DataService

class Settings(val userId: UserId, bot: Bot, dataService: DataService, val localization: Localization) extends SafeFSM[FSMState, FSMData] with Localizable {

  startWith(RequestAction, null)

  whenSafe(RequestAction) {
    case Event(Next, _) =>
      bot.sendMessage(userId.source, lang.settingsHeader, inlineKeyboard =
        createInlineKeyboard(Seq(Button(lang.language, Tags.Language))))
      goto(AwaitAction)
  }

  whenSafe(AwaitAction) {
    case Event(Command(_, _, Some(Tags.Language)), _) =>
      bot.sendMessage(userId.source, lang.chooseLanguage,
        inlineKeyboard = createInlineKeyboard(Lang.Langs.map(l => Button(l.label, l.id)), columns = 1))
      goto(AwaitLanguage)
  }

  whenSafe(AwaitLanguage) {
    case Event(Command(_, _, Some(langIdStr)), _) =>
      val langId = langIdStr.toInt
      localization.updateLanguage(userId.userId, Lang(langId))
      bot.sendMessage(userId.source, lang.languageUpdated)
      goto(RequestAction) using null
  }

  whenUnhandledSafe {
    case Event(Init, _) =>
      invokeNext()
      goto(RequestAction)
  }

  initialize()
}

object Settings {
  def props(userId: UserId, bot: Bot, dataService: DataService, localization: Localization): Props =
    Props(classOf[Settings], userId, bot, dataService, localization)

  object AwaitLanguage extends FSMState

  object RequestAction extends FSMState

  object AwaitAction extends FSMState

  object Tags {
    val Language = "language"
  }

}



















