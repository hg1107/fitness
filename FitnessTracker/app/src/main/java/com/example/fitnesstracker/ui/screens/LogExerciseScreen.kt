package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.theme.Black
import com.example.fitnesstracker.theme.BorderGray
import com.example.fitnesstracker.theme.CardGray
import com.example.fitnesstracker.theme.LightGray
import com.example.fitnesstracker.theme.MediumGray
import com.example.fitnesstracker.theme.MutedDarkGray
import com.example.fitnesstracker.theme.White
import com.example.fitnesstracker.ui.ChartMetric
import com.example.fitnesstracker.ui.SetInputState
import com.example.fitnesstracker.ui.WorkoutViewModel
import kotlin.math.max
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogExerciseScreen(
    exerciseName: String,
    onNavigateBack: () -> Unit,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val previousSession by viewModel.previousSession.collectAsState()
    val chartMetric by viewModel.chartMetric.collectAsState()
    val currentSets = viewModel.currentSets
    val sessionNotes by viewModel.sessionNotes.collectAsState()
    val currentVolume by viewModel.currentVolume.collectAsState(initial = 0.0)
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    val restTimerRunning by viewModel.restTimerRunning.collectAsState()
    val restTimerDuration by viewModel.restTimerDuration.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    // Initialize state when screen loads
    LaunchedEffect(exerciseName) {
        viewModel.startLogging(exerciseName)
    }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.timerCompletedEvent.collect {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(300)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbarHostState.showSnackbar(
                message = "Rest finished! Time for the next set.",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = exerciseName,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            fontSize = 18.sp
                        )
                        if (previousSession != null) {
                            Text(
                                text = "Last session available",
                                fontSize = 11.sp,
                                color = MediumGray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Black,
                    titleContentColor = White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Black,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Rest Timer Banner
            AnimatedVisibility(
                visible = restTimerRunning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    RestTimerBanner(
                        secondsLeft = restTimerSeconds,
                        totalSeconds = restTimerDuration,
                        onStop = { viewModel.stopRestTimer() }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Chart Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Performance Comparison",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightGray
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Custom date button
                    val customTimestamp by viewModel.sessionTimestamp.collectAsState()
                    val dateLabel = customTimestamp?.let {
                        SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))
                    } ?: "Today"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardGray)
                            .border(1.dp, BorderGray, RoundedCornerShape(6.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = dateLabel,
                            fontSize = 11.sp,
                            color = LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Current session volume
                    val formattedVol = currentVolume.let {
                        if (it % 1.0 == 0.0) "${it.toInt()} kg" else "${it} kg"
                    }
                    if (currentVolume > 0) {
                        Text(
                            text = "Vol: $formattedVol",
                            fontSize = 13.sp,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metric Toggle (Weight vs Reps)
            MetricToggle(
                selectedMetric = chartMetric,
                onMetricSelected = { viewModel.setChartMetric(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Chart view - use a key to trigger recompose on set changes
            val chartKey = currentSets.map { "${it.weight}-${it.reps}" }.joinToString()
            key(chartKey) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardGray)
                        .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    val previousValues = previousSession?.sets?.map {
                        if (chartMetric == ChartMetric.WEIGHT) it.weight else it.reps.toDouble()
                    } ?: emptyList()

                    val currentValues = currentSets.map {
                        val valueStr = if (chartMetric == ChartMetric.WEIGHT) it.weight else it.reps
                        valueStr.toDoubleOrNull() ?: 0.0
                    }

                    ComparisonChart(
                        previousValues = previousValues,
                        currentValues = currentValues,
                        metric = chartMetric
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls & Log Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sets  (${currentSets.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rest Timer button with dropdown presets
                    var showTimerPresets by remember { mutableStateOf(false) }
                    Box {
                        val timerLabel = if (restTimerRunning) {
                            formatTime(restTimerSeconds)
                        } else {
                            "Rest ${formatTime(restTimerDuration)}"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (restTimerRunning) White else CardGray)
                                .border(1.dp, BorderGray, RoundedCornerShape(6.dp))
                        ) {
                            // Left part: timer label (start or stop)
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        if (restTimerRunning) {
                                            viewModel.stopRestTimer()
                                        } else {
                                            viewModel.startRestTimer()
                                        }
                                    }
                                    .padding(start = 10.dp, end = if (restTimerRunning) 10.dp else 6.dp, top = 5.dp, bottom = 5.dp)
                            ) {
                                Text(
                                    text = timerLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (restTimerRunning) Black else LightGray
                                )
                            }
                            
                            // Right part: dropdown arrow (only visible when not running)
                            if (!restTimerRunning) {
                                Box(
                                    modifier = Modifier
                                        .clickable { showTimerPresets = !showTimerPresets }
                                        .padding(start = 4.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)
                                ) {
                                    Text(
                                        text = "▾",
                                        fontSize = 10.sp,
                                        color = MediumGray
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showTimerPresets,
                            onDismissRequest = { showTimerPresets = false },
                            containerColor = CardGray
                        ) {
                            listOf(30, 60, 90, 120).forEach { seconds ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            formatTime(seconds),
                                            color = if (seconds == restTimerDuration) White else LightGray,
                                            fontWeight = if (seconds == restTimerDuration) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        showTimerPresets = false
                                        viewModel.startRestTimer(seconds)
                                    }
                                )
                            }
                        }
                    }

                    if (previousSession != null) {
                        Text(
                            text = "Copy Last",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightGray,
                            modifier = Modifier
                                .clickable { viewModel.copyPreviousSessionSets() }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Table Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", modifier = Modifier.width(32.dp), color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Last", modifier = Modifier.width(80.dp), color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("kg", modifier = Modifier.weight(1f), color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reps", modifier = Modifier.weight(1f), color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(40.dp))
            }

            // Sets List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                itemsIndexed(currentSets) { index, set ->
                    val lastSessionSet = previousSession?.sets?.getOrNull(index)
                    val lastSessionText = if (lastSessionSet != null) {
                        val w = lastSessionSet.weight.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                        "${w}×${lastSessionSet.reps}"
                    } else {
                        "—"
                    }

                    SetRow(
                        setIndex = index + 1,
                        lastSessionText = lastSessionText,
                        weightValue = set.weight,
                        repsValue = set.reps,
                        isPersonalBest = set.isPersonalBest,
                        onWeightChange = { viewModel.updateSetWeight(index, it) },
                        onRepsChange = { viewModel.updateSetReps(index, it) },
                        onDelete = { viewModel.deleteSet(index) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.addSet() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardGray,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                    ) {
                        Text("+ Add Set", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                // Notes field
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { viewModel.updateSessionNotes(it) },
                        label = { Text("Session notes (optional)", color = MediumGray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = BorderGray,
                            unfocusedBorderColor = BorderGray,
                            focusedContainerColor = CardGray,
                            unfocusedContainerColor = CardGray
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            }

            // Save Button
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.saveSession(onSuccess = onNavigateBack)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = White,
                    contentColor = Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Save Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.sessionTimestamp.value ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSessionTimestamp(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = White)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = LightGray)
                ) {
                    Text("Cancel")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = CardGray
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CardGray,
                    titleContentColor = White,
                    headlineContentColor = White,
                    weekdayContentColor = MediumGray,
                    subheadContentColor = MediumGray,
                    navigationContentColor = White,
                    yearContentColor = LightGray,
                    selectedYearContentColor = Black,
                    selectedYearContainerColor = White,
                    dayContentColor = LightGray,
                    selectedDayContentColor = Black,
                    selectedDayContainerColor = White,
                    todayContentColor = White,
                    todayDateBorderColor = White
                )
            )
        }
    }
}

@Composable
fun RestTimerBanner(
    secondsLeft: Int,
    totalSeconds: Int,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardGray)
            .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rest Timer",
                color = MediumGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formatTime(secondsLeft),
                    color = White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BorderGray)
                        .clickable { onStop() }
                        .padding(6.dp)
                ) {
                    Text("✕", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = White,
            trackColor = BorderGray
        )
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s.toString().padStart(2, '0')}s" else "${s}s"
}

@Composable
fun MetricToggle(
    selectedMetric: ChartMetric,
    onMetricSelected: (ChartMetric) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardGray)
            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
            .padding(2.dp)
    ) {
        val options = listOf(
            ChartMetric.WEIGHT to "Weight (kg)",
            ChartMetric.REPS to "Reps"
        )
        
        options.forEach { (metric, label) ->
            val isSelected = selectedMetric == metric
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) White else Color.Transparent)
                    .clickable { onMetricSelected(metric) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Black else LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ComparisonChart(
    previousValues: List<Double>,
    currentValues: List<Double>,
    metric: ChartMetric,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Determine constraints
    val numSets = max(1, max(previousValues.size, currentValues.size))
    val maxVal = max(10.0, max(previousValues.maxOrNull() ?: 0.0, currentValues.maxOrNull() ?: 0.0))

    // Animate drawing progress — keyed on the data changing
    var animationTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(previousValues, currentValues) {
        animationTrigger++
    }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationTrigger > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "chartAnimation"
    )

    if (previousValues.isEmpty() && currentValues.all { it == 0.0 }) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Enter sets below to see your progress",
                color = MediumGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (previousValues.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MutedDarkGray)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Last", color = MediumGray, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(White)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Now", color = LightGray, fontSize = 10.sp)
        }

        Canvas(modifier = modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingBottom = 22.dp.toPx()
            val paddingTop = 16.dp.toPx()
            val usableHeight = height - paddingBottom - paddingTop
            val colWidth = width / numSets

            // Draw thin horizontal grid reference lines (0%, 50%, 100%)
            val gridLines = listOf(0.0, 0.5, 1.0)
            gridLines.forEach { percentage ->
                val y = paddingTop + usableHeight * (1f - percentage.toFloat())
                drawLine(
                    color = BorderGray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            for (i in 0 until numSets) {
                val colCenterX = i * colWidth + colWidth / 2f
                val barWidth = colWidth * 0.22f
                val spacing = colWidth * 0.04f

                // --- 1. Previous Set Bar (Muted Dark Gray) ---
                val prevVal = previousValues.getOrNull(i) ?: 0.0
                if (prevVal > 0.0) {
                    val prevBarHeight = (prevVal / maxVal).toFloat() * usableHeight * animationProgress
                    val prevY = paddingTop + usableHeight - prevBarHeight
                    
                    drawRoundRect(
                        color = MutedDarkGray,
                        topLeft = Offset(colCenterX - barWidth - spacing, prevY),
                        size = Size(barWidth, prevBarHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // Draw previous value text directly above the bar
                    val prevText = if (metric == ChartMetric.WEIGHT) {
                        prevVal.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                    } else {
                        prevVal.toInt().toString()
                    }
                    val textLayoutResult = textMeasurer.measure(
                        text = prevText,
                        style = TextStyle(color = MediumGray, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = colCenterX - barWidth - spacing + (barWidth - textLayoutResult.size.width) / 2f,
                            y = prevY - textLayoutResult.size.height - 2.dp.toPx()
                        )
                    )
                }

                // --- 2. Current Set Bar (Solid White) ---
                val currVal = currentValues.getOrNull(i) ?: 0.0
                if (currVal > 0.0) {
                    val currBarHeight = (currVal / maxVal).toFloat() * usableHeight * animationProgress
                    val currY = paddingTop + usableHeight - currBarHeight

                    drawRoundRect(
                        color = White,
                        topLeft = Offset(colCenterX + spacing, currY),
                        size = Size(barWidth, currBarHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // Draw current value text directly above the bar
                    val currText = if (metric == ChartMetric.WEIGHT) {
                        currVal.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                    } else {
                        currVal.toInt().toString()
                    }
                    val textLayoutResult = textMeasurer.measure(
                        text = currText,
                        style = TextStyle(color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = colCenterX + spacing + (barWidth - textLayoutResult.size.width) / 2f,
                            y = currY - textLayoutResult.size.height - 2.dp.toPx()
                        )
                    )
                }

                // --- 3. Set Label below ---
                val setLabel = "S${i + 1}"
                val labelLayoutResult = textMeasurer.measure(
                    text = setLabel,
                    style = TextStyle(color = MediumGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = labelLayoutResult,
                    topLeft = Offset(
                        x = colCenterX - labelLayoutResult.size.width / 2f,
                        y = height - labelLayoutResult.size.height
                    )
                )
            }
        }
    }
}

@Composable
fun SetRow(
    setIndex: Int,
    lastSessionText: String,
    weightValue: String,
    repsValue: String,
    isPersonalBest: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPersonalBest) CardGray else Color.Transparent)
            .border(
                width = if (isPersonalBest) 1.dp else 0.dp,
                color = if (isPersonalBest) LightGray else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = if (isPersonalBest) 8.dp else 0.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number + PR badge
        Column(
            modifier = Modifier.width(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = setIndex.toString(),
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (isPersonalBest) {
                Text(
                    text = "PR",
                    color = White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Previous performance
        Text(
            text = lastSessionText,
            modifier = Modifier.width(80.dp),
            color = MediumGray,
            fontSize = 13.sp
        )

        // Weight textfield
        OutlinedTextField(
            value = weightValue,
            onValueChange = onWeightChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            placeholder = { Text("0", color = MediumGray, fontSize = 14.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = White,
                unfocusedBorderColor = BorderGray,
                focusedContainerColor = CardGray,
                unfocusedContainerColor = CardGray
            ),
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Reps textfield
        OutlinedTextField(
            value = repsValue,
            onValueChange = onRepsChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = { Text("0", color = MediumGray, fontSize = 14.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = White,
                unfocusedBorderColor = BorderGray,
                focusedContainerColor = CardGray,
                unfocusedContainerColor = CardGray
            ),
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            shape = RoundedCornerShape(8.dp)
        )

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MediumGray)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Set",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
