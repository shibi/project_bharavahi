package com.bharavahi.core.domain.usecase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.fake.FakeAdminIndexRepository
import com.bharavahi.core.domain.fake.FakeAuthRepository
import com.bharavahi.core.domain.fake.FakeEventRepository
import com.bharavahi.core.domain.fake.FakeOrgContext
import com.bharavahi.core.domain.fake.FakeOrganizationRepository
import com.bharavahi.core.domain.fake.FakeScheduleRepository
import com.bharavahi.core.domain.fake.FakeSheetSyncScheduler
import com.bharavahi.core.domain.fake.eventMember
import com.bharavahi.core.domain.fake.org
import com.bharavahi.core.domain.model.AdminLink
import com.bharavahi.core.domain.model.AdminSessionState
import com.bharavahi.core.domain.model.EventDetails
import com.bharavahi.core.domain.model.EventMemberAssignment
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.BloodGroup
import com.bharavahi.core.domain.model.Gender
import com.bharavahi.core.domain.model.Member
import com.bharavahi.core.domain.model.MemberDetails
import com.bharavahi.core.domain.model.MemberExtras
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.model.NewEvent
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.domain.model.ScheduleDetails
import com.bharavahi.core.domain.model.ShirtSize
import com.bharavahi.core.domain.repository.MemberRepository
import com.bharavahi.core.domain.usecase.event.CreateEventUseCase
import com.bharavahi.core.domain.usecase.event.RemoveEventMemberUseCase
import com.bharavahi.core.domain.usecase.event.UpdateEventRolesUseCase
import com.bharavahi.core.domain.usecase.member.AddMemberUseCase
import com.bharavahi.core.domain.usecase.member.ObserveMembersUseCase
import com.bharavahi.core.domain.usecase.orgauth.ObserveAdminSessionUseCase
import com.bharavahi.core.domain.usecase.orgauth.OrgSignInUseCase
import com.bharavahi.core.domain.usecase.schedule.CreateScheduleUseCase
import com.bharavahi.core.domain.usecase.schedule.PreviewDuesUseCase
import com.bharavahi.core.domain.usecase.schedule.UpdateScheduleUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class OrgSignInUseCaseTest {
    private val auth = FakeAuthRepository()
    private val orgs = FakeOrganizationRepository().apply {
        this.orgs.value = mapOf(
            "org1" to org("org1", "Green Park", OrganizationStatus.ACTIVE),
            "org2" to org("org2", "Paused", OrganizationStatus.SUSPENDED),
        )
    }

    private fun useCase(vararg links: Pair<String, AdminLink>) =
        OrgSignInUseCase(auth, FakeAdminIndexRepository(links.toMap()), orgs)

    @Test
    fun `active admin of an active community signs in`() = runTest {
        val result = useCase("uid-a@x.com" to AdminLink("org1", true))("a@x.com", "secret123")
        assertThat(result.isSuccess).isTrue()
        assertThat(auth.signedOut).isFalse()
    }

    @Test
    fun `login without an admin link is refused`() = runTest {
        val result = useCase()("a@x.com", "secret123")
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainException.NotAuthorized::class.java)
        assertThat(auth.signedOut).isTrue()
    }

    @Test
    fun `deactivated admin is refused`() = runTest {
        val result = useCase("uid-a@x.com" to AdminLink("org1", false))("a@x.com", "secret123")
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainException.NotAuthorized::class.java)
    }

    @Test
    fun `admin of a suspended community is refused`() = runTest {
        val result = useCase("uid-a@x.com" to AdminLink("org2", true))("a@x.com", "secret123")
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainException.NotAuthorized::class.java)
        assertThat(auth.signedOut).isTrue()
    }

    @Test
    fun `session signs out when the admin is deactivated`() = runTest {
        val index = FakeAdminIndexRepository(mapOf("uid-a@x.com" to AdminLink("org1", true)))
        auth.signIn("a@x.com", "secret123")
        val session = ObserveAdminSessionUseCase(auth, index)
        assertThat(session().first()).isEqualTo(AdminSessionState.SignedIn(auth.user!!, "org1"))

        index.links.value = mapOf("uid-a@x.com" to AdminLink("org1", false))
        assertThat(session().first()).isEqualTo(AdminSessionState.SignedOut)
        assertThat(auth.signedOut).isTrue()
    }
}

