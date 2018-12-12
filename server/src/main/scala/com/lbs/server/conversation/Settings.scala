
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Settings._
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Lang, Localizable, Localization}
import com.lbs.server.service.DataService
import com.lbs.server.util.MessageExtractors.{CallbackCommand, IntString, TextCommand}
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
      val settings = getSettings
      bot.sendMessage(userId.source, lang.configureOffset,
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.alwaysAskOffset(settings.alwaysAskOffset), Tags.AlwaysAskOffset),
          Button(lang.changeDefaultOffset(settings.defaultOffset), Tags.ChangeDefaultOffset)), columns = 1))
    } onReply {
      case Msg(cmd@CallbackCommand(Tags.AlwaysAskOffset), _) =>
        val settings = getSettings
        settings.alwaysAskOffset = !settings.alwaysAskOffset
        dataService.saveSettings(settings)
        bot.sendEditMessage(userId.source, cmd.message.messageId,
          inlineKeyboard = createInlineKeyboard(Seq(Button(lang.alwaysAskOffset(settings.alwaysAskOffset), Tags.AlwaysAskOffset),
            Button(lang.changeDefaultOffset(settings.defaultOffset), Tags.ChangeDefaultOffset)), columns = 1))
        stay()
      case Msg(CallbackCommand(Tags.ChangeDefaultOffset), _) =>
        goto(askDefaultOffset)
    }
  }

  def askDefaultOffset: Step = {
    ask { _ =>
      val settings = getSettings
      bot.sendMessage(userId.source, lang.pleaseEnterOffset(settings.defaultOffset))
    } onReply {
      case Msg(TextCommand(IntString(offset)), _) =>
        val settings = getSettings
        settings.defaultOffset = offset
        dataService.saveSettings(settings)
        goto(showOffsetOptions)
    }
  }

  private def getSettings = {
    dataService.findSettings(userId.userId).getOrElse(model.Settings(userId.userId, lang.id, 0, alwaysAskOffset = false))
  }
}

object Settings {

  object Tags {
    val Language = "language"
    val Offset = "offset"
    val AlwaysAskOffset = "always_ask_offset"
    val ChangeDefaultOffset = "change_default_offset"
  }

}