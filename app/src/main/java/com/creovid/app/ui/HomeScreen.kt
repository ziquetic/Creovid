package com.creovid.app.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creovid.app.R
import com.creovid.app.data.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.random.Random

// =====================================================================
// Hero slides (3 slide, har ek ka apna rang)
// =====================================================================

private class Slide(
    val title: String,
    val sub: String,
    val sky: List<Color>,
    val glow: Color,
    val far: Color,
    val near: Color,
    val window: Color,
)

private val slides = listOf(
    Slide(
        "Professional\nColour Grading\nOn Your Phone",
        "Edit  •  Grade  •  Enhance",
        listOf(Color(0xFF0A0720), Color(0xFF2B1166), Color(0xFF7A3FC8), Color(0xFFE56E9C)),
        Color(0xFFFF8FB8), Color(0xFF3A1D78), Color(0xFF0C0820), Color(0xFFFFD27A),
    ),
    Slide(
        "12 Cinematic\nLooks Ready\nIn One Tap",
        "Teal  •  Noir  •  Golden Hour",
        listOf(Color(0xFF04131C), Color(0xFF0D4452), Color(0xFF2E8A94), Color(0xFFE39A52)),
        Color(0xFFFFB066), Color(0xFF0F4048), Color(0xFF05161B), Color(0xFFFFC27A),
    ),
    Slide(
        "Bring Your\nOwn LUTs\n.cube Support",
        "Free  •  No Watermark  •  1080p",
        listOf(Color(0xFF050A1F), Color(0xFF14297A), Color(0xFF4A63F0), Color(0xFFB68CFF)),
        Color(0xFFC9A8FF), Color(0xFF1B2F86), Color(0xFF060B22), Color(0xFF9FD0FF),
    ),
)

// =====================================================================
// Home screen
// =====================================================================

@Composable
fun HomeScreen(
    projects: List<Project>,
    onStartEditing: () -> Unit,
    onOpen: (Project) -> Unit,
    onRename: (Project, String) -> Unit,
    onDelete: (Project) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Project?>(null) }
    var deleting by remember { mutableStateOf<Project?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(CV.Bg)
            .drawBehind {
                val w = size.width
                val h = size.height
                // Upar halka purple glow
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x447C4DFF), Color.Transparent),
                        center = Offset(w * 0.85f, 0f),
                        radius = w * 0.9f,
                    ),
                )
                // Neeche ghumavdar purple curve (mockup jaisa)
                val curve = Path().apply {
                    moveTo(0f, h * 0.90f)
                    cubicTo(w * 0.30f, h * 0.935f, w * 0.72f, h * 0.90f, w, h * 0.82f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    curve,
                    Brush.verticalGradient(
                        listOf(Color(0x226A3DF0), Color(0x886A3DF0)),
                        startY = h * 0.80f,
                        endY = h,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x555B3CE0), Color.Transparent),
                        center = Offset(w * 0.85f, h),
                        radius = w * 0.8f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Header()
            HeroPager(onStartEditing)
            RecentHeader(
                showToggle = projects.size > 3,
                expanded = expanded,
                onToggle = { expanded = !expanded },
            )
            when {
                projects.isEmpty() -> EmptyProjects()
                expanded -> Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    projects.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { p ->
                                ProjectCard(
                                    p, Modifier.weight(1f),
                                    onOpen = { onOpen(p) },
                                    onRename = { renaming = p },
                                    onDelete = { deleting = p },
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                else -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(projects, key = { it.id }) { p ->
                        ProjectCard(
                            p, Modifier.width(148.dp),
                            onOpen = { onOpen(p) },
                            onRename = { renaming = p },
                            onDelete = { deleting = p },
                        )
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    // ---------- Rename ----------
    renaming?.let { p ->
        var text by remember(p.id) { mutableStateOf(p.title) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            containerColor = CV.Surface2,
            title = { Text("Rename project", color = CV.Text) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 30) text = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                GradientButton("Save") {
                    onRename(p, text)
                    renaming = null
                }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text("Cancel", color = CV.TextDim) }
            },
        )
    }

    // ---------- Delete ----------
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = CV.Surface2,
            title = { Text("Delete project?", color = CV.Text) },
            text = {
                Text(
                    "\"${p.title}\" Recent Projects se hat jayega. Gallery me export ki hui video aur original video safe rahegi.",
                    color = CV.TextDim,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                GradientButton("Delete") {
                    onDelete(p)
                    deleting = null
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel", color = CV.TextDim) }
            },
        )
    }
}

// =====================================================================
// Header
// =====================================================================

@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.creovid_mark), contentDescription = "Creovid", modifier = Modifier.size(58.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Creovid", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Colour Grade. Create More.", color = CV.TextDim, fontSize = 13.sp)
        }
    }
}

// =====================================================================
// Hero banner (pager)
// =====================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroPager(onStart: () -> Unit) {
    val pager = rememberPagerState(pageCount = { slides.size })

    LaunchedEffect(pager) {
        while (true) {
            delay(4500)
            if (!pager.isScrollInProgress) {
                pager.animateScrollToPage((pager.currentPage + 1) % slides.size)
            }
        }
    }

    HorizontalPager(
        state = pager,
        contentPadding = PaddingValues(horizontal = 20.dp),
        pageSpacing = 12.dp,
    ) { page ->
        HeroCard(slides[page], seed = page + 7, onStart = onStart)
    }
    PagerDots(count = slides.size, current = pager.currentPage)
}

