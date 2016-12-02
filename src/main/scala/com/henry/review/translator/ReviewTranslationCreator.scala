package com.henry.review.translator

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.henry.review.iterator.SentenceIterator
import com.henry.review.model.{ReviewBlockMarker, ReviewTranslation, SentenceBatch}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

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

        val originalBlocks = sentencesToBlocks(text.split("\n").toIterator.flatMap(SentenceIterator.apply).filterNot(_.isEmpty))
        val translatedBlocks = sentencesToBlocks(translation.split("\n").toIterator.flatMap(SentenceIterator.apply).filterNot(_.isEmpty))

        reviewBlockMarkers.zip(originalBlocks.zip(translatedBlocks)).foreach {
          case (ReviewBlockMarker(reviewId, _, reviewBlockIndex, reviewBlockNumber), (originalBlock, translatedBlock)) =>
            if (!reviewBlockMap.contains(reviewId)) reviewBlockMap(reviewId) = ListBuffer()
            reviewBlockMap(reviewId) += ReviewBlock(reviewBlockIndex, originalBlock, translatedBlock)
            if (reviewBlockMap(reviewId).size == reviewBlockNumber) {
              val sortedReviewBlocks = reviewBlockMap(reviewId).toList.sortBy(_.reviewBlockIndex)
              reviewTranslationQueue.enqueue(ReviewTranslation(
                reviewId,
                originalText = sortedReviewBlocks.map(_.originalBlock).mkString,
                translatedText = sortedReviewBlocks.map(_.translatedBlock).mkString
              ))
              reviewBlockMap.remove(reviewId)
            }
        }

        if (reviewTranslationQueue.nonEmpty) push(out, reviewTranslationQueue.dequeue)
        else pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        if (reviewTranslationQueue.nonEmpty)
          emitMultiple(out, reviewTranslationQueue.toIterator)
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