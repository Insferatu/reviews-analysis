package com.henry.review.translator

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Sink, Source}
import com.github.tototoshi.csv.CSVReader
import com.henry.review.google.{GoogleApiFake, IGoogleApi}
import com.henry.review.model.{Review, ReviewTranslation, SentenceBatch}
import com.henry.review.translator.ReviewTranslator.GoogleApiConcurrencyLimit

import scala.collection.mutable

class ReviewTranslator(googleApi: IGoogleApi) {
  private implicit val system = ActorSystem("Translation")
  private implicit val materializer = ActorMaterializer()

  def translate(filename: String): Unit = {
    val checkMap = mutable.Map[String, String]()

    val translateFlow: Flow[Seq[String], (SentenceBatch, String), NotUsed] =
      Flow.fromGraph(GraphDSL.create() { implicit builder =>
        val dispatchLine = builder.add(Balance[Seq[String]](GoogleApiConcurrencyLimit))
        val mergeTranslate = builder.add(Merge[(SentenceBatch, String)](GoogleApiConcurrencyLimit))
        val mapToReviewFlow = Flow[Seq[String]].map { seq =>
          val review = Review.apply(seq)
          checkMap(review.id) = review.text
          review
        }
        val batchCreatorFlow = Flow[Review].via(new SentenceBatchCreator)
        val queryApiFlow = Flow[SentenceBatch].map(batch => (batch, googleApi.translate("us", "fr", batch.text))).async

        (0 until GoogleApiConcurrencyLimit).foreach { index =>
          dispatchLine.out(index) ~> mapToReviewFlow ~> batchCreatorFlow ~> queryApiFlow ~> mergeTranslate.in(index)
        }

        FlowShape(dispatchLine.in, mergeTranslate.out)
      }).withAttributes(ActorAttributes.dispatcher("akka.stream.google-translate-api-dispatcher"))

    val reviewsReader = CSVReader.open(filename)
    val reviewsIterator = reviewsReader.iterator.drop(1).take(1000)
    Source(reviewsIterator.to[collection.immutable.Iterable])
      .via(translateFlow)
      .via(new ReviewTranslationCreator)
      .to(Sink.foreach { reviewTranslation =>
        val ReviewTranslation(reviewId, originalText, translatedText) = reviewTranslation
        println(s"${checkMap(reviewId) == originalText}", reviewTranslation)
      }).run()

  }
}

object ReviewTranslator {
  val GoogleApiConcurrencyLimit = 100
  val CharacterLimit = 1000
}