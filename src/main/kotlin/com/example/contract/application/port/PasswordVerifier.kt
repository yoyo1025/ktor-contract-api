package com.example.contract.application.port

interface PasswordVerifier {
    fun verify(
        rawPassword: String,
        hashedPassword: String,
    ): Boolean
}
