package com.henry.review.iterator

import org.scalatest.{FlatSpec, Matchers}

class SentenceIteratorSpec extends FlatSpec with Matchers {

  behavior of "SentenceIterator"

  it should "find all sentences in the text correctly" in {
    val sampleText =
      "The gods, lying beside their nectar on 'Lympus and peeping over " +
      "the edge of the cliff, perceive a difference in cities. Although " +
      "it would seem that to their vision towns must appear as large " +
      "or small ant-hills without special characteristics, yet it is " +
      "not so. Studying the habits of ants frm so great a height should " +
      "be but a mild diversion when coupled with the soft drink that " +
      "mythology tells us is their only solace. But doubtless they have " +
      "amused themselves by the comparison of villages and towns; and it " +
      "will be no news to them (nor, perhaps, to many mortals), that in " +
      "one particularity New York stands unique among the cities of the " +
      "world. This shall be the theme of a little story addressed to the " +
      "man who sits smoking with his Sabbath-slippered feet on another " +
      "chair, and to the woman who snatches the paper for a moment while " +
      "boiling greens or a narcotized baby leaves her free. With these " +
      "I love to sit upon the ground and tell sad stories of the death of Kings."

    val expectedSentencesInOrder = Seq(
      "The gods, lying beside their nectar on 'Lympus and peeping over the edge of the cliff, perceive a difference in cities. ",
      "Although it would seem that to their vision towns must appear as large or small ant-hills without special characteristics, yet it is not so. ",
      "Studying the habits of ants frm so great a height should be but a mild diversion when coupled with the soft drink that mythology tells us is their only solace. ",
      "But doubtless they have amused themselves by the comparison of villages and towns; and it will be no news to them (nor, perhaps, to many mortals), that in one particularity New York stands unique among the cities of the world. ",
      "This shall be the theme of a little story addressed to the man who sits smoking with his Sabbath-slippered feet on another chair, and to the woman who snatches the paper for a moment while boiling greens or a narcotized baby leaves her free. ",
      "With these I love to sit upon the ground and tell sad stories of the death of Kings."
    )

    new SentenceIterator(sampleText).toSeq should be(expectedSentencesInOrder)
  }
}