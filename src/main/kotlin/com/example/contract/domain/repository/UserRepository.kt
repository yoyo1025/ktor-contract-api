package com.example.contract.domain.repository

import com.example.contract.domain.model.User

interface UserRepository {
    fun findByLoginId(loginId: String): User?
}
