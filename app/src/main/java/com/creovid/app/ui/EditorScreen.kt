package com.creovid.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.creovid.app.EditorViewModel
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(vm: EditorViewModel, onPickLut: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    val focus = LocalFocusManager.current
    val player = vm.player

    // Toast messages
    LaunchedEffect(vm.message) {
        vm.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.message = null
        }
    }

    // Background me jaane par video pause + grading save
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) {
                player.pause()
                vm.saveCurrent()
            }
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

    // Player ki position (timeline + clock isi se chalte hain)
    val ph = remember { PlayheadState() }
    LaunchedEffect(player) {
        while (true) {
            if (!ph.scrubbing) {
                val d = player.duration.coerceAtLeast(0L)
                ph.durMs = d
                ph.posMs = if (d > 0L) player.currentPosition.coerceIn(0L, d) else 0L
            }
            ph.playing = player.isPlaying
            delay(40)
        }
    }

    var tool by remember { mutableStateOf<EditTool?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var name by remember(vm.currentTitle) { mutableStateOf(vm.currentTitle) }
    var resumeAfterScrub by remember { mutableStateOf(false) }

    // LUT import hone ke baad Filters panel khol do (wahan LUT strength milti hai)
    var lutSeen by remember { mutableIntStateOf(vm.lutImportCount) }
    LaunchedEffect(vm.lutImportCount) {
        if (vm.lutImportCount != lutSeen) {
            lutSeen = vm.lutImportCount
            tool = EditTool.Filters
        }
    }

    BackHandler {
        when {
            fullscreen -> fullscreen = false
            tool != null -> tool = null
            else -> {
                vm.renameCurrent(name)
                vm.closeVideo()
            }
        }
    }

    fun seekTo(ms: Long) {
        ph.posMs = ms
        player.seekTo(ms)
    }

    val gradeLabel = buildString {
        append("Grade - ")
        append(if (vm.presetName.isNotEmpty()) vm.presetName else "Custom")
        if (vm.lutName != null) append(" + LUT")
    }

    Box(Modifier.fillMaxSize().background(CV.Bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            if (!fullscreen) {
                TopBar(
                    vm = vm,
                    name = name,
                    onNameChange = { name = it.take(30) },
                    onNameDone = { focus.clearFocus() },
                    onBack = {
                        vm.renameCurrent(name)
                        vm.closeVideo()
                    },
                    onExport = { showExport = true },
                )
            }

            PreviewArea(
                vm = vm,
                ph = ph,
                player = player,
                fullscreen = fullscreen,
                onExitFullscreen = { fullscreen = false },
                onSeek = { seekTo(it) },
                modifier = Modifier.weight(1f),
            )

            if (!fullscreen) {
                TransportRow(
                    vm = vm,
                    ph = ph,
                    player = player,
                    onFullscreen = { fullscreen = true },
                )

                Timeline(
                    ph = ph,
                    thumbPath = vm.currentThumbPath,
                    gradeLabel = gradeLabel,
                    gradeHidden = vm.gradeHidden,
                    onToggleGrade = { vm.toggleGradeHidden() },
                    onSeek = { seekTo(it) },
                    onScrubStart = {
                        resumeAfterScrub = player.isPlaying
                        player.pause()
                        ph.scrubbing = true
                    },
                    onScrubEnd = {
                        ph.scrubbing = false
                        if (resumeAfterScrub) player.play()
                    },
                )

                val t = tool
                if (t != null) {
                    ToolPanel(
                        tool = t,
                        vm = vm,
                        onPickLut = onPickLut,
                        onClose = { tool = null },
                        modifier = Modifier.height(250.dp),
                    )
                }

                BottomToolBar(
                    selected = tool,
                    lutActive = vm.lutName != null,
                    onTool = { picked ->
                        if (picked == EditTool.Lut) {
                            onPickLut()
                        } else {
                            tool = if (tool == picked) null else picked
                        }
                    },
                )
            }
        }

        val progress = vm.exportProgress
        if (progress != null) {
            ExportOverlay(progress = progress, onCancel = { vm.cancelExport() })
        }
    }

    if (showExport && vm.exportProgress == null) {
        ExportQualityDialog(
            onDismiss = { showExport = false },
            onStart = { q ->
                showExport = false
                vm.export(q)
            },
        )
    }

    val done = vm.exportedUri
    if (done != null && vm.exportProgress == null) {
        ExportDoneDialog(uri = done, onClose = { vm.exportedUri = null })
    }
}

// =====================================================================
// Top bar: back, project name, settings, export
// =====================================================================

