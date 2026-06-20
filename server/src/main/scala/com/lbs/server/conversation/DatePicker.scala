package com.lbs.server.conversation

import com.lbs.bot.*
import com.lbs.bot.model.Button
import com.lbs.server.conversation.DatePicker.*
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.base.{Conversation, Interactional}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.util.DateTimeUtil.*
import com.lbs.server.util.MessageExtractors.{CallbackCommand, TextCommand}
import org.apache.pekko.actor.ActorSystem

import java.time.format.TextStyle
import java.time.{LocalDate, LocalDateTime, LocalTime, MonthDay}
import scala.util.control.NonFatal

/**
  * Date picker Inline Keyboard
  *
  * ⬆   ⬆    ⬆
  * dd   MM   yyyy
  * ⬇   ⬇    ⬇
  */
class DatePicker(val userId: UserId, val bot: Bot, val localization: Localization, originator: Interactional)(
  val actorSystem: ActorSystem
) extends Conversation[LocalDateTime]
    with Localizable {

  private var mode: Mode = DateFromMode

  entryPoint(configure)

  def configure: Step =
    monologue {
      case Msg(newMode: Mode, _) =>
        mode = newMode
        stay()
      case Msg(initialDate: LocalDateTime, _) =>
        goto(requestDate).using(initialDate)
    }

  def requestDate: Step =
    ask { initialDate =>
      val message = mode match {
        case DateFromMode => lang.chooseDateFrom(initialDate)
        case DateToMode   => lang.chooseDateTo(initialDate)
      }
      bot.sendMessage(userId.source, message, inlineKeyboard = dateButtons(initialDate))
    } onReply {
      case Msg(cmd @ CallbackCommand(QuickRange(rangeFactory)), _) if mode == DateFromMode =>
        val selectedRange = rangeFactory()
        val normalizedRange = range(selectedRange.from, selectedRange.to)
        bot.sendEditMessage(userId.source, cmd.message.messageId, lang.dateRangeIs(normalizedRange.from, normalizedRange.to))
        originator ! normalizedRange
        end()
      case Msg(cmd @ CallbackCommand(Tags.Done), finalDate) =>
        val (message, updatedDate) = mode match {
          case DateFromMode =>
            val dateFrom = normalizeDateFrom(finalDate)
            lang.dateFromIs(dateFrom) -> dateFrom
          case DateToMode =>
            val dateTo = normalizeDateTo(finalDate)
            lang.dateToIs(dateTo) -> dateTo
        }
        bot.sendEditMessage(userId.source, cmd.message.messageId, message)
        originator ! updatedDate
        end()
      case Msg(TextCommand(text), finalDate) =>
        try {
          parseTextDate(text, finalDate) match {
            case ParsedDateRange(range) if mode == DateFromMode =>
              bot.sendMessage(userId.source, lang.dateRangeIs(range.from, range.to))
              originator ! range
            case ParsedDate(date) =>
              val message = mode match {
                case DateFromMode =>
                  lang.dateFromIs(normalizeDateFrom(date))
                case DateToMode =>
                  lang.dateToIs(normalizeDateTo(date))
              }
              bot.sendMessage(userId.source, message)
              originator ! (mode match {
                case DateFromMode => normalizeDateFrom(date)
                case DateToMode   => normalizeDateTo(date)
              })
            case ParsedDateRange(_) =>
              throw IllegalArgumentException("Date range is allowed only for date from")
          }
          end()
        } catch {
          case NonFatal(ex) =>
            logger.error("Unable to parse date", ex)
            bot.sendMessage(userId.source, lang.incorrectDateFormat)
            goto(requestDate)
        }
      case Msg(cmd @ CallbackCommand(tag), date) =>
        val modifiedDate = modifyDate(date, tag)
        bot.sendEditMessage(userId.source, cmd.message.messageId, inlineKeyboard = dateButtons(modifiedDate))
        stay().using(modifiedDate)
    }

  private def modifyDate(date: LocalDateTime, tag: String) = {
    val dateModifier = tag match {
      case Tags.DayInc   => date.plusDays
      case Tags.MonthInc => date.plusMonths
      case Tags.YearInc  => date.plusYears
      case Tags.DayDec   => date.minusDays
      case Tags.MonthDec => date.minusMonths
      case Tags.YearDec  => date.minusYears
    }
    dateModifier(1)
  }

  private def dateButtons(date: LocalDateTime) = {
    val day = date.getDayOfMonth.toString
    val dayOfWeek = date.getDayOfWeek.getDisplayName(TextStyle.SHORT, lang.locale)
    val month = date.getMonth.getDisplayName(TextStyle.SHORT, lang.locale)
    val year = date.getYear.toString

    val quickRangeButtons =
      if (mode == DateFromMode)
        Seq(
          Seq(Button(lang.quickRangeToday, Tags.Today), Button(lang.quickRangeTomorrow, Tags.Tomorrow)),
          Seq(Button(lang.quickRangeNext7Days, Tags.Next7Days), Button(lang.quickRangeNext14Days, Tags.Next14Days))
        )
      else Seq.empty

    createInlineKeyboard(
      quickRangeButtons ++ Seq(
        Seq(Button("⬆", Tags.DayInc), Button("⬆", Tags.MonthInc), Button("⬆", Tags.YearInc)),
        Seq(Button(s"$day ($dayOfWeek)"), Button(month), Button(year)),
        Seq(Button("⬇", Tags.DayDec), Button("⬇", Tags.MonthDec), Button("⬇", Tags.YearDec)),
        Seq(Button(lang.done, Tags.Done))
      )
    )
  }

  private def normalizeDateFrom(date: LocalDateTime): LocalDateTime = {
    val now = LocalDateTime.now()
    val startOfTheDay = date.`with`(LocalTime.MIN)
    if (startOfTheDay.isBefore(now) && date.toLocalDate == now.toLocalDate) now
    else if (startOfTheDay.isBefore(now)) date
    else startOfTheDay
  }

  private def normalizeDateTo(date: LocalDateTime): LocalDateTime =
    date.`with`(LocalTime.MAX)

  private def range(from: LocalDateTime, to: LocalDateTime): DateRange = {
    val (first, second) =
      if (from.toLocalDate.isAfter(to.toLocalDate)) to -> from
      else from -> to
    val dateFrom = normalizeDateFrom(first)
    val dateTo = normalizeDateTo(second)
    DateRange(dateFrom, if (dateTo.isBefore(dateFrom)) dateFrom else dateTo)
  }

  private def parseTextDate(text: String, initialDate: LocalDateTime): ParsedTextDate = {
    val tokens = DateToken.findAllIn(text).toSeq
    tokens match {
      case first +: second +: _ =>
        val from = parseDateToken(first, initialDate.toLocalDate)
        val to = parseDateToken(second, initialDate.toLocalDate)
        ParsedDateRange(range(from, to))
      case Seq(single) =>
        ParsedDate(parseDateToken(single, initialDate.toLocalDate))
      case _ =>
        throw IllegalArgumentException(s"Unable to parse date '$text'")
    }
  }

  private def parseDateToken(token: String, baseDate: LocalDate): LocalDateTime = {
    val date =
      if (token.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
        val parts = token.split("-").map(_.toInt)
        LocalDate.of(parts(0), parts(1), parts(2))
      } else {
        val parts = token.split("-").map(_.toInt)
        val candidate = MonthDay.of(parts(1), parts(0)).atYear(baseDate.getYear)
        if (candidate.isBefore(baseDate)) candidate.plusYears(1) else candidate
      }
    date.atStartOfDay()
  }
}

