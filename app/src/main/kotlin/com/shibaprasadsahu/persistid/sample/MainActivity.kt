package com.shibaprasadsahu.persistid.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.PersistId
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersistIDTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PersistIdDemo(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalPersistIdApi::class)
@Composable
fun PersistIdDemo(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // State for identifier
    var deviceId by remember { mutableStateOf("Initializing...") }
    var hasIdentifier by remember { mutableStateOf(false) }

    // Use lifecycle-aware callback (recommended - auto cleanup!)
    DisposableEffect(Unit) {
        val callback = object : com.shibaprasadsahu.persistid.PersistIdCallback {
            override fun onReady(identifier: String) {
                // Called on main thread every time onStart is called
                deviceId = identifier
                scope.launch {
                    hasIdentifier = PersistId.getInstance().hasIdentifier()
                }
            }

            override fun onError(error: Exception) {
                deviceId = "Error: ${error.message}"
            }
        }

        // Lifecycle-aware observation - fires on every onStart, auto cleanup on destroy!
        PersistId.getInstance().observe(lifecycleOwner, callback)

        onDispose {
            // No manual cleanup needed - lifecycle handles it!
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "PersistID Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Your Device ID:",
            style = MaterialTheme.typography.bodyLarge
        )

        SelectionContainer {
            Text(
                text = deviceId,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = if (hasIdentifier) "Has Identifier: Yes" else "Has Identifier: No",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = {
                scope.launch {
                    val persistId = PersistId.getInstance()
                    val newId = persistId.regenerate()
                    deviceId = newId
                    hasIdentifier = persistId.hasIdentifier()
                }
            },
            enabled = deviceId != "Initializing..."
        ) {
            Text("Regenerate ID")
        }

        Button(
            onClick = {
                scope.launch {
                    val persistId = PersistId.getInstance()
                    persistId.clearIdentifier()
                    hasIdentifier = persistId.hasIdentifier()
                    deviceId = "Cleared - call regenerate() or restart app"
                }
            },
            enabled = deviceId != "Initializing..."
        ) {
            Text("Clear ID")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PersistIdDemoPreview() {
    PersistIDTheme {
        Text("PersistID Demo Preview")
    }
}