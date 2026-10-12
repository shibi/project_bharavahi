package com.bharavahi.core.domain.usecase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.fake.FakeActivityRepository
import com.bharavahi.core.domain.fake.FakeAuthRepository
import com.bharavahi.core.domain.fake.FakeOrganizationRepository
import com.bharavahi.core.domain.fake.org
import com.bharavahi.core.domain.model.AuthUser
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.domain.usecase.dashboard.ObserveDashboardUseCase
import com.bharavahi.core.domain.usecase.organization.DeleteOrganizationUseCase
import com.bharavahi.core.domain.usecase.organization.ObserveOrganizationsUseCase
import com.bharavahi.core.domain.usecase.organization.SetOrganizationStatusUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OrganizationRulesTest {
    private val auth = FakeAuthRepository(AuthUser("owner", "owner@x.com"))
    private val repo = FakeOrganizationRepository()
    private val activity = FakeActivityRepository()

    @Test
    fun `delete requires the exact community name`() = runTest {
        val green = org("o1", "Green Park RWA", OrganizationStatus.ACTIVE)
        repo.orgs.value = mapOf("o1" to green)
        val delete = DeleteOrganizationUseCase(auth, repo, activity)

        assertThat(delete(green, "green park").exceptionOrNull())
            .isInstanceOf(DomainException.ConfirmationMismatch::class.java)
        assertThat(repo.orgs.value["o1"]!!.status).isEqualTo(OrganizationStatus.ACTIVE)

        delete(green, "Green Park RWA").getOrThrow()
        assertThat(repo.orgs.value["o1"]!!.status).isEqualTo(OrganizationStatus.DELETED)
    }

    @Test
    fun `suspend and reactivate`() = runTest {
        val green = org("o1", "Green Park RWA", OrganizationStatus.ACTIVE)
        repo.orgs.value = mapOf("o1" to green)
        val setStatus = SetOrganizationStatusUseCase(auth, repo, activity)

        setStatus(green, suspend = true).getOrThrow()
        assertThat(repo.orgs.value["o1"]!!.status).isEqualTo(OrganizationStatus.SUSPENDED)
        setStatus(green, suspend = false).getOrThrow()
        assertThat(repo.orgs.value["o1"]!!.status).isEqualTo(OrganizationStatus.ACTIVE)
    }

    @Test
    fun `filter by name, type and status`() {
        val all = listOf(
            org("1", "Green Park RWA", OrganizationStatus.ACTIVE),
            org("2", "Lake View Club", OrganizationStatus.SUSPENDED, type = "Club"),
            org("3", "Green Valley", OrganizationStatus.SUSPENDED),
        )
        assertThat(ObserveOrganizationsUseCase.filter(all, "green", null).map { it.id }).containsExactly("1", "3")
        assertThat(ObserveOrganizationsUseCase.filter(all, "club", null).map { it.id }).containsExactly("2")
        assertThat(ObserveOrganizationsUseCase.filter(all, "", OrganizationStatus.SUSPENDED).map { it.id })
            .containsExactly("2", "3")
    }

    @Test
    fun `stats ignore deleted communities`() {
        val stats = ObserveDashboardUseCase.statsOf(
            listOf(
                org("1", "A", OrganizationStatus.ACTIVE, members = 40, events = 2),
                org("2", "B", OrganizationStatus.SUSPENDED, members = 10, events = 1),
                org("3", "C", OrganizationStatus.DELETED, members = 99, events = 9),
            ),
        )
        assertThat(stats.activeOrganizations).isEqualTo(1)
        assertThat(stats.suspendedOrganizations).isEqualTo(1)
        assertThat(stats.totalOrganizations).isEqualTo(2)
        assertThat(stats.totalMembers).isEqualTo(50)
        assertThat(stats.totalEvents).isEqualTo(3)
    }
}
