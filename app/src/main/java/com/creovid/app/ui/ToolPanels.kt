package com.creovid.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoFilter
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creovid.app.EditorViewModel
import com.creovid.app.engine.GradeParams
import com.creovid.app.engine.Preset
import com.creovid.app.engine.Presets
import com.creovid.app.engine.Wheel

/** Neeche ki bar ke 5 tools: icon + naam. */
enum class EditTool(val label: String, val icon: ImageVector) {
    Filters("Filters", Icons.Rounded.PhotoFilter),
    Adjust("Adjust", Icons.Rounded.Tune),
    Wheels("Colour Wheel", Icons.Rounded.Palette),
    Fx("FX", Icons.Rounded.AutoAwesome),
    Lut("Import LUT", Icons.Rounded.UploadFile),
}

// =====================================================================
// Bottom tool bar
// =====================================================================

@Composable
fun BottomToolBar(
    selected: EditTool?,
    lutActive: Boolean,
    onTool: (EditTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(CV.Surface)
            .padding(top = 6.dp, bottom = 8.dp),
    ) {
        EditTool.values().forEach { t ->
            val sel = t == selected
            val lit = sel || (t == EditTool.Lut && lutActive)
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onTool(t) }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(
                            if (sel) Modifier.background(CV.Gradient)
                            else Modifier.background(CV.Surface2).border(1.dp, if (lit) CV.Sky else CV.Outline, CircleShape),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        t.icon,
                        contentDescription = t.label,
                        tint = if (sel) Color.White else if (lit) CV.Sky else CV.Text,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    t.label,
                    color = if (sel) CV.Text else CV.TextDim,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

// =====================================================================
// Panel
// =====================================================================

@Composable
fun ToolPanel(
    tool: EditTool,
    vm: EditorViewModel,
    onPickLut: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(CV.Surface)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                tool.label,
                color = CV.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { resetTool(vm, tool) }) {
                Text("Reset", color = CV.TextDim, fontSize = 13.sp)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Check, contentDescription = "Done", tint = CV.Sky)
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            when (tool) {
                EditTool.Filters, EditTool.Lut -> FiltersPanel(vm, onPickLut)
                EditTool.Adjust -> AdjustPanel(vm)
                EditTool.Wheels -> WheelsPanel(vm)
                EditTool.Fx -> FxPanel(vm)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

private fun resetTool(vm: EditorViewModel, tool: EditTool) {
    val d = GradeParams()
    when (tool) {
        EditTool.Filters, EditTool.Lut -> vm.resetAll()
        EditTool.Adjust -> {
            vm.update {
                it.copy(
                    exposure = d.exposure, contrast = d.contrast, highlights = d.highlights,
                    shadows = d.shadows, temperature = d.temperature, tint = d.tint,
                    saturation = d.saturation, vibrance = d.vibrance, fade = d.fade, filmCurve = d.filmCurve,
                )
            }
            vm.finishEdit()
        }
        EditTool.Wheels -> {
            vm.update { it.copy(lift = Wheel(), gamma = Wheel(), gain = Wheel()) }
            vm.finishEdit()
        }
        EditTool.Fx -> {
            vm.update {
                it.copy(
                    grain = d.grain, bloom = d.bloom, halation = d.halation,
                    vignette = d.vignette, letterbox = d.letterbox,
                )
            }
            vm.finishEdit()
        }
    }
}

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
        onFinished = { vm.finishEdit() },
        default = default,
    )
}

// =====================================================================
// Filters
// =====================================================================

@Composable
private fun FiltersPanel(vm: EditorViewModel, onPickLut: () -> Unit) {
    Text("Cinematic looks", color = CV.TextDim, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(Presets.all) { p ->
            PresetCard(p, selected = vm.presetName == p.name) { vm.applyPreset(p) }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("Your LUT (.cube)", color = CV.TextDim, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlineButton(if (vm.lutName == null) "Import LUT" else "Change LUT") { onPickLut() }
        val name = vm.lutName
        if (name != null) {
            Spacer(Modifier.width(10.dp))
            Text(name, color = CV.Sky, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.clearLut() }) { Text("Remove", color = CV.TextDim) }
        }
    }
    if (vm.lutName != null) {
        Spacer(Modifier.height(8.dp))
        ParamSlider(vm, "LUT strength", vm.params.lutIntensity, 0f..1f, default = 1f) { s, v ->
            s.copy(lutIntensity = v)
        }
    }
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

// =====================================================================
// Adjust
// =====================================================================

@Composable
private fun AdjustPanel(vm: EditorViewModel) {
    val p = vm.params
    ParamSlider(vm, "Exposure", p.exposure, -2f..2f) { s, v -> s.copy(exposure = v) }
    ParamSlider(vm, "Contrast", p.contrast, -1f..1f) { s, v -> s.copy(contrast = v) }
    ParamSlider(vm, "Highlights", p.highlights, -1f..1f) { s, v -> s.copy(highlights = v) }
    ParamSlider(vm, "Shadows", p.shadows, -1f..1f) { s, v -> s.copy(shadows = v) }
    ParamSlider(vm, "Temperature (cool - warm)", p.temperature, -1f..1f) { s, v -> s.copy(temperature = v) }
    ParamSlider(vm, "Tint (green - magenta)", p.tint, -1f..1f) { s, v -> s.copy(tint = v) }
    ParamSlider(vm, "Saturation", p.saturation, -1f..1f) { s, v -> s.copy(saturation = v) }
    ParamSlider(vm, "Vibrance", p.vibrance, -1f..1f) { s, v -> s.copy(vibrance = v) }
    ParamSlider(vm, "Fade (matte)", p.fade, 0f..1f) { s, v -> s.copy(fade = v) }
    ParamSlider(vm, "Film curve", p.filmCurve, 0f..1f) { s, v -> s.copy(filmCurve = v) }
    Text("Tip: double-tap a label to reset that value.", color = CV.TextDim, fontSize = 11.sp)
}

// =====================================================================
// Colour wheels
// =====================================================================

@Composable
private fun WheelsPanel(vm: EditorViewModel) {
    val p = vm.params
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorWheel(
            "Shadows", p.lift,
            onChange = { w -> vm.update { it.copy(lift = w) } },
            onFinished = { vm.finishEdit() },
            modifier = Modifier.weight(1f),
        )
        ColorWheel(
            "Midtones", p.gamma,
            onChange = { w -> vm.update { it.copy(gamma = w) } },
            onFinished = { vm.finishEdit() },
            modifier = Modifier.weight(1f),
        )
        ColorWheel(
            "Highlights", p.gain,
            onChange = { w -> vm.update { it.copy(gain = w) } },
            onFinished = { vm.finishEdit() },
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Drag towards the edge for more colour. For a cinematic look try teal in the shadows and orange in the highlights. Double-tap a wheel to reset it.",
        color = CV.TextDim,
        fontSize = 11.sp,
    )
}

// =====================================================================
// FX
// =====================================================================

@Composable
private fun FxPanel(vm: EditorViewModel) {
    val p = vm.params
    ParamSlider(vm, "Film grain", p.grain, 0f..1f) { s, v -> s.copy(grain = v) }
    ParamSlider(vm, "Bloom (glow)", p.bloom, 0f..1f) { s, v -> s.copy(bloom = v) }
    ParamSlider(vm, "Halation (red glow)", p.halation, 0f..1f) { s, v -> s.copy(halation = v) }
    ParamSlider(vm, "Vignette", p.vignette, 0f..1f) { s, v -> s.copy(vignette = v) }
    Spacer(Modifier.height(10.dp))
    Text("Letterbox (cinema bars)", color = CV.Text, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    val options = listOf("Off" to 0f, "2.39:1" to 2.39f, "2:1" to 2.0f, "1.85:1" to 1.85f)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, value) ->
            ChoiceChip(label, selected = p.letterbox == value) {
                vm.update { it.copy(letterbox = value) }
                vm.finishEdit()
            }
        }
    }
}
