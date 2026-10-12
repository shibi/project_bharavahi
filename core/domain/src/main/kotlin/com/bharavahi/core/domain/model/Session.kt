package com.bharavahi.core.domain.model

data class AuthUser(val uid: String, val email: String)

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: AuthUser) : SessionState
}
