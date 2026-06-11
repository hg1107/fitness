package com.example.fitnesstracker.util

import com.example.fitnesstracker.data.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal client for the free Open Food Facts product API.
 * Looks up a scanned barcode and maps the result to a local [FoodItem]
 * with nutrition values per 100 g.
 */
object OpenFoodFactsClient {

    suspend fun fetchProduct(barcode: String): FoodItem? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://world.openfoodfacts.org/api/v2/product/$barcode.json" +
                    "?fields=product_name,nutriments"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "FitnessTracker-Android/1.0")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val product = json.optJSONObject("product") ?: return@withContext null
            val name = product.optString("product_name", "").trim()
            if (name.isEmpty()) return@withContext null
            val n = product.optJSONObject("nutriments") ?: return@withContext null
            val kcal = n.optDouble("energy-kcal_100g", -1.0)
            if (kcal < 0) return@withContext null

            FoodItem(
                name = name,
                calories = kcal,
                protein = n.optDouble("proteins_100g", 0.0),
                carbs = n.optDouble("carbohydrates_100g", 0.0),
                fat = n.optDouble("fat_100g", 0.0),
                fiber = n.optDouble("fiber_100g", 0.0),
                servingSizeG = 100.0,
                servingUnit = "g",
                isVegetarian = false,
                isVegan = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
