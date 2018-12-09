
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Settings._
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Lang, Localizable, Localization}
import com.lbs.server.service.DataService

class Settings(val userId: UserId, bot: Bot, dataService: DataService, val localization: Localization)(val actorSystem: ActorSystem) extends Conversation[Unit] with Localizable {

  entryPoint(askForAction)

  def askForAction: Step =
    ask { _ =>
      bot.sendMessage(userId.source, lang.settingsHeader, inlineKeyboard =
        createInlineKeyboard(Seq(Button(lang.language, Tags.Language))))
    } onReply {
      case Msg(Command(_, _, Some(Tags.Language)), _) =>
        goto(askLanguage)
    }

  def askLanguage: Step =
    ask { _ =>
      bot.sendMessage(userId.source, lang.chooseLanguage,
        inlineKeyboard = createInlineKeyboard(Lang.Langs.map(l => Button(l.label, l.id)), columns = 1))
    } onReply {
      case Msg(Command(_, _, Some(langIdStr)), _) =>
        val langId = langIdStr.toInt
        localization.updateLanguage(userId.userId, Lang(langId))
        bot.sendMessage(userId.source, lang.languageUpdated)
        end()
    }
}

object Settings {

  object Tags {
    val Language = "language"
  }

}