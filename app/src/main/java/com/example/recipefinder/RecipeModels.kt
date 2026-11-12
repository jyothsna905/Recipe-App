package com.example.recipefinder

data class MealSummary(
    val idMeal: String,
    val strMeal: String,
    val strMealThumb: String?
)

data class MealListResponse(val meals: List<MealSummary>?)

data class MealDetail(
    val idMeal: String,
    val strMeal: String?,
    val strMealThumb: String?,
    val strInstructions: String?,
    val strYoutube: String?,
    val strCategory: String?,
    val strArea: String?,
    val strIngredient1: String?, val strIngredient2: String?,
    val strIngredient3: String?, val strIngredient4: String?,
    val strIngredient5: String?, val strIngredient6: String?,
    val strIngredient7: String?, val strIngredient8: String?,
    val strIngredient9: String?, val strIngredient10: String?,
    val strIngredient11: String?, val strIngredient12: String?,
    val strIngredient13: String?, val strIngredient14: String?,
    val strIngredient15: String?, val strIngredient16: String?,
    val strIngredient17: String?, val strIngredient18: String?,
    val strIngredient19: String?, val strIngredient20: String?,
    val strMeasure1: String?, val strMeasure2: String?,
    val strMeasure3: String?, val strMeasure4: String?,
    val strMeasure5: String?, val strMeasure6: String?,
    val strMeasure7: String?, val strMeasure8: String?,
    val strMeasure9: String?, val strMeasure10: String?,
    val strMeasure11: String?, val strMeasure12: String?,
    val strMeasure13: String?, val strMeasure14: String?,
    val strMeasure15: String?, val strMeasure16: String?,
    val strMeasure17: String?, val strMeasure18: String?,
    val strMeasure19: String?, val strMeasure20: String?
) {
    fun ingredientLines(): List<String> {
        val ingredients = listOf(
            strIngredient1, strIngredient2, strIngredient3, strIngredient4, strIngredient5,
            strIngredient6, strIngredient7, strIngredient8, strIngredient9, strIngredient10,
            strIngredient11, strIngredient12, strIngredient13, strIngredient14, strIngredient15,
            strIngredient16, strIngredient17, strIngredient18, strIngredient19, strIngredient20
        )
        val measures = listOf(
            strMeasure1, strMeasure2, strMeasure3, strMeasure4, strMeasure5,
            strMeasure6, strMeasure7, strMeasure8, strMeasure9, strMeasure10,
            strMeasure11, strMeasure12, strMeasure13, strMeasure14, strMeasure15,
            strMeasure16, strMeasure17, strMeasure18, strMeasure19, strMeasure20
        )
        return ingredients.mapIndexedNotNull { i, ingredient ->
            ingredient?.takeIf { it.isNotBlank() }?.let {
                "${measures[i].orEmpty().trim()} ${it.trim()}".trim()
            }
        }
    }
}

data class MealDetailResponse(val meals: List<MealDetail>?)
