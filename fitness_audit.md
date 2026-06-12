# 🏋️ FitnessTracker — Full App Audit

> Covers: Bugs · UI/UX Issues · Code Quality · Security · Performance · New Feature Recommendations

---

## 🐛 Bugs

### 1. `ProfileScreen` — Early `return` inside a Composable (Critical)
**File:** [ProfileScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/ProfileScreen.kt#L47)

```kotlin
val profile = userProfileState ?: return  // ❌ NEVER use bare return in a Composable
```
Early returns mid-composable violate Compose's rules and can cause **runtime crashes, blank screens, or subtle state corruption**. The correct pattern is to conditionally render a loading/empty state:

```kotlin
if (userProfileState == null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
    return
}
val profile = userProfileState
```

---

### 2. `OnboardingScreen` — Unchecked `toDouble()` crash on final step
**File:** [OnboardingScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/OnboardingScreen.kt#L221-L228)

```kotlin
val savedWeight = if (preferredUnits == "Imperial") {
    UnitConverter.lbsToKg(weightStr.toDouble())  // ❌ throws NumberFormatException if blank
} else {
    weightStr.toDouble()  // ❌ same issue
}
```
Step 3 validation only runs *as a guard for the Next button*, but if the user somehow reaches step 7 via back-navigation and re-presses "Get Started", the unchecked `.toDouble()` will **crash the app**. Use `toDoubleOrNull() ?: 70.0` as a safe fallback.

---

### 3. `TrackScreen` — Activity clicking navigates to `ActivitySummary` instead of `ActivityDetail`
**File:** [TrackScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/TrackScreen.kt#L265-L267)

```kotlin
.clickable { onActivitySaved(activity.id) }  // ❌ wrong callback — this goes to summary
```
Tapping a recent activity in the pre-tracking view calls `onActivitySaved`, which triggers the **post-workout summary flow**, not the detail view. It should call a separate `onViewActivityDetail` callback.

---

### 4. `TrackingService` — Static `MutableStateFlow` inside companion object (Memory / Process leak risk)
**File:** [TrackingService.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/service/TrackingService.kt#L78-L79)

```kotlin
private val _trackingState = MutableStateFlow(TrackingState())
```
A `StateFlow` as a static field in the companion means **the old state persists after process restart**. On a system-initiated service restart (STICKY), `isTracking=true` may cause ghost tracking with no active session. The service partially handles this, but the state should be explicitly reset in `onCreate()` if no active session is detected.

---

### 5. `NutritionViewModel` — `foodRecommendations` flow recalculates on every food-log emission
**File:** [NutritionViewModel.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/NutritionViewModel.kt#L120)

```kotlin
val foodRecommendations: Flow<List<RecommendedFoodItem>> = combine(
    nutritionDao.getAllFoodItems(),  // fires on EVERY food DB change
    ...
```
`getAllFoodItems()` is a full-table scan that re-runs every time the user logs any food, emitting a new `combine()` update and re-scoring all items. Should use `distinctUntilChanged()` on the foods list.

---

### 6. `SecureStore` — `MasterKeys` is deprecated (Android 13+)
**File:** [SecureStore.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/util/SecureStore.kt#L27)

```kotlin
val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
```
`MasterKeys` is deprecated in favour of `MasterKey.Builder`. While it works today, it may emit deprecation warnings and could break on future API levels. Migrate to:
```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()
```

---

### 7. `HistoryScreen` — `GymLogsSection` uses `Box(modifier = Modifier.weight(1f))` outside a `RowScope/ColumnScope`
**File:** [HistoryScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/HistoryScreen.kt#L267)

```kotlin
Box(modifier = Modifier.weight(1f)) { ... }  // ❌ weight() only valid in Row/Column
```
The outer container at this point is `Column`, so `weight(1f)` inside it is valid — but **only if** it's a direct child. If the composable hierarchy ever changes, this silently breaks layout. It should be `Modifier.fillMaxHeight()` with a bounded parent, and the `Box` should be replaced with a `Column` + `LazyColumn` inside.

---

### 8. `Notification` — always shows distance in km regardless of user preference
**File:** [TrackingService.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/service/TrackingService.kt#L146)

```kotlin
val distFormatted = String.format("%.2f km", state.distanceMeters / 1000.0)  // ❌ always km
```
Users with **Imperial** preference will see km in the notification, contradicting what they see in the app.

---

### 9. `OnboardingScreen` — gender list missing "Other"
**File:** [OnboardingScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/OnboardingScreen.kt#L330)

```kotlin
val genders = listOf("Male", "Female")  // ❌ ProfileScreen has "Other" but Onboarding doesn't
```
`ProfileScreen` has three gender options (Male, Female, Other) but `OnboardingScreen` only has two. Inconsistency leads to state drift if the user selects "Other" post-onboarding.

---

## 🎨 UI / UX Issues

### 10. `ProfileScreen` `saveSuccess` banner never auto-dismisses
The "✓ Profile saved" text appears permanently under the avatar after saving. It should auto-clear after ~3 seconds using `LaunchedEffect`:
```kotlin
LaunchedEffect(saveSuccess) {
    if (saveSuccess) { delay(3000); saveSuccess = false }
}
```

### 11. `TrackScreen` — Settings dialog has no scroll on small screens
The profile settings `Dialog` in `TrackScreen` uses `Column.verticalScroll()` inside a `Dialog`, which works — but the dialog has no maximum height constraint. On small phones, the keyboard can push content entirely off screen. Apply `Modifier.heightIn(max = 500.dp)` on the `Surface`.

### 12. `DashboardScreen` — FAB overlaps last exercise card
The `FloatingActionButton` uses `padding(bottom = 16.dp)` but the `LazyColumn` uses `contentPadding = PaddingValues(bottom = 80.dp)`. However, the FAB is positioned *inside* the weight Box, so on devices with a tall navigation bar the FAB can still cover the last item.

### 13. `OnboardingScreen` — Step 2 age field allows non-numeric input momentarily
`onAgeChange = { age = it }` — there's no filter. A user can type letters before the validation kicks in. The field should use `{ age = it.filter { c -> c.isDigit() } }` (same fix already applied on `TrackScreen` settings dialog, L647).

### 14. Three-way tab in `HistoryScreen` uses inconsistent selected indicator colours
- "Workouts" tab → selected background: `White` (Black text)
- "Activities" tab → selected background: `StravaOrange` (DarkBackground text)  
- "Analysis" tab → selected background: `White` (Black text)

Only "Activities" uses orange. For visual consistency all three should use the same colour or explicitly differentiate by purpose.

### 15. `ActivityMapView` WebView settings — `allowFileAccess = true` is overly permissive
**File:** [TrackScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/TrackScreen.kt#L868)

`allowFileAccess` and `domStorageEnabled` on a WebView that loads local HTML could be tightened. The WebView only needs to load `file:///android_asset/map.html`, which is safe — but setting `allowFileAccess = false` and `settings.allowFileAccessFromFileURLs = false` (API < 30) would reduce attack surface.

---

## 🔒 Security Issues

### 16. Mapbox Token stored in plain Room DB
**File:** [WorkoutDatabase.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/data/WorkoutDatabase.kt#L172)

```kotlin
val mapboxToken: String = "",
```
The Gemini API key is migrated to `SecureStore`, but the Mapbox token **is not** — it remains in the Room `user_profile` table, which is a plain SQLite database file on disk. The comment in `NutritionViewModel` acknowledges this ("client-side token"), but it should still be moved to `SecureStore` for consistency and to prevent exposure via ADB backup.

### 17. `BackupManager` — backup file contains unencrypted SQLite (sensitive user data)
**File:** [BackupManager.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/util/BackupManager.kt)

The backup feature copies the raw `.db` file directly (likely via file streams). This exports all personal data — weight, food logs, activity history — in an unencrypted SQLite file to wherever the user saves it (Downloads, Google Drive, etc.). Consider encrypting the backup with a user-chosen PIN or using Android's `EncryptedFile`.

### 18. `WebView` JavaScript bridge not sanitized
**File:** [TrackScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/TrackScreen.kt#L882)

```kotlin
evaluateJavascript("initMap('$mapboxToken', $startLat, $startLon)", null)
```
The `mapboxToken` string is interpolated directly into a JavaScript call. If the token somehow contains a single quote `'`, this would break (benign) but could also be an injection vector if the token source ever changed. Use proper JSON encoding: `JSONObject.quote(mapboxToken)`.

---

## ⚡ Performance Issues

### 19. `NutritionViewModel.weeklyFoodLogs` and `weeklyWaterLogs` — in-memory filtering of ALL logs
**File:** [NutritionViewModel.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/NutritionViewModel.kt#L74-L93)

Both flows call `getAllFoodLogs()` (full table scan) and then filter in-memory. As the user's history grows, this gets increasingly expensive. Move the date filter into a SQL query:
```sql
SELECT * FROM food_logs WHERE date >= :since ORDER BY date DESC
```

### 20. `HistoryScreen` PR calculation — O(n²) complexity
**File:** [HistoryScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/HistoryScreen.kt#L702-L706)

For each session row in the `LazyColumn`, it calls:
```kotlin
allSessionsForExercise.filter { ... }.flatMap { it.sets }.maxOfOrNull { it.weight }
```
This iterates over all sessions × all sets for every rendered row. For users with years of data, this will cause visible jank. Cache the PR map in the `WorkoutViewModel` using `getAllSessionsWithSets()` and compute it once.

### 21. `ActivityMapView` — `setRoute` called on EVERY recomposition during tracking
**File:** [TrackScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/TrackScreen.kt#L892-L902)

The `update` block of `AndroidView` evaluates `setRoute('$pointsJson', ...)` which **serializes the entire route array** and sends it via JS bridge on every location update. For long activities (e.g. a 2-hour run), the pointsJson string grows to hundreds of kilobytes. This should be incremental — only send new points, not the full route every time.

### 22. `NutritionViewModel.serializeLogsToJson` — custom pipe/semicolon format is fragile
**File:** [NutritionViewModel.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/NutritionViewModel.kt#L737)

A food name with a `|` or `;` character would corrupt the serialization silently. Use `Gson` (already likely available via `generativeai` transitive deps) or `kotlinx.serialization`.

---

## 🆕 Recommended New Features (User Growth & Engagement)

### 🏆 Feature 1: **Personal Records (PR) History Dashboard**
A dedicated "Records" screen showing all-time PRs per exercise with a timeline chart. Users love seeing their strongest ever lift. Highly shareable moments.

### 📊 Feature 2: **Calorie Balance / Net Calorie Card on Dashboard**
Show **Calories In − Calories Burned** for today directly on the Dashboard home screen. This is the #1 feature users expect in a fitness tracker. Currently, calorie data lives in separate Nutrition and Track tabs with no unified view.

### 🔔 Feature 3: **Workout Reminders per Day**
The `WorkoutReminderWorker` exists but the UI to schedule it per day/time is missing. Add a notification schedule setting in Profile so users can set "Remind me at 6 AM on Mon/Wed/Fri."

### 📈 Feature 4: **Progress Photos**
Allow users to take/import a photo tagged with date, weight, and body measurements. Monthly comparison grid. Very high engagement driver for fitness apps.

### 🤝 Feature 5: **Social Sharing — Activity Share Card**
After an activity finishes, generate a shareable image card (via `Canvas`/`Bitmap`) showing route map, stats, and app branding. Instagram/WhatsApp friendly. This is the #1 organic user acquisition channel for Strava.

### 🌊 Feature 6: **Water Reminder Notifications**
A periodic notification (e.g. every 2 hours) reminding the user to drink water if they haven't logged their target yet. Use `WorkManager` with a repeating interval.

### 🧘 Feature 7: **Rest Day / Recovery Suggestions**
After `N` consecutive workout days, show an in-app suggestion to take a rest day or do light cardio. Uses existing streak data.

### 📅 Feature 8: **Workout Templates (Quick Start)**
Let users save a full day's planned workout as a template (e.g., "Push Day A", "Leg Day"). One-tap to apply the template to any day. Currently users have to add exercises one by one every time.

### ⌚ Feature 9: **Wear OS / Watch Face**
Display today's step count, calories, and water from a Wear OS tile. Extremely high engagement for fitness apps — users check the watch multiple times daily.

### 🍽️ Feature 10: **Meal Prep Planner**
A weekly meal plan view where the AI coach generates a 7-day meal plan based on the user's TDEE goal, preferences, and available foods. Display as a grid calendar.

### 📍 Feature 11: **Running Routes — Saved & Shared**
Allow users to name and save GPS routes. Show route thumbnails in a "Saved Routes" tab. Tap to start a run along a saved route with on-route guidance.

### 🔄 Feature 12: **Google Fit / Apple Health Sync (Two-way)**
Currently only push-to-Health-Connect is supported. Add pull-to-sync: import steps, active calories from Google Fit/Health Connect to show unified "today" stats.

---

## 🔧 Code Quality Issues

| # | Issue | File | Priority |
|---|-------|------|----------|
| 23 | `TrackScreen` defines `StravaOrange`, `DarkBackground`, etc. as top-level vals — should be in theme | [TrackScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/TrackScreen.kt#L49-L54) | Medium |
| 24 | `Navigation.kt` creates all 3 ViewModels at the top-level — they should be scoped via Hilt or a DI graph | [Navigation.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/Navigation.kt#L56-L65) | Medium |
| 25 | `NutritionViewModel` is 1200+ lines; should be split into `WorkoutLogVM`, `NutritionVM`, `CoachVM` | [NutritionViewModel.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/NutritionViewModel.kt) | Medium |
| 26 | `WorkoutDatabase` version 6 but only 1 migration declared (5→6). Migrations 1–5 are missing — fresh installs use `fallbackToDestructiveMigration` implicitly? | [WorkoutDatabase.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/data/WorkoutDatabase.kt#L414) | High |
| 27 | `StepName`, `StepAgeGender`, etc. in `OnboardingScreen` are all `public` top-level functions — should be `private` | [OnboardingScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/OnboardingScreen.kt#L269) | Low |
| 28 | `formatDuration()` and `formatPace()` are defined globally in `TrackScreen.kt` but used in other screens — move to a shared `FormatUtils.kt` | [TrackScreen.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/ui/screens/TrackScreen.kt#L909) | Low |
| 29 | Duplicate `getUserProfile()` / `getUserProfileSync()` in both `ActivityDao` and `WorkoutDao` — single source of truth should own the user profile | [WorkoutDatabase.kt](file:///c:/Users/harde/OneDrive/Desktop/fitness-master/FitnessTracker/app/src/main/java/com/example/fitnesstracker/data/WorkoutDatabase.kt#L116) | Medium |
| 30 | No unit tests for `calculateGoal()`, `calculateCalories()`, or any DAO query — high-risk business logic without coverage | — | High |

---

## 📋 Priority Fix Summary

| Priority | Issues | Impact |
|----------|--------|--------|
| 🔴 Critical (fix now) | #1 (bare return), #2 (crash), #3 (wrong nav), #26 (missing migrations) | App crashes / data loss |
| 🟠 High | #16 (mapbox token), #19 (perf), #20 (perf), #21 (perf) | User experience degradation |
| 🟡 Medium | #4, #8, #9, #10, #11, #13, #14, #17, #18, #22 | Inconsistency / minor bugs |
| 🟢 Enhancement | #23–30 (code quality), all new features | Long-term maintainability & growth |
