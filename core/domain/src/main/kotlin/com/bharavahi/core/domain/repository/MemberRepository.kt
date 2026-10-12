package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.Member
import com.bharavahi.core.domain.model.MemberDetails
import com.bharavahi.core.domain.model.MemberStatus
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    /** All members sorted by name. */
    fun observeAll(orgId: String): Flow<List<Member>>
    fun observe(orgId: String, memberId: String): Flow<Member?>

    /** Adds the member and increments organization.memberCount in one batch (BR-12). Returns the id. */
    suspend fun add(orgId: String, details: MemberDetails): String

    /** Also refreshes the name copy kept in every event the member is part of. */
    suspend fun update(orgId: String, memberId: String, details: MemberDetails)

    /** Also sets eventMembers.active in every event, so disabled members drop out of pending lists. */
    suspend fun setStatus(orgId: String, memberId: String, status: MemberStatus)
}
