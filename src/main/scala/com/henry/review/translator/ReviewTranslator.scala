package com.henry.review.translator

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Source}
import com.github.tototoshi.csv.CSVReader
import com.henry.review.google.IGoogleApi
import com.henry.review.model.{Review, ReviewTranslation, SentenceBatch}
import com.henry.review.translator.ReviewTranslator.GoogleApiConcurrencyLimit

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ReviewTranslator(googleApi: IGoogleApi) {
  def translate(filename: String): Unit = {
    implicit val system = ActorSystem("TranslationSystem")
    implicit val materializer = ActorMaterializer()

    val reviewVerificationMap = mutable.Map[String, String]()

    val reviewsReader = CSVReader.open(filename)
    val reviewsStream = reviewsReader.toStream.drop(1)
    val futureDone =
      Source(reviewsStream)
        .map { seq =>
          val review = Review(seq)
          reviewVerificationMap(review.id) = review.text
          review
        }
        .via(translateReviewFlow)
        .runForeach { reviewTranslation =>
          val reviewId = reviewTranslation.reviewId
          val originalText = reviewTranslation.originalText
          if (reviewVerificationMap(reviewId) != originalText)
            println(
              s"""There is a difference between original text before passing pipeline and after:
                 |  Before pipeline: ${reviewVerificationMap(reviewId)}
                 |  After pipeline: $originalText
                """.stripMargin)
        }

    Await.result(futureDone, Duration.Inf)
    system.terminate()
  }

  private[translator] val translateReviewFlow: Flow[Review, ReviewTranslation, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      val dispatchLine = builder.add(Balance[Review](GoogleApiConcurrencyLimit))
      val mergeTranslate = builder.add(Merge[(SentenceBatch, String)](GoogleApiConcurrencyLimit))
      val batchCreatorFlow = Flow[Review].via(new SentenceBatchCreator)
      val translateThroughGoogleApiFlow = Flow[SentenceBatch].map { batch =>
        (batch, googleApi.translate("us", "fr", batch.text))
      }.async

      (0 until GoogleApiConcurrencyLimit).foreach { index =>
        dispatchLine.out(index) ~> batchCreatorFlow ~> translateThroughGoogleApiFlow ~> mergeTranslate.in(index)
      }

      FlowShape(dispatchLine.in, mergeTranslate.out)
    }).via(new ReviewTranslationCreator)
}

object ReviewTranslator {
  val GoogleApiConcurrencyLimit = 100
  val CharacterLimit = 1000
}
