package com.henry.review.iterator

class WordIterator(text: String) extends Iterator[String] {
  val allMatches = "[a-zA-Z][\\w']*".r.findAllMatchIn(text)

  override def hasNext = allMatches.hasNext

  override def next() = allMatches.next().matched
}

object WordIterator {
  def apply(text: String): WordIterator = new WordIterator(text)
}
