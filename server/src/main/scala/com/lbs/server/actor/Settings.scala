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
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor.Settings._
import com.lbs.server.actor.conversation.Conversation
import com.lbs.server.lang.{Lang, Localizable, Localization}
import com.lbs.server.service.DataService

class Settings(val userId: UserId, bot: Bot, dataService: DataService, val localization: Localization) extends Conversation[Unit] with Localizable {

  entryPoint(askForAction)

  def askForAction: QA =
    question { _ =>
      bot.sendMessage(userId.source, lang.settingsHeader, inlineKeyboard =
        createInlineKeyboard(Seq(Button(lang.language, Tags.Language))))
    } answer {
      case Msg(Command(_, _, Some(Tags.Language)), _) =>
        goto(askLanguage)
    }

  def askLanguage: QA =
    question { _ =>
      bot.sendMessage(userId.source, lang.chooseLanguage,
        inlineKeyboard = createInlineKeyboard(Lang.Langs.map(l => Button(l.label, l.id)), columns = 1))
    } answer {
      case Msg(Command(_, _, Some(langIdStr)), _) =>
        val langId = langIdStr.toInt
        localization.updateLanguage(userId.userId, Lang(langId))
        bot.sendMessage(userId.source, lang.languageUpdated)
        end()
    }
}

object Settings {
  def props(userId: UserId, bot: Bot, dataService: DataService, localization: Localization): Props =
    Props(new Settings(userId, bot, dataService, localization))

  object Tags {
    val Language = "language"
  }

}