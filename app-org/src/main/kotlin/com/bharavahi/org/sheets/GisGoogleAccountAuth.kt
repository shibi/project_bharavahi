package com.bharavahi.org.sheets

import android.accounts.Account
import android.content.Context
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.repository.GoogleAccountAuth
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ORG-35: Google Identity Services authorization for the `drive.file` scope. The connected account is
 * remembered per phone, so only phones signed in to it run the sheet sync.
 */
@Singleton
class GisGoogleAccountAuth @Inject constructor(
    @ApplicationContext private val context: Context,
) : GoogleAccountAuth {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun connectedEmail(): String? = prefs.getString(KEY_EMAIL, null)

    override fun setConnectedEmail(email: String?) {
        prefs.edit().apply { if (email == null) remove(KEY_EMAIL) else putString(KEY_EMAIL, email) }.apply()
    }

    override suspend fun accessToken(email: String): String {
        val result = try {
            Identity.getAuthorizationClient(context).authorize(request(Account(email, ACCOUNT_TYPE))).await()
        } catch (e: ApiException) {
            throw DomainException.Network(e)
        }
        // A resolution means Google wants the user to approve again, which a background job can't show.
        if (result.hasResolution()) throw DomainException.NotAuthorized()
        return result.accessToken ?: throw DomainException.NotAuthorized()
    }

    companion object {
        private const val PREFS = "google_sheets"
        private const val KEY_EMAIL = "connectedEmail"
        private const val ACCOUNT_TYPE = "com.google"
        private const val DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"

        /** [account] null lets the user pick one, which is what connecting needs. */
        fun request(account: Account? = null): AuthorizationRequest =
            AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE)))
                .apply { if (account != null) setAccount(account) }
                .build()
    }
}
