package com.creovid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.creovid.app.ui.CreovidTheme
import com.creovid.app.ui.EditorScreen
import com.creovid.app.ui.HomeScreen

class MainActivity : ComponentActivity() {

    private val vm: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CreovidTheme {
                CreovidApp(vm)
            }
        }
    }
}

@Composable
private fun CreovidApp(vm: EditorViewModel) {
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.loadVideo(uri)
    }
    val pickLut = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.loadLut(uri)
    }

    if (vm.videoUri == null) {
        HomeScreen(
            onPickVideo = {
                pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
        )
    } else {
        BackHandler { vm.closeVideo() }
        EditorScreen(
            vm = vm,
            onPickLut = { pickLut.launch(arrayOf("*/*")) },
        )
    }
}
