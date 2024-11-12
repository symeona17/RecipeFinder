package com.example.recipefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecipeSearch()
        }
    }
}

@Composable
fun RecipeSearch(modifier: Modifier = Modifier) {
    var ingredient by remember { mutableStateOf("") }
    var recipes by remember { mutableStateOf<List<Recipe>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Input field for the ingredient
        OutlinedTextField(
            value = ingredient,
            onValueChange = { ingredient = it },
            label = { Text("Enter an ingredient") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search button
        Button(onClick = {
            searchRecipes(ingredient) { result, error ->
                recipes = result
                errorMessage = error
            }
        }) {
            Text("Search Recipes")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display error message if exists
        if (errorMessage != null) {
            Text(text = errorMessage ?: "", color = Color.Red)
        } else {
            // Display list of recipes if available
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                recipes?.let { recipeList ->
                    items(recipeList) { recipe ->
                        RecipeCard(recipe = recipe)
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCard(recipe: Recipe) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Corrected elevation usage
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Load the image asynchronously using Coil
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(recipe.strMealThumb)
                    .crossfade(true)
                    .build(),
                contentDescription = recipe.strMeal,
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 16.dp)
                    .clip(RoundedCornerShape(50)), // Corrected clip modifier
                contentScale = ContentScale.Crop // Crop image into circle
            )

            // Display the recipe name
            Column {
                Text(
                    text = recipe.strMeal,
                    style = MaterialTheme.typography.bodyLarge, // Corrected typography usage for Material3
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

// Function to call the API and handle the response
private fun searchRecipes(
    ingredient: String,
    onResult: (List<Recipe>?, String?) -> Unit
) {
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RecipeSearch()
}