@Composable
private fun HeroCard(slide: Slide, seed: Int, onStart: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.45f)
            .clip(shape)
            .border(1.dp, Color(0x33FFFFFF), shape),
    ) {
        SkylineArt(slide, seed, Modifier.fillMaxSize())
        // Text padhne layak rahe, isliye bayein taraf halka andhera
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xE6000000), Color(0x99000000), Color.Transparent),
                    ),
                ),
        )
        Column(
            Modifier.fillMaxHeight().padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                slide.title,
                color = Color.White,
                fontSize = 21.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(slide.sub, color = Color(0xFFB8B4D0), fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF5B3CF0), Color(0xFF7C4DFF))))
                    .clickable(onClick = onStart)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Start Editing", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PagerDots(count: Int, current: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val selected = i == current
            val w by animateDpAsState(if (selected) 22.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(w)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) CV.Violet else Color(0xFF3A3556)),
            )
        }
    }
}

/** Code se bani sheher ki skyline + silhouette (koi bahari photo nahi). */
@Composable
private fun SkylineArt(slide: Slide, seed: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(slide.sky))

        val glowCenter = Offset(w * 0.74f, h * 0.60f)
        drawCircle(
            Brush.radialGradient(
                listOf(slide.glow.copy(alpha = 0.55f), Color.Transparent),
                center = glowCenter,
                radius = w * 0.5f,
            ),
            radius = w * 0.5f,
            center = glowCenter,
        )

        val rnd = Random(seed)
        drawSkyline(rnd, w * 0.22f, w, h, h * 0.18f, h * 0.44f, slide.far, null)
        drawSkyline(rnd, w * 0.35f, w, h, h * 0.12f, h * 0.34f, slide.near, slide.window)

        // Peeche se dikhta hua insaan (hoodie wala silhouette)
        val dark = Color(0xFF05030C)
        val body = Path().apply {
            moveTo(w * 0.56f, h)
            cubicTo(w * 0.57f, h * 0.80f, w * 0.62f, h * 0.72f, w * 0.69f, h * 0.69f)
            lineTo(w * 0.79f, h * 0.69f)
            cubicTo(w * 0.87f, h * 0.72f, w * 0.92f, h * 0.80f, w * 0.93f, h)
            close()
        }
        drawPath(body, dark)
        drawRect(dark, Offset(w * 0.70f, h * 0.60f), Size(w * 0.08f, h * 0.12f))
        drawCircle(dark, radius = h * 0.105f, center = Offset(w * 0.74f, h * 0.55f))
    }
}

private fun DrawScope.drawSkyline(
    rnd: Random,
    x0: Float,
    x1: Float,
    baseY: Float,
    minH: Float,
    maxH: Float,
    color: Color,
    window: Color?,
) {
    val unit = size.width * 0.013f
    var x = x0
    while (x < x1) {
        val bw = size.width * (0.028f + rnd.nextFloat() * 0.04f)
        val bh = minH + rnd.nextFloat() * (maxH - minH)
        drawRect(color, Offset(x, baseY - bh), Size(bw, bh))
        if (window != null) {
            var wy = baseY - bh + unit
            while (wy < baseY - unit) {
                var wx = x + unit * 0.6f
                while (wx < x + bw - unit) {
                    if (rnd.nextFloat() < 0.22f) {
                        drawRect(
                            window.copy(alpha = 0.35f + rnd.nextFloat() * 0.55f),
                            Offset(wx, wy),
                            Size(unit * 0.5f, unit * 0.7f),
                        )
                    }
                    wx += unit
                }
                wy += unit * 1.7f
            }
        }
        x += bw + size.width * 0.004f
    }
}

// =====================================================================
// Recent Projects
// =====================================================================

@Composable
private fun RecentHeader(showToggle: Boolean, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Recent Projects", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        if (showToggle) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onToggle).padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (expanded) "Show Less" else "See All", color = CV.TextDim, fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = CV.TextDim, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyProjects() {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(shape)
            .border(1.dp, CV.Outline, shape)
            .background(Color(0x99100C22))
            .padding(22.dp),
    ) {
        Text("No projects yet", color = CV.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Start Editing dabao aur video chuno. Tumhare graded projects yahin dikhenge.",
            color = CV.TextDim,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ProjectCard(
    p: Project,
    modifier: Modifier,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier
            .clip(shape)
            .background(Color(0xFF14102A))
            .clickable(onClick = onOpen),
    ) {
        Thumb(p.thumbPath, Modifier.fillMaxWidth().aspectRatio(0.95f))
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 9.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    p.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${timeAgo(p.updatedAt)}  •  ${formatDuration(p.durationMs)}",
                    color = CV.TextDim,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { menu = true }
                        .padding(4.dp),
                )
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    modifier = Modifier.background(CV.Surface2),
                ) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun Thumb(path: String?, modifier: Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = if (path == null) {
            null
        } else {
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
        }
    }
    Box(
        modifier.background(Brush.linearGradient(listOf(Color(0xFF2A1A5E), Color(0xFF0E0A22)))),
        contentAlignment = Alignment.Center,
    ) {
        val b = bitmap
        if (b != null) {
            Image(bitmap = b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0x66FFFFFF), modifier = Modifier.size(34.dp))
        }
    }
}

// =====================================================================
// Helpers
// =====================================================================

private fun timeAgo(ts: Long): String {
    val minutes = max(0L, System.currentTimeMillis() - ts) / 60_000L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        minutes < 60 * 24 * 30 -> "${minutes / (60 * 24)}d ago"
        else -> "${minutes / (60 * 24 * 30)}mo ago"
    }
}

private fun formatDuration(ms: Long): String {
    val s = max(0L, ms) / 1000L
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}
