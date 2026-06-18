package com.example.fitnesstracker.util

import com.example.fitnesstracker.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import org.json.JSONArray
import java.util.Locale

object GeminiNutritionParser {

    fun isAvailable(): Boolean {
        return BuildConfig.GEMINI_API_KEY.isNotBlank()
    }

    suspend fun parseFoodInput(input: String): List<ParsedFoodItem> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is not configured. Add GEMINI_API_KEY to local.properties.")
        }

        val systemInstruction = """
            You are an expert nutritionist and data parser.
            You take natural language descriptions of foods/meals eaten, and output a JSON array of the individual items with their nutrition values.
            Use standard nutritional databases to estimate the values if they are not explicitly specified.
            Provide macros and calories PER ONE UNIT of the serving unit, and output the quantity.
            
            Each object in the JSON array MUST have exactly these keys:
            - "foodName": String (name of the food)
            - "calories": Double (calories per serving unit)
            - "protein": Double (grams of protein per serving unit)
            - "carbs": Double (grams of carbs per serving unit)
            - "fat": Double (grams of fat per serving unit)
            - "fiber": Double (grams of fiber per serving unit)
            - "quantity": Double (number of serving units)
            - "servingUnit": String (e.g., "g", "ml", "pcs", "slice", "cup", "bowl")
            
            Do NOT wrap the output in markdown code blocks. Output ONLY raw, valid JSON.
            Example input: "2 boiled eggs and a cup of milk"
            Example output:
            [
              {
                "foodName": "Boiled Egg",
                "calories": 78.0,
                "protein": 6.3,
                "carbs": 0.6,
                "fat": 5.3,
                "fiber": 0.0,
                "quantity": 2.0,
                "servingUnit": "pcs"
              },
              {
                "foodName": "Whole Milk",
                "calories": 149.0,
                "protein": 7.7,
                "carbs": 11.7,
                "fat": 8.0,
                "fiber": 0.0,
                "quantity": 1.0,
                "servingUnit": "cup"
              }
            ]
        """.trimIndent()

        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )

        val response = model.generateContent(
            content {
                text(systemInstruction)
                text("Input: $input")
            }
        )

        val responseText = response.text ?: throw Exception("Empty response from AI")
        
        // Strip out any markdown code blocks if the model ignored instructions
        val cleanJson = responseText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val jsonArray = JSONArray(cleanJson)
        val items = mutableListOf<ParsedFoodItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            items.add(
                ParsedFoodItem(
                    foodName = obj.getString("foodName"),
                    calories = obj.getDouble("calories"),
                    protein = obj.getDouble("protein"),
                    carbs = obj.getDouble("carbs"),
                    fat = obj.getDouble("fat"),
                    fiber = obj.optDouble("fiber", 0.0),
                    quantity = obj.getDouble("quantity"),
                    servingUnit = obj.getString("servingUnit")
                )
            )
        }
        return items
    }
}

data class ParsedFoodItem(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val quantity: Double = 1.0,
    val servingUnit: String = "g"
)
