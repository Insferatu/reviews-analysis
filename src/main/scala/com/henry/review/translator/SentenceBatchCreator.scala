package com.henry.review.translator

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.henry.review.iterator.SentenceIterator
import com.henry.review.model.{Review, ReviewBlockMarker, SentenceBatch}
import com.henry.review.translator.ReviewTranslator.CharacterLimit

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

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
              if (currentBatchSize + sentence.length < CharacterLimit)
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
                if (batchIndex == 0) batchSentences.size - 1
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
            sentenceBatchText.clear()
            val (lastBatchSentences, lastSentenceNumber, lastReviewBlockIndex) = lastBatch
            sentenceBatchText.append(lastBatchSentences.mkString + "\n")
            ownershipMarkers += ReviewBlockMarker(review.id, lastSentenceNumber, lastReviewBlockIndex, reviewBlockNumber)
        }

        if (sentenceBatchQueue.nonEmpty) push(out, sentenceBatchQueue.dequeue)
        else pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        if (ownershipMarkers.nonEmpty)
          sentenceBatchQueue.enqueue(SentenceBatch(sentenceBatchText.toString, ownershipMarkers.toList))
        if (sentenceBatchQueue.nonEmpty)
          emitMultiple(out, sentenceBatchQueue.toIterator)
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