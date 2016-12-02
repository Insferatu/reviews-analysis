package com.henry.review.analyzer

import com.henry.review.model.Review
import org.apache.spark.sql.SparkSession
import org.scalatest.{FlatSpec, Matchers}

class ReviewAnalyzerSpec extends FlatSpec with Matchers {
  val sc = SparkSession
    .builder()
    .appName("Test app")
    .master("local[2]")
    .getOrCreate().sparkContext

  behavior of "ReviewAnalyzer"

  it should "find the most active users without dropping unnecessary results" in {
    val testReviews = Seq(
      Review(profileName = "testUser3"),
      Review(profileName = "testUser4"),
      Review(profileName = "testUser2"),
      Review(profileName = "testUser3"),
      Review(profileName = "testUser4"),
      Review(profileName = "testUser1"),
      Review(profileName = "testUser2"),
      Review(profileName = "testUser4"),
      Review(profileName = "testUser2"),
      Review(profileName = "testUser5"),
      Review(profileName = "testUser4"),
      Review(profileName = "testUser1"),
      Review(profileName = "testUser3"),
      Review(profileName = "testUser2"),
      Review(profileName = "testUser4")
    )
    val reviewsRdd = sc.parallelize(testReviews)

    val expectedMostActiveUsers = Seq(
      "testUser4" -> 5,
      "testUser2" -> 4,
      "testUser3" -> 3,
      "testUser1" -> 2,
      "testUser5" -> 1
    )

    val reviewAnalyzer = new ReviewAnalyzer
    reviewAnalyzer.determineMostActiveUsers(reviewsRdd, 10) should be(expectedMostActiveUsers)
  }

  it should "find the most active users and drop unnecessary results" in {
    val testReviews = Seq(
      Review(profileName = "testUser3"),
      Review(profileName = "testUser4"),
      Review(profileName = "testUser2"),
      Review(profileName = "testUser3"),
      Review(profileName = "testUser4"),
      Review(profileName = "testUser1"),
      Review(profileName = "testUser2"),
      Review(profileName = "testUser4"),
      Review(profileName = "testUser2"),
      Review(profileName = "testUser5"),
      Review(profileName = "testUser4"),
      Review(profileName = "testUser1"),
      Review(profileName = "testUser3"),
      Review(profileName = "testUser2"),
      Review(profileName = "testUser4")
    )
    val reviewsRdd = sc.parallelize(testReviews)

    val expectedMostActiveUsers = Seq(
      "testUser4" -> 5,
      "testUser2" -> 4,
      "testUser3" -> 3
    )

    val reviewAnalyzer = new ReviewAnalyzer
    reviewAnalyzer.determineMostActiveUsers(reviewsRdd, 3) should be(expectedMostActiveUsers)
  }

  it should "find the most commented items without dropping unnecessary results" in {
    val testReviews = Seq(
      Review(productId = "product3"),
      Review(productId = "product4"),
      Review(productId = "product2"),
      Review(productId = "product3"),
      Review(productId = "product4"),
      Review(productId = "product1"),
      Review(productId = "product2"),
      Review(productId = "product4"),
      Review(productId = "product2"),
      Review(productId = "product5"),
      Review(productId = "product4"),
      Review(productId = "product1"),
      Review(productId = "product3"),
      Review(productId = "product2"),
      Review(productId = "product4")
    )
    val reviewsRdd = sc.parallelize(testReviews)

    val expectedMostActiveUsers = Seq(
      "product4" -> 5,
      "product2" -> 4,
      "product3" -> 3,
      "product1" -> 2,
      "product5" -> 1
    )

    val reviewAnalyzer = new ReviewAnalyzer
    reviewAnalyzer.determineMostCommentedItems(reviewsRdd, 10) should be(expectedMostActiveUsers)
  }

  it should "find the most commented items and drop unnecessary results" in {
    val testReviews = Seq(
      Review(productId = "product3"),
      Review(productId = "product4"),
      Review(productId = "product2"),
      Review(productId = "product3"),
      Review(productId = "product4"),
      Review(productId = "product1"),
      Review(productId = "product2"),
      Review(productId = "product4"),
      Review(productId = "product2"),
      Review(productId = "product5"),
      Review(productId = "product4"),
      Review(productId = "product1"),
      Review(productId = "product3"),
      Review(productId = "product2"),
      Review(productId = "product4")
    )
    val reviewsRdd = sc.parallelize(testReviews)

    val expectedMostActiveUsers = Seq(
      "product4" -> 5,
      "product2" -> 4,
      "product3" -> 3
    )

    val reviewAnalyzer = new ReviewAnalyzer
    reviewAnalyzer.determineMostCommentedItems(reviewsRdd, 3) should be(expectedMostActiveUsers)
  }

  it should "find the most used words in the reviews without dropping unnecessary results" in {
    val testReviews = Seq(
      Review(text = "dog: elephant mouse monkey"),
      Review(text = "seal; mouse monkey bird"),
      Review(text = "dog cat tiger? monkey"),
      Review(text = "dog elephant. seal bird"),
      Review(text = "cat seal mouse, monkey"),
      Review(text = "dog elephant seal mouse monkey bird"),
      Review(text = "dog elephant seal monkey!"),
      Review(text = "dog,monkey"),
      Review(text = "seal mouse-dog,monkey")
    )
    val reviewsRdd = sc.parallelize(testReviews)

    val expectedMostActiveUsers = Seq(
      "monkey" -> 8,
      "dog" -> 7,
      "seal" -> 6,
      "mouse" -> 5,
      "elephant" -> 4,
      "bird" -> 3,
      "cat" -> 2,
      "tiger" -> 1
    )

    val reviewAnalyzer = new ReviewAnalyzer
    reviewAnalyzer.determineMostUsedWords(reviewsRdd, 10) should be(expectedMostActiveUsers)
  }

  it should "find the most used words in the reviews and drop unnecessary results" in {
    val testReviews = Seq(
      Review(text = "dog: elephant mouse monkey"),
      Review(text = "seal; mouse monkey bird"),
      Review(text = "dog cat tiger? monkey"),
      Review(text = "dog elephant. seal bird"),
      Review(text = "cat seal mouse, monkey"),
      Review(text = "dog elephant seal mouse monkey bird"),
      Review(text = "dog elephant seal monkey!"),
      Review(text = "dog,monkey"),
      Review(text = "seal mouse-dog,monkey")
    )
    val reviewsRdd = sc.parallelize(testReviews)

    val expectedMostActiveUsers = Seq(
      "monkey" -> 8,
      "dog" -> 7,
      "seal" -> 6,
      "mouse" -> 5,
      "elephant" -> 4
    )

    val reviewAnalyzer = new ReviewAnalyzer
    reviewAnalyzer.determineMostUsedWords(reviewsRdd, 5) should be(expectedMostActiveUsers)
  }
}
