package com.bharavahi.core.domain.model

/** Gmail sender used for payment backup emails (stored in config/mail). */
data class MailConfig(
    val senderEmail: String,
    val appPassword: String,
)
