package com.bharavahi.core.data.mail

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.MailConfig
import com.bharavahi.core.domain.repository.MailSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject
import javax.mail.AuthenticationFailedException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/** ORG-29: sends from the dedicated Gmail account with its app password, over SMTP with STARTTLS. */
internal class GmailSmtpMailSender @Inject constructor() : MailSender {

    override suspend fun send(
        config: MailConfig,
        to: List<String>,
        subject: String,
        html: String,
        attachmentName: String,
        attachmentCsv: String,
    ) = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.host", HOST)
            put("mail.smtp.port", PORT)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
            put("mail.smtp.connectiontimeout", TIMEOUT_MILLIS)
            put("mail.smtp.timeout", TIMEOUT_MILLIS)
        }
        val session = Session.getInstance(
            props,
            object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(config.senderEmail, config.appPassword)
            },
        )
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(config.senderEmail, "Bharavahi"))
            setRecipients(Message.RecipientType.TO, to.map { InternetAddress(it) }.toTypedArray())
            setSubject(subject, "UTF-8")
            setContent(
                MimeMultipart().apply {
                    addBodyPart(MimeBodyPart().apply { setContent(html, "text/html; charset=UTF-8") })
                    addBodyPart(
                        MimeBodyPart().apply {
                            setContent(attachmentCsv, "text/csv; charset=UTF-8")
                            fileName = attachmentName
                        },
                    )
                },
            )
        }
        try {
            Transport.send(message)
        } catch (e: AuthenticationFailedException) {
            throw DomainException.MailNotConfigured()
        } catch (e: MessagingException) {
            throw DomainException.Network(e)
        }
    }

    private companion object {
        const val HOST = "smtp.gmail.com"
        const val PORT = "587"
        const val TIMEOUT_MILLIS = "20000"
    }
}
