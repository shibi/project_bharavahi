package com.bharavahi.org.sheets

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.bharavahi.core.domain.error.DomainException
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ORG-35: returns a function that asks the admin to pick a Google account and allow access to the
 * files this app creates. [onToken] gets the access token; a cancelled prompt calls neither callback.
 */
@Composable
fun rememberGoogleSheetsConnect(onToken: (String) -> Unit, onError: (Throwable) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnToken by rememberUpdatedState(onToken)
    val currentOnError by rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        // Google also returns RESULT_CANCELED when authorization fails (e.g. DEVELOPER_ERROR from a missing
        // Android OAuth client), so read the status from the intent instead of trusting the result code.
        runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data).accessToken }
            .onSuccess { token ->
                when {
                    token != null -> currentOnToken(token)
                    result.resultCode == Activity.RESULT_OK -> currentOnError(DomainException.NotAuthorized())
                }
            }
            .onFailure { e ->
                Log.w(TAG, "Google authorization failed (resultCode=${result.resultCode})", e)
                val userCancelled = e is ApiException && e.statusCode == CommonStatusCodes.CANCELED
                if (!userCancelled) currentOnError(DomainException.Unknown(e))
            }
    }
    return {
        scope.launch {
            runCatching { Identity.getAuthorizationClient(context).authorize(GisGoogleAccountAuth.request()).await() }
                .onSuccess { result ->
                    val pending = result.pendingIntent
                    val token = result.accessToken
                    when {
                        result.hasResolution() && pending != null ->
                            launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                        token != null -> currentOnToken(token)
                        else -> currentOnError(DomainException.NotAuthorized())
                    }
                }
                .onFailure {
                    Log.w(TAG, "Google authorization request failed", it)
                    currentOnError(DomainException.Network(it))
                }
        }
    }
}

private const val TAG = "GoogleSheetsConnect"
