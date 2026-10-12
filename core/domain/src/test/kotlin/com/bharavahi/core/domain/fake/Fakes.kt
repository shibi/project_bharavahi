package com.bharavahi.core.domain.fake

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.ActivityLog
import com.bharavahi.core.domain.model.AuthUser
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.model.OrganizationDetails
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.domain.model.SessionState
import com.bharavahi.core.domain.repository.ActivityRepository
import com.bharavahi.core.domain.repository.AdminAccountGateway
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.OrganizationRepository
import com.bharavahi.core.domain.repository.PendingLogin
import com.bharavahi.core.domain.repository.SuperAdminRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAuthRepository(var user: AuthUser? = null) : AuthRepository {
    var signedOut = false
    var failSignIn = false
    private val state = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val session: Flow<SessionState> = state
    override suspend fun signIn(email: String, password: String): AuthUser {
        if (failSignIn) throw DomainException.InvalidCredentials()
        return AuthUser("uid-$email", email).also { user = it; state.value = SessionState.SignedIn(it) }
    }
    override suspend fun signOut() { signedOut = true; user = null; state.value = SessionState.SignedOut }
    override fun currentUser(): AuthUser? = user
}

class FakeSuperAdminRepository(private val superUids: Set<String>) : SuperAdminRepository {
    override suspend fun isSuperAdmin(uid: String) = uid in superUids
}

class FakePendingLogin(override val uid: String) : PendingLogin {
    var committed = false
    var rolledBack = false
    override suspend fun commit() { committed = true }
    override suspend fun rollback() { rolledBack = true }
}

class FakeAdminAccountGateway : AdminAccountGateway {
    val created = mutableListOf<FakePendingLogin>()
    override suspend fun createLogin(email: String, password: String): PendingLogin =
        FakePendingLogin("admin-$email").also { created += it }
}

class FakeOrganizationRepository : OrganizationRepository {
    val orgs = MutableStateFlow<Map<String, Organization>>(emptyMap())
    val firstAdmins = mutableMapOf<String, OrgAdmin>()
    var failCreate = false

    override fun observeAll(): Flow<List<Organization>> = orgs.map { it.values.sortedBy(Organization::name) }
    override fun observe(orgId: String): Flow<Organization?> = orgs.map { it[orgId] }

    override suspend fun create(details: OrganizationDetails, firstAdmin: OrgAdmin): String {
        if (failCreate) throw DomainException.Network()
        val id = "org${orgs.value.size + 1}"
        orgs.value = orgs.value + (id to org(id, details.name, OrganizationStatus.ACTIVE))
        firstAdmins[id] = firstAdmin.copy(orgId = id)
        return id
    }

    override suspend fun update(orgId: String, details: OrganizationDetails) {
        val o = orgs.value[orgId] ?: throw DomainException.NotFound()
        orgs.value = orgs.value + (orgId to o.copy(name = details.name, type = details.type))
    }

    override suspend fun setStatus(orgId: String, status: OrganizationStatus) {
        val o = orgs.value[orgId] ?: throw DomainException.NotFound()
        orgs.value = orgs.value + (orgId to o.copy(status = status))
    }
}

class FakeActivityRepository : ActivityRepository {
    val logs = mutableListOf<Pair<String, ActivityAction>>()
    override fun observeRecent(limit: Int): Flow<List<ActivityLog>> = MutableStateFlow(emptyList())
    override fun observeForOrganization(orgId: String, limit: Int): Flow<List<ActivityLog>> = MutableStateFlow(emptyList())
    override suspend fun log(orgId: String, action: ActivityAction, actorUid: String, target: String) {
        logs += orgId to action
    }
}

fun org(
    id: String,
    name: String,
    status: OrganizationStatus,
    members: Long = 0,
    events: Long = 0,
    type: String = "Residents association",
) = Organization(id, name, type, "", "", status, members, events, null)
