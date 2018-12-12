
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Settings._
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Lang, Localizable, Localization}
import com.lbs.server.service.DataService
import com.lbs.server.util.MessageExtractors.{CallbackCommand, IntString}
import com.lbs.server.repository.model

class Settings(val userId: UserId, bot: Bot, dataService: DataService, val localization: Localization)(val actorSystem: ActorSystem) extends Conversation[Unit] with Localizable {

  entryPoint(askForAction)

  def askForAction: Step =
    ask { _ =>
      bot.sendMessage(userId.source, lang.settingsHeader, inlineKeyboard =
        createInlineKeyboard(Seq(Button(lang.language, Tags.Language),
          Button(lang.offset, Tags.Offset)), columns = 1))
    } onReply {
      case Msg(Command(_, _, Some(Tags.Language)), _) =>
        goto(askLanguage)
      case Msg(Command(_, _, Some(Tags.Offset)), _) =>
        goto(showOffsetOptions)
    }

  def askLanguage: Step =
    ask { _ =>
      bot.sendMessage(userId.source, lang.chooseLanguage,
        inlineKeyboard = createInlineKeyboard(Lang.Langs.map(l => Button(l.label, l.id)), columns = 1))
    } onReply {
      case Msg(CallbackCommand(IntString(langId)), _) =>
        localization.updateLanguage(userId.userId, Lang(langId))
        bot.sendMessage(userId.source, lang.languageUpdated)
        end()
    }

  def showOffsetOptions: Step = {
    ask { _ =>
      val settingsMaybe = dataService.findSettings(userId.userId)
      val (defaultOffset, askOffset) = settingsMaybe match {
        case Some(settings) => (settings.defaultOffset, settings.alwaysAskOffset)
        case None => (0, false)
      }
      bot.sendMessage(userId.source, lang.configureOffset,
        inlineKeyboard = createInlineKeyboard(Seq(Button(s"${if(askOffset) "✅" else " "} Always ask offset", Tags.AskOffset)), columns = 1))
    } onReply {
      case Msg(cmd@CallbackCommand(Tags.AskOffset), _) =>
        val settings = dataService.findSettings(userId.userId).getOrElse(model.Settings(userId.userId, lang.id, 0, alwaysAskOffset = false))
        settings.alwaysAskOffset = !settings.alwaysAskOffset
        dataService.saveSettings(settings)
        bot.sendEditMessage(userId.source, cmd.message.messageId,
          inlineKeyboard = createInlineKeyboard(Seq(Button(s"${if(settings.alwaysAskOffset) "✅" else " "} Always ask offset", Tags.AskOffset)), columns = 1))
        stay()
    }
  }
}

object Settings {

  object Tags {
    val Language = "language"
    val Offset = "offset"
    val AskOffset = "ask_offset"
  }

}