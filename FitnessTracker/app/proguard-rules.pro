# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Room entities and relation models (safe default for @Embedded/@Relation)
-keep class com.example.fitnesstracker.data.** { *; }

# --- Kotlin coroutines ---
-dontwarn kotlinx.coroutines.**

# --- kotlinx.serialization (used by Navigation 3 keys) ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.fitnesstracker.**$$serializer { *; }
-keepclassmembers class com.example.fitnesstracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.fitnesstracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Google Generative AI SDK (Ktor under the hood) ---
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# --- AndroidX Security (Tink) ---
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# --- ML Kit barcode scanning ---
-dontwarn com.google.mlkit.**

# --- Health Connect ---
-keep class androidx.health.connect.client.** { *; }
-dontwarn androidx.health.connect.**
