package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.AuthUser
import com.bharavahi.core.domain.model.SessionState
import com.bharavahi.core.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {

    override val session: Flow<SessionState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            trySend(if (user == null) SessionState.SignedOut else SessionState.SignedIn(user.toAuthUser()))
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): AuthUser = firebaseCall {
        val user = auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw DomainException.InvalidCredentials()
        user.toAuthUser()
    }

    override suspend fun signOut() = auth.signOut()

    override fun currentUser(): AuthUser? = auth.currentUser?.toAuthUser()

    private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email.orEmpty())
}
