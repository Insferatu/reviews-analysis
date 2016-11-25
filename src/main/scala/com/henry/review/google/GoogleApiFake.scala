package com.henry.review.google

import com.henry.review.iterator.SentenceIterator

class GoogleApiFake extends IGoogleApi {
  def translate(inputLang: String, outputLang: String, text: String): String = {
    Thread.sleep(200)
    Seq.fill(text.split("\n").toIterator.flatMap(SentenceIterator.apply)
      .filterNot(_.isEmpty).size)("Salut Jean, comment vas tu? ").mkString
  }
}
