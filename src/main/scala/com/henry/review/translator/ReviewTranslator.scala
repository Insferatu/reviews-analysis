package com.henry.review.translator

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream._
import com.github.tototoshi.csv.CSVReader
import com.henry.review.GoogleAPIFake
import com.henry.review.iterator.SentenceIterator
import com.henry.review.model.{Review, ReviewBlockMarker, ReviewTranslation, SentenceBatch}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ReviewTranslator {
  private val GoogleApiConcurrencyLimit = 100
  private val CharacterLimit = 1000
  private implicit val system = ActorSystem("Translation")
  private implicit val materializer = ActorMaterializer()
  private val googleAPIFake = new GoogleAPIFake

  class SentenceBatchCreator extends GraphStage[FlowShape[Review, SentenceBatch]] {
    private val in = Inlet[Review]("SentenceBatchCreator.in")
    private val out = Outlet[SentenceBatch]("SentenceBatchCreator.out")

    val shape = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      private val ownershipMarkers = ListBuffer[ReviewBlockMarker]()
      private val sentenceBatchText = new StringBuilder
      private val sentenceBatchQueue = new mutable.Queue[SentenceBatch]

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val review = grab(in)

          var reviewBlockNumber = 0
          val batches = SentenceIterator(review.text).filterNot(_.isEmpty).toList
            .scanLeft((0, sentenceBatchText.length, sentenceBatchText.toString)) {
              case ((batchIndex, currentBatchSize, _), sentence) =>
                if (currentBatchSize + sentence.length <= CharacterLimit)
                  (batchIndex, currentBatchSize + sentence.length, sentence)
                else
                  (batchIndex + 1, sentence.length, sentence)
            }
            .foldLeft(mutable.Map[Int, ListBuffer[String]]()) {
              case (map, (currentBatchIndex, _, sentence)) =>
                if (!map.contains(currentBatchIndex)) map(currentBatchIndex) = ListBuffer()
                map(currentBatchIndex) += sentence
                map
            }.toSeq
            .sortBy { case (batchIndex, _) => batchIndex }
            .map {
              case (batchIndex, batchSentences) =>
                val sentenceNumber =
                  if (batchIndex == 0 && sentenceBatchText.nonEmpty) batchSentences.size - 1
                  else batchSentences.size
                val reviewBlockIndex = if (sentenceNumber > 0) {
                  reviewBlockNumber += 1
                  reviewBlockNumber
                } else 0
                (batchSentences.toList, sentenceNumber, reviewBlockIndex)
            }

          batches match {
            case precedingBatches :+ lastBatch =>
              precedingBatches.foreach {
                case (batchSentences, sentenceNumber, reviewBlockIndex) =>
                  if (sentenceNumber > 0)
                    ownershipMarkers += ReviewBlockMarker(review.id, sentenceNumber, reviewBlockIndex, reviewBlockNumber)
                  sentenceBatchQueue.enqueue(SentenceBatch(batchSentences.mkString, ownershipMarkers.toList))
                  ownershipMarkers.clear()
              }
              if (precedingBatches.nonEmpty) sentenceBatchText.clear()
              val (lastBatchSentences, lastSentenceNumber, lastReviewBlockIndex) = lastBatch
              sentenceBatchText.append(lastBatchSentences.mkString)
              ownershipMarkers += ReviewBlockMarker(review.id, lastSentenceNumber, lastReviewBlockIndex, reviewBlockNumber)
          }

          if (sentenceBatchQueue.nonEmpty) push(out, sentenceBatchQueue.dequeue)
          else pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          sentenceBatchQueue.foreach(emit(out, _: SentenceBatch))
          if (ownershipMarkers.nonEmpty)
            emit(out, SentenceBatch(sentenceBatchText.toString, ownershipMarkers.toList))
          complete(out)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          if (sentenceBatchQueue.nonEmpty) push(out, sentenceBatchQueue.dequeue)
          else pull(in)
        }
      })
    }
  }

  class ReviewTranslationCreator extends GraphStage[FlowShape[(SentenceBatch, String), ReviewTranslation]] {
    private case class ReviewBlock(reviewBlockIndex: Int, originalBlock: String, translatedBlock: String)
    
    private val in = Inlet[(SentenceBatch, String)]("ReviewTranslationCreator.in")
    private val out = Outlet[ReviewTranslation]("ReviewTranslationCreator.out")

    val shape = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      private val reviewBlockMap = mutable.Map[String, ListBuffer[ReviewBlock]]()
      private val reviewTranslationQueue = mutable.Queue[ReviewTranslation]()

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val (SentenceBatch(text, reviewBlockMarkers), translation) = grab(in)
          val groupIndices = reviewBlockMarkers.map(_.sentenceNumber).zipWithIndex
            .flatMap { case (sentenceNumber, groupIndex) => Seq.fill(sentenceNumber)(groupIndex) }
          val sentencesToBlocks = (sentenceIterator: Iterator[String]) =>
            sentenceIterator.zip(groupIndices.toIterator).toSeq
              .foldLeft(mutable.Map[Int, StringBuilder]()) {
                case (map, (sentence, groupIndex)) =>
                  if (!map.contains(groupIndex)) map(groupIndex) = new StringBuilder()
                  map(groupIndex).append(sentence)
                  map
              }.toSeq
              .sortBy { case (groupIndex, _) => groupIndex }
              .map { case (_, sentences) => sentences.toString }

          val originalBlocks = sentencesToBlocks(SentenceIterator(text).filterNot(_.isEmpty))
          val translatedBlocks = sentencesToBlocks(SentenceIterator(translation).filterNot(_.isEmpty))

          reviewBlockMarkers.zip(originalBlocks.zip(translatedBlocks)).foreach {
            case (ReviewBlockMarker(reviewId, _, reviewBlockIndex, reviewBlockNumber), (originalBlock, translatedBlock)) =>
              if (!reviewBlockMap.contains(reviewId)) reviewBlockMap(reviewId) = ListBuffer()
              reviewBlockMap(reviewId) += ReviewBlock(reviewBlockIndex, originalBlock, translatedBlock)
              if (reviewBlockMap(reviewId).size == reviewBlockNumber) {
                val sortedReviewBlocks = reviewBlockMap(reviewId).toList.sortBy(_.reviewBlockIndex)
                val originalText = sortedReviewBlocks.map(_.originalBlock).mkString
                val translatedText = sortedReviewBlocks.map(_.translatedBlock).mkString
                println(s"$reviewId - $reviewBlockNumber - $originalText")
                reviewTranslationQueue.enqueue(ReviewTranslation(
                  reviewId,
                  originalText,
                  translatedText
                ))
              }
          }

          if (reviewTranslationQueue.nonEmpty) push(out, reviewTranslationQueue.dequeue)
          else pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          reviewTranslationQueue.foreach(emit(out, _: ReviewTranslation))
          complete(out)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          if (reviewTranslationQueue.nonEmpty) push(out, reviewTranslationQueue.dequeue)
          else pull(in)
        }
      })
    }
  }

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
        val queryApiFlow = Flow[SentenceBatch].map(batch => (batch, googleAPIFake.translate("us", "fr", batch.text))).async

        (0 until GoogleApiConcurrencyLimit).foreach { index =>
          dispatchLine.out(index) ~> mapToReviewFlow ~> batchCreatorFlow ~> queryApiFlow ~> mergeTranslate.in(index)
        }

        FlowShape(dispatchLine.in, mergeTranslate.out)
      }).withAttributes(ActorAttributes.dispatcher("akka.stream.google-translate-api-dispatcher"))

    val reviewsReader = CSVReader.open(filename)
    val reviewsIterator = reviewsReader.toStream.drop(1).take(1000)
    Source(reviewsIterator)
      .via(translateFlow)
      .via(new ReviewTranslationCreator)
      .runWith(Sink.foreach { reviewTranslation =>
        val ReviewTranslation(reviewId, originalText, translatedText) = reviewTranslation
        //assert(checkMap(reviewId) == originalText)
        //assert(translatedText == "Salut Jean, comment vas tu?")
        println(s"${checkMap(reviewId) == originalText}", reviewTranslation)
      })//reviewTranslation => assert(trans == "Salut Jean, comment vas tu?")))
  }
}
