package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

    // Initialize state when screen loads
    LaunchedEffect(exerciseName) {
        viewModel.startLogging(exerciseName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = exerciseName,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        fontSize = 20.sp
                    )
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
        containerColor = Black,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Chart Section
            Text(
                text = "Performance Comparison",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LightGray
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Metric Toggle (Weight vs Reps)
            MetricToggle(
                selectedMetric = chartMetric,
                onMetricSelected = { viewModel.setChartMetric(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Chart view
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardGray)
                    .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                    .padding(16.dp)
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

            Spacer(modifier = Modifier.height(20.dp))

            // Controls & Log Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sets",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                
                if (previousSession != null) {
                    Text(
                        text = "Copy Last Session",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightGray,
                        modifier = Modifier
                            .clickable { viewModel.copyPreviousSessionSets() }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Table Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set", modifier = Modifier.width(40.dp), color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Last", modifier = Modifier.width(90.dp), color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Weight", modifier = Modifier.weight(1f), color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Reps", modifier = Modifier.weight(1f), color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(48.dp)) // space for delete icon
            }

            // Sets List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(currentSets) { index, set ->
                    val lastSessionSet = previousSession?.sets?.getOrNull(index)
                    val lastSessionText = if (lastSessionSet != null) {
                        val w = lastSessionSet.weight.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                        "${w}kg x ${lastSessionSet.reps}"
                    } else {
                        "—"
                    }

                    SetRow(
                        setIndex = index + 1,
                        lastSessionText = lastSessionText,
                        weightValue = set.weight,
                        repsValue = set.reps,
                        onWeightChange = { viewModel.updateSetWeight(index, it) },
                        onRepsChange = { viewModel.updateSetReps(index, it) },
                        onDelete = { viewModel.deleteSet(index) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
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
            }

            // Save Button
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
                    .height(50.dp)
                    .padding(bottom = 8.dp)
            ) {
                Text("Save Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
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

    // Animate drawing progress
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500),
        label = "chartAnimation"
    )

    if (previousValues.isEmpty() && currentValues.all { it == 0.0 }) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Enter sets below to see progress chart",
                color = MediumGray,
                fontSize = 13.sp
            )
        }
    } else {
        Canvas(modifier = modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingBottom = 24.dp.toPx()
            val paddingTop = 20.dp.toPx()
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
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
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
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
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
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Text(
            text = setIndex.toString(),
            modifier = Modifier.width(40.dp),
            color = White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        // Previous performance
        Text(
            text = lastSessionText,
            modifier = Modifier.width(90.dp),
            color = MediumGray,
            fontSize = 14.sp
        )

        // Weight textfield
        OutlinedTextField(
            value = weightValue,
            onValueChange = onWeightChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = { Text("0", color = MediumGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = White,
                unfocusedBorderColor = BorderGray,
                focusedContainerColor = CardGray,
                unfocusedContainerColor = CardGray
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Reps textfield
        OutlinedTextField(
            value = repsValue,
            onValueChange = onRepsChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = { Text("0", color = MediumGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = White,
                unfocusedBorderColor = BorderGray,
                focusedContainerColor = CardGray,
                unfocusedContainerColor = CardGray
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        )

        // Delete button
        IconButton(
            onClick = onDelete,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MediumGray)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Set",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
