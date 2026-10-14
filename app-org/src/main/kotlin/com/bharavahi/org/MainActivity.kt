package com.bharavahi.org

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.AdminSessionState
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.theme.BharavahiTheme
import com.bharavahi.core.ui.theme.TealOrangePalette
import com.bharavahi.org.navigation.OrgNavHost
import com.bharavahi.org.ui.theme.OrgTypography
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

            BharavahiTheme(palette = TealOrangePalette, mode = themeMode, typography = OrgTypography) {
                when (val state = session) {
                    AdminSessionState.Loading -> LoadingContent()
                    else -> OrgNavHost(signedIn = state is AdminSessionState.SignedIn)
                }
            }
        }
    }
}
