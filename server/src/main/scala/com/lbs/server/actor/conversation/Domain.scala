package com.lbs.server.actor.conversation

trait Domain[D] {
  protected type AskFn = D => Unit

  protected type MessageProcessorFn = PartialFunction[Msg, NextStep]

  protected type ProcessFn = D => NextStep

  protected case class Msg(message: Any, data: D)

  sealed trait Step

  private[conversation] object End extends Step

  protected case class Process(processFn: ProcessFn) extends Step

  protected case class Dialogue(askFn: AskFn, replyProcessorFn: MessageProcessorFn) extends Step

  protected case class Monologue(replyProcessorFn: MessageProcessorFn) extends Step

  private[conversation] case class NextStep(step: Step, data: Option[D] = None)

  private[conversation] case class Ask(askFn: AskFn)

  protected implicit class RichQuestion(ask: Ask) {
    def onReply(replyProcessorFn: MessageProcessorFn): Dialogue = Dialogue(ask.askFn, replyProcessorFn)
  }

  protected implicit class NextStepOps(nextStep: NextStep) {
    def using(data: D): NextStep = {
      nextStep.copy(data = Some(data))
    }
  }

}
