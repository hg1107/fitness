package com.example.fitnesstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.fitnesstracker.service.WeeklySummaryWorker
import com.example.fitnesstracker.service.WorkoutReminderWorker
import com.example.fitnesstracker.theme.FitnessTrackerTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Schedule the daily workout reminder and weekly recap (no-ops if already scheduled)
    WorkoutReminderWorker.schedule(applicationContext)
    WeeklySummaryWorker.schedule(applicationContext)

    enableEdgeToEdge()
    setContent {
      FitnessTrackerTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
