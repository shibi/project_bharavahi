package com.bharavahi.core.domain.usecase.payment

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.EmailStatus
import com.bharavahi.core.domain.repository.EventRepository
import com.bharavahi.core.domain.repository.MailConfigRepository
import com.bharavahi.core.domain.repository.MailSender
import com.bharavahi.core.domain.repository.OrgAdminRepository
import com.bharavahi.core.domain.repository.OrganizationRepository
import com.bharavahi.core.domain.repository.PaymentRepository
import com.bharavahi.core.domain.repository.ScheduleRepository
import com.bharavahi.core.domain.rules.MemberDuesRules
import com.bharavahi.core.domain.rules.PaymentReportBuilder
import com.bharavahi.core.domain.rules.PaymentReportInput
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * ORG-29: run by the background job. Reads the mail settings, the event and its members, builds the
 * report (ORG-30), emails every active admin and marks the payment's email as sent.
 * Throws on failure so the job can retry; [markFailed] is called after the last attempt.
 */
class SendPaymentBackupUseCase @Inject constructor(
    private val mailConfig: MailConfigRepository,
    private val mailSender: MailSender,
    private val organizations: OrganizationRepository,
    private val admins: OrgAdminRepository,
    private val events: EventRepository,
    private val schedules: ScheduleRepository,
    private val payments: PaymentRepository,
) {
    suspend operator fun invoke(orgId: String, eventId: String, paymentId: String, today: LocalDate, recordedAt: String) {
        val config = mailConfig.get() ?: throw DomainException.MailNotConfigured()
        val payment = payments.observeAll(orgId, eventId).first().firstOrNull { it.id == paymentId }
            ?: throw DomainException.NotFound()
        if (payment.emailStatus == EmailStatus.SENT) return

        val event = events.observe(orgId, eventId).first() ?: throw DomainException.NotFound()
        val scheduleList = schedules.observeAll(orgId, eventId).first()
        val members = events.observeMembers(orgId, eventId).first()
        val adminList = admins.observeAdmins(orgId).first()
        val recipients = adminList.filter { it.active }.map { it.email }.filter { it.isNotBlank() }
        if (recipients.isEmpty()) throw DomainException.NotFound()

        val schedule = scheduleList.firstOrNull { it.id == payment.scheduleId }
        val report = PaymentReportBuilder.build(
            PaymentReportInput(
                communityName = organizations.observe(orgId).first()?.name.orEmpty(),
                event = event,
                payment = payment,
                memberName = members.firstOrNull { it.memberId == payment.memberId }?.name ?: payment.memberId,
                scheduleName = schedule?.name.orEmpty(),
                dueLabel = schedule?.dues?.firstOrNull { it.index == payment.dueIndex }?.label.orEmpty(),
                recordedByName = adminList.firstOrNull { it.uid == payment.recordedBy }?.name ?: payment.recordedBy,
                recordedAt = recordedAt,
                members = MemberDuesRules.computeAll(members, scheduleList, today),
            ),
        )
        mailSender.send(config, recipients, report.subject, report.html, report.csvFileName, report.csv)
        payments.setEmailStatus(orgId, eventId, paymentId, EmailStatus.SENT)
    }

    suspend fun markFailed(orgId: String, eventId: String, paymentId: String) {
        payments.setEmailStatus(orgId, eventId, paymentId, EmailStatus.FAILED)
    }
}
