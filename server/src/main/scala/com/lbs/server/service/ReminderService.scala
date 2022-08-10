package com.lbs.server.service

import com.lbs.bot.Bot
import com.lbs.bot.model.{MessageSource, MessageSourceSystem}
import com.lbs.common.Scheduler
import com.lbs.server.lang.Localization
import com.lbs.server.repository.model._
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.time.{Instant, LocalDateTime, ZoneId}
import javax.annotation.PostConstruct
import scala.concurrent.duration._

@Service
class ReminderService extends StrictLogging {

  @Autowired
  private var bot: Bot = _
  @Autowired
  private var dataService: DataService = _
  @Autowired
  private var localization: Localization = _

  private val dbChecker = new Scheduler(1)

  private def deactivateReminder(accountId: JLong, reminderId: JLong): Unit = {
    dataService.findReminder(accountId, reminderId).foreach { reminder =>
      reminder.active = false
      dataService.saveReminder(reminder)
    }
  }

  def remindUserAboutAppointment(reminder: Reminder): Unit = {
    deactivateReminder(reminder.accountId, reminder.recordId)

    val messages = lang(reminder.userId)
    val message = messages.youHaveAppointmentAt(reminder)

    bot.sendMessage(reminder.source, message)
  }

  private def checkReminders(): Unit = {
    logger.debug(s"Looking for active reminders")

    val activeReminders = dataService.getActiveReminders
    logger.debug(s"Found [${activeReminders.size}] active reminders")
    val now = LocalDateTime.now()

    activeReminders.foreach {
      case reminder if reminder.remindAt.isAfter(now) =>
        logger.debug(s"Notifying user [${reminder.userId}] about appointment at [${reminder.appointmentTime}]")
        remindUserAboutAppointment(reminder)
    }
  }

  private def initializeDbChecker(): Unit = {
    dbChecker.schedule(checkReminders(), 20.seconds)
  }

  def createInactiveReminder(reminder: Reminder): Reminder = {
    reminder.active = false
    dataService.saveReminder(reminder)
  }

  def activateReminder(accountId: Long, reminderId: Long, timeMillis: Long): Unit = {
    val time = Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()).toLocalDateTime
    val reminderMaybe = dataService.findReminder(accountId, reminderId)

    reminderMaybe.foreach { reminder =>
      if (reminder.appointmentTime.isBefore(time)) {
        reminder.active = true
        reminder.remindAt = time
        dataService.saveReminder(reminder)
      } else {
        val messages = lang(reminder.userId)
        val message = messages.appointmentIsOutdated(reminder.appointmentTime)
        bot.sendMessage(reminder.source, message)
      }
    }
  }

  implicit class ReminderAsSource(reminder: Reminder) {
    def source: MessageSource = MessageSource(MessageSourceSystem(reminder.sourceSystemId), reminder.chatId)
  }

  private def lang(userId: Long) = localization.lang(userId)

  @PostConstruct
  private def initialize(): Unit = {
    initializeDbChecker()
  }
}
