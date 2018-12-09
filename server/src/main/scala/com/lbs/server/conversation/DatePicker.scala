
package com.lbs.server.conversation

import java.time.format.TextStyle
import java.time.{LocalTime, ZonedDateTime}

import akka.actor.ActorSystem
import com.lbs.bot.model.Button
import com.lbs.bot.{Bot, _}
import com.lbs.server.conversation.DatePicker._
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.base.{Conversation, Interactional}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.util.DateTimeUtil._
import com.lbs.server.util.MessageExtractors.{CallbackCommand, TextCommand}

import scala.util.control.NonFatal

/**
  * Date picker Inline Keyboard
  *
  * ⬆   ⬆    ⬆
  * dd   MM   yyyy
  * ⬇   ⬇    ⬇
  *
  */
class DatePicker(val userId: UserId, val bot: Bot, val localization: Localization, originator: Interactional)
                (val actorSystem: ActorSystem) extends Conversation[ZonedDateTime] with Localizable {

  private var mode: Mode = DateFromMode

  entryPoint(configure)

  def configure: Step =
    monologue {
      case Msg(newMode: Mode, _) =>
        mode = newMode
        stay()
      case Msg(initialDate: ZonedDateTime, _) =>
        goto(requestDate) using initialDate
    }

  def requestDate: Step =
    ask { initialDate =>
      val message = mode match {
        case DateFromMode => lang.chooseDateFrom(initialDate)
        case DateToMode => lang.chooseDateTo(initialDate)
      }
      bot.sendMessage(userId.source, message, inlineKeyboard = dateButtons(initialDate))
    } onReply {
      case Msg(cmd@CallbackCommand(Tags.Done), finalDate) =>
        val (message, updatedDate) = mode match {
          case DateFromMode =>
            val startOfTheDay = finalDate.`with`(LocalTime.MIN)
            val dateFrom = if (startOfTheDay.isBefore(ZonedDateTime.now())) finalDate else startOfTheDay
            lang.dateFromIs(dateFrom) -> dateFrom
          case DateToMode =>
            val dateTo = finalDate.`with`(LocalTime.MAX).minusHours(2)
            lang.dateToIs(dateTo) -> dateTo
        }
        bot.sendEditMessage(userId.source, cmd.message.messageId, message)
        originator ! updatedDate
        end()
      case Msg(TextCommand(dayMonth), finalDate) =>
        try {
          val updatedDate = applyDayMonth(dayMonth, finalDate)
          val message = mode match {
            case DateFromMode =>
              lang.dateFromIs(updatedDate)
            case DateToMode =>
              lang.dateToIs(updatedDate)
          }
          bot.sendMessage(userId.source, message)
          originator ! updatedDate
          end()
        } catch {
          case NonFatal(ex) =>
            error("Unable to parse date", ex)
            bot.sendMessage(userId.source, "Incorrect date. Please use format dd-MM")
            goto(requestDate)
        }
      case Msg(cmd@CallbackCommand(tag), date) =>
        val modifiedDate = modifyDate(date, tag)
        bot.sendEditMessage(userId.source, cmd.message.messageId, inlineKeyboard = dateButtons(modifiedDate))
        stay() using modifiedDate
    }

  private def modifyDate(date: ZonedDateTime, tag: String) = {
    val dateModifier = tag match {
      case Tags.DayInc => date.plusDays _
      case Tags.MonthInc => date.plusMonths _
      case Tags.YearInc => date.plusYears _
      case Tags.DayDec => date.minusDays _
      case Tags.MonthDec => date.minusMonths _
      case Tags.YearDec => date.minusYears _
    }
    dateModifier(1)
  }

  private def dateButtons(date: ZonedDateTime) = {
    val day = date.getDayOfMonth.toString
    val dayOfWeek = date.getDayOfWeek.getDisplayName(TextStyle.SHORT, lang.locale)
    val month = date.getMonth.getDisplayName(TextStyle.SHORT, lang.locale)
    val year = date.getYear.toString

    createInlineKeyboard(Seq(
      Seq(Button("⬆", Tags.DayInc), Button("⬆", Tags.MonthInc), Button("⬆", Tags.YearInc)),
      Seq(Button(s"$day ($dayOfWeek)"), Button(month), Button(year)),
      Seq(Button("⬇", Tags.DayDec), Button("⬇", Tags.MonthDec), Button("⬇", Tags.YearDec)),
      Seq(Button("Done", Tags.Done))
    ))
  }
}

object DatePicker {

  trait Mode

  object DateFromMode extends Mode

  object DateToMode extends Mode

  object Tags {
    val DayInc = "day_inc"
    val MonthInc = "month_inc"
    val YearInc = "year_inc"
    val DayDec = "day_dec"
    val MonthDec = "month_dec"
    val YearDec = "year_dec"
    val Done = "done"
  }

}