package com.henry.review

import com.henry.review.analyzer.ReviewAnalyzer

object Runner {
  val reviewsFilename = "Reviews.csv"

  def main(args: Array[String]): Unit = {
    val reviewAnalyzer = new ReviewAnalyzer
    reviewAnalyzer.analyze(reviewsFilename)
  }
}
