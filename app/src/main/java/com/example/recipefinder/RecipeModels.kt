package com.example.recipefinder

// Model for the API response
data class RecipeResponse(
    val meals: List<Recipe>?
)

// Model for a single recipe
data class Recipe(
    val idMeal: String,
    val strMeal: String,
    val strMealThumb: String,
    val strInstructions: String,
    val strTags: String?,
    val ingredients: List<String>,
)

data class IngredientResponse(
    val meals: List<Ingredient>
)

data class Ingredient(
    val strIngredient: String
)

data class CategoryResponse(
    val categories: List<Category>
)

data class Category(
    val idCategory: String,
    val strCategory: String,
    val strCategoryThumb: String,
    val strCategoryDescription: String
)