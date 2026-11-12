package com.example.recipefinder

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface RecipeApi {
    @GET("filter.php")
    suspend fun byIngredient(@Query("i") ingredient: String): MealListResponse

    @GET("search.php")
    suspend fun byName(@Query("s") name: String): MealDetailResponse

    @GET("lookup.php")
    suspend fun details(@Query("i") id: String): MealDetailResponse

    companion object {
        val service: RecipeApi by lazy {
            Retrofit.Builder()
                .baseUrl("https://www.themealdb.com/api/json/v1/1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RecipeApi::class.java)
        }
    }
}
