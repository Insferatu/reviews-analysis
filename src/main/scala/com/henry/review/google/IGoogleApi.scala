package com.henry.review.google

trait IGoogleApi {
  def translate(inputLang: String, outputLang: String, text: String): String
}
