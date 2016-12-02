package com.henry.review.translator

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import com.henry.review.model.{Review, ReviewBlockMarker, SentenceBatch}
import com.henry.review.translator.ReviewTranslator.CharacterLimit
import org.scalatest.FlatSpec

class SentenceBatchCreatorSpec extends FlatSpec {
  implicit val system = ActorSystem("TestSystem")
  implicit val materializer = ActorMaterializer()

  behavior of "SentenceBatchCreator"

  it should s"create one sentence batch for a review if its text smaller than $CharacterLimit characters" in {
    val testReviews = List(
      Review(id = "1",
        text = "The gods, lying beside their nectar on 'Lympus and peeping over " +
          "the edge of the cliff, perceive a difference in cities. Although " +
          "it would seem that to their vision towns must appear as large " +
          "or small ant-hills without special characteristics, yet it is " +
          "not so. Studying the habits of ants frm so great a height should " +
          "be but a mild diversion when coupled with the soft drink that " +
          "mythology tells us is their only solace."
      )
    )

    val expectedSentenceBatch = SentenceBatch(
      text = "The gods, lying beside their nectar on 'Lympus and peeping over " +
        "the edge of the cliff, perceive a difference in cities. Although " +
        "it would seem that to their vision towns must appear as large " +
        "or small ant-hills without special characteristics, yet it is " +
        "not so. Studying the habits of ants frm so great a height should " +
        "be but a mild diversion when coupled with the soft drink that " +
        "mythology tells us is their only solace.\n",
      reviewBlockMarkers = Seq(ReviewBlockMarker("1", 3, 1, 1))
    )

    Source(testReviews)
      .via(new SentenceBatchCreator)
      .runWith(TestSink.probe[SentenceBatch])
      .request(1)
      .expectNext(expectedSentenceBatch)
      .expectComplete()
  }

  it should s"create one sentence batch for reviews if their texts in total smaller than $CharacterLimit characters" in {
    val testReviews = List(
      Review(id = "1",
        text = "The gods, lying beside their nectar on 'Lympus and peeping over " +
          "the edge of the cliff, perceive a difference in cities. Although " +
          "it would seem that to their vision towns must appear as large " +
          "or small ant-hills without special characteristics, yet it is " +
          "not so. Studying the habits of ants frm so great a height should " +
          "be but a mild diversion when coupled with the soft drink that " +
          "mythology tells us is their only solace."
      ),
      Review(id = "2",
        text = "But doubtless they have amused themselves by the comparison of " +
          "villages and towns; and it will be no news to them (nor, perhaps, to " +
          "many mortals), that in one particularity New York stands unique among " +
          "the cities of the world. This shall be the theme of a little story " +
          "addressed to the man who sits smoking with his Sabbath-slippered feet " +
          "on another chair, and to the woman who snatches the paper for a moment " +
          "while boiling greens or a narcotized baby leaves her free."
      ),
      Review(id = "3",
        text = "With these I love to sit upon the ground and tell sad stories of the death of Kings."
      )
    )

    val expectedSentenceBatch = SentenceBatch(
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
    )

    Source(testReviews)
      .via(new SentenceBatchCreator)
      .runWith(TestSink.probe[SentenceBatch])
      .request(1)
      .expectNext(expectedSentenceBatch)
      .expectComplete()
  }

  it should s"create more than one sentence batch for a review if its text bigger than $CharacterLimit characters" in {
    val testReviews = List(
      Review(id = "1",
        text = "The gods, lying beside their nectar on 'Lympus and peeping over " +
          "the edge of the cliff, perceive a difference in cities. Although it " +
          "would seem that to their vision towns must appear as large or small " +
          "ant-hills without special characteristics, yet it is not so. Studying " +
          "the habits of ants frm so great a height should be but a mild diversion " +
          "when coupled with the soft drink that mythology tells us is their only " +
          "solace. But doubtless they have amused themselves by the comparison of " +
          "villages and towns; and it will be no news to them (nor, perhaps, to " +
          "many mortals), that in one particularity New York stands unique among " +
          "the cities of the world. This shall be the theme of a little story " +
          "addressed to the man who sits smoking with his Sabbath-slippered feet " +
          "on another chair, and to the woman who snatches the paper for a moment " +
          "while boiling greens or a narcotized baby leaves her free. With these " +
          "I love to sit upon the ground and tell sad stories of the death of Kings. " +
          "The gods, lying beside their nectar on 'Lympus and peeping over the edge " +
          "of the cliff, perceive a difference in cities. Although it would seem " +
          "that to their vision towns must appear as large or small ant-hills " +
          "without special characteristics, yet it is not so. Studying the habits " +
          "of ants frm so great a height should be but a mild diversion when coupled " +
          "with the soft drink that mythology tells us is their only solace. But " +
          "doubtless they have amused themselves by the comparison of villages and " +
          "towns; and it will be no news to them (nor, perhaps, to many mortals), " +
          "that in one particularity New York stands unique among the cities of the " +
          "world. This shall be the theme of a little story addressed to the man who " +
          "sits smoking with his Sabbath-slippered feet on another chair, and to the " +
          "woman who snatches the paper for a moment while boiling greens or a " +
          "narcotized baby leaves her free. With these I love to sit upon the ground " +
          "and tell sad stories of the death of Kings. The gods, lying beside their " +
          "nectar on 'Lympus and peeping over the edge of the cliff, perceive a " +
          "difference in cities. Although it would seem that to their vision towns " +
          "must appear as large or small ant-hills without special characteristics, " +
          "yet it is not so. Studying the habits of ants frm so great a height " +
          "should be but a mild diversion when coupled with the soft drink that " +
          "mythology tells us is their only solace."
      )
    )

    val expectedSentenceBatches = List(
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
      ),
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
      ),
      SentenceBatch(
        text = "The gods, lying beside their nectar on 'Lympus and peeping over the " +
          "edge of the cliff, perceive a difference in cities. Although it would seem " +
          "that to their vision towns must appear as large or small ant-hills without " +
          "special characteristics, yet it is not so. Studying the habits of ants frm " +
          "so great a height should be but a mild diversion when coupled with the soft " +
          "drink that mythology tells us is their only solace.\n",
        reviewBlockMarkers = Seq(ReviewBlockMarker("1", 3, 3, 3))
      )
    )

    Source(testReviews)
      .via(new SentenceBatchCreator)
      .runWith(TestSink.probe[SentenceBatch])
      .request(expectedSentenceBatches.size)
      .expectNextUnorderedN(expectedSentenceBatches)
      .expectComplete()
  }
}
