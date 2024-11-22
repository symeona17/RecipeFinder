package com.example.recipefinder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

//Main screen for searching recipes by ingredient
@Composable
fun RecipeSearch(
    modifier: Modifier = Modifier,
    ingredients: List<String> = emptyList(),
    onRecipeClick: (String) -> Unit
) {
    var ingredient by remember { mutableStateOf(TextFieldValue("")) }
    var recipes by remember { mutableStateOf<List<Recipe>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    keyboardController?.hide()
                })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = ingredient,
                onValueChange = { newIngredient ->
                    if (ingredient.text != newIngredient.text) {
                        ingredient = newIngredient.copy(
                            text = newIngredient.text,
                            selection = TextRange(newIngredient.text.length)
                        )
                        recipes = null
                        suggestions = ingredients.filter { it.contains(newIngredient.text, ignoreCase = true) }
                    }
                },
                label = { Text("Enter an ingredient") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (ingredient.text.isNotEmpty()) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Clear text",
                            modifier = Modifier.clickable {
                                ingredient = TextFieldValue("")
                                recipes = null
                                suggestions = emptyList()
                            }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (ingredient.text.isNotEmpty()) {
                LazyColumn {
                    items(suggestions) { suggestion ->
                        Text(
                            text = suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ingredient = TextFieldValue(
                                        text = suggestion,
                                        selection = TextRange(suggestion.length, suggestion.length)
                                    )
                                    keyboardController?.hide()
                                    suggestions = emptyList()
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }

            LaunchedEffect(ingredient.text) {
                val exactMatch = ingredients.any { it.equals(ingredient.text, ignoreCase = true) }
                if (exactMatch) {
                    isLoading = true
                    searchRecipes(ingredient.text) { result, error ->
                        recipes = result
                        errorMessage = error
                        isLoading = false
                    }
                }
            }

            if (isLoading) {
                Text(text = "Loading...", color = Color.Gray)
            } else if (errorMessage != null) {
                Text(text = errorMessage ?: "", color = Color.Red)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    recipes?.let { recipeList ->
                        items(recipeList) { recipe ->
                            RecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe.idMeal) })
                        }
                    }
                }
            }
        }
    }
}

//Function to search for recipes by ingredient
private fun searchRecipes(ingredient: String, onResult: (List<Recipe>?, String?) -> Unit) {
    val call = ApiClient.retrofitService.getRecipesByIngredient(ingredient)
    call.enqueue(object : Callback<RecipeResponse> {
        override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.meals != null) {
                    onResult(body.meals, null)
                } else {
                    onResult(null, "No meals found.")
                }
            } else {
                onResult(null, "Failed to fetch recipes: ${response.message()}")
            }
        }

        override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {
            onResult(null, "Network error: ${t.message}")
        }
    })
}