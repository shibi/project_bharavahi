package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.model.MailConfig
import com.bharavahi.core.domain.repository.MailConfigRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirestoreMailConfigRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : MailConfigRepository {

    private val doc get() = db.collection(FirestorePaths.CONFIG).document(FirestorePaths.MAIL)

    override suspend fun get(): MailConfig? = firebaseCall {
        val snapshot = doc.get().await()
        if (!snapshot.exists()) {
            null
        } else {
            MailConfig(
                senderEmail = snapshot.getString(FirestorePaths.Mail.SENDER_EMAIL).orEmpty(),
                appPassword = snapshot.getString(FirestorePaths.Mail.APP_PASSWORD).orEmpty(),
            )
        }
    }

    override suspend fun save(config: MailConfig) = firebaseCall {
        doc.set(
            mapOf(
                FirestorePaths.Mail.SENDER_EMAIL to config.senderEmail,
                FirestorePaths.Mail.APP_PASSWORD to config.appPassword,
                FirestorePaths.Mail.UPDATED_AT to FieldValue.serverTimestamp(),
            ),
        ).await()
        Unit
    }
}
