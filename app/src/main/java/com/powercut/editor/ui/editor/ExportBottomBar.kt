package com.powercut.editor.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Export bottom bar with a two-option dialog:
 *
 *  1. "Watch Ad → Remove Watermark" — fires [onWatchAd]; the caller should show a
 *     rewarded AdMob ad and, on completion, start an export with
 *     `removeWatermark = true` (clean video).
 *
 *  2. "Export with PowerCut Watermark" — fires [onExportWithWatermark]; the caller
 *     starts an export with `removeWatermark = false` (semi-transparent "PowerCut"
 *     overlay in the bottom-right corner).
 *
 * The orange EXPORT button mirrors the existing editor accent and opens the dialog.
 */
@Composable
fun ExportBottomBar(
    onWatchAd: () -> Unit,
    onExportWithWatermark: () -> Unit,
    // When true, the dialog opens automatically (triggered by the editor's
    // existing export button). The editor sets this and we observe it.
    triggerDialog: Boolean = false,
    onDialogDismissed: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }

    // React to external trigger from the editor's export button
    androidx.compose.runtime.LaunchedEffect(triggerDialog) {
        if (triggerDialog) showDialog = true
    }

    // The standalone EXPORT button (bottom bar) is hidden when used inside the
    // editor — the editor already has its own export button. We only render the
    // dialog here.
    if (false) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A3C)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(text = "EXPORT", color = Color.White)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; onDialogDismissed() },
            title = { Text("Export Video") },
            text = { Text("Choose your export option:") },
            confirmButton = {
                Column {
                    Button(
                        onClick = { showDialog = false; onDialogDismissed(); onWatchAd() },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text("📺 Watch Ad → Remove Watermark")
                    }
                    OutlinedButton(
                        onClick = { showDialog = false; onDialogDismissed(); onExportWithWatermark() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export with PowerCut Watermark")
                    }
                }
            }
        )
    }
}
