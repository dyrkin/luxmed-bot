package com.lbs.server.conversation.base

private[conversation] object DomainTypes {
  sealed trait Step { def name: String }
  object End extends Step { override val name: String = "end" }

  case class Msg[D](message: Any, data: D)
  case class NextStep[D](step: Step, data: Option[D] = None)
  case class Ask[D](askFn: D => Unit)
  case class Process[D](name: String, processFn: D => NextStep[D]) extends Step
  case class Dialogue[D](name: String, askFn: D => Unit, replyProcessorFn: PartialFunction[Msg[D], NextStep[D]]) extends Step
  case class Monologue[D](name: String, replyProcessorFn: PartialFunction[Msg[D], NextStep[D]]) extends Step
}

trait Domain[D] {
  export DomainTypes.*

  protected type AskFn              = D => Unit
  protected type MessageProcessorFn = PartialFunction[Msg[D], NextStep[D]]
  protected type ProcessFn          = D => NextStep[D]

  extension (nextStep: NextStep[D])
    protected def using(data: D): NextStep[D] = nextStep.copy(data = Some(data))
}
