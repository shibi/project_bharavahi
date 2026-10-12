package com.bharavahi.core.domain.usecase.organization

import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** SA-3: list with search by name and an optional status filter. */
class ObserveOrganizationsUseCase @Inject constructor(
    private val organizations: OrganizationRepository,
) {
    operator fun invoke(): Flow<List<Organization>> = organizations.observeAll()

    companion object {
        fun filter(
            all: List<Organization>,
            query: String,
            status: OrganizationStatus?,
        ): List<Organization> {
            val q = query.trim()
            return all.filter { org ->
                (status == null || org.status == status) &&
                    (q.isEmpty() || org.name.contains(q, ignoreCase = true) || org.type.contains(q, ignoreCase = true))
            }
        }
    }
}
