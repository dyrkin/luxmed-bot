
package com.lbs.bot.telegram

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.api.{AkkaTelegramBot, Polling, RequestHandler}
import com.bot4s.telegram.clients.AkkaHttpClient
import com.bot4s.telegram.marshalling._
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models.{InlineKeyboardMarkup, InputFile, Message}
import com.lbs.common.Logger
import io.circe.{Decoder, Encoder, Json}

import scala.concurrent.Future

class TelegramClient(onReceive: TelegramEvent => Unit, botToken: String) extends AkkaTelegramBot with Polling with Commands with Callbacks with Logger {

  val client: RequestHandler = new AkkaHttpClient(botToken) {

    import AkkaHttpMarshalling._

    private val http = Http()
    private val apiBaseUrl = s"https://api.telegram.org/bot$botToken/"

    override def sendRequest[R, T <: Request[_]](request: T)(implicit encT: Encoder[T], decR: Decoder[R]): Future[R] = {
      Marshal(request).to[RequestEntity]
        .map(re => HttpRequest(HttpMethods.POST, Uri(apiBaseUrl + request.methodName), entity = re))
        .flatMap(http.singleRequest(_))
        .flatMap(r => {
          request match {
            case _: GetUpdates =>
              Unmarshal(r.entity).to[Json].flatMap { json =>
                val patchedJson = json.mapObject { jsonObject =>
                  jsonObject.mapValues { value =>
                    if (value.isArray) {
                      value.mapArray { update =>
                        update.filterNot(_.findAllByKey("myChatMember").nonEmpty)
                      }
                    } else {
                      value
                    }
                  }
                }

                Unmarshal(HttpEntity(ContentTypes.`application/json`, patchedJson.noSpaces)).to[Response[R]]
              }
            case _ =>
              Unmarshal(r.entity).to[Response[R]]
          }
        })
        .map(processApiResponse[R])
    }
  }

  def sendMessage(chatId: Long, text: String): Future[Message] =
    loggingRequest(SendMessage(chatId, text, parseMode = Some(ParseMode.HTML)))

  def sendMessage(chatId: Long, text: String, replyMarkup: Option[InlineKeyboardMarkup] = None): Future[Message] =
    loggingRequest(SendMessage(chatId, text, parseMode = Some(ParseMode.HTML), replyMarkup = replyMarkup))

  def sendEditMessage(chatId: Long, messageId: Int, replyMarkup: Option[InlineKeyboardMarkup]): Future[Either[Boolean, Message]] =
    loggingRequest(EditMessageReplyMarkup(Some(chatId), Some(messageId), replyMarkup = replyMarkup))

  def sendEditMessage(chatId: Long, messageId: Int, text: String, replyMarkup: Option[InlineKeyboardMarkup] = None): Future[Either[Boolean, Message]] =
    loggingRequest(EditMessageText(Some(chatId), Some(messageId), text = text, parseMode = Some(ParseMode.HTML), replyMarkup = replyMarkup))

  def sendFile(chatId: Long, filename: String, contents: Array[Byte], caption: Option[String] = None): Future[Message] =
    loggingRequest(SendDocument(chatId, InputFile(filename, contents), caption))

  private def loggingRequest[R: Manifest](req: Request[R]): Future[R] = {
    debug(s"Sending telegram request: $req")
    request(req)
  }


  override def receiveMessage(msg: Message): Unit = {
    debug(s"Received telegram message: $msg")
    onReceive(TelegramEvent(msg, None))
  }

  onCallbackWithTag(TagPrefix) { implicit cbq =>
    debug(s"Received telegram callback: $cbq")
    ackCallback()
    for {
      data <- cbq.data.map(_.stripPrefix(TagPrefix))
      msg <- cbq.message
    } {
      onReceive(TelegramEvent(msg, Some(data)))
    }
  }
}