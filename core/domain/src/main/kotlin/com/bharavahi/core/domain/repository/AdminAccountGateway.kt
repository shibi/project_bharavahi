package com.bharavahi.core.domain.repository

/**
 * Creates Firebase logins for other people without signing out the current user.
 */
interface AdminAccountGateway {
    suspend fun createLogin(email: String, password: String): PendingLogin
}

/**
 * A login that exists but is not yet linked to a community.
 * Call [commit] once the community data is saved, or [rollback] to delete the login.
 */
interface PendingLogin {
    val uid: String
    suspend fun commit()
    suspend fun rollback()
}
