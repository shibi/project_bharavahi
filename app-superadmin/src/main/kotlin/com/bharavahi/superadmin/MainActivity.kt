package com.bharavahi.superadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.SessionState
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.theme.BharavahiTheme
import com.bharavahi.core.ui.theme.SlatePalette
import com.bharavahi.superadmin.navigation.SuperAdminNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val session by appViewModel.session.collectAsStateWithLifecycle()
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()

            BharavahiTheme(palette = SlatePalette, mode = themeMode) {
                when (val state = session) {
                    SessionState.Loading -> LoadingContent()
                    else -> SuperAdminNavHost(signedIn = state is SessionState.SignedIn)
                }
            }
        }
    }
}
