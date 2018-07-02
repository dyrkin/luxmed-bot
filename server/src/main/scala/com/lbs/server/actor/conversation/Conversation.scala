package com.lbs.server.actor.conversation

import akka.actor.Actor
import com.lbs.common.Logger
import com.lbs.server.actor.conversation.Conversation.{ContinueConversation, InitConversation, StartConversation}

trait Conversation[D] extends Actor with Domain[D] with Logger {
  private var currentData: D = _

  private var currentStep: Step = _

  private var startWithData: D = _

  private var startWithStep: Step = _

  private val defaultMsgHandler: AnswerFn = {
    case Msg(any, data) =>
      debug(s"Unhandled fact message received. [$any, $data]")
      NextStep(currentStep, Some(data))
  }

  private var msgHandler: AnswerFn = defaultMsgHandler

  private var runAfterInit: () => Unit = () => {}

  override def receive: Receive = {
    case InitConversation => init()
    case StartConversation | ContinueConversation =>
      currentStep match {
        case qa: QuestionAnswer => qa.question.questionFn(currentData)
        case InternalConfiguration(fn) =>
          val nextStep = fn(currentData)
          moveToNextStep(nextStep)
        case _ => //do nothing
      }
    case any => makeTransition(any)
  }

  private def moveToNextStep(nextStep: NextStep): Unit = {
    currentStep = nextStep.step
    nextStep.data.foreach { data =>
      currentData = data
    }
  }

  private def makeTransition(any: Any): Unit = {
    def handle[X](unit: X, fn: PartialFunction[X, NextStep], defaultFn: PartialFunction[X, NextStep]): Unit = {
      val nextStep = if (fn.isDefinedAt(unit)) fn(unit) else defaultFn(unit)
      moveToNextStep(nextStep)
    }

    currentStep match {
      case ExternalConfiguration(fn) =>
        val conf = Msg(any, currentData)
        handle(conf, fn, msgHandler)
      case QuestionAnswer(_, Answer(fn)) =>
        val fact = Msg(any, currentData)
        handle(fact, fn, msgHandler)
      case Monologue(fn) =>
        val fact = Msg(any, currentData)
        handle(fact, fn, msgHandler)
      case _ => //do nothing
    }
  }

  private def init(): Unit = {
    require(startWithStep != null, "Entry point must be defined")
    currentStep = startWithStep
    currentData = startWithData
    runAfterInit()
  }

  override def preStart(): Unit = {
    init()
  }

  protected def monologue(answerFn: AnswerFn): Monologue = Monologue(answerFn)

  protected def question(questionFn: D => Unit): Question = Question(questionFn)

  protected def externalConfig(receiveConfFunction: ExternalConfigFn): ExternalConfiguration = ExternalConfiguration(receiveConfFunction)

  protected def internalConfig(receiveConfFunction: InternalConfigFn): InternalConfiguration = InternalConfiguration(receiveConfFunction)

  protected def end(): NextStep = NextStep(End)

  protected def goto(step: Step): NextStep = {
    self ! ContinueConversation
    NextStep(step)
  }

  protected def stay(): NextStep = NextStep(currentStep)

  protected def whenUnhandledMsg(receiveMsgFn: AnswerFn): Unit = {
    msgHandler = receiveMsgFn orElse defaultMsgHandler
  }

  protected def afterInit(fn: => Unit): Unit = {
    runAfterInit = () => fn
  }

  protected def entryPoint(step: Step, data: D): Unit = {
    startWithStep = step
    startWithData = data
  }

  protected def entryPoint(step: Step): Unit = {
    entryPoint(step, null.asInstanceOf[D])
  }
}

object Conversation {

  object StartConversation

  object ContinueConversation

  object InitConversation

}