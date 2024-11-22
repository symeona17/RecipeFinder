package com.example.recipefinder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

//Main screen for displaying categories
@Composable
fun CategoriesScreen(
    modifier: Modifier = Modifier,
    categories: List<Category>?,
    onCategoryClick: (Category) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (categories == null) {
            Text("Loading categories...", color = Color.Gray)
        } else {
            LazyColumn {
                items(categories) { category ->
                    CategoryCard(category = category, onClick = { onCategoryClick(category) })
                }
            }
        }
    }
}

//The code to display the loaded category cards to be clicked on & navigated to recipes belonging in that category
@Composable
fun CategoryCard(category: Category, onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(category.strCategoryThumb)
                    .crossfade(true)
                    .build(),
                contentDescription = category.strCategory,
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 16.dp)
                    .clip(RoundedCornerShape(50)),
                contentScale = ContentScale.Crop
            )
            Column {
                Text(
                    text = category.strCategory,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

//The code to display the recipes belonging to a category
@Composable
fun CategoryRecipesScreen(
    categoryName: String,
    onBackClick: () -> Unit,
    onRecipeClick: (String) -> Unit
) {
    var recipes by remember { mutableStateOf<List<Recipe>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(categoryName) {
        fetchRecipesByCategory(categoryName) { result, error ->
            recipes = result
            errorMessage = error
            isLoading = false
        }
    }

    Column {
        Icon(
            painter = painterResource(id = R.drawable.arrow_back),
            contentDescription = "Back",
            modifier = Modifier
                .padding(16.dp)
                .clickable { onBackClick() }
        )
        Text(
            text = "Recipes for $categoryName",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        if (isLoading) {
            Text(text = "Loading...", color = Color.Gray, modifier = Modifier.padding(16.dp))
        } else if (errorMessage != null) {
            Text(text = errorMessage ?: "", color = Color.Red, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                recipes?.let { recipeList ->
                    items(recipeList) { recipe ->
                        RecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe.idMeal) })
                    }
                }
            }
        }
    }
}

//The code to fetch recipes by category from the API
private fun fetchRecipesByCategory(category: String, onResult: (List<Recipe>?, String?) -> Unit) {
    val call = ApiClient.retrofitService.getRecipesByCategory(category)
    call.enqueue(object : Callback<RecipeResponse> {
        override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.meals != null) {
                    onResult(body.meals, null)
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