class MemberFilterTest {
    private val members = listOf(
        Member("1", "Anil Kumar", "Appu", "98470 12345", "", MemberStatus.ACTIVE, null),
        Member("2", "Beena", "", "9847099999", "", MemberStatus.DISABLED, null),
        Member("3", "Joseph", "Jo", "", "", MemberStatus.ARCHIVED, null),
    )

    @Test
    fun `search matches name, nickname and phone digits`() {
        assertThat(ObserveMembersUseCase.filter(members, "anil", null).map { it.id }).containsExactly("1")
        assertThat(ObserveMembersUseCase.filter(members, "jo", null).map { it.id }).containsExactly("3")
        assertThat(ObserveMembersUseCase.filter(members, "9847012345", null).map { it.id }).containsExactly("1")
    }

    @Test
    fun `status filter`() {
        assertThat(ObserveMembersUseCase.filter(members, "", MemberStatus.DISABLED).map { it.id }).containsExactly("2")
        assertThat(ObserveMembersUseCase.filter(members, "", null)).hasSize(3)
    }
}

class MemberUseCasesTest {
    private val repo = object : MemberRepository {
        var added: MemberDetails? = null
        override fun observeAll(orgId: String) = emptyFlow<List<Member>>()
        override fun observe(orgId: String, memberId: String) = emptyFlow<Member?>()
        override suspend fun add(orgId: String, details: MemberDetails): String {
            added = details
            return "m1"
        }
        override suspend fun update(orgId: String, memberId: String, details: MemberDetails) = Unit
        override suspend fun setStatus(orgId: String, memberId: String, status: MemberStatus) = Unit
    }
    private val add = AddMemberUseCase(FakeOrgContext(), repo)
    private val today = LocalDate.parse("2026-10-14")

    @Test
    fun `additional details are optional`() = runTest {
        assertThat(add(MemberDetails("Anil", "", "", ""), today).isSuccess).isTrue()
        assertThat(repo.added!!.extras).isEqualTo(MemberExtras())
    }

    @Test
    fun `additional details are kept and trimmed`() = runTest {
        val extras = MemberExtras(
            dateOfBirth = LocalDate.parse("1990-05-01"),
            gender = Gender.FEMALE,
            bloodGroup = BloodGroup.O_NEG,
            job = " Teacher ",
            houseName = " Rose Villa ",
            shirtSize = ShirtSize.M,
        )
        add(MemberDetails("Beena", "", "", "", extras), today).getOrThrow()
        assertThat(repo.added!!.extras).isEqualTo(extras.copy(job = "Teacher", houseName = "Rose Villa"))
    }

    @Test
    fun `date of birth in the future is rejected`() = runTest {
        val details = MemberDetails("Anil", "", "", "", MemberExtras(dateOfBirth = today.plusDays(1)))
        val error = add(details, today).exceptionOrNull() as DomainException.Validation
        assertThat(error.errors).containsEntry(Field.DATE_OF_BIRTH, ValidationError.FUTURE_DATE)
        assertThat(add(details.copy(extras = MemberExtras(dateOfBirth = today)), today).isSuccess).isTrue()
    }
}