@Composable
private fun TopBar(
    vm: EditorViewModel,
    name: String,
    onNameChange: (String) -> Unit,
    onNameDone: () -> Unit,
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val pill = RoundedCornerShape(50)

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = CV.Text)
        }

        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            textStyle = TextStyle(
                color = CV.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(CV.Sky),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onNameDone() }),
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(pill)
                .background(CV.Surface2)
                .border(1.dp, CV.Outline, pill)
                .onFocusChanged {
                    if (!it.hasFocus) {
                        if (name.isBlank()) onNameChange(vm.currentTitle) else vm.renameCurrent(name)
                    }
                }
                .padding(horizontal = 14.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
            },
        )

        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = CV.Text)
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                modifier = Modifier.background(CV.Surface2),
            ) {
                DropdownMenuItem(
                    text = { Text("Reset all grading", color = CV.Text) },
                    onClick = {
                        menu = false
                        vm.resetAll()
                    },
                )
                if (vm.lutName != null) {
                    DropdownMenuItem(
                        text = { Text("Remove LUT", color = CV.Text) },
                        onClick = {
                            menu = false
                            vm.clearLut()
                        },
                    )
                }
            }
        }

        Box(
            Modifier
                .padding(end = 6.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(CV.GradientH)
                .clickable(onClick = onExport),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.FileUpload, contentDescription = "Export", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

// =====================================================================
// Preview
// =====================================================================

@Composable
private fun PreviewArea(
    vm: EditorViewModel,
    ph: PlayheadState,
    player: ExoPlayer,
    fullscreen: Boolean,
    onExitFullscreen: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.fillMaxWidth().background(Color.Black),
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

        ClockChip(ph, Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = if (fullscreen) 64.dp else 8.dp))

        if (vm.comparing || vm.gradeHidden) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("ORIGINAL", color = CV.Sky, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (fullscreen) {
            FullscreenControls(ph, player, onExitFullscreen, onSeek, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun ClockChip(ph: PlayheadState, modifier: Modifier = Modifier) {
    val now by remember(ph) { derivedStateOf { clockText(ph.posMs) } }
    val total by remember(ph) { derivedStateOf { clockText(ph.durMs) } }
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text("$now / $total", color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun FullscreenControls(
    ph: PlayheadState,
    player: ExoPlayer,
    onExit: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarIcon(if (ph.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause") {
            if (player.isPlaying) player.pause() else player.play()
        }
        val dur = ph.durMs
        Slider(
            value = if (dur > 0L) (ph.posMs.toFloat() / dur).coerceIn(0f, 1f) else 0f,
            onValueChange = {
                ph.scrubbing = true
                if (dur > 0L) onSeek((it * dur).toLong())
            },
            onValueChangeFinished = { ph.scrubbing = false },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = CV.Sky,
                inactiveTrackColor = CV.Outline,
            ),
            modifier = Modifier.weight(1f).height(28.dp),
        )
        BarIcon(Icons.Rounded.FullscreenExit, "Exit full preview", onClick = onExit)
    }
}

// =====================================================================
// Transport row: undo/redo, prev/play/next, compare, fullscreen
// =====================================================================

@Composable
private fun BarIcon(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    tint: Color = CV.Text,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) tint else CV.Outline,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun TransportRow(
    vm: EditorViewModel,
    ph: PlayheadState,
    player: ExoPlayer,
    onFullscreen: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(CV.Surface).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarIcon(Icons.AutoMirrored.Rounded.Undo, "Undo", enabled = vm.canUndo) { vm.undo() }
        BarIcon(Icons.AutoMirrored.Rounded.Redo, "Redo", enabled = vm.canRedo) { vm.redo() }

        Spacer(Modifier.weight(1f))

        BarIcon(Icons.Rounded.SkipPrevious, "Go to start") {
            player.seekTo(0L)
            ph.posMs = 0L
        }
        Box(
            Modifier
                .padding(horizontal = 6.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(CV.GradientH)
                .clickable { if (player.isPlaying) player.pause() else player.play() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (ph.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Play or pause",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        BarIcon(Icons.Rounded.SkipNext, "Go to end") {
            val d = player.duration.coerceAtLeast(0L)
            if (d > 0L) {
                player.pause()
                val end = (d - 40L).coerceAtLeast(0L)
                player.seekTo(end)
                ph.posMs = end
            }
        }

        Spacer(Modifier.weight(1f))

        CompareButton(vm)
        BarIcon(Icons.Rounded.Fullscreen, "Full preview", onClick = onFullscreen)
    }
}

/** Dabaye rakho to original (bina grading) dikhta hai. */
@Composable
private fun CompareButton(vm: EditorViewModel) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (vm.comparing) CV.Surface2 else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    vm.holdOriginal(true)
                    tryAwaitRelease()
                    vm.holdOriginal(false)
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Compare,
            contentDescription = "Hold to see original",
            tint = if (vm.comparing) CV.Sky else CV.Text,
            modifier = Modifier.size(22.dp),
        )
    }
}
