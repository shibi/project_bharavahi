package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.MailConfig

/** ORG-28: queues the backup email; it is sent when the phone is online, even after the app closes. */
interface PaymentBackupScheduler {
    fun enqueue(orgId: String, eventId: String, paymentId: String)
}

/** Sends one HTML email with a CSV attachment through the configured Gmail account. */
interface MailSender {
    suspend fun send(
        config: MailConfig,
        to: List<String>,
        subject: String,
        html: String,
        attachmentName: String,
        attachmentCsv: String,
    )
}
