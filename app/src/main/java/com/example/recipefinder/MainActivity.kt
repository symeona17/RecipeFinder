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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.Crossfade

class MainActivity : ComponentActivity() {
    private var isAppBackButtonVisible by mutableStateOf(false)
    private var currentScreen by mutableStateOf("search")
    private var categoryName by mutableStateOf<String?>(null)
    private var mealId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    icon = { Icon(painter = painterResource(id = R.drawable.search), contentDescription = "Search") },
                    label = { Text("Search") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onScreenChange("search", null, null)
                    }
                )
                NavigationBarItem(
                    icon = { Icon(painter = painterResource(id = R.drawable.sort), contentDescription = "Categories") },
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
                        selectedTab = 1
                        onScreenChange("categories", null, null)
                    },
                    onRecipeClick = { mealId ->
                        onScreenChange("recipeDetails", null, mealId)
                    }
                )
                "recipeDetails" -> RecipeDetailsScreen(
                    mealId = mealId ?: "",
                    onBackClick = {
                        onScreenChange("categoryRecipes", categoryName, null)
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

    Column {
        Icon(
            painter = painterResource(id = R.drawable.arrow_back),
            contentDescription = "Back",
            modifier = Modifier
                .padding(16.dp)
                .clickable { onBackClick() }
        )
        if (isLoading) {
            Text(text = "Loading...", color = Color.Gray, modifier = Modifier.padding(16.dp))
        } else if (errorMessage != null) {
            Text(text = errorMessage ?: "", color = Color.Red, modifier = Modifier.padding(16.dp))
        } else {
            recipe?.let {
                Text(text = it.strMeal, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                AsyncImage(
                    model = it.strMealThumb,
                    contentDescription = it.strMeal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "Loading...",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )            }
        }
    }
}

@Composable
fun CategoriesScreen(modifier: Modifier = Modifier, categories: List<Category>?, onCategoryClick: (Category) -> Unit) {
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

private fun fetchRecipesByCategory(category: String, onResult: (List<Recipe>?, String?) -> Unit) {
    val call = ApiClient.retrofitService.getRecipesByCategory(category)
    call.enqueue(object : Callback<RecipeResponse> {
        override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.meals != null) {
                    onResult(body.meals, null)
                } else {
                    onResult(null, "No recipes found.")
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


private fun fetchRecipeDetails(mealId: String, onResult: (Recipe?, String?) -> Unit) {
    val call = ApiClient.retrofitService.getRecipeDetails(mealId)
    call.enqueue(object : Callback<RecipeResponse> {
        override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.meals != null && body.meals.isNotEmpty()) {
                    onResult(body.meals[0], null)
                } else {
                    onResult(null, "No recipe found.")
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