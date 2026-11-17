package com.creovid.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Recent Projects se baad me bhi video khul sake, isliye access yaad rakhne ki koshish
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            vm.loadVideo(uri)
        }
    }
    val pickLut = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.loadLut(uri)
    }

    if (vm.videoUri == null) {
        HomeScreen(
            projects = vm.projects,
            onStartEditing = {
                pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            onOpen = { vm.openProject(it) },
            onRename = { p, title -> vm.renameProject(p.id, title) },
            onDelete = { vm.deleteProject(it.id) },
        )
    } else {
        BackHandler { vm.closeVideo() }
        EditorScreen(
            vm = vm,
            onPickLut = { pickLut.launch(arrayOf("*/*")) },
        )
    }
}
