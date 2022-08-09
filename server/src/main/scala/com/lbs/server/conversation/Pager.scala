package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot._
import com.lbs.bot.model.{Button, Command}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Pager._
import com.lbs.server.conversation.base.{Conversation, Interactional}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.util.MessageExtractors
import com.typesafe.scalalogging.StrictLogging

class Pager[Data](
  val userId: UserId,
  bot: Bot,
  makeMessage: (Data, Int, Int) => String,
  makeHeader: (Int, Int) => String,
  selectionPrefix: Option[String],
  val localization: Localization,
  originator: Interactional
)(val actorSystem: ActorSystem)
    extends Conversation[(ItemsProvider[Data], Option[String])]
    with Localizable
    with StrictLogging {

  private val Selection = s"/${selectionPrefix.getOrElse("")}_(\\d+)".r

  entryPoint(awaitForData)

  private def awaitForData: Step =
    monologue {
      case Msg(Left(error: Throwable), _) =>
        bot.sendMessage(userId.source, error.getMessage)
        end()
      case Msg(Right(itemsProvider: ItemsProvider[Data]), _) if itemsProvider.isEmpty =>
        originator ! NoItemsFound
        end()
      case Msg(Right(itemsProvider: ItemsProvider[Data]), _) =>
        goto(displayPage) using itemsProvider -> None
    }

  private def displayPage: Step =
    ask { case (itemsProvider, massageIdMaybe) =>
      sendPage(itemsProvider, massageIdMaybe)
    } onReply {
      case Msg(Command(_, msg, Some(Tags.Next)), (itemsProvider, _)) =>
        itemsProvider.next()
        goto(displayPage) using itemsProvider -> Some(msg.messageId)
      case Msg(Command(_, msg, Some(Tags.Previous)), (itemsProvider, _)) =>
        itemsProvider.previous()
        goto(displayPage) using itemsProvider -> Some(msg.messageId)
      case Msg(MessageExtractors.TextCommand(Selection(indexStr)), (itemsProvider, _)) if selectionPrefix.nonEmpty =>
        val index = indexStr.toInt
        originator ! itemsProvider.items(index)
        end()
    }

  private def sendPage(itemsProvider: ItemsProvider[Data], messageId: Option[String] = None): Unit = {
    val message =
      makeHeader(itemsProvider.currentPage, itemsProvider.pages) + "\n\n" + itemsProvider.items.zipWithIndex.map {
        case (d, index) =>
          makeMessage(d, itemsProvider.currentPage, index)
      }.mkString

    val previousButton = if (itemsProvider.currentPage > 0) Some(Button(lang.previous, Tags.Previous)) else None
    val nextButton =
      if (itemsProvider.currentPage >= 0 && itemsProvider.currentPage < itemsProvider.pages - 1)
        Some(Button(lang.next, Tags.Next))
      else None
    val buttons = previousButton.toSeq ++ nextButton.toSeq

    messageId match {
      case Some(id) =>
        bot.sendEditMessage(userId.source, id, message, inlineKeyboard = createInlineKeyboard(buttons))
      case None =>
        bot.sendMessage(userId.source, message, inlineKeyboard = createInlineKeyboard(buttons))
    }
  }
}

object Pager {
  val PageSize = 5

  trait ItemsProvider[Data] {
    def pages: Int

    def next(): Unit

    def previous(): Unit

    def items: Seq[Data]

    def currentPage: Int

    def itemsCount: Int

    def isEmpty: Boolean
  }

  class SimpleItemsProvider[Data](fromItems: Seq[Data]) extends ItemsProvider[Data] {
    private val allPages = fromItems.grouped(PageSize).toSeq

    private var index: Int = 0

    override def pages: Int = allPages.size

    override def next(): Unit = index += 1

    override def previous(): Unit = index -= 1

    override def items: Seq[Data] = allPages(index)

    override def currentPage: Int = index

    override def itemsCount: Int = fromItems.size

    override def isEmpty: Boolean = fromItems.isEmpty
  }

  object NoItemsFound

  object Tags {
    val Previous = "previous"
    val Next = "next"
  }

}
