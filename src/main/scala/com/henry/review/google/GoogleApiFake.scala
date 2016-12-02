package com.henry.review.google

import com.henry.review.iterator.SentenceIterator

class GoogleApiFake(responseTimeMs: Long = 200) extends IGoogleApi {
  def translate(inputLang: String, outputLang: String, text: String): String = {
    Thread.sleep(responseTimeMs)
    Seq.fill(text.split("\n").toIterator.flatMap(SentenceIterator.apply)
      .filterNot(_.isEmpty).size)("Salut Jean, comment vas tu? ").mkString
  }
}
