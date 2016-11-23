package com.henry.review.iterator

import org.scalatest.{FlatSpec, Matchers}

class WordIteratorSpec extends FlatSpec with Matchers {

  it should "find all words in the text correctly" in {
    val sampleText =
      "!True; and therefore women, 24 being the weaker vessels, " +
      "are ever thrust to the wall: therefore I will push " +
      "Montague's men from the wall, and thrust his maids " +
      "to the wall"

    val expectedWordsInOrder = Seq(
      "True", "and", "therefore", "women", "being", "the", "weaker", "vessels", "are", "ever", "thrust",
      "to", "the", "wall", "therefore", "I", "will", "push", "Montague's", "men", "from", "the", "wall",
      "and", "thrust", "his", "maids", "to", "the", "wall"
    )

    WordIterator(sampleText).toSeq should be(expectedWordsInOrder)
  }

}