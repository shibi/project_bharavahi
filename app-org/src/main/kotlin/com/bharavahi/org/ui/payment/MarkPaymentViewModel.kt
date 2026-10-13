package com.bharavahi.org.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.MemberDueLine
import com.bharavahi.core.domain.model.NewPayment
import com.bharavahi.core.domain.model.PaymentMethod
import com.bharavahi.core.domain.rules.MemberDuesRules
import com.bharavahi.core.domain.usecase.event.ObserveEventsUseCase
import com.bharavahi.core.domain.usecase.payment.BuildWhatsAppMessageUseCase
import com.bharavahi.core.domain.usecase.payment.EventDues
import com.bharavahi.core.domain.usecase.payment.ObserveEventDuesUseCase
import com.bharavahi.core.domain.usecase.payment.RecordPaymentUseCase
import com.bharavahi.core.domain.validation.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MarkPaymentUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val events: List<Event> = emptyList(),
    val eventId: String? = null,
    val dues: EventDues? = null,
    val memberId: String? = null,
    val scheduleId: String? = null,
    val dueIndex: Int? = null,
    val amount: String = "",
    val method: PaymentMethod = PaymentMethod.OFFLINE,
    val note: String = "",
    val paidOn: LocalDate = LocalDate.now(),
    val fieldErrors: Map<Field, ValidationError> = emptyMap(),
    val error: Throwable? = null,
    /** Set once saved: the confirmation dialog with Share to WhatsApp (ORG-23). */
    val saved: Boolean = false,
) {
    /** BR-10: disabled members can't be picked. */
    val members get() = dues?.members.orEmpty().map { it.member }.filter { it.active }

    val schedules get() = dues?.schedules.orEmpty()

    /** Each due of the chosen schedule for the chosen member, with what is still owed. */
    val lines: List<MemberDueLine>
        get() {
            val d = dues ?: return emptyList()
            val member = d.members.firstOrNull { it.member.memberId == memberId }?.member ?: return emptyList()
            val schedule = d.schedules.firstOrNull { it.id == scheduleId } ?: return emptyList()
            return MemberDuesRules.lines(member, listOf(schedule), LocalDate.now())
        }

    val selectedLine: MemberDueLine? get() = lines.firstOrNull { it.due.index == dueIndex }
}

/** ORG-21 to ORG-23: member → event → schedule → due, amount prefilled with what is owed. */
@HiltViewModel
class MarkPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeEvents: ObserveEventsUseCase,
    private val observeEventDues: ObserveEventDuesUseCase,
    private val recordPayment: RecordPaymentUseCase,
    private val buildWhatsApp: BuildWhatsAppMessageUseCase,
) : ViewModel() {

    private val initialEventId: String? = savedStateHandle["eventId"]
    private val initialMemberId: String? = savedStateHandle["memberId"]

    private val _state = MutableStateFlow(MarkPaymentUiState())
    val state: StateFlow<MarkPaymentUiState> = _state.asStateFlow()

    private var duesJob: Job? = null

    init {
        viewModelScope.launch {
            val events = runCatching { observeEvents().first() }
            val active = events.getOrNull().orEmpty().filter { it.status == EventStatus.ACTIVE }
            _state.update { it.copy(loading = false, events = active, error = events.exceptionOrNull()) }
            val start = active.firstOrNull { it.id == initialEventId } ?: active.singleOrNull()
            start?.let { selectEvent(it.id, initialMemberId) }
        }
    }

    fun selectEvent(eventId: String, memberId: String? = null) {
        _state.update {
            it.copy(eventId = eventId, dues = null, memberId = memberId, scheduleId = null, dueIndex = null, amount = "", fieldErrors = emptyMap())
        }
        duesJob?.cancel()
        duesJob = viewModelScope.launch {
            observeEventDues(eventId, LocalDate.now())
                .catch { e -> _state.update { it.copy(error = e) } }
                .collect { dues ->
                    _state.update { s ->
                        val first = s.dues == null
                        val next = s.copy(dues = dues)
                        if (first && next.schedules.size == 1) next.pickSchedule(next.schedules.first().id) else next
                    }
                }
        }
    }

    fun selectMember(memberId: String) = _state.update {
        it.copy(memberId = memberId, fieldErrors = it.fieldErrors - Field.MEMBER).let { s ->
            s.scheduleId?.let { id -> s.pickSchedule(id) } ?: s
        }
    }

    fun selectSchedule(scheduleId: String) = _state.update { it.pickSchedule(scheduleId) }

    fun selectDue(index: Int) = _state.update { s ->
        val line = s.lines.firstOrNull { it.due.index == index }
        s.copy(dueIndex = index, amount = line?.owed?.takeIf { it > 0 }?.let(Money::toInput).orEmpty(), fieldErrors = s.fieldErrors - Field.DUE - Field.AMOUNT)
    }

    fun onAmountChange(value: String) = _state.update { it.copy(amount = value, fieldErrors = it.fieldErrors - Field.AMOUNT) }
    fun onMethodChange(method: PaymentMethod) = _state.update { it.copy(method = method) }
    fun onNoteChange(value: String) = _state.update { it.copy(note = value) }
    fun onPaidOnChange(date: LocalDate) = _state.update { it.copy(paidOn = date) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        val eventId = s.eventId
        val missing = buildMap<Field, ValidationError> {
            if (eventId == null) put(Field.EVENT, ValidationError.REQUIRED)
            if (s.memberId == null) put(Field.MEMBER, ValidationError.REQUIRED)
            if (s.scheduleId == null) put(Field.SCHEDULE, ValidationError.REQUIRED)
            if (s.dueIndex == null) put(Field.DUE, ValidationError.REQUIRED)
        }
        if (eventId == null || s.memberId == null || s.scheduleId == null || s.dueIndex == null) {
            _state.update { it.copy(fieldErrors = missing) }
            return
        }
        val payment = NewPayment(
            memberId = s.memberId,
            scheduleId = s.scheduleId,
            dueIndex = s.dueIndex,
            amount = Money.parseRupees(s.amount) ?: -1L,
            method = s.method,
            note = s.note,
            paidOn = s.paidOn,
        )
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            recordPayment(eventId, payment)
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            saving = false,
                            fieldErrors = (e as? DomainException.Validation)?.errors.orEmpty(),
                            error = e.takeUnless { e is DomainException.Validation },
                        )
                    }
                }
        }
    }

    /** Clears the form for the next payment in the same event and schedule. */
    fun markAnother() = _state.update {
        it.copy(saved = false, memberId = null, dueIndex = null, amount = "", note = "", fieldErrors = emptyMap())
    }

    suspend fun whatsAppText(): Result<String> {
        val eventId = _state.value.eventId ?: return Result.failure(DomainException.NotFound())
        return buildWhatsApp(eventId, LocalDate.now())
    }

    /** Picks the schedule and, for the chosen member, the earliest due not yet fully paid. */
    private fun MarkPaymentUiState.pickSchedule(scheduleId: String): MarkPaymentUiState {
        val next = copy(scheduleId = scheduleId, dueIndex = null, amount = "", fieldErrors = fieldErrors - Field.SCHEDULE)
        val line = next.lines.firstOrNull { it.owed > 0 } ?: return next
        return next.copy(dueIndex = line.due.index, amount = Money.toInput(line.owed))
    }
}
