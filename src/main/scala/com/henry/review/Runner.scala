package com.henry.review

import java.io.BufferedReader
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Balance, FileIO, Flow, Framing, GraphDSL, Merge, Sink, Source}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.util.ByteString
import com.github.tototoshi.csv.CSVReader
import com.henry.review.analyzer.ReviewAnalyzer
import com.henry.review.model.Review
import com.henry.review.translator.ReviewTranslator

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io

object Runner {
  val ReviewsFilename = "Reviews.csv"

  def main(args: Array[String]): Unit = {
    //val reviewAnalyzer = new ReviewAnalyzer
    //reviewAnalyzer.analyze(ReviewsFilename)

    val reviewTranslator = new ReviewTranslator
    reviewTranslator.translate(ReviewsFilename)
  }
}