object DatePicker {

  case class DateRange(from: LocalDateTime, to: LocalDateTime)

  private val DateToken = "\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}-\\d{1,2}".r

  private trait ParsedTextDate

  private case class ParsedDate(date: LocalDateTime) extends ParsedTextDate

  private case class ParsedDateRange(range: DateRange) extends ParsedTextDate

  trait Mode

  object DateFromMode extends Mode

  object DateToMode extends Mode

  object Tags {
    val Today = "today"
    val Tomorrow = "tomorrow"
    val Next7Days = "next_7_days"
    val Next14Days = "next_14_days"
    val DayInc = "day_inc"
    val MonthInc = "month_inc"
    val YearInc = "year_inc"
    val DayDec = "day_dec"
    val MonthDec = "month_dec"
    val YearDec = "year_dec"
    val Done = "done"
  }

  object QuickRange {
    def unapply(tag: String): Option[() => DateRange] =
      tag match {
        case Tags.Today =>
          Some(() => DateRange(LocalDateTime.now(), LocalDateTime.now()))
        case Tags.Tomorrow =>
          Some(() => {
            val tomorrow = LocalDate.now().plusDays(1).atStartOfDay()
            DateRange(tomorrow, tomorrow)
          })
        case Tags.Next7Days =>
          Some(() => DateRange(LocalDateTime.now(), LocalDateTime.now().plusDays(7)))
        case Tags.Next14Days =>
          Some(() => DateRange(LocalDateTime.now(), LocalDateTime.now().plusDays(14)))
        case _ => None
      }
  }

}
