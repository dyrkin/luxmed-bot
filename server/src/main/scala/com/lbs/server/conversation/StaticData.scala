
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.api.json.model.IdName
import com.lbs.bot.model.{Button, Command, TaggedButton}
import com.lbs.bot.{Bot, _}
import com.lbs.server.ThrowableOr
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.StaticData._
import com.lbs.server.conversation.base.{Conversation, Interactional}
import com.lbs.server.lang.{Localizable, Localization}

class StaticData(val userId: UserId, bot: Bot, val localization: Localization, originator: Interactional)(val actorSystem: ActorSystem) extends Conversation[List[TaggedButton]] with Localizable {

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

  case class StaticDataConfig(name: String, partialExample: String, example: String, isAnyAllowed: Boolean)

  object LatestOptions

  case class LatestOptions(options: Seq[IdName])

  case class FindOptions(searchText: String)

  case class FoundOptions(option: ThrowableOr[List[IdName]])

}