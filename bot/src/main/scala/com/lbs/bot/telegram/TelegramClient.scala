package com.lbs.bot.telegram

import cats.implicits.toFunctorOps
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot as TelegramBoT}
import com.bot4s.telegram.methods.*
import com.bot4s.telegram.methods.{Request as BotRequest}
import com.bot4s.telegram.models.{InlineKeyboardMarkup, InputFile, Message}
import com.typesafe.scalalogging.StrictLogging
import com.bot4s.telegram.marshalling.*
import io.circe.{Decoder, Encoder}
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend

import scala.concurrent.{ExecutionContext, Future}

class TelegramClient(onReceive: TelegramEvent => Unit, botToken: String)
    extends TelegramBoT
    with Polling
    with Commands[Future]
    with Callbacks[Future]
    with StrictLogging {

  protected given ExecutionContext = ExecutionContext.global

  private given SttpBackend[Future, Any] = AsyncHttpClientFutureBackend()

  override val client: RequestHandler[Future] =
    FutureSttpClient(botToken)

  def sendMessage(chatId: Long, text: String): Future[Message] =
    loggingRequest(SendMessage(chatId, text, parseMode = Some(ParseMode.HTML)))

  def sendMessage(chatId: Long, text: String, replyMarkup: Option[InlineKeyboardMarkup] = None): Future[Message] =
    loggingRequest(SendMessage(chatId, text, parseMode = Some(ParseMode.HTML), replyMarkup = replyMarkup))

  def sendEditMessage(
    chatId: Long,
    messageId: Int,
    replyMarkup: Option[InlineKeyboardMarkup]
  ): Future[Either[Boolean, Message]] =
    loggingRequest(EditMessageReplyMarkup(Some(chatId), Some(messageId), replyMarkup = replyMarkup))

  def sendEditMessage(
    chatId: Long,
    messageId: Int,
    text: String,
    replyMarkup: Option[InlineKeyboardMarkup] = None
  ): Future[Either[Boolean, Message]] =
    loggingRequest(
      EditMessageText(
        Some(chatId),
        Some(messageId),
        text = text,
        parseMode = Some(ParseMode.HTML),
        replyMarkup = replyMarkup
      )
    )

  def sendFile(chatId: Long, filename: String, contents: Array[Byte], caption: Option[String] = None): Future[Message] =
    loggingRequest(SendDocument(chatId, InputFile(filename, contents), caption = caption))

  private def loggingRequest[T <: BotRequest](req: T)(using e: Encoder[T], d: Decoder[req.Response]): Future[req.Response] = {
    logger.debug(s"Sending telegram request: $req")
    client(req)
  }

  override def receiveMessage(msg: Message): Future[Unit] = {
    logger.debug(s"Received telegram message: $msg")
    Future.successful(onReceive(TelegramEvent(msg, None)))
  }

  onCallbackWithTag(TagPrefix) { implicit cbq =>
    logger.debug(s"Received telegram callback: $cbq")
    val ack = ackCallback()
    val maybeOnReceive = for {
      data <- cbq.data.map(_.stripPrefix(TagPrefix))
      msg  <- cbq.message
    } yield onReceive(TelegramEvent(msg, Some(data)))
    ack.zip(Future.successful(maybeOnReceive)).void
  }
}
