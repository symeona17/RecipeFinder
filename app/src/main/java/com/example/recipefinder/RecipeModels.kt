package com.example.recipefinder

// Model for the API response
data class RecipeResponse(
    val meals: List<Recipe>?
)

// Model for a single recipe
data class Recipe(
    val idMeal: String,
    val strMeal: String,
    val strInstructions: String?,
    val strMealThumb: String
)
