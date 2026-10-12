package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.ActivityLog
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun observeRecent(limit: Int): Flow<List<ActivityLog>>
    fun observeForOrganization(orgId: String, limit: Int): Flow<List<ActivityLog>>
    suspend fun log(orgId: String, action: ActivityAction, actorUid: String, target: String)
}
