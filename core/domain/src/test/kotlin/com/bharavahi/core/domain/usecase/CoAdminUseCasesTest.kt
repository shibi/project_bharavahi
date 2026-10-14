package com.bharavahi.core.domain.usecase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.fake.FakeActivityRepository
import com.bharavahi.core.domain.fake.FakeAdminAccountGateway
import com.bharavahi.core.domain.fake.FakeAuthRepository
import com.bharavahi.core.domain.fake.FakeOrgAdminRepository
import com.bharavahi.core.domain.fake.FakeOrgContext
import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.AuthUser
import com.bharavahi.core.domain.model.NewAdmin
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.usecase.orgadmin.AddCoAdminUseCase
import com.bharavahi.core.domain.usecase.orgadmin.SetCoAdminActiveUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CoAdminUseCasesTest {
    private val auth = FakeAuthRepository(AuthUser("me", "me@x.com"))
    private val org = FakeOrgContext("org1")
    private val accounts = FakeAdminAccountGateway()
    private val activity = FakeActivityRepository()

    private fun admin(uid: String, active: Boolean = true) =
        OrgAdmin(uid = uid, orgId = "org1", name = uid, email = "$uid@x.com", phone = "", active = active)

    private val newAdmin = NewAdmin(" Beena ", "beena@x.com", "", "temp-pass-1")

    @Test
    fun `adds a co-admin with a committed login and a log entry`() = runTest {
        val admins = FakeOrgAdminRepository(listOf(admin("me")))
        AddCoAdminUseCase(auth, org, admins, accounts, activity)(newAdmin).getOrThrow()

        val added = admins.admins.value.single { it.uid == "admin-beena@x.com" }
        assertThat(added.name).isEqualTo("Beena")
        assertThat(added.active).isTrue()
        assertThat(accounts.created.single().committed).isTrue()
        assertThat(activity.logs).containsExactly("org1" to ActivityAction.ADMIN_ADDED)
    }

    @Test
    fun `refuses a fourth active admin without creating a login`() = runTest {
        val admins = FakeOrgAdminRepository(listOf(admin("me"), admin("a"), admin("b")))
        val result = AddCoAdminUseCase(auth, org, admins, accounts, activity)(newAdmin)

        assertThat(result.exceptionOrNull()).isInstanceOf(DomainException.AdminLimitReached::class.java)
        assertThat(accounts.created).isEmpty()
    }

    @Test
    fun `inactive admins do not use up a slot`() = runTest {
        val admins = FakeOrgAdminRepository(listOf(admin("me"), admin("a"), admin("b", active = false)))
        assertThat(AddCoAdminUseCase(auth, org, admins, accounts, activity)(newAdmin).isSuccess).isTrue()
    }

    @Test
    fun `rolls back the login when linking it fails`() = runTest {
        val admins = FakeOrgAdminRepository(listOf(admin("me"))).apply { failAdd = true }
        val result = AddCoAdminUseCase(auth, org, admins, accounts, activity)(newAdmin)

        assertThat(result.isFailure).isTrue()
        assertThat(accounts.created.single().rolledBack).isTrue()
    }

    @Test
    fun `validates the form before creating a login`() = runTest {
        val admins = FakeOrgAdminRepository(listOf(admin("me")))
        val result = AddCoAdminUseCase(auth, org, admins, accounts, activity)(NewAdmin("", "bad", "", "x"))

        val errors = (result.exceptionOrNull() as DomainException.Validation).errors
        assertThat(errors.keys).containsAtLeast(Field.ADMIN_NAME, Field.ADMIN_EMAIL, Field.PASSWORD)
        assertThat(accounts.created).isEmpty()
    }

    @Test
    fun `deactivates another admin`() = runTest {
        val admins = FakeOrgAdminRepository(listOf(admin("me"), admin("a")))
        SetCoAdminActiveUseCase(auth, org, admins, activity)(admin("a"), false).getOrThrow()

        assertThat(admins.admins.value.single { it.uid == "a" }.active).isFalse()
        assertThat(activity.logs).containsExactly("org1" to ActivityAction.ADMIN_DEACTIVATED)
    }

    @Test
    fun `an admin cannot deactivate themselves`() = runTest {
        val admins = FakeOrgAdminRepository(listOf(admin("me"), admin("a")))
        val result = SetCoAdminActiveUseCase(auth, org, admins, activity)(admin("me"), false)

        assertThat(result.exceptionOrNull()).isInstanceOf(DomainException.NotAuthorized::class.java)
        assertThat(admins.admins.value.single { it.uid == "me" }.active).isTrue()
    }

    @Test
    fun `reactivating is refused when three admins are already active`() = runTest {
        val admins = FakeOrgAdminRepository(listOf(admin("me"), admin("a"), admin("b"), admin("c", active = false)))
        val result = SetCoAdminActiveUseCase(auth, org, admins, activity)(admin("c", active = false), true)

        assertThat(result.exceptionOrNull()).isInstanceOf(DomainException.AdminLimitReached::class.java)
    }
}
