package com.bekacodes.alphapulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bekacodes.alphapulse.data.local.AlphaPulseDatabase
import com.bekacodes.alphapulse.presentation.AlphaPulseScreen
import com.bekacodes.alphapulse.presentation.AlphaPulseViewModel

class MainActivity : ComponentActivity() {

    private val database by lazy { AlphaPulseDatabase.getInstance(applicationContext) }
    private val viewModel: AlphaPulseViewModel by viewModels {
        AlphaPulseViewModel.provideFactory(database.dao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AlphaPulseScreen(
                    uiState = uiState,
                    onIntent = viewModel::onIntent
                )
            }
        }
    }
}