package com.bharavahi.core.data.di

import com.bharavahi.core.data.firebase.FirebaseAdminAccountGateway
import com.bharavahi.core.data.firebase.FirebaseAuthRepository
import com.bharavahi.core.data.firebase.FirestoreActivityRepository
import com.bharavahi.core.data.firebase.FirestoreMailConfigRepository
import com.bharavahi.core.data.firebase.FirestoreOrgAdminRepository
import com.bharavahi.core.data.firebase.FirestoreOrganizationRepository
import com.bharavahi.core.data.firebase.FirestoreSuperAdminRepository
import com.bharavahi.core.data.preferences.DataStoreThemePreferenceRepository
import com.bharavahi.core.domain.repository.ActivityRepository
import com.bharavahi.core.domain.repository.AdminAccountGateway
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.MailConfigRepository
import com.bharavahi.core.domain.repository.OrgAdminRepository
import com.bharavahi.core.domain.repository.OrganizationRepository
import com.bharavahi.core.domain.repository.SuperAdminRepository
import com.bharavahi.core.domain.repository.ThemePreferenceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /** NFR-1: keep a local copy of data so lists open without network. */
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = firestoreSettings {
            setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds abstract fun authRepository(impl: FirebaseAuthRepository): AuthRepository
    @Binds abstract fun superAdminRepository(impl: FirestoreSuperAdminRepository): SuperAdminRepository
    @Binds abstract fun organizationRepository(impl: FirestoreOrganizationRepository): OrganizationRepository
    @Binds abstract fun orgAdminRepository(impl: FirestoreOrgAdminRepository): OrgAdminRepository
    @Binds abstract fun activityRepository(impl: FirestoreActivityRepository): ActivityRepository
    @Binds abstract fun mailConfigRepository(impl: FirestoreMailConfigRepository): MailConfigRepository
    @Binds abstract fun adminAccountGateway(impl: FirebaseAdminAccountGateway): AdminAccountGateway
    @Binds abstract fun themePreferenceRepository(impl: DataStoreThemePreferenceRepository): ThemePreferenceRepository
}
