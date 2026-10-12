package com.figmaexport.onboarding.pkg8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.figmaexport.onboarding.pkg8.ui.Onboarding8Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Onboarding8Screen()
        }
    }
}
