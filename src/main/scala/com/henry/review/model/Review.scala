package com.henry.review.model

import org.apache.spark.sql.Row

case class Review(id: String = "",
                  productId: String = "",
                  userId: String = "",
                  profileName: String = "",
                  helpfulnessNumerator: String = "",
                  helpfulnessDenominator: String = "",
                  score: String = "",
                  time: String = "",
                  summary: String = "",
                  text: String = "")

object Review {
  def apply(row: Row): Review = {
    Review.apply(
      id = row.getString(0),
      productId = row.getString(1),
      userId = row.getString(2),
      profileName = row.getString(3),
      helpfulnessNumerator = row.getString(4),
      helpfulnessDenominator = row.getString(5),
      score = row.getString(6),
      time = row.getString(7),
      summary = row.getString(8),
      text = row.getString(9)
    )
  }

  def apply(fields: Seq[String]): Review = {
    Review.apply(
      id = fields(0),
      productId = fields(1),
      userId = fields(2),
      profileName = fields(3),
      helpfulnessNumerator = fields(4),
      helpfulnessDenominator = fields(5),
      score = fields(6),
      time = fields(7),
      summary = fields(8),
      text = fields(9)
    )
  }
}
