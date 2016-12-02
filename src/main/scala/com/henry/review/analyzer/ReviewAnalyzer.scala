package com.henry.review.analyzer

import com.henry.review.iterator.WordIterator
import com.henry.review.model.Review
import org.apache.hadoop.conf.Configuration
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

class ReviewAnalyzer {
  private val spark = SparkSession
    .builder()
    .appName("Reviews analysis")
    .master("local[4]")
    .getOrCreate()
  val hadoopConfig: Configuration = spark.sparkContext.hadoopConfiguration
  hadoopConfig.set("fs.file.impl", classOf[org.apache.hadoop.fs.LocalFileSystem].getName)


  def analyze(filename: String): Unit = {
    val reviewsDataframe = spark.read.format("com.databricks.spark.csv")
      .option("header", "true")
      .load(filename)

    // In order to make sure that duplicates won't be handled
    // reviewsDataframe.distinct.rdd can be used here
    val reviewsRdd = reviewsDataframe.rdd.map(Review.apply).cache()

    val mostActiveUsers = determineMostActiveUsers(reviewsRdd, 1000)
    println("The most active users:")
    mostActiveUsers.sortBy(_._1).foreach(user => println(s"\t${user._1} - ${user._2} reviews"))

    val mostCommentedItems = determineMostCommentedItems(reviewsRdd, 1000)
    println("The most commented food items:")
    mostCommentedItems.sortBy(_._1).foreach(item => println(s"\t${item._1} - ${item._2} comments"))

    val mostUsedWords = determineMostUsedWords(reviewsRdd, 1000)
    println("The most used words in the reviews:")
    mostUsedWords.sortBy(_._1).foreach(word => println(s"\t${word._1} - ${word._2} mentions"))
  }

  private[analyzer] def determineMostActiveUsers(reviewsRdd: RDD[Review], limit: Int) = reviewsRdd
    .map(review => (review.profileName, 1)).reduceByKey(_ + _).sortBy(_._2, ascending = false).take(limit).toSeq

  private[analyzer]def determineMostCommentedItems(reviewsRdd: RDD[Review], limit: Int) = reviewsRdd
    .map(review => (review.productId, 1)).reduceByKey(_ + _).sortBy(_._2, ascending = false).take(limit).toSeq

  private[analyzer]def determineMostUsedWords(reviewsRdd: RDD[Review], limit: Int) = reviewsRdd
    // If taking into account different forms of the same word is important
    // then stemming should be applied here
    .flatMap(review => WordIterator(review.text).map(_.toLowerCase -> 1))
    .reduceByKey(_ + _).sortBy(_._2, ascending = false).take(limit).toSeq
}
