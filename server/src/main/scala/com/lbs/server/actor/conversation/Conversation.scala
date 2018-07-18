package com.lbs.server.actor.conversation

import akka.actor.Actor
import com.lbs.common.Logger
import com.lbs.server.actor.conversation.Conversation.{ContinueConversation, InitConversation, StartConversation}

import scala.util.control.NonFatal

trait Conversation[D] extends Actor with Domain[D] with Logger {
  private var currentData: D = _

  private var currentStep: Step = _

  private var startWithData: D = _

  private var startWithStep: Step = _

  private val defaultMsgHandler: MessageProcessorFn = {
    case Msg(any, data) =>
      warn(s"Unhandled message received in step '${currentStep.stepName}'. Message: [$any]. Data: [$data]")
      NextStep(currentStep, Some(data))
  }

  private var msgHandler: MessageProcessorFn = defaultMsgHandler

  private var runAfterInit: () => Unit = () => {}

  override def receive: Receive = {
    case InitConversation => init()
    case StartConversation | ContinueConversation => execute()
    case any => makeTransition(any)
  }

  def execute(): Unit = {
    try {
      currentStep match {
        case qa: Dialogue => qa.askFn(currentData)
        case Process(_, fn) =>
          val nextStep = fn(currentData)
          moveToNextStep(nextStep)
        case _ => //do nothing
      }
    } catch {
      case NonFatal(ex) => error("Step execution failed", ex)
    }
  }

  private def makeTransition(any: Any): Unit = {
    def handle[X](unit: X, fn: PartialFunction[X, NextStep], defaultFn: PartialFunction[X, NextStep]): Unit = {
      try {
        val nextStep = if (fn.isDefinedAt(unit)) fn(unit) else defaultFn(unit)
        moveToNextStep(nextStep)
      } catch {
        case NonFatal(ex) => error("Step transition failed", ex)
      }
    }

    currentStep match {
      case Dialogue(_, _, fn) =>
        val fact = Msg(any, currentData)
        handle(fact, fn, msgHandler)
      case Monologue(_, fn) =>
        val fact = Msg(any, currentData)
        handle(fact, fn, msgHandler)
      case _ => //do nothing
    }
  }

  private def moveToNextStep(nextStep: NextStep): Unit = {
    trace(s"Moving from step '${currentStep.stepName}' to step '${nextStep.step.stepName}'")
    currentStep = nextStep.step
    nextStep.data.foreach { data =>
      currentData = data
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

  protected def monologue(answerFn: MessageProcessorFn)(implicit functionName: sourcecode.Name): Monologue = Monologue(functionName.value, answerFn)

  protected def ask(askFn: D => Unit): Ask = Ask(askFn)

  protected def process(processFn: ProcessFn)(implicit functionName: sourcecode.Name): Process = Process(functionName.value, processFn)

  protected def end(): NextStep = NextStep(End)

  protected implicit class AskOps(ask: Ask) {
    def onReply(replyProcessorFn: MessageProcessorFn)(implicit functionName: sourcecode.Name): Dialogue = {
      Dialogue(functionName.value, ask.askFn, replyProcessorFn)
    }
  }

  protected def goto(step: Step): NextStep = {
    self ! ContinueConversation
    NextStep(step)
  }

  protected def stay(): NextStep = NextStep(currentStep)

  protected def whenUnhandledMsg(receiveMsgFn: MessageProcessorFn): Unit = {
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