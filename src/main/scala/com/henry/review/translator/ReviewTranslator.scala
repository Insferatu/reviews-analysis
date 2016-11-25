package com.henry.review.translator

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Source}
import com.github.tototoshi.csv.CSVReader
import com.henry.review.google.IGoogleApi
import com.henry.review.model.{Review, SentenceBatch}
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
      })

    val reviewsReader = CSVReader.open(filename)
    val reviewsIterator = reviewsReader.iterator.drop(1)
    Source(reviewsIterator.to[collection.immutable.Iterable])
      .via(translateFlow)
      .via(new ReviewTranslationCreator)
      .runForeach { reviewTranslation =>
        val reviewId = reviewTranslation.reviewId
        val originalText = reviewTranslation.originalText
        if (checkMap(reviewId) != originalText)
          println(
            s"""There is a difference between original text before passing pipeline and after:
               |  Before pipeline: ${checkMap(reviewId)}
               |  After pipeline: $originalText
             """.stripMargin)
      }

  }
}

object ReviewTranslator {
  val GoogleApiConcurrencyLimit = 100
  val CharacterLimit = 1000
}
