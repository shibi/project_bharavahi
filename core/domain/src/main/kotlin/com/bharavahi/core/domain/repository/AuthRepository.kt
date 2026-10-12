package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.AuthUser
import com.bharavahi.core.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<SessionState>
    suspend fun signIn(email: String, password: String): AuthUser
    suspend fun signOut()
    fun currentUser(): AuthUser?
}
