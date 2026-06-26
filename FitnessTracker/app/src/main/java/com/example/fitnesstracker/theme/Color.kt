package com.example.fitnesstracker.theme

import androidx.compose.ui.graphics.Color

// --- Neutral Scale ---
val Black = Color(0xFF000000)
val DarkGray = Color(0xFF121212)
val CardGray = Color(0xFF1C1C1E)
val BorderGray = Color(0xFF2C2C2E)
val MediumGray = Color(0xFF8E8E93)
val LightGray = Color(0xFFE5E5EA)
val White = Color(0xFFFFFFFF)
val MutedDarkGray = Color(0xFF48484A)
val SurfaceCard = Color(0xFF1A1A1A)       // Slightly lighter than CardGray for nested cards

// --- Brand ---
val StravaOrange = Color(0xFFFC4C02)
val DarkBackground = Color(0xFF000000)    // alias for background — used in tab selected text

// --- Semantic Accent Colours (replace hard-coded 0xFFXXXXXX literals) ---
val GreenAccent = Color(0xFF00E676)        // protein/macro bars, positive indicators
val GreenLight = Color(0xFF4CAF50)         // success states, completion markers
val OrangeAccent = Color(0xFFFF9800)       // carbs macro, warning states, streak banner
val BlueAccent = Color(0xFF2196F3)         // fat macro, information
val PurpleAccent = Color(0xFF9C27B0)       // fibre, secondary metrics
val PinkAccent = Color(0xFFE91E63)         // calories burned, energy metrics
val YellowAccent = Color(0xFFFFEB3B)       // personal best, highlights
val RedAccent = Color(0xFFF44336)          // delete, danger states
val TealAccent = Color(0xFF009688)         // water tracking

// --- Surface Overlays ---
val OverlayLight = Color(0x1AFFFFFF)       // 10% white overlay
val OverlayDark = Color(0x33000000)        // 20% black overlay
