package com.bharavahi.core.domain.usecase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.fake.FakeAuthRepository
import com.bharavahi.core.domain.fake.FakeSuperAdminRepository
import com.bharavahi.core.domain.usecase.auth.SignInUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignInUseCaseTest {
    private val auth = FakeAuthRepository()

    @Test
    fun `super admin signs in`() = runTest {
        val useCase = SignInUseCase(auth, FakeSuperAdminRepository(setOf("uid-owner@x.com")))
        val result = useCase("owner@x.com", "secret123")
        assertThat(result.getOrThrow().uid).isEqualTo("uid-owner@x.com")
        assertThat(auth.signedOut).isFalse()
    }

    @Test
    fun `non super admin is signed out and refused`() = runTest {
        val useCase = SignInUseCase(auth, FakeSuperAdminRepository(emptySet()))
        val result = useCase("admin@x.com", "secret123")
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainException.NotAuthorized::class.java)
        assertThat(auth.signedOut).isTrue()
    }

    @Test
    fun `invalid email and empty password fail validation without calling auth`() = runTest {
        auth.failSignIn = true
        val useCase = SignInUseCase(auth, FakeSuperAdminRepository(emptySet()))
        val error = useCase("not-an-email", "").exceptionOrNull() as DomainException.Validation
        assertThat(error.errors.keys).containsExactly(Field.EMAIL, Field.PASSWORD)
    }
}
