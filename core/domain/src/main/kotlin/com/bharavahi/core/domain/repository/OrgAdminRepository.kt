package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.OrgAdmin
import kotlinx.coroutines.flow.Flow

interface OrgAdminRepository {
    fun observeAdmins(orgId: String): Flow<List<OrgAdmin>>
    suspend fun setActive(orgId: String, uid: String, active: Boolean)
}
