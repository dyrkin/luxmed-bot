package com.lbs.server.service

import com.lbs.api.exception.SessionExpiredException
import com.lbs.api.http.Session
import com.lbs.common.ParametrizedLock
import com.lbs.server.ThrowableOr
import com.lbs.server.exception.UserNotFoundException
import com.typesafe.scalalogging.StrictLogging

import scala.collection.mutable

trait SessionSupport extends StrictLogging {

  def fullLogin(username: String, password: String, secondAttempt: Boolean = false): ThrowableOr[Session]

  protected def dataService: DataService

  private val sessions = mutable.Map[Long, Session]()

  private val lock = new ParametrizedLock[Long]

  protected def withSession[T](accountId: Long)(fn: Session => ThrowableOr[T]): ThrowableOr[T] =
    lock.obtainLock(accountId).synchronized {

      def auth: ThrowableOr[Session] = {
        val credentialsMaybe = dataService.getCredentials(accountId)
        credentialsMaybe match {
          case Some(credentials) => fullLogin(credentials.username, credentials.password)
          case None              => Left(UserNotFoundException(accountId))
        }
      }

      def getSession: ThrowableOr[Session] = {
        sessions.get(accountId) match {
          case Some(sess) => Right(sess)
          case None =>
            for {
              session <- auth
            } yield {
              sessions.put(accountId, session)
              session
            }
        }
      }

      def doApiCall = {
        for {
          session <- getSession
          result <- fn(session)
        } yield result
      }

      for {
        result <- doApiCall match {
          case Left(_: SessionExpiredException) =>
            logger.debug(s"The session for account [#$accountId] has expired. Try to relogin")
            sessions.remove(accountId)
            doApiCall
          case another =>
            logger.debug(s"Call to remote api function has completed with result:\n$another")
            another
        }
      } yield result
    }

  def addSession(accountId: Long, session: Session): Unit =
    lock.obtainLock(accountId).synchronized {
      sessions.put(accountId, session)
    }
}
