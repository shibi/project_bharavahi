package com.bharavahi.core.data.firebase

import android.content.Context
import com.bharavahi.core.domain.repository.AdminAccountGateway
import com.bharavahi.core.domain.repository.PendingLogin
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

/**
 * SA-8: creating a user with the default FirebaseAuth would sign the super admin out and the new
 * admin in. Instead each login is created through a short-lived secondary FirebaseApp.
 */
internal class FirebaseAdminAccountGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdminAccountGateway {

    override suspend fun createLogin(email: String, password: String): PendingLogin {
        val app = FirebaseApp.initializeApp(
            context,
            FirebaseApp.getInstance().options,
            "admin-creator-${UUID.randomUUID()}",
        )
        val secondaryAuth = FirebaseAuth.getInstance(app)
        return try {
            val user = firebaseCall {
                secondaryAuth.createUserWithEmailAndPassword(email, password).await().user
                    ?: error("Firebase returned no user")
            }
            SecondaryAppLogin(app, secondaryAuth, user)
        } catch (e: Exception) {
            app.delete()
            throw e
        }
    }
}

private class SecondaryAppLogin(
    private val app: FirebaseApp,
    private val auth: FirebaseAuth,
    private val user: FirebaseUser,
) : PendingLogin {
    override val uid: String = user.uid

    override suspend fun commit() = release()

    /** The secondary app is still signed in as the new user, so it may delete that user. */
    override suspend fun rollback() {
        runCatching { user.delete().await() }
        release()
    }

    private fun release() {
        auth.signOut()
        app.delete()
    }
}
