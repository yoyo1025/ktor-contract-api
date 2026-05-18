package com.example.contract.config

import com.example.contract.application.port.PasswordVerifier
import com.example.contract.application.port.TokenGenerator
import com.example.contract.application.usecase.ContractUseCase
import com.example.contract.application.usecase.LoginUseCase
import com.example.contract.domain.repository.ContractRepository
import com.example.contract.domain.repository.UserRepository
import com.example.contract.infrastructure.repository.ExposedContractRepository
import com.example.contract.infrastructure.repository.ExposedUserRepository
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule =
    module {
        single { JwtConfig.fromEnvironment() }
        single<ContractRepository> { ExposedContractRepository() }
        single<UserRepository> { ExposedUserRepository() }
        single<PasswordVerifier> { BcryptPasswordVerifier() }
        single<TokenGenerator> { JwtTokenGenerator(get()) }
        single { ContractUseCase(get()) }
        single { LoginUseCase(get(), get(), get()) }
    }

fun setupKoin(modules: List<Module> = listOf(appModule)) {
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(modules)
        }
    }
}

inline fun <reified T : Any> koinGet(): T = GlobalContext.get().get()
