package com.creovid.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.creovid.app.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(onPickVideo: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(CV.Bg)
            .drawBehind {
                // Peeche halka purple glow (Savi jaisa)
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x557C4DFF), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.30f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            Image(
                painterResource(R.drawable.creovid_logo),
                contentDescription = "Creovid",
                modifier = Modifier.widthIn(max = 260.dp).fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Cinematic colour grading,\nseedha tumhare phone pe.",
                color = CV.Text,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "100% free  -  no watermark  -  no login",
                color = CV.TextDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            GradientButton("Video chuno", Modifier.fillMaxWidth()) { onPickVideo() }
            Spacer(Modifier.height(28.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(
                    "12 cinematic looks",
                    "LUT import (.cube)",
                    "Colour wheels",
                    "Film grain",
                    "Bloom + Halation",
                    "Vignette",
                    "Cinema bars",
                    "1080p / Original export",
                ).forEach { FeatureChip(it) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureChip(text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(CV.Surface2)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(CV.Violet, 6)
        Spacer(Modifier.padding(start = 6.dp))
        Text(text, color = CV.Text, fontSize = 12.sp)
    }
}
