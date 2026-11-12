package com.example.recipefinder

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private lateinit var root: LinearLayout
    private var currentMeals: List<MealSummary> = emptyList()
    private val favourites = linkedSetOf<String>()
    private var currentUserEmail: String? = null
    private var pendingCameraBitmap: Bitmap? = null

    private val galleryPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) recognize(InputImage.fromFilePath(this, uri))
    }
    private val cameraPicker = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            pendingCameraBitmap = bitmap
            recognize(InputImage.fromBitmap(bitmap, 0))
        } else toast("No photo captured")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebaseIfConfigured()
        showLogin()
    }

    private fun initializeFirebaseIfConfigured() {
        val apiKey = getString(R.string.firebase_api_key)
        val appId = getString(R.string.firebase_app_id)
        val projectId = getString(R.string.firebase_project_id)
        if (apiKey.startsWith("PASTE_") || appId.startsWith("PASTE_") || projectId.startsWith("PASTE_")) return
        try {
            val options = FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId(appId)
                .setProjectId(projectId)
                .setGcmSenderId(getString(R.string.firebase_project_number))
                .build()
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, options)
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
        } catch (_: Exception) { auth = null; db = null }
    }

    private fun setup(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 20)
            setBackgroundColor(0xFFFFF7F0.toInt())
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        text(title, 26f, true)
    }

    private fun text(value: String, size: Float = 16f, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(0xFF30251F.toInt())
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 8)
            root.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun button(label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }
        root.addView(b, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 6; bottomMargin = 4 })
    }

    private fun input(hint: String, password: Boolean = false): EditText {
        val e = EditText(this).apply {
            this.hint = hint
            textSize = 16f
            if (password) inputType = 129
        }
        root.addView(e, LinearLayout.LayoutParams(-1, -2))
        return e
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun showLogin() {
        setup("Recipe Finder")
        text("Sign in to discover recipes from ingredients you have.", 15f)
        val email = input("Email address")
        val password = input("Password", true)
        button("Login") {
            val a = auth
            if (a == null) {
                toast("Connect Firebase first. See README.md → Firebase setup.")
            } else lifecycleScope.launch {
                try {
                    a.signInWithEmailAndPassword(email.text.toString().trim(), password.text.toString()).await()
                    currentUserEmail = a.currentUser?.email
                    showHome()
                } catch (e: Exception) { toast(e.message ?: "Login failed") }
            }
        }
        button("Create account") {
            if (auth == null) toast("Connect Firebase first. See README.md")
            else showRegister()
        }
        text("Firebase must be configured before authentication can be used.", 13f)
    }

    private fun showRegister() {
        setup("Create account")
        val email = input("Email address")
        val password = input("Password (6+ characters)", true)
        button("Register") {
            val a = auth ?: return@button toast("Configure Firebase first")
            lifecycleScope.launch {
                try {
                    a.createUserWithEmailAndPassword(email.text.toString().trim(), password.text.toString()).await()
                    currentUserEmail = a.currentUser?.email
                    showHome()
                } catch (e: Exception) { toast(e.message ?: "Registration failed") }
            }
        }
        button("Back to login") { showLogin() }
    }

    private fun showHome() {
        setup("What would you like to cook?")
        text("Photograph an ingredient, choose a photo, or search by recipe name.")
        button("📷  Take a photo") { cameraPicker.launch(null) }
        button("🖼  Choose photo from gallery") { galleryPicker.launch("image/*") }
        val query = input("Search recipes by name (e.g. pasta)")
        button("Search recipes") { searchByName(query.text.toString().trim()) }
        button("♡  My favourites") { showFavourites() }
        button("Logout") {
            auth?.signOut()
            currentUserEmail = null
            showLogin()
        }
        if (auth == null) text("Firebase setup is required for login. Recipe search and recognition work after opening the app; configure Firebase to use accounts.", 13f)
    }

    private fun recognize(image: InputImage) {
        setup("Recognising ingredient…")
        text("Analysing your photo with ML Kit.")
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                val top = labels.sortedByDescending { it.confidence }.take(3)
                setup("Choose an ingredient")
                if (top.isEmpty()) text("No ingredient labels detected. Try a clearer photo.")
                top.forEach { label ->
                    button("${label.text}  (${(label.confidence * 100).toInt()}%)") {
                        searchByIngredient(label.text)
                    }
                }
                button("Back home") { showHome() }
            }
            .addOnFailureListener { toast("Recognition failed: ${it.localizedMessage}") }
    }

    private fun searchByIngredient(ingredient: String) {
        setup("Recipes with $ingredient")
        text("Searching TheMealDB…")
        lifecycleScope.launch {
            try {
                val response = RecipeApi.service.byIngredient(ingredient)
                currentMeals = response.meals.orEmpty()
                showRecipeGrid("Recipes with $ingredient", currentMeals)
            } catch (e: Exception) {
                setup("Could not load recipes")
                text("Check your internet connection and try again.")
                button("Retry") { searchByIngredient(ingredient) }
                button("Home") { showHome() }
            }
        }
    }

    private fun searchByName(name: String) {
        if (name.isBlank()) return toast("Enter a recipe name")
        setup("Search results")
        text("Searching…")
        lifecycleScope.launch {
            try {
                val details = RecipeApi.service.byName(name).meals.orEmpty()
                currentMeals = details.map {
                    MealSummary(it.idMeal, it.strMeal.orEmpty(), it.strMealThumb)
                }
                showRecipeGrid("Results for $name", currentMeals)
            } catch (_: Exception) { showNetworkError { searchByName(name) } }
        }
    }

    private fun showRecipeGrid(title: String, meals: List<MealSummary>) {
        setup(title)
        if (meals.isEmpty()) text("No recipes found. Try another ingredient or search term.")
        val rv = RecyclerView(this)
        rv.layoutManager = GridLayoutManager(this, 2)
        rv.adapter = MealAdapter(meals) { openDetails(it) }
        root.addView(rv, LinearLayout.LayoutParams(-1, 900))
        button("Home") { showHome() }
    }

    private fun openDetails(meal: MealSummary) {
        setup("Recipe details")
        text("Loading ${meal.strMeal}…")
        lifecycleScope.launch {
            try {
                val detail = RecipeApi.service.details(meal.idMeal).meals?.firstOrNull()
                if (detail == null) return@launch toast("Recipe details not available")
                showDetails(detail)
            } catch (_: Exception) { showNetworkError { openDetails(meal) } }
        }
    }

    private fun showDetails(meal: MealDetail) {
        setup(meal.strMeal ?: "Recipe")
        val image = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 500)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        root.addView(image)
        meal.strMealThumb?.let { Glide.with(this).load(it).into(image) }
        text(meal.strCategory.orEmpty() + "  •  " + meal.strArea.orEmpty(), 14f)
        text("Ingredients", 20f, true)
        meal.ingredientLines().forEach { text("• $it") }
        text("Instructions", 20f, true)
        text(meal.strInstructions ?: "No instructions available.")
        button(if (isFavourite(meal.idMeal)) "♥ Remove favourite" else "♡ Add to favourites") {
            toggleFavourite(meal.idMeal)
            showDetails(meal)
        }
        button("Watch on YouTube") {
            val url = meal.strYoutube
            if (!url.isNullOrBlank()) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            else toast("No video link available")
        }
        button("Back to home") { showHome() }
    }

    private fun isFavourite(id: String): Boolean =
        getSharedPreferences("recipe_favourites", MODE_PRIVATE).getStringSet("ids", emptySet())?.contains(id) == true

    private fun toggleFavourite(id: String) {
        val prefs = getSharedPreferences("recipe_favourites", MODE_PRIVATE)
        val ids = prefs.getStringSet("ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!ids.add(id)) ids.remove(id)
        prefs.edit().putStringSet("ids", ids).apply()
        toast(if (id in ids) "Added to favourites" else "Removed from favourites")
    }

    private fun showFavourites() {
        val ids = getSharedPreferences("recipe_favourites", MODE_PRIVATE).getStringSet("ids", emptySet()).orEmpty()
        setup("My favourites")
        if (ids.isEmpty()) {
            text("You have not saved any recipes yet.")
            button("Home") { showHome() }
            return
        }
        text("Loading saved recipes…")
        lifecycleScope.launch {
            val saved = mutableListOf<MealSummary>()
            ids.forEach { id ->
                try {
                    RecipeApi.service.details(id).meals?.firstOrNull()?.let {
                        saved.add(MealSummary(it.idMeal, it.strMeal.orEmpty(), it.strMealThumb))
                    }
                } catch (_: Exception) { }
            }
            showRecipeGrid("My favourites", saved)
        }
    }

    private fun showNetworkError(retry: () -> Unit) {
        setup("Network error")
        text("Unable to reach TheMealDB. Check your internet connection.")
        button("Retry", retry)
        button("Home") { showHome() }
    }

    inner class MealAdapter(
        private val items: List<MealSummary>,
        private val onClick: (MealSummary) -> Unit
    ) : RecyclerView.Adapter<MealAdapter.Holder>() {
        inner class Holder(val box: LinearLayout, val photo: ImageView, val name: TextView)
            : RecyclerView.ViewHolder(box)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val box = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8)
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            val photo = ImageView(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(-1, 230)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            val name = TextView(this@MainActivity).apply {
                textSize = 15f
                setTextColor(0xFF30251F.toInt())
                setPadding(4, 8, 4, 8)
            }
            box.addView(photo); box.addView(name)
            return Holder(box, photo, name)
        }
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val meal = items[position]
            holder.name.text = meal.strMeal
            Glide.with(this@MainActivity).load(meal.strMealThumb).centerCrop().into(holder.photo)
            holder.box.setOnClickListener { onClick(meal) }
        }
    }
}
