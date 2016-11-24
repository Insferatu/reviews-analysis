package com.henry.review

import java.util.concurrent.atomic.AtomicInteger

import com.henry.review.iterator.SentenceIterator

class GoogleAPIFake {
  private val counter = new AtomicInteger
  def translate(inputLang: String, outputLang: String, text: String): String = {
    println(counter.incrementAndGet())
    Thread.sleep(1000)
    counter.decrementAndGet()
    Seq.fill(SentenceIterator(text).filterNot(_.isEmpty).size)("Salut Jean, comment vas tu? ").mkString
  }
}
