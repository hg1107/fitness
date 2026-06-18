package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.data.BodyMeasurement
import com.example.fitnesstracker.theme.*
import com.example.fitnesstracker.ui.NutritionViewModel
import com.example.fitnesstracker.ui.components.ProgressChartView
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMeasurementScreen(
    viewModel: NutritionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val measurements by viewModel.bodyMeasurements.collectAsState(initial = emptyList())

    var chestInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var hipsInput by remember { mutableStateOf("") }
    var armsInput by remember { mutableStateOf("") }
    var thighsInput by remember { mutableStateOf("") }

    var selectedChartMetric by remember { mutableStateOf(0) } // 0 = Waist, 1 = Chest, 2 = Hips, 3 = Arms, 4 = Thighs
    val chartMetricLabels = listOf("Waist", "Chest", "Hips", "Arms", "Thighs")

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Body Measurements", fontWeight = FontWeight.Bold, color = White, fontSize = 18.sp)
                        Text("Track size and composition changes", fontSize = 11.sp, color = MediumGray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Trend Chart Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Measurement Trends", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = White)
                Spacer(modifier = Modifier.height(8.dp))

                // Chart selector tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardGray)
                        .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                        .padding(3.dp)
                ) {
                    chartMetricLabels.forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedChartMetric == index) StravaOrange else Color.Transparent)
                                .clickable { selectedChartMetric = index }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selectedChartMetric == index) DarkBackground else LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Extract data points for chart
                val chartPoints = measurements
                    .filter {
                        val valDouble = when (selectedChartMetric) {
                            0 -> it.waistCm
                            1 -> it.chestCm
                            2 -> it.hipsCm
                            3 -> it.armsCm
                            4 -> it.thighsCm
                            else -> null
                        }
                        valDouble != null && valDouble > 0.0
                    }
                    .map {
                        val parsedDate = try {
                            dateFormat.parse(it.date)?.time ?: 0L
                        } catch (e: Exception) {
                            0L
                        }
                        parsedDate to when (selectedChartMetric) {
                            0 -> it.waistCm ?: 0.0
                            1 -> it.chestCm ?: 0.0
                            2 -> it.hipsCm ?: 0.0
                            3 -> it.armsCm ?: 0.0
                            4 -> it.thighsCm ?: 0.0
                            else -> 0.0
                        }
                    }
                    .filter { it.first > 0L }

                ProgressChartView(
                    points = chartPoints,
                    valueSuffix = "cm",
                    lineColor = StravaOrange,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Log New Measurements Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardGray),
                    border = BorderStroke(1.dp, BorderGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Log Today's Sizes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = White)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = waistInput,
                                onValueChange = { waistInput = it },
                                label = { Text("Waist (cm)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = White, unfocusedTextColor = White,
                                    focusedBorderColor = StravaOrange, unfocusedBorderColor = BorderGray
                                )
                            )
                            OutlinedTextField(
                                value = chestInput,
                                onValueChange = { chestInput = it },
                                label = { Text("Chest (cm)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = White, unfocusedTextColor = White,
                                    focusedBorderColor = StravaOrange, unfocusedBorderColor = BorderGray
                                )
                            )
                            OutlinedTextField(
                                value = hipsInput,
                                onValueChange = { hipsInput = it },
                                label = { Text("Hips (cm)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = White, unfocusedTextColor = White,
                                    focusedBorderColor = StravaOrange, unfocusedBorderColor = BorderGray
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = armsInput,
                                onValueChange = { armsInput = it },
                                label = { Text("Arms (cm)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = White, unfocusedTextColor = White,
                                    focusedBorderColor = StravaOrange, unfocusedBorderColor = BorderGray
                                )
                            )
                            OutlinedTextField(
                                value = thighsInput,
                                onValueChange = { thighsInput = it },
                                label = { Text("Thighs (cm)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = White, unfocusedTextColor = White,
                                    focusedBorderColor = StravaOrange, unfocusedBorderColor = BorderGray
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val c = chestInput.toDoubleOrNull()
                                val w = waistInput.toDoubleOrNull()
                                val h = hipsInput.toDoubleOrNull()
                                val a = armsInput.toDoubleOrNull()
                                val t = thighsInput.toDoubleOrNull()
                                if (c != null || w != null || h != null || a != null || t != null) {
                                    viewModel.logBodyMeasurement(c, w, h, a, t)
                                    chestInput = ""
                                    waistInput = ""
                                    hipsInput = ""
                                    armsInput = ""
                                    thighsInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black)
                        ) {
                            Text("Log Sizes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // History Log List Section
            item {
                Text("History Logs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = White)
            }

            if (measurements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No logs recorded yet.", color = MediumGray, fontSize = 13.sp)
                    }
                }
            } else {
                items(measurements) { log ->
                    BodyMeasurementCard(
                        log = log,
                        onDelete = { viewModel.deleteBodyMeasurement(log) }
                    )
                }
            }
        }
    }
}

@Composable
fun BodyMeasurementCard(
    log: BodyMeasurement,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.date,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Log", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                log.waistCm?.let { MeasurementMiniText("Waist", "${it}cm") }
                log.chestCm?.let { MeasurementMiniText("Chest", "${it}cm") }
                log.hipsCm?.let { MeasurementMiniText("Hips", "${it}cm") }
                log.armsCm?.let { MeasurementMiniText("Arms", "${it}cm") }
                log.thighsCm?.let { MeasurementMiniText("Thighs", "${it}cm") }
            }
        }
    }
}

@Composable
fun MeasurementMiniText(label: String, valStr: String) {
    Column {
        Text(label, color = MediumGray, fontSize = 10.sp)
        Text(valStr, color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
