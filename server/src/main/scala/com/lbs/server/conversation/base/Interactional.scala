package com.lbs.server.conversation.base

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContextExecutor

trait Interactional extends StrictLogging {

  private[base] object StartConversation

  private[base] object ContinueConversation

  private[base] object InitConversation

  protected def actorSystem: ActorSystem

  private[base] def initializeConversation(): Unit

  private[base] def executeCurrentStep(): Unit

  private[base] def makeStepTransition(any: Any): Unit

  protected val self: Interactional = this

  private var onDestroy: () => Unit = () => {}

  private def actorCreator: Actor = new Actor {
    private implicit val dispatcher: ExecutionContextExecutor = context.system.dispatcher

    override def receive: Receive = {
      case InitConversation => initializeConversation()
      case StartConversation | ContinueConversation => executeCurrentStep()
      case any => makeStepTransition(any)
    }

    override def preStart(): Unit = {
      initializeConversation()
    }
  }

  private[base] lazy val ref = actorSystem.actorOf(Props(actorCreator))

  def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit = {
    ref ! message
  }

  def init(): Unit = {
    ref ! InitConversation
  }

  def continue(): Unit = {
    ref ! ContinueConversation
  }

  def start(): Unit = {
    ref ! StartConversation
  }

  def restart(): Unit = {
    init()
    start()
  }

  protected def beforeDestroy(f: => Unit): Unit = {
    onDestroy = () => f
  }

  def destroy(): Unit = {
    onDestroy()
    ref ! PoisonPill
  }
}

