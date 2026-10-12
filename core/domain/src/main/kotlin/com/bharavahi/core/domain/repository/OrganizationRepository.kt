package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.model.OrganizationDetails
import com.bharavahi.core.domain.model.OrganizationStatus
import kotlinx.coroutines.flow.Flow

interface OrganizationRepository {
    /** All communities except soft-deleted ones, sorted by name. */
    fun observeAll(): Flow<List<Organization>>
    fun observe(orgId: String): Flow<Organization?>

    /**
     * Creates the community and links [firstAdmin] to it in one batch:
     * organizations/{orgId}, admins/{uid} and adminIndex/{uid}.
     * Returns the new orgId.
     */
    suspend fun create(details: OrganizationDetails, firstAdmin: OrgAdmin): String
    suspend fun update(orgId: String, details: OrganizationDetails)

    /**
     * Changes status. Security rules refuse every admin of a non-active community, so suspension takes effect
     * immediately. DELETED also deactivates every adminIndex entry of the community for good.
     */
    suspend fun setStatus(orgId: String, status: OrganizationStatus)
}
