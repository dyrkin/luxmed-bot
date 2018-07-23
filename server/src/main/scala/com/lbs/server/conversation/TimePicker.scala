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
package com.lbs.server.conversation

import java.time.LocalTime

import akka.actor.ActorSystem
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.TimePicker.{Mode, Tags, TimeFromMode, TimeToMode}
import com.lbs.server.conversation.base.{Conversation, Interactional}
import com.lbs.server.lang.{Localizable, Localization}

/**
  * Time picker Inline Keyboard
  *
  * ⬆   ⬆
  * HH   mm
  * ⬇   ⬇
  *
  */
class TimePicker(val userId: UserId, val bot: Bot, val localization: Localization, originator: Interactional)(val actorSystem: ActorSystem) extends Conversation[LocalTime] with Localizable {

  private var mode: Mode = TimeFromMode

  entryPoint(configure)

  def configure: Step =
    monologue {
      case Msg(newMode: Mode, _) =>
        mode = newMode
        stay()
      case Msg(initialTime: LocalTime, _) =>
        goto(requestTime) using initialTime
    }

  def requestTime: Step =
    ask { initialDate =>
      val message = mode match {
        case TimeFromMode => lang.chooseTimeFrom
        case TimeToMode => lang.chooseTimeTo
      }
      bot.sendMessage(userId.source, message, inlineKeyboard = timeButtons(initialDate))
    } onReply {
      case Msg(Command(_, msg, Some(Tags.Done)), finalTime) =>
        val (message, updateTime) = mode match {
          case TimeFromMode =>
            val startOfTheWorkingDay = LocalTime.of(7, 0)
            val timeFrom = if (startOfTheWorkingDay.isBefore(LocalTime.now())) finalTime else startOfTheWorkingDay
            lang.timeFromIs(timeFrom) -> timeFrom
          case TimeToMode =>
            val timeTo = finalTime
            lang.timeToIs(timeTo) -> timeTo
        }
        bot.sendEditMessage(userId.source, msg.messageId, message)
        originator ! updateTime
        goto(configure) using null

      case Msg(Command(_, msg, Some(tag)), time) =>
        val modifiedTime = modifyTime(time, tag)
        bot.sendEditMessage(userId.source, msg.messageId, inlineKeyboard = timeButtons(modifiedTime))
        stay() using modifiedTime
    }

  private def modifyTime(time: LocalTime, tag: String) = {
    tag match {
      case Tags.HourInc => time.plusHours(1)
      case Tags.MinuteInc => time.plusMinutes(30)
      case Tags.HourDec => time.minusHours(1)
      case Tags.MinuteDec => time.minusMinutes(30)
    }
  }

  private def timeButtons(time: LocalTime) = {
    val hour = f"${time.getHour}%02d"
    val minute = f"${time.getMinute}%02d"

    createInlineKeyboard(Seq(
      Seq(Button("⬆", Tags.HourInc), Button("⬆", Tags.MinuteInc)),
      Seq(Button(hour), Button(minute)),
      Seq(Button("⬇", Tags.HourDec), Button("⬇", Tags.MinuteDec)),
      Seq(Button("Done", Tags.Done))
    ))
  }
}

object TimePicker {

  trait Mode

  object TimeFromMode extends Mode

  object TimeToMode extends Mode

  object Tags {
    val HourInc = "hour_inc"
    val MinuteInc = "minute_inc"
    val HourDec = "hour_dec"
    val MinuteDec = "minute_dec"
    val Done = "done"
  }

}

