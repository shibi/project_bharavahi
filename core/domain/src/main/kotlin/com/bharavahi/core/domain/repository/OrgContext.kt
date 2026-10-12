package com.bharavahi.core.domain.repository

/** The community the signed-in admin works in. Throws NotAuthorized when nobody is signed in. */
interface OrgContext {
    suspend fun orgId(): String
}
