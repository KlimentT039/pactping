package com.klt.pactping.domain

data class Witness(
  val id: String,
  val name: String,
  val phoneNumber: String,
) {
  val initials: String
    get() = name
      .split(" ")
      .filter { it.isNotBlank() }
      .take(2)
      .map { it.first().uppercaseChar() }
      .joinToString("")
}
