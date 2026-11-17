package com.creovid.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creovid.app.R
import com.creovid.app.engine.ExportQuality

// =====================================================================
// 1) Quality chunne wala dialog
// =====================================================================

@Composable
fun ExportQualityDialog(onDismiss: () -> Unit, onStart: (ExportQuality) -> Unit) {
    var quality by remember { mutableStateOf(ExportQuality.P1080) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CV.Surface2,
        title = { Text("Export video", color = CV.Text) },
        text = {
            Column {
                Text(
                    "Choose a quality. Your video is saved to Movies/Creovid, with no watermark.",
                    color = CV.TextDim,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportQuality.values().forEach { q ->
                        ChoiceChip(q.label, selected = quality == q) { quality = q }
                    }
                }
            }
        },
        confirmButton = { GradientButton("Start export") { onStart(quality) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = CV.TextDim) } },
    )
}

// =====================================================================
// 2) Full-screen "Exporting Video"
// =====================================================================

@Composable
fun ExportOverlay(progress: Float, onCancel: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val p by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(350),
        label = "exportProgress",
    )

    // Export ke dauraan back dabane se screen band na ho
    BackHandler { confirm = true }

    Box(
        Modifier
            .fillMaxSize()
            .background(CV.Bg)
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.creovid_mark), contentDescription = null, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(14.dp))
            Text("Exporting Video", color = CV.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(30.dp))

            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 11.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = CV.Outline,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                    if (p > 0.001f) {
                        rotate(-90f) {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(CV.Sky, CV.Blue, CV.Violet, CV.Lilac, CV.Sky)),
                                startAngle = 0f,
                                sweepAngle = 360f * p,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                    }
                }
                Text(
                    "${(p * 100).toInt()}%",
                    color = CV.Text,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(30.dp))
            Text(
                "Please do not leave Creovid or lock your screen while export is in progress.",
                color = CV.TextDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )
        }

        OutlineButton(
            "Cancel",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
        ) { confirm = true }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = CV.Surface2,
            title = { Text("Stop exporting?", color = CV.Text) },
            text = {
                Text(
                    "Your video is not saved yet. If you stop now, the export will be lost.",
                    color = CV.TextDim,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onCancel()
                }) { Text("Stop export", color = CV.Lilac) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Keep exporting", color = CV.Sky) }
            },
        )
    }
}

// =====================================================================
// 3) Export ho gaya
// =====================================================================

@Composable
fun ExportDoneDialog(uri: Uri, onClose: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CV.Surface2,
        title = { Text("Video ready!", color = CV.Text) },
        text = {
            Text(
                "Saved to your gallery in Movies/Creovid.",
                color = CV.TextDim,
                fontSize = 13.sp,
            )
        },
        confirmButton = {
            GradientButton("Share") {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Share video"))
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Close", color = CV.TextDim) } },
    )
}
