package com.henry.review.translator

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import com.henry.review.model.{ReviewBlockMarker, ReviewTranslation, SentenceBatch}
import org.scalatest.FlatSpec

class ReviewTranslationCreatorSpec extends FlatSpec {
  implicit val system = ActorSystem("TestSystem")
  implicit val materializer = ActorMaterializer()

  behavior of "ReviewTranslationCreator"

  it should "create one ReviewTranslation from SentenceBatch containing one ReviewBlock" in {
    val testSentenceBatchTranslationPairs = List(
      SentenceBatch(
        text = "The gods, lying beside their nectar on 'Lympus and peeping over " +
          "the edge of the cliff, perceive a difference in cities. Although " +
          "it would seem that to their vision towns must appear as large " +
          "or small ant-hills without special characteristics, yet it is " +
          "not so. Studying the habits of ants frm so great a height should " +
          "be but a mild diversion when coupled with the soft drink that " +
          "mythology tells us is their only solace.\n",
        reviewBlockMarkers = Seq(ReviewBlockMarker("1", 3, 1, 1))
      ) -> "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu?\n"
    )

    val expectedReviewTranslation = ReviewTranslation(reviewId = "1",
      originalText = "The gods, lying beside their nectar on 'Lympus and peeping over " +
        "the edge of the cliff, perceive a difference in cities. Although " +
        "it would seem that to their vision towns must appear as large " +
        "or small ant-hills without special characteristics, yet it is " +
        "not so. Studying the habits of ants frm so great a height should " +
        "be but a mild diversion when coupled with the soft drink that " +
        "mythology tells us is their only solace.",
      translatedText = "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu?"
    )

    Source(testSentenceBatchTranslationPairs)
      .via(new ReviewTranslationCreator)
      .runWith(TestSink.probe[ReviewTranslation])
      .request(1)
      .expectNext(expectedReviewTranslation)
      .expectComplete()
  }

  it should "create more than one ReviewTranslations from SentenceBatch containing multiple ReviewBlocks" in {
    val testSentenceBatchTranslationPairs = List(
      SentenceBatch(
        text = "The gods, lying beside their nectar on 'Lympus and peeping over " +
          "the edge of the cliff, perceive a difference in cities. Although " +
          "it would seem that to their vision towns must appear as large " +
          "or small ant-hills without special characteristics, yet it is " +
          "not so. Studying the habits of ants frm so great a height should " +
          "be but a mild diversion when coupled with the soft drink that " +
          "mythology tells us is their only solace.\nBut doubtless they have " +
          "amused themselves by the comparison of villages and towns; and it " +
          "will be no news to them (nor, perhaps, to many mortals), that in " +
          "one particularity New York stands unique among the cities of the " +
          "world. This shall be the theme of a little story addressed to the " +
          "man who sits smoking with his Sabbath-slippered feet on another " +
          "chair, and to the woman who snatches the paper for a moment while " +
          "boiling greens or a narcotized baby leaves her free.\nWith these " +
          "I love to sit upon the ground and tell sad stories of the death of Kings.\n",
        reviewBlockMarkers = Seq(
          ReviewBlockMarker("1", 3, 1, 1), ReviewBlockMarker("2", 2, 1, 1), ReviewBlockMarker("3", 1, 1, 1)
        )
      ) -> ("Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu?\n" +
        "Salut Jean, comment vas tu? Salut Jean, comment vas tu?\nSalut Jean, comment vas tu?\n")
    )

    val expectedReviewTranslations = List(
      ReviewTranslation(reviewId = "1",
        originalText = "The gods, lying beside their nectar on 'Lympus and peeping over " +
          "the edge of the cliff, perceive a difference in cities. Although " +
          "it would seem that to their vision towns must appear as large " +
          "or small ant-hills without special characteristics, yet it is " +
          "not so. Studying the habits of ants frm so great a height should " +
          "be but a mild diversion when coupled with the soft drink that " +
          "mythology tells us is their only solace.",
        translatedText = "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu?"
      ),
      ReviewTranslation(reviewId = "2",
        originalText = "But doubtless they have " +
          "amused themselves by the comparison of villages and towns; and it " +
          "will be no news to them (nor, perhaps, to many mortals), that in " +
          "one particularity New York stands unique among the cities of the " +
          "world. This shall be the theme of a little story addressed to the " +
          "man who sits smoking with his Sabbath-slippered feet on another " +
          "chair, and to the woman who snatches the paper for a moment while " +
          "boiling greens or a narcotized baby leaves her free.",
        translatedText = "Salut Jean, comment vas tu? Salut Jean, comment vas tu?"
      ),
      ReviewTranslation(reviewId = "3",
        originalText = "With these I love to sit upon the ground and tell sad stories of the death of Kings.",
        translatedText = "Salut Jean, comment vas tu?"
      )
    )

    Source(testSentenceBatchTranslationPairs)
      .via(new ReviewTranslationCreator)
      .runWith(TestSink.probe[ReviewTranslation])
      .request(expectedReviewTranslations.size)
      .expectNextUnorderedN(expectedReviewTranslations)
      .expectComplete()
  }

