package com.bharavahi.core.domain.usecase.auth

import com.bharavahi.core.domain.model.SessionState
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.SuperAdminRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Emits the session, treating a signed-in user who is not a super admin as signed out. */
class ObserveSessionUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val superAdmins: SuperAdminRepository,
) {
    operator fun invoke(): Flow<SessionState> = auth.session.map { state ->
        if (state is SessionState.SignedIn && !isSuperAdminSafely(state.user.uid)) {
            auth.signOut()
            SessionState.SignedOut
        } else {
            state
        }
    }

    private suspend fun isSuperAdminSafely(uid: String): Boolean =
        // Offline at startup: trust the cached login rather than locking the owner out.
        runCatching { superAdmins.isSuperAdmin(uid) }.getOrDefault(true)
}
