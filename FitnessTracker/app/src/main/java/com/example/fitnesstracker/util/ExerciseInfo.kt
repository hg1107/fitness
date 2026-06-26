package com.example.fitnesstracker.util

data class ExerciseDetails(
    val muscles: String,
    val cues: List<String>
)

/**
 * Offline knowledge base of target muscles and form cues for common exercises.
 * Lookup is keyword-based, so "Incline Dumbbell Bench Press" matches "bench press".
 */
object ExerciseInfo {

    private val knownExercises = linkedMapOf(
        "bench press" to ExerciseDetails(
            "Chest, Triceps, Front Delts",
            listOf("Retract and depress your shoulder blades", "Keep feet planted and maintain a slight arch", "Lower the bar to mid-chest with control", "Drive the bar up and slightly back toward your face")
        ),
        "incline press" to ExerciseDetails(
            "Upper Chest, Front Delts, Triceps",
            listOf("Set the bench to 30-45 degrees", "Touch the bar just below your collarbone", "Keep elbows at ~45 degrees from your torso")
        ),
        "squat" to ExerciseDetails(
            "Quads, Glutes, Hamstrings, Core",
            listOf("Brace your core before descending", "Push knees out in line with your toes", "Hit at least parallel depth", "Drive through your whole foot, not just toes")
        ),
        "deadlift" to ExerciseDetails(
            "Hamstrings, Glutes, Lower Back, Traps",
            listOf("Keep the bar close to your shins", "Set your lats and keep a neutral spine", "Push the floor away rather than pulling with your back", "Lock out by squeezing glutes, not leaning back")
        ),
        "romanian deadlift" to ExerciseDetails(
            "Hamstrings, Glutes, Lower Back",
            listOf("Hinge at the hips with soft knees", "Lower until you feel a deep hamstring stretch", "Keep the bar dragging along your thighs")
        ),
        "overhead press" to ExerciseDetails(
            "Shoulders, Triceps, Upper Chest",
            listOf("Squeeze glutes to avoid arching your lower back", "Press the bar in a straight line, moving your head back", "Finish with biceps by your ears")
        ),
        "shoulder press" to ExerciseDetails(
            "Shoulders, Triceps",
            listOf("Keep your core braced and ribs down", "Lower to ear level with control", "Avoid flaring elbows excessively")
        ),
        "pull up" to ExerciseDetails(
            "Lats, Biceps, Upper Back",
            listOf("Start from a dead hang with engaged shoulders", "Pull your chest to the bar, elbows down and back", "Control the descent fully")
        ),
        "pull-up" to ExerciseDetails(
            "Lats, Biceps, Upper Back",
            listOf("Start from a dead hang with engaged shoulders", "Pull your chest to the bar, elbows down and back", "Control the descent fully")
        ),
        "lat pulldown" to ExerciseDetails(
            "Lats, Biceps, Rear Delts",
            listOf("Pull the bar to your upper chest", "Drive elbows down and into your sides", "Avoid leaning back to cheat the weight")
        ),
        "barbell row" to ExerciseDetails(
            "Lats, Rhomboids, Rear Delts, Biceps",
            listOf("Hinge to roughly 45 degrees or lower", "Pull the bar to your lower ribs", "Squeeze shoulder blades at the top")
        ),
        "row" to ExerciseDetails(
            "Upper Back, Lats, Biceps",
            listOf("Lead the pull with your elbows", "Keep your torso stable, no jerking", "Pause briefly at peak contraction")
        ),
        "bicep curl" to ExerciseDetails(
            "Biceps, Forearms",
            listOf("Keep elbows pinned to your sides", "Avoid swinging from the hips", "Lower slowly for 2-3 seconds")
        ),
        "curl" to ExerciseDetails(
            "Biceps, Forearms",
            listOf("Keep elbows pinned to your sides", "Full extension at the bottom", "Squeeze hard at the top")
        ),
        "tricep" to ExerciseDetails(
            "Triceps",
            listOf("Keep elbows tucked and stationary", "Fully lock out each rep", "Use a weight you can control through the full range")
        ),
        "dip" to ExerciseDetails(
            "Chest, Triceps, Front Delts",
            listOf("Lean forward to bias chest, stay upright for triceps", "Descend until upper arms are parallel", "Avoid shrugging your shoulders at lockout")
        ),
        "leg press" to ExerciseDetails(
            "Quads, Glutes, Hamstrings",
            listOf("Lower under control to ~90 degrees of knee bend", "Never lock knees harshly at the top", "Keep your lower back pressed against the pad")
        ),
        "lunge" to ExerciseDetails(
            "Quads, Glutes, Hamstrings, Core",
            listOf("Take a long enough step to keep your front knee over your ankle", "Lower the back knee toward the floor", "Drive up through the front heel")
        ),
        "leg curl" to ExerciseDetails(
            "Hamstrings",
            listOf("Control the negative for 2-3 seconds", "Avoid lifting your hips off the pad", "Squeeze fully at peak flexion")
        ),
        "leg extension" to ExerciseDetails(
            "Quads",
            listOf("Pause briefly at full extension", "Lower with control, do not let the stack slam", "Align knees with the machine pivot")
        ),
        "calf raise" to ExerciseDetails(
            "Calves",
            listOf("Full stretch at the bottom, full squeeze at the top", "Pause 1-2 seconds at both ends", "Avoid bouncing")
        ),
        "lateral raise" to ExerciseDetails(
            "Side Delts",
            listOf("Lead with your elbows, slight bend in the arms", "Raise to shoulder height only", "Lower slowly, no swinging")
        ),
        "face pull" to ExerciseDetails(
            "Rear Delts, Rotator Cuff, Traps",
            listOf("Pull the rope toward your forehead", "Externally rotate so knuckles face the ceiling", "Use light weight and strict form")
        ),
        "hip thrust" to ExerciseDetails(
            "Glutes, Hamstrings",
            listOf("Tuck your chin and keep ribs down", "Drive through your heels to full hip extension", "Squeeze glutes hard for 1-2 seconds at the top")
        ),
        "plank" to ExerciseDetails(
            "Core, Shoulders",
            listOf("Keep a straight line from head to heels", "Squeeze glutes and brace your abs", "Do not let hips sag or pike")
        )
    )

    fun lookup(exerciseName: String): ExerciseDetails {
        val lower = exerciseName.lowercase()
        knownExercises.forEach { (keyword, details) ->
            if (lower.contains(keyword)) return details
        }
        return ExerciseDetails(
            muscles = "General / Full Body",
            cues = listOf(
                "Control the eccentric (lowering) phase",
                "Use a full, pain-free range of motion",
                "Brace your core and keep a stable base",
                "Leave 1-2 reps in reserve on most working sets"
            )
        )
    }
}
