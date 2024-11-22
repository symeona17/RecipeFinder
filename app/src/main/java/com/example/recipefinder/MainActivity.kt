package com.example.recipefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.Crossfade
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

class MainActivity : ComponentActivity() {
    private var isAppBackButtonVisible by mutableStateOf(false)
    private var currentScreen by mutableStateOf("search")
    private var categoryName by mutableStateOf<String?>(null)
    private var mealId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentScreen != "search") {
                    currentScreen = "search"
                    isAppBackButtonVisible = false
                } else {
                    finish()
                }
            }
        })
        setContent {
            var ingredients by remember { mutableStateOf<List<String>>(emptyList()) }
            var categories by remember { mutableStateOf<List<Category>?>(null) }

            LaunchedEffect(Unit) {
                fetchAllIngredients { result ->
                    ingredients = result ?: emptyList()
                }
                fetchMealCategories { result ->
                    categories = result
                }
            }

            MainScreen(
                ingredients = ingredients,
                categories = categories,
                currentScreen = currentScreen,
                categoryName = categoryName,
                mealId = mealId,
                onScreenChange = { screen, category, meal ->
                    currentScreen = screen
                    categoryName = category
                    mealId = meal
                    isAppBackButtonVisible = screen == "categoryRecipes" || screen == "recipeDetails"
                }
            )
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

    private fun fetchMealCategories(onResult: (List<Category>?) -> Unit) {
        val call = ApiClient.retrofitService.getMealCategories()
        call.enqueue(object : Callback<CategoryResponse> {
            override fun onResponse(call: Call<CategoryResponse>, response: Response<CategoryResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    onResult(body?.categories)
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<CategoryResponse>, t: Throwable) {
                onResult(null)
            }
        })
    }
}

@Composable
fun MainScreen(
    ingredients: List<String>,
    categories: List<Category>?,
    currentScreen: String,
    categoryName: String?,
    mealId: String?,
    onScreenChange: (String, String?, String?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentScreen) {
        selectedTab = when (currentScreen) {
            "search" -> 0
            "categories", "categoryRecipes" -> 1
            else -> 0
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.search),
                            contentDescription = "Search"
                        )
                    },
                    label = { Text("Search") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onScreenChange("search", null, null)
                    }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.sort),
                            contentDescription = "Categories"
                        )
                    },
                    label = { Text("Categories") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onScreenChange("categories", null, null)
                    }
                )
            }
        }
    ) { innerPadding ->
        Crossfade(targetState = currentScreen, label = "Screen Crossfade") { screen ->
            when (screen) {
                "search" -> RecipeSearch(
                    modifier = Modifier.padding(innerPadding),
                    ingredients = ingredients,
                    onRecipeClick = { mealId ->
                        onScreenChange("recipeDetails", null, mealId)
                    }
                )

                "categories" -> CategoriesScreen(
                    modifier = Modifier.padding(innerPadding),
                    categories = categories,
                    onCategoryClick = { category ->
                        selectedTab = 1
                        onScreenChange("categoryRecipes", category.strCategory, null)
                    }
                )

                "categoryRecipes" -> CategoryRecipesScreen(
                    categoryName = categoryName ?: "",
                    onBackClick = {
                        onScreenChange("search", null, null)
                    },
                    onRecipeClick = { mealId ->
                        onScreenChange("recipeDetails", null, mealId)
                    }
                )

                "recipeDetails" -> RecipeDetailsScreen(
                    mealId = mealId ?: "",
                    onBackClick = {
                        onScreenChange("search", null, null)
                    }
                )
            }
        }
    }
}

@Composable
fun RecipeDetailsScreen(mealId: String, onBackClick: () -> Unit) {
    var recipe by remember { mutableStateOf<Recipe?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mealId) {
        fetchRecipeDetails(mealId) { result, error ->
            recipe = result
            errorMessage = error
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Icon(
            painter = painterResource(id = R.drawable.arrow_back),
            contentDescription = "Back",
            modifier = Modifier
                .padding(16.dp)
                .clickable { onBackClick() }
        )
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (errorMessage != null) {
            Text(text = "Error: $errorMessage", color = Color.Red, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            recipe?.let {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(text = it.strMeal, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                        AsyncImage(
                            model = it.strMealThumb,
                            contentDescription = it.strMeal,
                            modifier = Modifier
                                .size(150.dp)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(text = "Ingredients", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
                    }
                    items(it.ingredients) { ingredient ->
                        Text(text = ingredient, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                    }
                    item {
                        Text(text = "Instructions", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
                        Text(text = it.strInstructions, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                        it.strTags?.let { tags ->
                            Text(text = "Tags: $tags", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                        }
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

private fun fetchRecipeDetails(mealId: String, onResult: (Recipe?, String?) -> Unit) {
    val call = ApiClient.retrofitService.getRecipeDetails(mealId)
    call.enqueue(object : Callback<RecipeResponse> {
        override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
            if (response.isSuccessful) {
                val body = response.body()
                try {
                    if (body?.meals != null && body.meals.isNotEmpty()) {
                        val recipe = body.meals[0]
                        recipe.ingredients = listOfNotNull(
                            recipe.strIngredient1,
                            recipe.strIngredient2,
                            recipe.strIngredient3,
                            recipe.strIngredient4,
                            recipe.strIngredient5,
                            recipe.strIngredient6,
                            recipe.strIngredient7,
                            recipe.strIngredient8,
                            recipe.strIngredient9,
                            recipe.strIngredient10,
                            recipe.strIngredient11,
                            recipe.strIngredient12,
                            recipe.strIngredient13,
                            recipe.strIngredient14,
                            recipe.strIngredient15,
                            recipe.strIngredient16,
                            recipe.strIngredient17,
                            recipe.strIngredient18,
                            recipe.strIngredient19,
                            recipe.strIngredient20
                        ).filter { it.isNotBlank() }
                        onResult(recipe, null)
                    } else {
                        onResult(null, "No recipe found.")
                    }
                } catch (e: Exception) {
                    onResult(null, "Failed to parse recipe details: ${e.message}")
                }
            } else {
                onResult(null, "Failed to fetch recipe details: ${response.message()}")
            }
        }

        override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {
            onResult(null, "Network error: ${t.message}")
        }
    })
}

@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
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