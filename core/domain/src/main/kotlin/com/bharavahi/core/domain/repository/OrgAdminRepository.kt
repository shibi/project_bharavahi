package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.OrgAdmin
import kotlinx.coroutines.flow.Flow

interface OrgAdminRepository {
    fun observeAdmins(orgId: String): Flow<List<OrgAdmin>>
    suspend fun setActive(orgId: String, uid: String, active: Boolean)

    /** Writes the admin record and its login index together, linking an existing login to the community. */
    suspend fun add(orgId: String, admin: OrgAdmin, addedBy: String)
}
