
package com.lbs.server.conversation

import java.time.LocalTime

import akka.actor.ActorSystem
import com.lbs.bot.model.Button
import com.lbs.bot.{Bot, _}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.TimePicker.{Mode, Tags, TimeFromMode, TimeToMode}
import com.lbs.server.conversation.base.{Conversation, Interactional}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.util.DateTimeUtil.applyHourMinute
import com.lbs.server.util.MessageExtractors.{CallbackCommand, TextCommand}

import scala.util.control.NonFatal

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
    ask { initialTime =>
      val message = mode match {
        case TimeFromMode => lang.chooseTimeFrom(initialTime)
        case TimeToMode => lang.chooseTimeTo(initialTime)
      }
      bot.sendMessage(userId.source, message, inlineKeyboard = timeButtons(initialTime))
    } onReply {
      case Msg(cmd@CallbackCommand(Tags.Done), selectedTime) =>
        val message = mode match {
          case TimeFromMode =>
            lang.timeFromIs(selectedTime)
          case TimeToMode =>
            lang.timeToIs(selectedTime)
        }
        bot.sendEditMessage(userId.source, cmd.message.messageId, message)
        originator ! selectedTime
        end()
      case Msg(TextCommand(hourMinute), _) =>
        try {
          val time = applyHourMinute(hourMinute)
          val message = mode match {
            case TimeFromMode =>
              lang.timeFromIs(time)
            case TimeToMode =>
              lang.timeToIs(time)
          }
          bot.sendMessage(userId.source, message)
          originator ! time
          end()
        } catch {
          case NonFatal(ex) =>
            error("Unable to parse time", ex)
            bot.sendMessage(userId.source, "Incorrect time. Please use format HH:mm")
            goto(requestTime)
        }
      case Msg(cmd@CallbackCommand(tag), time) =>
        val modifiedTime = modifyTime(time, tag)
        bot.sendEditMessage(userId.source, cmd.message.messageId, inlineKeyboard = timeButtons(modifiedTime))
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

