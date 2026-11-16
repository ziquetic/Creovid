package com.creovid.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.creovid.app.EditorViewModel
import com.creovid.app.R
import com.creovid.app.engine.ExportQuality
import com.creovid.app.engine.GradeParams
import com.creovid.app.engine.Preset
import com.creovid.app.engine.Presets
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(vm: EditorViewModel, onPickLut: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    val player = vm.player

    // Message toast
    LaunchedEffect(vm.message) {
        vm.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.message = null
        }
    }

    // Background me jaane par video pause
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // Export ke dauraan screen on rakho
    val exporting = vm.exportProgress != null
    DisposableEffect(exporting) {
        view.keepScreenOn = exporting
        onDispose { view.keepScreenOn = false }
    }

    var pos by remember { mutableFloatStateOf(0f) }
    var dur by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(true) }
    var scrubbing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            if (!scrubbing) {
                dur = player.duration.coerceAtLeast(0L)
                pos = if (dur > 0) player.currentPosition.toFloat() / dur else 0f
            }
            playing = player.isPlaying
            delay(150)
        }
    }

    var tab by remember { mutableIntStateOf(0) }
    var showExport by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(CV.Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ---------- Top bar ----------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.closeVideo() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = CV.Text)
            }
            Image(painterResource(R.drawable.creovid_mark), null, Modifier.size(24.dp))
            Text(
                "  Creovid",
                color = CV.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f),
            )
            CompareButton(vm)
            Spacer(Modifier.width(8.dp))
            GradientButton("Export") { showExport = true }
            Spacer(Modifier.width(6.dp))
        }

        // ---------- Preview ----------
        Box(
            Modifier.fillMaxWidth().weight(1f).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        this.player = vm.player
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (player.isPlaying) player.pause() else player.play()
                        })
                    },
            )
        }

        // ---------- Transport ----------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = CV.Text,
                )
            }
            Slider(
                value = pos,
                onValueChange = {
                    scrubbing = true
                    pos = it
                    if (dur > 0) player.seekTo((it * dur).toLong())
                },
                onValueChangeFinished = { scrubbing = false },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = CV.Sky,
                    inactiveTrackColor = CV.Outline,
                ),
                modifier = Modifier.weight(1f).height(28.dp),
            )
        }

        // ---------- Tabs ----------
        val tabs = listOf("Looks", "Basic", "Wheels", "FX")
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            tabs.forEachIndexed { i, name ->
                Column(
                    Modifier.weight(1f).clickable { tab = i }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        name,
                        color = if (tab == i) CV.Text else CV.TextDim,
                        fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .height(3.dp)
                            .width(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (tab == i) CV.GradientH else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))),
                    )
                }
            }
        }

        // ---------- Controls ----------
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1.25f)
                .background(CV.Surface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            when (tab) {
                0 -> LooksTab(vm, onPickLut)
                1 -> BasicTab(vm)
                2 -> WheelsTab(vm)
                else -> FxTab(vm)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    ExportDialogs(vm, showExport) { showExport = false }
}

// =====================================================================
// Compare button: dabaye rakho to original dikhta hai
// =====================================================================

@Composable
private fun CompareButton(vm: EditorViewModel) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .border(1.dp, if (vm.comparing) CV.Sky else CV.Outline, shape)
            .background(if (vm.comparing) CV.Surface2 else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    vm.holdOriginal(true)
                    tryAwaitRelease()
                    vm.holdOriginal(false)
                })
            }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (vm.comparing) "Original" else "Dabake dekho",
            color = if (vm.comparing) CV.Sky else CV.TextDim,
            fontSize = 12.sp,
        )
    }
}

// =====================================================================
// Tabs
// =====================================================================

@Composable
private fun ParamSlider(
    vm: EditorViewModel,
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    default: Float = 0f,
    set: (GradeParams, Float) -> GradeParams,
) {
    GradeSlider(
        label = label,
        value = value,
        range = range,
        onChange = { v -> vm.update { set(it, v) } },
        onFinished = { vm.refreshFrame(force = true) },
        default = default,
    )
}

@Composable
private fun LooksTab(vm: EditorViewModel, onPickLut: () -> Unit) {
    Text("Cinematic looks", color = CV.TextDim, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(Presets.all) { p ->
            PresetCard(p, selected = vm.presetName == p.name) { vm.applyPreset(p) }
        }
    }
    Spacer(Modifier.height(18.dp))
    Text("Apni LUT (.cube)", color = CV.TextDim, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlineButton("LUT import karo") { onPickLut() }
        val name = vm.lutName
        if (name != null) {
            Spacer(Modifier.width(10.dp))
            Text(name, color = CV.Sky, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.clearLut() }) { Text("Hatao", color = CV.TextDim) }
        }
    }
    if (vm.lutName != null) {
        Spacer(Modifier.height(8.dp))
        ParamSlider(vm, "LUT strength", vm.params.lutIntensity, 0f..1f, default = 1f) { s, v ->
            s.copy(lutIntensity = v)
        }
    }
    Spacer(Modifier.height(14.dp))
    OutlineButton("Sab reset karo") { vm.resetAll() }
}

