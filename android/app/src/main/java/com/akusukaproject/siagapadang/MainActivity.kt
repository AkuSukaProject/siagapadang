package com.akusukaproject.siagapadang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.akusukaproject.siagapadang.ui.evacuation.EvacuationScreen
import com.akusukaproject.siagapadang.ui.familyplan.FamilyPlanScreen
import com.akusukaproject.siagapadang.ui.menu.MenuScreen
import com.akusukaproject.siagapadang.ui.facilities.FacilitiesScreen
import com.akusukaproject.siagapadang.ui.info.AboutScreen
import com.akusukaproject.siagapadang.ui.info.GuideScreen
import com.akusukaproject.siagapadang.ui.info.SettingsScreen
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
                var screen by rememberSaveable { mutableStateOf(AppScreen.EVACUATION) }
                var aboutReturn by rememberSaveable { mutableStateOf(AppScreen.MENU) }
                when (screen) {
                    AppScreen.EVACUATION -> EvacuationScreen(
                        showArrivalEvidence = showArrivalEvidence,
                        evidenceDestinationName = EVIDENCE_DESTINATION_NAME,
                        evidenceDestinationCapacity = EVIDENCE_DESTINATION_CAPACITY,
                        onOpenMenu = { screen = AppScreen.MENU },
                    )
                    AppScreen.MENU -> MenuScreen(
                        onBack = { screen = AppScreen.EVACUATION },
                        onOpenFamilyPlan = { screen = AppScreen.FAMILY_PLAN },
                        onOpenFacilities = { screen = AppScreen.FACILITIES },
                        onOpenGuide = { screen = AppScreen.GUIDE },
                        onOpenSettings = { screen = AppScreen.SETTINGS },
                        onOpenAbout = {
                            aboutReturn = AppScreen.MENU
                            screen = AppScreen.ABOUT
                        },
                    )
                    AppScreen.FAMILY_PLAN -> FamilyPlanScreen(onBack = { screen = AppScreen.MENU })
                    AppScreen.FACILITIES -> FacilitiesScreen(onBack = { screen = AppScreen.MENU })
                    AppScreen.GUIDE -> GuideScreen(onBack = { screen = AppScreen.MENU })
                    AppScreen.SETTINGS -> SettingsScreen(
                        onBack = { screen = AppScreen.MENU },
                        onOpenAbout = {
                            aboutReturn = AppScreen.SETTINGS
                            screen = AppScreen.ABOUT
                        },
                    )
                    AppScreen.ABOUT -> AboutScreen(onBack = { screen = aboutReturn })
                }
            }
        }
    }

    /** Layar evakuasi selalu menjadi layar awal; halaman lain adalah persiapan masa tenang. */
    private enum class AppScreen { EVACUATION, MENU, FAMILY_PLAN, FACILITIES, GUIDE, SETTINGS, ABOUT }

    private companion object {
        const val EXTRA_SHOW_ARRIVAL_EVIDENCE = "show_arrival_evidence"
        const val EVIDENCE_DESTINATION_NAME = "MESJID RAYA IKUR KOTO"
        const val EVIDENCE_DESTINATION_CAPACITY = 1_452
        const val SPLASH_EXIT_DURATION_MILLIS = 220L
    }
}
