package com.lbs.server.conversation.base

import com.lbs.common.Logger

import scala.util.control.NonFatal

trait Conversation[D] extends Domain[D] with Interactional with Logger {

  private var currentData: D = _

  private var currentStep: Step = _

  private var initialData: D = _

  private var initialStep: Step = _

  private val defaultMsgHandler: MessageProcessorFn = {
    case Msg(any, data) =>
      warn(s"Unhandled message received in step '${currentStep.name}'. Message: [$any]. Data: [$data]")
      NextStep(currentStep, Some(data))
  }

  private var msgHandler: MessageProcessorFn = defaultMsgHandler

  private[base] def executeCurrentStep(): Unit = {
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

  private[base] def makeStepTransition(any: Any): Unit = {
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
    trace(s"Moving from step '${currentStep.name}' to step '${nextStep.step.name}'")
    currentStep = nextStep.step
    nextStep.data.foreach { data =>
      currentData = data
    }
  }

  private[base] def initializeConversation(): Unit = {
    require(initialStep != null, "Entry point must be defined")
    currentStep = initialStep
    currentData = initialData
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
    continue()
    NextStep(step)
  }

  protected def stay(): NextStep = NextStep(currentStep)

  protected def whenUnhandledMsg(receiveMsgFn: MessageProcessorFn): Unit = {
    msgHandler = receiveMsgFn orElse defaultMsgHandler
  }

  protected def entryPoint(step: Step, data: D): Unit = {
    initialStep = step
    initialData = data
  }

  protected def entryPoint(step: Step): Unit = {
    entryPoint(step, null.asInstanceOf[D])
  }
}

