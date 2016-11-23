package com.henry.review.iterator

import java.text.BreakIterator
import java.util.Locale

class SentenceIterator(text: String) extends Iterator[String] {
  private val breakIterator = BreakIterator.getSentenceInstance(Locale.US)
  breakIterator.setText(text)

  private var start = breakIterator.first
  private var end = breakIterator.next

  override def hasNext = BreakIterator.DONE != end

  override def next(): String = {
    val result = text.substring(start, end)
    start = end
    end = breakIterator.next
    result
  }
}

object SentenceIterator {
  def apply(text: String): SentenceIterator = new SentenceIterator(text)
}