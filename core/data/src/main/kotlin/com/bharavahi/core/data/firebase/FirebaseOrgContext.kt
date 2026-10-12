package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.repository.AdminIndexRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

/** Looks up the signed-in admin's community once per login and remembers it. */
@Singleton
internal class FirebaseOrgContext @Inject constructor(
    private val auth: FirebaseAuth,
    private val adminIndex: AdminIndexRepository,
) : OrgContext {

    @Volatile private var cached: Pair<String, String>? = null

    override suspend fun orgId(): String {
        val uid = auth.currentUser?.uid ?: throw DomainException.NotAuthorized()
        cached?.let { (cachedUid, orgId) -> if (cachedUid == uid) return orgId }
        val link = adminIndex.find(uid)
        if (link == null || !link.active || link.orgId.isEmpty()) throw DomainException.NotAuthorized()
        cached = uid to link.orgId
        return link.orgId
    }
}
