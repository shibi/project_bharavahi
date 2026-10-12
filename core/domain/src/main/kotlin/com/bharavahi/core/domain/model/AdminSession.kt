package com.bharavahi.core.domain.model

/** Links a community admin's login to their community (adminIndex/{uid}). */
data class AdminLink(val orgId: String, val active: Boolean)

/** Session for the Organization app: a signed-in user is always tied to one community. */
sealed interface AdminSessionState {
    data object Loading : AdminSessionState
    data object SignedOut : AdminSessionState
    data class SignedIn(val user: AuthUser, val orgId: String) : AdminSessionState
}
