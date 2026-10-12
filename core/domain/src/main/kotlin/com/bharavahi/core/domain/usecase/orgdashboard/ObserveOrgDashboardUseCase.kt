package com.bharavahi.core.domain.usecase.orgdashboard

import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.repository.EventRepository
import com.bharavahi.core.domain.repository.MemberRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class OrgDashboard(
    val communityName: String,
    val activeMembers: Int,
    val activeEvents: List<Event>,
)

/** ORG-5 (first part): community name, member count and active events. Due figures arrive with payments. */
class ObserveOrgDashboardUseCase @Inject constructor(
    private val org: OrgContext,
    private val organizations: OrganizationRepository,
    private val members: MemberRepository,
    private val events: EventRepository,
) {
    operator fun invoke(): Flow<OrgDashboard> = flow {
        val orgId = org.orgId()
        emitAll(
            combine(
                organizations.observe(orgId),
                members.observeAll(orgId),
                events.observeAll(orgId),
            ) { organization, memberList, eventList ->
                OrgDashboard(
                    communityName = organization?.name.orEmpty(),
                    activeMembers = memberList.count { it.status == MemberStatus.ACTIVE },
                    activeEvents = eventList.filter { it.status == EventStatus.ACTIVE },
                )
            },
        )
    }
}
