package com.powercut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.powercut.ui.editor.EditorScreen
import com.powercut.ui.theme.PowerCutTheme

/**
 * Single-activity host. Hardware acceleration is enabled at the
 * application/window level (see AndroidManifest + themes); the editor screen
 * owns its own GL/EGL surface for the live preview.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PowerCutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F1A)
                ) {
                    EditorScreen()
                }
            }
        }
    }
}
