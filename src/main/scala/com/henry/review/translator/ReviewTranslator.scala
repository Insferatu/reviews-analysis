package com.henry.review.translator

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Flow, Source}
import com.github.tototoshi.csv.CSVReader
import com.henry.review.google.IGoogleApi
import com.henry.review.model.{Review, ReviewTranslation}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class ReviewTranslator(googleApi: IGoogleApi) {
  def translate(filename: String): Unit = {
    implicit val system = ActorSystem("TranslationSystem")
    implicit val dispatcher = system.dispatcher
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

  private[translator] def translateReviewFlow(implicit dispatcher: ExecutionContext): Flow[Review, ReviewTranslation, NotUsed] =
    Flow[Review]
      .via(new SentenceBatchCreator)
      .mapAsync(100) { batch =>
        Future(batch -> googleApi.translate("us", "fr", batch.text))
      }
      .via(new ReviewTranslationCreator)
}

object ReviewTranslator {
  val GoogleApiConcurrencyLimit = 100
  val CharacterLimit = 1000
}
