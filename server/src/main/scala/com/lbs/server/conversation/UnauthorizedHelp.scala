
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.Bot
import com.lbs.bot.model.{Command, MessageSource}
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.En

class UnauthorizedHelp(source: MessageSource, bot: Bot)(val actorSystem: ActorSystem) extends Conversation[Unit] {
  entryPoint(displayHelp)

  def displayHelp: Step =
    monologue {
      case Msg(_: Command, _) =>
        bot.sendMessage(source, En.help)
        stay()
    }
}