@Composable
private fun PresetCard(p: Preset, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.width(84.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(p.swatchA), Color(p.swatchB))))
                .then(if (selected) Modifier.border(BorderStroke(2.dp, CV.Gradient), shape) else Modifier),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            p.name,
            color = if (selected) CV.Text else CV.TextDim,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun BasicTab(vm: EditorViewModel) {
    val p = vm.params
    ParamSlider(vm, "Exposure", p.exposure, -2f..2f) { s, v -> s.copy(exposure = v) }
    ParamSlider(vm, "Contrast", p.contrast, -1f..1f) { s, v -> s.copy(contrast = v) }
    ParamSlider(vm, "Highlights", p.highlights, -1f..1f) { s, v -> s.copy(highlights = v) }
    ParamSlider(vm, "Shadows", p.shadows, -1f..1f) { s, v -> s.copy(shadows = v) }
    ParamSlider(vm, "Temperature (thanda - garam)", p.temperature, -1f..1f) { s, v -> s.copy(temperature = v) }
    ParamSlider(vm, "Tint (hara - magenta)", p.tint, -1f..1f) { s, v -> s.copy(tint = v) }
    ParamSlider(vm, "Saturation", p.saturation, -1f..1f) { s, v -> s.copy(saturation = v) }
    ParamSlider(vm, "Vibrance", p.vibrance, -1f..1f) { s, v -> s.copy(vibrance = v) }
    ParamSlider(vm, "Fade (matte)", p.fade, 0f..1f) { s, v -> s.copy(fade = v) }
    ParamSlider(vm, "Film curve", p.filmCurve, 0f..1f) { s, v -> s.copy(filmCurve = v) }
    Text("Tip: label par double-tap karo to value reset ho jaati hai.", color = CV.TextDim, fontSize = 11.sp)
}

@Composable
private fun WheelsTab(vm: EditorViewModel) {
    val p = vm.params
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorWheel(
            "Shadows", p.lift,
            onChange = { w -> vm.update { it.copy(lift = w) } },
            onFinished = { vm.refreshFrame(force = true) },
            modifier = Modifier.weight(1f),
        )
        ColorWheel(
            "Midtones", p.gamma,
            onChange = { w -> vm.update { it.copy(gamma = w) } },
            onFinished = { vm.refreshFrame(force = true) },
            modifier = Modifier.weight(1f),
        )
        ColorWheel(
            "Highlights", p.gain,
            onChange = { w -> vm.update { it.copy(gain = w) } },
            onFinished = { vm.refreshFrame(force = true) },
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Wheel ke kinare ki taraf drag karo = zyada colour. Cinematic look ke liye shadows me teal/blue, highlights me orange try karo. Double-tap = reset.",
        color = CV.TextDim,
        fontSize = 11.sp,
    )
}

@Composable
private fun FxTab(vm: EditorViewModel) {
    val p = vm.params
    ParamSlider(vm, "Film grain", p.grain, 0f..1f) { s, v -> s.copy(grain = v) }
    ParamSlider(vm, "Bloom (glow)", p.bloom, 0f..1f) { s, v -> s.copy(bloom = v) }
    ParamSlider(vm, "Halation (laal glow)", p.halation, 0f..1f) { s, v -> s.copy(halation = v) }
    ParamSlider(vm, "Vignette", p.vignette, 0f..1f) { s, v -> s.copy(vignette = v) }
    Spacer(Modifier.height(10.dp))
    Text("Letterbox (cinema bars)", color = CV.Text, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    val options = listOf("Off" to 0f, "2.39:1" to 2.39f, "2:1" to 2.0f, "1.85:1" to 1.85f)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, value) ->
            ChoiceChip(label, selected = p.letterbox == value) {
                vm.update { it.copy(letterbox = value) }
                vm.refreshFrame(force = true)
            }
        }
    }
}

// =====================================================================
// Export dialogs
// =====================================================================

@Composable
private fun ExportDialogs(vm: EditorViewModel, show: Boolean, dismiss: () -> Unit) {
    val context = LocalContext.current
    var quality by remember { mutableStateOf(ExportQuality.P1080) }
    val progress = vm.exportProgress
    val done = vm.exportedUri

    if (show && progress == null) {
        AlertDialog(
            onDismissRequest = dismiss,
            containerColor = CV.Surface2,
            title = { Text("Export", color = CV.Text) },
            text = {
                Column {
                    Text("Quality chuno. Video Movies/Creovid folder me save hogi, bina watermark ke.", color = CV.TextDim, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportQuality.values().forEach { q ->
                            ChoiceChip(q.label, selected = quality == q) { quality = q }
                        }
                    }
                }
            },
            confirmButton = {
                GradientButton("Export shuru") {
                    dismiss()
                    vm.export(quality)
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text("Cancel", color = CV.TextDim) }
            },
        )
    }

    if (progress != null) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CV.Surface2,
            title = { Text("Export ho raha hai...", color = CV.Text) },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = CV.Violet,
                        trackColor = CV.Outline,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("${(progress * 100).toInt()}%  -  app khula rakho, screen on rahegi.", color = CV.TextDim, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.cancelExport() }) { Text("Cancel", color = CV.Lilac) }
            },
        )
    }

    if (progress == null && done != null) {
        AlertDialog(
            onDismissRequest = { vm.exportedUri = null },
            containerColor = CV.Surface2,
            title = { Text("Video ready!", color = CV.Text) },
            text = { Text("Gallery ke Movies/Creovid folder me save ho gayi.", color = CV.TextDim, fontSize = 13.sp) },
            confirmButton = {
                GradientButton("Share") {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(Intent.EXTRA_STREAM, done)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Share karo"))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.exportedUri = null }) { Text("Band karo", color = CV.TextDim) }
            },
        )
    }
}
