package com.example.recipefinder

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call

// Define the API service interface
interface RecipeApiService {
    // Update endpoint to filter by ingredient
    @GET("filter.php")
    fun getRecipesByIngredient(@Query("i") ingredient: String): Call<RecipeResponse>
    @GET("list.php?i=list")
    fun getAllIngredients(): Call<IngredientResponse>
    @GET("categories.php")
    fun getMealCategories(): Call<CategoryResponse>
}

// Set up the Retrofit instance
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
