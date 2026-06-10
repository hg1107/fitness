package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.data.CoachMessage
import com.example.fitnesstracker.theme.*
import com.example.fitnesstracker.ui.NutritionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICoachScreen(
    viewModel: NutritionViewModel,
    onNavigateBack: () -> Unit
) {
    val userProfileState by viewModel.userProfile.collectAsState(initial = null)
    val coachMessages by viewModel.coachMessages.collectAsState(initial = emptyList())
    val isThinking by viewModel.isCoachThinking.collectAsState()
    
    val profile = userProfileState ?: return
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var messageText by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    // Settings fields
    var budget by remember(profile) { mutableStateOf(profile.budget) }
    var region by remember(profile) { mutableStateOf(profile.region) }
    var cuisine by remember(profile) { mutableStateOf(profile.preferredCuisine) }
    var timings by remember(profile) { mutableStateOf(profile.mealTimings) }
    var gymSchedule by remember(profile) { mutableStateOf(profile.gymSchedule) }
    var homeFoods by remember(profile) { mutableStateOf(profile.availableFoodsAtHome) }
    var apiKey by remember(profile) { mutableStateOf(profile.geminiApiKey) }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(coachMessages.size, isThinking) {
        if (coachMessages.isNotEmpty()) {
            listState.animateScrollToItem(coachMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Nutrition Coach", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isThinking) "Coach is formulating advice..." else "Online & analyzing intake",
                            fontSize = 11.sp,
                            color = if (isThinking) Color(0xFF00E676) else MediumGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Coach settings",
                            tint = if (showSettings) Color(0xFF00E676) else White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Black,
                    titleContentColor = White,
                    navigationIconContentColor = White,
                    actionIconContentColor = White
                )
            )
        },
        containerColor = Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Settings Panel (expandable)
            AnimatedVisibility(
                visible = showSettings,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CardGray),
                    border = BorderStroke(1.dp, BorderGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Coach Preferences Settings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Budget Selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Budget Tier", fontSize = 11.sp, color = MediumGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                var expandedBudget by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { expandedBudget = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BorderGray),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(budget, fontSize = 12.sp, color = White)
                                    }
                                    DropdownMenu(
                                        expanded = expandedBudget,
                                        onDismissRequest = { expandedBudget = false },
                                        modifier = Modifier.background(CardGray)
                                    ) {
                                        listOf("Student", "Moderate", "Premium").forEach { tier ->
                                            DropdownMenuItem(
                                                text = { Text(tier, color = White) },
                                                onClick = {
                                                    budget = tier
                                                    expandedBudget = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Region Selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Region/Country", fontSize = 11.sp, color = MediumGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = region,
                                    onValueChange = { region = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = White, fontSize = 12.sp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = White,
                                        unfocusedBorderColor = BorderGray
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Cuisine
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Preferred Cuisine", fontSize = 11.sp, color = MediumGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = cuisine,
                                    onValueChange = { cuisine = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = White, fontSize = 12.sp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = White,
                                        unfocusedBorderColor = BorderGray
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Gym schedule
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gym Days", fontSize = 11.sp, color = MediumGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = gymSchedule,
                                    onValueChange = { gymSchedule = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = White, fontSize = 12.sp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = White,
                                        unfocusedBorderColor = BorderGray
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Timings
                        Text("Meal Timings", fontSize = 11.sp, color = MediumGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = timings,
                            onValueChange = { timings = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = White, fontSize = 12.sp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = White,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Available foods
                        Text("Available Foods at Home (comma-separated)", fontSize = 11.sp, color = MediumGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = homeFoods,
                            onValueChange = { homeFoods = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = White, fontSize = 12.sp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = White,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Gemini API Key
                        Text("Gemini API Key (Optional)", fontSize = 11.sp, color = MediumGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            singleLine = true,
                            placeholder = { Text("Paste API Key for true AI", fontSize = 12.sp, color = MediumGray) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = White, fontSize = 12.sp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = White,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.clearChat()
                                    showSettings = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear Chat", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.saveCoachSettings(
                                        budget = budget.trim(),
                                        region = region.trim(),
                                        preferredCuisine = cuisine.trim(),
                                        mealTimings = timings.trim(),
                                        gymSchedule = gymSchedule.trim(),
                                        availableFoodsAtHome = homeFoods.trim(),
                                        geminiApiKey = apiKey.trim()
                                    )
                                    showSettings = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black)
                            ) {
                                Text("Apply Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Chat Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (coachMessages.isEmpty()) {
                    // Empty state welcome instructions
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CardGray,
                            modifier = Modifier.size(72.dp),
                            border = BorderStroke(1.dp, BorderGray)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🥗", fontSize = 32.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Meet your AI Nutrition Coach",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ask questions, generate custom meal plans, verify goal alignment, or check food substitutions based on your profile and available foods.",
                            fontSize = 13.sp,
                            color = MediumGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                    ) {
                        items(coachMessages) { message ->
                            ChatBubble(message = message)
                        }

                        if (isThinking) {
                            item {
                                ThinkingBubble()
                            }
                        }
                    }
                }
            }

            // Quick suggestion chips
            val suggestions = listOf(
                "What should I eat tonight?",
                "How can I hit my protein goal?",
                "Suggest a vegetarian meal with 40g protein.",
                "Give me a 700 calorie bulking meal."
            )
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { text ->
                    Surface(
                        onClick = {
                            viewModel.sendMessage(text)
                            keyboardController?.hide()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = CardGray,
                        border = BorderStroke(1.dp, BorderGray),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(text, color = LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Message Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Ask your coach anything...", color = MediumGray, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = White,
                        unfocusedBorderColor = BorderGray,
                        focusedContainerColor = CardGray,
                        unfocusedContainerColor = CardGray
                    )
                )

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText.trim())
                            messageText = ""
                            keyboardController?.hide()
                        }
                    },
                    enabled = messageText.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (messageText.isNotBlank() && !isThinking) White else CardGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank() && !isThinking) Black else MediumGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: CoachMessage) {
    val isUser = message.sender == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp))
                    .background(Color(0xFF2C2C2E))
                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = message.text,
                    color = White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp))
                    .background(CardGray)
                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp))
                    .padding(14.dp)
            ) {
                MarkdownText(text = message.text)
            }
        }
    }
}

@Composable
fun ThinkingBubble() {
    var dots by remember { mutableStateOf(".") }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dots = when (dots) {
                "." -> ".."
                ".." -> "..."
                else -> "."
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp))
                .background(CardGray)
                .border(1.dp, BorderGray, RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Thinking$dots",
                color = MediumGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MarkdownText(text: String) {
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("###")) {
                val headerText = trimmed.removePrefix("###").trim()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = headerText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
            } else if (trimmed.startsWith("-")) {
                val bulletText = trimmed.removePrefix("-").trim()
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = 8.dp)) {
                    Text("• ", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = parseBoldText(bulletText),
                        fontSize = 12.sp,
                        color = LightGray,
                        lineHeight = 16.sp
                    )
                }
            } else if (trimmed.isNotEmpty()) {
                Text(
                    text = parseBoldText(trimmed),
                    fontSize = 12.sp,
                    color = LightGray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

fun parseBoldText(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val startBold = text.indexOf("**", cursor)
            if (startBold == -1) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, startBold))
            val endBold = text.indexOf("**", startBold + 2)
            if (endBold == -1) {
                append(text.substring(startBold))
                break
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = White)) {
                append(text.substring(startBold + 2, endBold))
            }
            cursor = endBold + 2
        }
    }
}
