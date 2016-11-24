package com.henry.review.model

case class SentenceBatch(text: String, reviewBlockMarkers: Seq[ReviewBlockMarker])

case class ReviewBlockMarker(reviewId: String, sentenceNumber: Int, reviewBlockIndex: Int, reviewBlockNumber: Int)