package com.bharavahi.core.domain.usecase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.fake.FakeBackupScheduler
import com.bharavahi.core.domain.fake.FakeEventRepository
import com.bharavahi.core.domain.fake.FakeOrgContext
import com.bharavahi.core.domain.fake.FakePaymentRepository
import com.bharavahi.core.domain.fake.FakeScheduleRepository
import com.bharavahi.core.domain.fake.eventMember
import com.bharavahi.core.domain.model.NewExpense
import com.bharavahi.core.domain.model.NewPayment
import com.bharavahi.core.domain.model.PaymentMethod
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.repository.ExpenseRepository
import com.bharavahi.core.domain.rules.DueGenerator
import com.bharavahi.core.domain.usecase.expense.AddExpenseUseCase
import com.bharavahi.core.domain.usecase.payment.RecordPaymentUseCase
import com.bharavahi.core.domain.usecase.payment.ReversePaymentUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class PaymentUseCasesTest {
    private val start = LocalDate.parse("2026-01-01")
    private val schedule = Schedule(
        "s1", "Monthly", "", 150_000, 3, start, LocalDate.parse("2026-04-01"),
        DueGenerator.generate(150_000, 3, start, LocalDate.parse("2026-04-01")), false,
    )
    private val events = FakeEventRepository().apply {
        members.value = mapOf("m1" to eventMember("m1", paidByDue = mapOf("s1_0" to 20_000L)))
    }
    private val schedules = FakeScheduleRepository().apply { schedules.value = mapOf("s1" to schedule) }
    private val payments = FakePaymentRepository()
    private val backup = FakeBackupScheduler()
    private val record = RecordPaymentUseCase(FakeOrgContext(), events, schedules, payments, backup)

    private fun pay(amount: Long, due: Int = 0) =
        NewPayment("m1", "s1", due, amount, PaymentMethod.OFFLINE, " cash ", LocalDate.parse("2026-01-05"))

    @Test
    fun `records within what is owed and queues the backup email`() = runTest {
        val id = record("e1", pay(30_000)).getOrThrow()
        assertThat(payments.payments.value.single().note).isEqualTo("cash")
        assertThat(backup.queued).containsExactly(id)
    }

    @Test
    fun `BR-8 more than owed on the due is refused`() = runTest {
        val error = record("e1", pay(30_001)).exceptionOrNull() as DomainException.Validation
        assertThat(error.errors).containsEntry(Field.AMOUNT, ValidationError.AMOUNT_TOO_LARGE)
        assertThat(backup.queued).isEmpty()
    }

    @Test
    fun `zero amount and unknown member are refused`() = runTest {
        val zero = record("e1", pay(0)).exceptionOrNull() as DomainException.Validation
        assertThat(zero.errors).containsEntry(Field.AMOUNT, ValidationError.INVALID_AMOUNT)
        val stranger = record("e1", pay(100).copy(memberId = "x")).exceptionOrNull() as DomainException.Validation
        assertThat(stranger.errors).containsEntry(Field.MEMBER, ValidationError.REQUIRED)
    }

    @Test
    fun `fully paid due is refused`() = runTest {
        events.members.value = mapOf("m1" to eventMember("m1", paidByDue = mapOf("s1_0" to 50_000L)))
        val error = record("e1", pay(100)).exceptionOrNull() as DomainException.Validation
        assertThat(error.errors).containsEntry(Field.DUE, ValidationError.DUE_ALREADY_PAID)
    }

    @Test
    fun `ORG-25 reversal is a negative linked entry and only once`() = runTest {
        val id = record("e1", pay(30_000)).getOrThrow()
        val reverse = ReversePaymentUseCase(FakeOrgContext(), payments, backup)
        val reversalId = reverse("e1", id, LocalDate.parse("2026-01-06")).getOrThrow()
        val reversal = payments.payments.value.first { it.id == reversalId }
        assertThat(reversal.amount).isEqualTo(-30_000)
        assertThat(reversal.reversesPaymentId).isEqualTo(id)
        assertThat(reverse("e1", id, LocalDate.parse("2026-01-06")).exceptionOrNull())
            .isInstanceOf(DomainException.AlreadyReversed::class.java)
        assertThat(reverse("e1", reversalId, LocalDate.parse("2026-01-06")).exceptionOrNull())
            .isInstanceOf(DomainException.AlreadyReversed::class.java)
    }

    @Test
    fun `expense needs a title, an amount and a date`() = runTest {
        var saved: NewExpense? = null
        val repo = object : ExpenseRepository {
            override fun observeAll(orgId: String, eventId: String) = flowOf(emptyList<com.bharavahi.core.domain.model.Expense>())
            override suspend fun add(orgId: String, eventId: String, expense: NewExpense): String { saved = expense; return "x1" }
        }
        val add = AddExpenseUseCase(FakeOrgContext(), repo)
        val error = add("e1", NewExpense(" ", 0, null, "", null)).exceptionOrNull() as DomainException.Validation
        assertThat(error.errors.keys).containsExactly(Field.TITLE, Field.AMOUNT, Field.DATE)
        add("e1", NewExpense(" Lights ", 20_000, start, "", "m1")).getOrThrow()
        assertThat(saved?.title).isEqualTo("Lights")
    }
}
