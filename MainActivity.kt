package com.elva.music

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.elva.music.ui.ElvaApp
import com.elva.music.ui.ElvaTheme

class MainActivity : ComponentActivity() {
    private val vm: ElvaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        setContent { ElvaTheme { ElvaApp(vm) } }
    }
}
