package com.bharavahi.core.domain.usecase.auth

import com.bharavahi.core.domain.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke() = auth.signOut()
}
