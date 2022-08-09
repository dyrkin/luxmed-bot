package com.lbs.bot.model

object Button {
  def apply(label: String, id: Long) = new TaggedButton(label, id.toString)

  def apply(label: String, id: String) = new TaggedButton(label, id)

  def apply(label: String) = new LabeledButton(label)
}

trait Button

class TaggedButton(val label: String, val tag: String) extends Button

class LabeledButton(val label: String) extends Button
