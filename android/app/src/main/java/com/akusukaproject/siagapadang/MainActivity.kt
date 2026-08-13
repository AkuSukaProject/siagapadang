package com.akusukaproject.siagapadang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.akusukaproject.siagapadang.ui.evacuation.EvacuationScreen
import com.akusukaproject.siagapadang.ui.theme.SiagaPadangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }
}
