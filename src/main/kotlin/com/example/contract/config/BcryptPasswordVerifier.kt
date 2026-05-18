package com.example.contract.config

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.contract.application.port.PasswordVerifier

class BcryptPasswordVerifier : PasswordVerifier {
    override fun verify(
        rawPassword: String,
        hashedPassword: String,
    ): Boolean = BCrypt.verifyer().verify(rawPassword.toCharArray(), hashedPassword).verified
}
