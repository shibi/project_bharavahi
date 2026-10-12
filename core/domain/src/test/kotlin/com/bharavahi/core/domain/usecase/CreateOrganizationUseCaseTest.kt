package com.bharavahi.core.domain.usecase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.fake.FakeActivityRepository
import com.bharavahi.core.domain.fake.FakeAdminAccountGateway
import com.bharavahi.core.domain.fake.FakeAuthRepository
import com.bharavahi.core.domain.fake.FakeOrganizationRepository
import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.AuthUser
import com.bharavahi.core.domain.model.NewAdmin
import com.bharavahi.core.domain.model.OrganizationDetails
import com.bharavahi.core.domain.usecase.organization.CreateOrganizationUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreateOrganizationUseCaseTest {
    private val auth = FakeAuthRepository(AuthUser("owner", "owner@x.com"))
    private val accounts = FakeAdminAccountGateway()
    private val orgs = FakeOrganizationRepository()
    private val activity = FakeActivityRepository()
    private val useCase = CreateOrganizationUseCase(auth, accounts, orgs, activity)

    private val details = OrganizationDetails(" Green Park RWA ", "Residents association", "", "+91 98765 43210")
    private val admin = NewAdmin("Anil", "anil@x.com", "", "temp-pass-1")

    @Test
    fun `creates login, community and log entry`() = runTest {
        val orgId = useCase(details, admin).getOrThrow()

        assertThat(orgs.orgs.value[orgId]!!.name).isEqualTo("Green Park RWA")
        assertThat(orgs.firstAdmins[orgId]!!.uid).isEqualTo("admin-anil@x.com")
        assertThat(accounts.created.single().committed).isTrue()
        assertThat(activity.logs).containsExactly(orgId to ActivityAction.ORG_CREATED)
    }

    @Test
    fun `rolls back the login when saving the community fails`() = runTest {
        orgs.failCreate = true

        val result = useCase(details, admin)

        assertThat(result.exceptionOrNull()).isInstanceOf(DomainException.Network::class.java)
        assertThat(accounts.created.single().rolledBack).isTrue()
        assertThat(accounts.created.single().committed).isFalse()
    }

    @Test
    fun `validation errors stop before any login is created`() = runTest {
        val bad = NewAdmin("", "nope", "12", "short")
        val error = useCase(details.copy(name = ""), bad).exceptionOrNull() as DomainException.Validation

        assertThat(error.errors.keys).containsExactly(
            Field.NAME, Field.ADMIN_NAME, Field.ADMIN_EMAIL, Field.ADMIN_PHONE, Field.PASSWORD,
        )
        assertThat(accounts.created).isEmpty()
    }
}
