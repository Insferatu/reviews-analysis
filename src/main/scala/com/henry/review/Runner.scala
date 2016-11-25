package com.henry.review

import com.henry.review.analyzer.ReviewAnalyzer
import com.henry.review.google.GoogleApiFake
import com.henry.review.translator.ReviewTranslator

object Runner {
  val ReviewsFilename = "Reviews.csv"

  def main(args: Array[String]): Unit = {
    //val reviewAnalyzer = new ReviewAnalyzer
    //reviewAnalyzer.analyze(ReviewsFilename)

    if (args.contains("translate=true")) {
      val reviewTranslator = new ReviewTranslator(new GoogleApiFake)
      reviewTranslator.translate(ReviewsFilename)
    }
  }
}
