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

import java.time.format.TextStyle
import java.time.{LocalTime, ZonedDateTime}

import akka.actor.{ActorRef, Props}
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.server.actor.Chat.Init
import com.lbs.server.actor.DatePicker._
import com.lbs.server.actor.Login.UserId
import com.lbs.server.lang.{Localizable, Localization}

/**
  * Date picker Inline Keyboard
  *
  * ⬆   ⬆    ⬆
  * dd   MM   yyyy
  * ⬇   ⬇    ⬇
  *
  */
class DatePicker(val userId: UserId, val bot: Bot, val localization: Localization, originator: ActorRef) extends SafeFSM[FSMState, ZonedDateTime] with Localizable {

  startWith(AwaitMode, null)

  private var mode: Mode = DateFromMode

  whenSafe(AwaitMode) {
    case Event(newMode: Mode, _) =>
      mode = newMode
      goto(RequestDate)
  }

  whenSafe(RequestDate) {
    case Event(initialDate: ZonedDateTime, _) =>
      val message = mode match {
        case DateFromMode => lang.chooseDateFrom
        case DateToMode => lang.chooseDateTo
      }
      bot.sendMessage(userId.source, message, inlineKeyboard = dateButtons(initialDate))
      goto(AwaitDate) using initialDate
  }

  whenSafe(AwaitDate) {
    case Event(Command(_, msg, Some(Tags.Done)), finalDate: ZonedDateTime) =>

      val (message, updatedDate) = mode match {
        case DateFromMode =>
          val startOfTheDay = finalDate.`with`(LocalTime.MIN)
          val dateFrom = if (startOfTheDay.isBefore(ZonedDateTime.now())) finalDate else startOfTheDay
          lang.dateFromIs(dateFrom) -> dateFrom
        case DateToMode =>
          val dateTo = finalDate.`with`(LocalTime.MAX).minusHours(2)
          lang.dateToIs(dateTo) -> dateTo
      }
      bot.sendEditMessage(userId.source, msg.messageId, message)
      originator ! updatedDate
      goto(AwaitMode) using null

    case Event(Command(_, msg, Some(tag)), date: ZonedDateTime) =>
      val modifiedDate = modifyDate(date, tag)
      bot.sendEditMessage(userId.source, msg.messageId, inlineKeyboard = dateButtons(modifiedDate))
      stay() using modifiedDate
  }

  whenUnhandledSafe {
    case Event(Init, _) =>
      goto(AwaitMode) using null
    case e: Event =>
      LOG.error(s"Unhandled event in state:$stateName. Event: $e")
      stay()
  }

  initialize()

  private def modifyDate(date: ZonedDateTime, tag: String) = {
    val dateModifier = tag match {
      case Tags.DayUp => date.plusDays _
      case Tags.MonthUp => date.plusMonths _
      case Tags.YearUp => date.plusYears _
      case Tags.DayDown => date.minusDays _
      case Tags.MonthDown => date.minusMonths _
      case Tags.YearDown => date.minusYears _
    }
    dateModifier(1)
  }

  private def dateButtons(date: ZonedDateTime) = {
    val day = date.getDayOfMonth.toString
    val dayOfWeek = date.getDayOfWeek.getDisplayName(TextStyle.SHORT, lang.locale)
    val month = date.getMonth.getDisplayName(TextStyle.SHORT, lang.locale)
    val year = date.getYear.toString

    createInlineKeyboard(Seq(
      Seq(Button("⬆", Tags.DayUp), Button("⬆", Tags.MonthUp), Button("⬆", Tags.YearUp)),
      Seq(Button(s"$day ($dayOfWeek)"), Button(month), Button(year)),
      Seq(Button("⬇", Tags.DayDown), Button("⬇", Tags.MonthDown), Button("⬇", Tags.YearDown)),
      Seq(Button("Done", Tags.Done))
    ))
  }
}

object DatePicker {
  def props(userId: UserId, bot: Bot, localization: Localization, originator: ActorRef): Props =
    Props(classOf[DatePicker], userId, bot, localization, originator)

  object RequestDate extends FSMState

  object AwaitDate extends FSMState

  object AwaitMode extends FSMState

  trait Mode

  object DateFromMode extends Mode

  object DateToMode extends Mode

  object Tags {
    val DayUp = "day_up"
    val MonthUp = "month_up"
    val YearUp = "year_up"
    val DayDown = "day_down"
    val MonthDown = "month_down"
    val YearDown = "year_down"
    val Done = "done"
  }

}



