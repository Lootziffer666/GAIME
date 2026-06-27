package com.aiundb.gaime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.App
import rpg.save.SaveStorageContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Provide an application context for save/load persistence (no leak).
        SaveStorageContext.appContext = applicationContext
        setContent {
            var lifecycleActive by remember { mutableStateOf(true) }

            // Observe lifecycle to pause animation when app is backgrounded
            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            lifecycleActive = true
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            lifecycleActive = false
                        }
                        else -> { /* no-op */ }
                    }
                }
                this@MainActivity.lifecycle.addObserver(observer)
                onDispose {
                    this@MainActivity.lifecycle.removeObserver(observer)
                }
            }

            App(lifecycleActive = lifecycleActive)
        }
    }
}
