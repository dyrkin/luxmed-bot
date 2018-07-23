package com.lbs.server.conversation.base

trait Domain[D] {
  protected type AskFn = D => Unit

  protected type MessageProcessorFn = PartialFunction[Msg, NextStep]

  protected type ProcessFn = D => NextStep

  protected case class Msg(message: Any, data: D)

  sealed trait Step {
    def name: String
  }

  private[base] object End extends Step {
    override val name: String = "end"
  }

  protected case class Process(name: String, processFn: ProcessFn) extends Step

  protected case class Dialogue(name: String, askFn: AskFn, replyProcessorFn: MessageProcessorFn) extends Step

  protected case class Monologue(name: String, replyProcessorFn: MessageProcessorFn) extends Step

  private[base] case class NextStep(step: Step, data: Option[D] = None)

  private[base] case class Ask(askFn: AskFn)

  protected implicit class NextStepOps(nextStep: NextStep) {
    def using(data: D): NextStep = {
      nextStep.copy(data = Some(data))
    }
  }

}
