package com.bharavahi.core.domain.usecase.organization

import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveOrganizationUseCase @Inject constructor(
    private val organizations: OrganizationRepository,
) {
    operator fun invoke(orgId: String): Flow<Organization?> = organizations.observe(orgId)
}
