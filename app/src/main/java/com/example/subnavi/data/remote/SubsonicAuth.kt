package com.example.subnavi.data.remote

import java.security.MessageDigest
import java.util.UUID

object SubsonicAuth {
    fun generateToken(password: String): Pair<String, String> {
        val salt = UUID.randomUUID().toString()
        val token = md5(password + salt)
        return Pair(token, salt)
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
