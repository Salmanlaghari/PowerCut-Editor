package com.powercut.editor.ui.drafts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.ui.editor.DraftItem
import com.powercut.editor.ui.theme.AccentSecondary
import com.powercut.editor.ui.theme.CyberCyan
import com.powercut.editor.ui.theme.GlassBackground
import com.powercut.editor.ui.theme.NeonOrange
import com.powercut.editor.ui.theme.SignatureOrange
import com.powercut.editor.ui.theme.SignaturePurple
import com.powercut.editor.ui.theme.glassCard3D
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.tactileClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DraftsScreen(
    draftsList: List<DraftItem>,
    onDraftSelected: (DraftItem) -> Unit,
    language: String,
    onDeleteDraft: ((DraftItem) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language == "ur") "\u0645\u062d\u0641\u0648\u0638 \u0634\u062f\u06c1 \u0688\u0631\u0627\u0641\u0679\u0633" else "Saved Video Drafts",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .background(SignatureOrange.copy(0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${draftsList.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SignatureOrange
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (draftsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .glassCard3D(
                                shape = CircleShape,
                                glowColor = SignaturePurple.copy(0.4f),
                                backColor = GlassBackground
                            )
                            .border(1.dp, SignatureOrange.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Drafts,
                            contentDescription = "No Drafts",
                            tint = SignatureOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (language == "ur") "\u0627\u0628\u06be\u06cc \u062a\u06a9 \u06a9\u0648\u0626\u06cc \u0688\u0631\u0627\u0641\u0679 \u0645\u0648\u062c\u0648\u062f \u0646\u06c1\u06cc\u06ba \u06c1\u06d2" else "No drafts yet",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (language == "ur") "\u0627\u06cc\u0688\u0679 \u06a9\u0631\u0646\u06d2 \u06a9\u06d2 \u0628\u0639\u062f \u0688\u0631\u0627\u0641\u0679 \u0645\u062d\u0641\u0648\u0638 \u06a9\u0631\u06cc\u06ba" else "Save a draft after editing a video",
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(draftsList) { draft ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard3D(
                                shape = RoundedCornerShape(20.dp),
                                glowColor = SignatureOrange.copy(0.25f),
                                backColor = GlassBackground
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                            .clickable { onDraftSelected(draft) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(SignatureOrange, SignaturePurple)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Draft icon",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = draft.projectName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "Edited: ${formatDate(draft.lastEditedTime)}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Duration: ${formatDuration(draft.durationMs)}",
                                fontSize = 10.sp,
                                color = CyberCyan,
                                modifier = Modifier.padding(top = 2.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (onDeleteDraft != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = 0.12f))
                                    .clickable { onDeleteDraft(draft) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete draft",
                                    tint = Color.Red.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(time: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
    return sdf.format(Date(time))
}

private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
