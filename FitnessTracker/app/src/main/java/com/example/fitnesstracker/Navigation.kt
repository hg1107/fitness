package com.example.fitnesstracker

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.fitnesstracker.data.WorkoutDatabase
import com.example.fitnesstracker.theme.Black
import com.example.fitnesstracker.theme.CardGray
import com.example.fitnesstracker.theme.MediumGray
import com.example.fitnesstracker.theme.White
import com.example.fitnesstracker.ui.WorkoutViewModel
import com.example.fitnesstracker.ui.WorkoutViewModelFactory
import com.example.fitnesstracker.ui.ActivityViewModel
import com.example.fitnesstracker.ui.ActivityViewModelFactory
import com.example.fitnesstracker.ui.screens.DashboardScreen
import com.example.fitnesstracker.ui.screens.HistoryScreen
import com.example.fitnesstracker.ui.screens.LogExerciseScreen
import com.example.fitnesstracker.ui.screens.TrackScreen
import com.example.fitnesstracker.ui.screens.ActivitySummaryScreen
import com.example.fitnesstracker.ui.screens.ActivityDetailScreen

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val database = WorkoutDatabase.getDatabase(context)
  val workoutDao = database.workoutDao()
  val activityDao = database.activityDao()
  
  // Set up view models
  val workoutViewModel: WorkoutViewModel = viewModel(
      factory = WorkoutViewModelFactory(workoutDao)
  )
  val activityViewModel: ActivityViewModel = viewModel(
      factory = ActivityViewModelFactory(activityDao, context.applicationContext)
  )

  val backStack = rememberNavBackStack(Dashboard)
  val currentKey = backStack.lastOrNull()

  Scaffold(
      bottomBar = {
          if (currentKey == Dashboard || currentKey == Track || currentKey == History) {
              NavigationBar(
                  containerColor = CardGray,
                  tonalElevation = NavigationBarDefaults.Elevation
              ) {
                  NavigationBarItem(
                      selected = currentKey == Dashboard,
                      onClick = {
                          if (currentKey != Dashboard) {
                              backStack.clear()
                              backStack.add(Dashboard)
                          }
                      },
                      icon = { Icon(Icons.Default.Home, contentDescription = "Plan") },
                      label = { Text("Plan") },
                      colors = NavigationBarItemDefaults.colors(
                          selectedIconColor = Black,
                          selectedTextColor = White,
                          indicatorColor = White,
                          unselectedIconColor = MediumGray,
                          unselectedTextColor = MediumGray
                      )
                  )
                  NavigationBarItem(
                      selected = currentKey == Track,
                      onClick = {
                          if (currentKey != Track) {
                              backStack.removeAll { it == Track }
                              backStack.add(Track)
                          }
                      },
                      icon = { Icon(Icons.Default.LocationOn, contentDescription = "Track") },
                      label = { Text("Track") },
                      colors = NavigationBarItemDefaults.colors(
                          selectedIconColor = Black,
                          selectedTextColor = White,
                          indicatorColor = White,
                          unselectedIconColor = MediumGray,
                          unselectedTextColor = MediumGray
                      )
                  )
                  NavigationBarItem(
                       selected = currentKey == History,
                       onClick = {
                           if (currentKey != History) {
                               backStack.removeAll { it == History }
                               backStack.add(History)
                           }
                       },
                      icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History") },
                      label = { Text("History") },
                      colors = NavigationBarItemDefaults.colors(
                          selectedIconColor = Black,
                          selectedTextColor = White,
                          indicatorColor = White,
                          unselectedIconColor = MediumGray,
                          unselectedTextColor = MediumGray
                      )
                  )
              }
          }
      },
      containerColor = Black
  ) { padding ->
      NavDisplay(
          backStack = backStack,
          onBack = { backStack.removeLastOrNull() },
          entryProvider = entryProvider {
              entry<Dashboard> {
                  DashboardScreen(
                      viewModel = workoutViewModel,
                      onLogExercise = { exerciseName ->
                          backStack.add(LogExercise(exerciseName))
                      },
                      modifier = Modifier.padding(padding)
                  )
              }
              entry<LogExercise> { logKey ->
                  LogExerciseScreen(
                      exerciseName = logKey.exerciseName,
                      viewModel = workoutViewModel,
                      onNavigateBack = { backStack.removeLastOrNull() }
                  )
              }
              entry<Track> {
                  TrackScreen(
                      viewModel = activityViewModel,
                      onActivitySaved = { activityId ->
                          // Navigate to summary screen
                          backStack.removeAll { it == Track || it is ActivitySummary }
                          backStack.add(ActivitySummary(activityId))
                      },
                      modifier = Modifier.padding(padding)
                  )
              }
              entry<ActivitySummary> { summaryKey ->
                  ActivitySummaryScreen(
                      activityId = summaryKey.activityId,
                      viewModel = activityViewModel,
                      onSaveOrDiscard = {
                          // Clear stack back to tracking
                          backStack.clear()
                          backStack.add(Track)
                      }
                  )
              }
              entry<ActivityDetail> { detailKey ->
                  ActivityDetailScreen(
                      activityId = detailKey.activityId,
                      viewModel = activityViewModel,
                      onNavigateBack = { backStack.removeLastOrNull() }
                  )
              }
              entry<History> {
                  HistoryScreen(
                      workoutViewModel = workoutViewModel,
                      activityViewModel = activityViewModel,
                      onViewActivityDetail = { activityId ->
                          backStack.add(ActivityDetail(activityId))
                      },
                      modifier = Modifier.padding(padding)
                  )
              }
          }
      )
  }
}

