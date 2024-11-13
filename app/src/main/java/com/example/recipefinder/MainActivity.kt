package com.example.recipefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var ingredients by remember { mutableStateOf<List<String>>(emptyList()) }

            LaunchedEffect(Unit) {
                fetchAllIngredients { result ->
                    ingredients = result ?: emptyList()
                }
            }

            MainScreen(ingredients = ingredients)
        }
    }

    private fun fetchAllIngredients(onResult: (List<String>?) -> Unit) {
        val call = ApiClient.retrofitService.getAllIngredients()
        call.enqueue(object : Callback<IngredientResponse> {
            override fun onResponse(call: Call<IngredientResponse>, response: Response<IngredientResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    onResult(body?.meals?.map { it.strIngredient })
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<IngredientResponse>, t: Throwable) {
                onResult(null)
            }
        })
    }
}

@Composable
fun MainScreen(ingredients: List<String>) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(painter = painterResource(id = R.drawable.search), contentDescription = "Search") },
                    label = { Text("Search") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(painter = painterResource(id = R.drawable.sort), contentDescription = "Categories") },
                    label = { Text("Categories") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> RecipeSearch(modifier = Modifier.padding(innerPadding), ingredients = ingredients)
            1 -> ShowAllScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun ShowAllScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Show all content goes here")
    }
}

@Composable
fun RecipeSearch(modifier: Modifier = Modifier, ingredients: List<String> = emptyList()) {
    var ingredient by remember { mutableStateOf(TextFieldValue("")) }
    var recipes by remember { mutableStateOf<List<Recipe>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
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
                        }
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        val suggestions = ingredients.filter { it.contains(ingredient.text, ignoreCase = true) }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    .clip(RoundedCornerShape(50)),
                contentScale = ContentScale.Crop
            )

            // Display the recipe name
            Column {
                Text(
                    text = recipe.strMeal,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

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