  it should "create one ReviewTranslation from multiple SentenceBatches all containing ReviewBlocks of the same Review" in {
    val testSentenceBatchTranslationPairs = List(
      SentenceBatch(
        text = "The gods, lying beside their nectar on 'Lympus and peeping over the " +
          "edge of the cliff, perceive a difference in cities. Although it would seem " +
          "that to their vision towns must appear as large or small ant-hills without " +
          "special characteristics, yet it is not so. Studying the habits of ants frm " +
          "so great a height should be but a mild diversion when coupled with the soft " +
          "drink that mythology tells us is their only solace. But doubtless they have " +
          "amused themselves by the comparison of villages and towns; and it will be " +
          "no news to them (nor, perhaps, to many mortals), that in one particularity " +
          "New York stands unique among the cities of the world. This shall be the theme " +
          "of a little story addressed to the man who sits smoking with his " +
          "Sabbath-slippered feet on another chair, and to the woman who snatches the " +
          "paper for a moment while boiling greens or a narcotized baby leaves her free. " +
          "With these I love to sit upon the ground and tell sad stories of the death of Kings. ",
        reviewBlockMarkers = Seq(ReviewBlockMarker("1", 6, 1, 3))
      ) -> ("Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu? " +
        "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu? "),
      SentenceBatch(
        text = "The gods, lying beside their nectar on 'Lympus and peeping over the " +
          "edge of the cliff, perceive a difference in cities. Although it would seem " +
          "that to their vision towns must appear as large or small ant-hills without " +
          "special characteristics, yet it is not so. Studying the habits of ants frm " +
          "so great a height should be but a mild diversion when coupled with the soft " +
          "drink that mythology tells us is their only solace. But doubtless they have " +
          "amused themselves by the comparison of villages and towns; and it will be " +
          "no news to them (nor, perhaps, to many mortals), that in one particularity " +
          "New York stands unique among the cities of the world. This shall be the theme " +
          "of a little story addressed to the man who sits smoking with his " +
          "Sabbath-slippered feet on another chair, and to the woman who snatches the " +
          "paper for a moment while boiling greens or a narcotized baby leaves her free. " +
          "With these I love to sit upon the ground and tell sad stories of the death of Kings. ",
        reviewBlockMarkers = Seq(ReviewBlockMarker("1", 6, 2, 3))
      ) -> ("Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu? " +
        "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu? "),
      SentenceBatch(
        text = "The gods, lying beside their nectar on 'Lympus and peeping over the " +
          "edge of the cliff, perceive a difference in cities. Although it would seem " +
          "that to their vision towns must appear as large or small ant-hills without " +
          "special characteristics, yet it is not so. Studying the habits of ants frm " +
          "so great a height should be but a mild diversion when coupled with the soft " +
          "drink that mythology tells us is their only solace.\n",
        reviewBlockMarkers = Seq(ReviewBlockMarker("1", 3, 3, 3))
      ) -> "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu?\n"
    )

    val expectedReviewTranslation = ReviewTranslation(reviewId = "1",
      originalText = "The gods, lying beside their nectar on 'Lympus and peeping over the edge " +
        "of the cliff, perceive a difference in cities. Although it would seem that to their " +
        "vision towns must appear as large or small ant-hills without special characteristics, " +
        "yet it is not so. Studying the habits of ants frm so great a height should be but a mild " +
        "diversion when coupled with the soft drink that mythology tells us is their only solace. " +
        "But doubtless they have amused themselves by the comparison of villages and towns; and " +
        "it will be no news to them (nor, perhaps, to many mortals), that in one particularity " +
        "New York stands unique among the cities of the world. This shall be the theme of a little " +
        "story addressed to the man who sits smoking with his Sabbath-slippered feet on another " +
        "chair, and to the woman who snatches the paper for a moment while boiling greens or a " +
        "narcotized baby leaves her free. With these I love to sit upon the ground and tell sad " +
        "stories of the death of Kings. The gods, lying beside their nectar on 'Lympus and peeping " +
        "over the edge of the cliff, perceive a difference in cities. Although it would seem that " +
        "to their vision towns must appear as large or small ant-hills without special characteristics, " +
        "yet it is not so. Studying the habits of ants frm so great a height should be but a mild " +
        "diversion when coupled with the soft drink that mythology tells us is their only solace. " +
        "But doubtless they have amused themselves by the comparison of villages and towns; and " +
        "it will be no news to them (nor, perhaps, to many mortals), that in one particularity " +
        "New York stands unique among the cities of the world. This shall be the theme of a little " +
        "story addressed to the man who sits smoking with his Sabbath-slippered feet on another " +
        "chair, and to the woman who snatches the paper for a moment while boiling greens or a " +
        "narcotized baby leaves her free. With these I love to sit upon the ground and tell sad " +
        "stories of the death of Kings. The gods, lying beside their nectar on 'Lympus and peeping " +
        "over the edge of the cliff, perceive a difference in cities. Although it would seem that " +
        "to their vision towns must appear as large or small ant-hills without special characteristics, " +
        "yet it is not so. Studying the habits of ants frm so great a height should be but a mild " +
        "diversion when coupled with the soft drink that mythology tells us is their only solace.",
      translatedText = "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu? " +
        "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu? " +
        "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu? " +
        "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu? " +
        "Salut Jean, comment vas tu? Salut Jean, comment vas tu? Salut Jean, comment vas tu?"
    )

    Source(testSentenceBatchTranslationPairs)
      .via(new ReviewTranslationCreator)
      .runWith(TestSink.probe[ReviewTranslation])
      .request(1)
      .expectNext(expectedReviewTranslation)
      .expectComplete()
  }
}
