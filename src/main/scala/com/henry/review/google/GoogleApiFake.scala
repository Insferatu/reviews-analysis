package com.henry.review.google

import java.util.concurrent.atomic.AtomicInteger

import com.henry.review.iterator.SentenceIterator

class GoogleApiFake extends IGoogleApi {
  private val counter = new AtomicInteger
  def translate(inputLang: String, outputLang: String, text: String): String = {
    println(counter.incrementAndGet())
    Thread.sleep(200)
    counter.decrementAndGet()
    Seq.fill(text.split("\n").toIterator.flatMap(SentenceIterator.apply)
      .filterNot(_.isEmpty).size)("Salut Jean, comment vas tu? ").mkString
  }
}
