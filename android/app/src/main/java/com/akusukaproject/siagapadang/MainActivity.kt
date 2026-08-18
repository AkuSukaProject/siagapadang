package com.akusukaproject.siagapadang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.akusukaproject.siagapadang.ui.evacuation.EvacuationScreen
import com.akusukaproject.siagapadang.ui.theme.SiagaPadangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.view.animate()
                .alpha(0f)
                .setDuration(SPLASH_EXIT_DURATION_MILLIS)
                .withEndAction(splashScreenView::remove)
                .start()
        }
        val showArrivalEvidence = BuildConfig.DEBUG &&
            intent.getBooleanExtra(EXTRA_SHOW_ARRIVAL_EVIDENCE, false)
        setContent {
            SiagaPadangTheme {
                EvacuationScreen(
                    showArrivalEvidence = showArrivalEvidence,
                    evidenceDestinationName = EVIDENCE_DESTINATION_NAME,
                    evidenceDestinationCapacity = EVIDENCE_DESTINATION_CAPACITY,
                )
            }
        }
    }

    private companion object {
        const val EXTRA_SHOW_ARRIVAL_EVIDENCE = "show_arrival_evidence"
        const val EVIDENCE_DESTINATION_NAME = "MESJID RAYA IKUR KOTO"
        const val EVIDENCE_DESTINATION_CAPACITY = 1_452
        const val SPLASH_EXIT_DURATION_MILLIS = 220L
    }
}
