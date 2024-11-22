package com.example.recipefinder

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call

// Define the API service interface
interface RecipeApiService {
    // Load recipes based on ingredient
    @GET("filter.php")
    fun getRecipesByIngredient(@Query("i") ingredient: String): Call<RecipeResponse>
    // Load all ingredients (for search suggestions)
    @GET("list.php?i=list")
    fun getAllIngredients(): Call<IngredientResponse>
    // Load all meal categories (for recipe categories)
    @GET("categories.php")
    fun getMealCategories(): Call<CategoryResponse>
    // Load recipes based on category
    @GET("filter.php")
    fun getRecipesByCategory(@Query("c") category: String): Call<RecipeResponse>
    //Load recipe details based on meal ID
    @GET("lookup.php")
    fun getRecipeDetails(@Query("i") mealId: String): Call<RecipeResponse>
}

// Connecting to the API
object ApiClient {
    private const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"

    val retrofitService: RecipeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Converts JSON to Kotlin objects
            .build()
            .create(RecipeApiService::class.java)
    }
}
