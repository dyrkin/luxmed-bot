package com.lbs.server.actor.conversation

trait Domain[D] {
  protected type QuestionFn = D => Unit

  protected type AnswerFn = PartialFunction[Msg, NextStep]

  protected type ExternalConfigFn = AnswerFn

  protected type InternalConfigFn = D => NextStep

  protected case class Msg(message: Any, data: D)

  sealed trait Step

  private[conversation] object End extends Step

  protected case class ExternalConfiguration(configFn: ExternalConfigFn) extends Step

  protected case class InternalConfiguration(configFn: InternalConfigFn) extends Step

  protected case class QuestionAnswer(question: Question, answer: Answer) extends Step

  protected case class Monologue(answerFn: AnswerFn) extends Step

  private[conversation] case class NextStep(step: Step, data: Option[D] = None)

  private[conversation] case class Question(questionFn: QuestionFn)

  private[conversation] case class Answer(answerFn: AnswerFn)

  protected implicit class RichQuestion(question: Question) {
    def answer(answerFn: AnswerFn): QuestionAnswer = QuestionAnswer(question, Answer(answerFn))
  }

  protected implicit class NextStepOps(nextStep: NextStep) {
    def using(data: D): NextStep = {
      nextStep.copy(data = Some(data))
    }
  }

}
