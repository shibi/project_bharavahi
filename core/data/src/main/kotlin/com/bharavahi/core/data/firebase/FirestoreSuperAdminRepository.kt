package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.repository.SuperAdminRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirestoreSuperAdminRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : SuperAdminRepository {
    override suspend fun isSuperAdmin(uid: String): Boolean = firebaseCall {
        db.collection(FirestorePaths.SUPER_ADMINS).document(uid).get().await().exists()
    }
}
