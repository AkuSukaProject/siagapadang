package com.akusukaproject.siagapadang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.akusukaproject.siagapadang.ui.evacuation.EvacuationScreen
import com.akusukaproject.siagapadang.ui.theme.SiagaPadangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SiagaPadangTheme {
                EvacuationScreen()
            }
        }
    }
}

