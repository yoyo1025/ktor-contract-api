package com.example.contract.config

import com.example.contract.application.port.TokenGenerator
import com.example.contract.application.port.TokenResult
import com.example.contract.domain.model.UserId

/**
 * JWT認証実装(Step 7)までの仮実装。
 * ユーザIDをそのままトークンとして返す。
 */
class StubTokenGenerator : TokenGenerator {
    override fun generate(userId: UserId): TokenResult =
        TokenResult(
            accessToken = "stub-token-${userId.value}",
            tokenType = "Bearer",
            expiresIn = 3600,
        )
}
