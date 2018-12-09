
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.Bot
import com.lbs.bot.model.Command
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Localizable, Localization}

class Help(val userId: UserId, bot: Bot, val localization: Localization)(val actorSystem: ActorSystem) extends Conversation[Unit] with Localizable {

  entryPoint(displayHelp)

  def displayHelp: Step =
    monologue {
      case Msg(_: Command, _) =>
        bot.sendMessage(userId.source, lang.help)
        stay()
    }
}