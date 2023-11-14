package com.gravitycode.solitaryfitness.util.test

class TestException(message: String, cause: Throwable? = null) : Exception(message, cause)

fun testError(message: String, cause: Throwable? = null) {
    throw TestException(message, cause)
}