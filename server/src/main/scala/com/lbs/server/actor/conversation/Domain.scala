package com.lbs.server.actor.conversation

trait Domain[D] {
  protected type AskFn = D => Unit

  protected type MessageProcessorFn = PartialFunction[Msg, NextStep]

  protected type ProcessFn = D => NextStep

  protected case class Msg(message: Any, data: D)

  sealed trait Step {
    def stepName: String
  }

  private[conversation] object End extends Step {
    val stepName: String = "end"
  }

  protected case class Process(stepName: String, processFn: ProcessFn) extends Step

  protected case class Dialogue(stepName: String, askFn: AskFn, replyProcessorFn: MessageProcessorFn) extends Step

  protected case class Monologue(stepName: String, replyProcessorFn: MessageProcessorFn) extends Step

  private[conversation] case class NextStep(step: Step, data: Option[D] = None)

  private[conversation] case class Ask(askFn: AskFn)

  protected implicit class NextStepOps(nextStep: NextStep) {
    def using(data: D): NextStep = {
      nextStep.copy(data = Some(data))
    }
  }

}