class EventUseCasesTest {
    private val events = FakeEventRepository()
    private val details = EventDetails("Onam 2026", "", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), 0)

    @Test
    fun `create drops unknown roles and duplicate members`() = runTest {
        val roles = listOf(EventRole("r1", " President "))
        val members = listOf(
            EventMemberAssignment("m1", "Anil", "r1"),
            EventMemberAssignment("m1", "Anil", "r1"),
            EventMemberAssignment("m2", "Beena", "gone"),
        )
        CreateEventUseCase(FakeOrgContext(), events)(NewEvent(details, roles, members)).getOrThrow()
        val saved = events.created!!
        assertThat(saved.members.map { it.memberId }).containsExactly("m1", "m2")
        assertThat(saved.members.last().roleId).isNull()
        assertThat(saved.roles.single().name).isEqualTo("President")
    }

    @Test
    fun `duplicate role names and end before start are rejected`() = runTest {
        val bad = details.copy(endDate = LocalDate.parse("2025-12-01"))
        val roles = listOf(EventRole("r1", "Secretary"), EventRole("r2", "secretary"))
        val error = CreateEventUseCase(FakeOrgContext(), events)(NewEvent(bad, roles, emptyList()))
            .exceptionOrNull() as DomainException.Validation
        assertThat(error.errors).containsEntry(Field.ROLE_NAME, ValidationError.DUPLICATE)
        assertThat(error.errors).containsEntry(Field.END_DATE, ValidationError.END_BEFORE_START)
    }

    @Test
    fun `removing a role clears it from members`() = runTest {
        events.members.value = mapOf("m1" to eventMember("m1", roleId = "r1"), "m2" to eventMember("m2", roleId = "r2"))
        UpdateEventRolesUseCase(FakeOrgContext(), events)("e1", listOf(EventRole("r2", "Treasurer"))).getOrThrow()
        assertThat(events.members.value["m1"]!!.roleId).isNull()
        assertThat(events.members.value["m2"]!!.roleId).isEqualTo("r2")
    }

    @Test
    fun `member with payments cannot be removed`() = runTest {
        events.members.value = mapOf("m1" to eventMember("m1", paidByDue = mapOf("s1_0" to 100L)), "m2" to eventMember("m2"))
        val remove = RemoveEventMemberUseCase(FakeOrgContext(), events, FakeSheetSyncScheduler())
        assertThat(remove("e1", "m1").exceptionOrNull()).isInstanceOf(DomainException.MemberHasPayments::class.java)
        assertThat(remove("e1", "m2").isSuccess).isTrue()
        assertThat(events.members.value.keys).containsExactly("m1")
    }
}

class ScheduleUseCasesTest {
    private val schedules = FakeScheduleRepository()
    private val sheets = FakeSheetSyncScheduler()
    private val details = ScheduleDetails(
        name = "Monthly",
        description = "",
        startDate = LocalDate.parse("2026-01-01"),
        endDate = LocalDate.parse("2026-12-31"),
        amountPerMember = 300_000,
        numberOfDues = 12,
    )

    @Test
    fun `create generates and saves the dues`() = runTest {
        val id = CreateScheduleUseCase(FakeOrgContext(), schedules, sheets)("e1", details).getOrThrow()
        val saved = schedules.schedules.value.getValue(id)
        assertThat(saved.dues).hasSize(12)
        assertThat(saved.dues.first().amount).isEqualTo(25_000)
        assertThat(saved.locked).isFalse()
        assertThat(sheets.queued).containsExactly("e1")
    }

    @Test
    fun `preview validates amount and count`() {
        val preview = PreviewDuesUseCase()
        val error = preview(details.copy(amountPerMember = 0, numberOfDues = 0)).exceptionOrNull() as DomainException.Validation
        assertThat(error.errors).containsEntry(Field.AMOUNT, ValidationError.INVALID_AMOUNT)
        assertThat(error.errors).containsEntry(Field.NUMBER_OF_DUES, ValidationError.INVALID_COUNT)
    }

    @Test
    fun `less than a rupee per due is rejected`() {
        val error = PreviewDuesUseCase()(details.copy(amountPerMember = 500)).exceptionOrNull() as DomainException.Validation
        assertThat(error.errors).containsEntry(Field.AMOUNT, ValidationError.AMOUNT_TOO_SMALL)
    }

    @Test
    fun `ORG-16 locked schedule cannot be edited`() = runTest {
        val id = CreateScheduleUseCase(FakeOrgContext(), schedules, sheets)("e1", details).getOrThrow()
        schedules.schedules.value = schedules.schedules.value.mapValues { it.value.copy(locked = true) }
        val result = UpdateScheduleUseCase(FakeOrgContext(), schedules, sheets)("e1", id, details.copy(name = "New"))
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainException.ScheduleLocked::class.java)
    }
}
