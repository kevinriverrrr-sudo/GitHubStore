package com.githubstore.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class FavoritesManager(context: Context) {
    private val gson = Gson()
    private val favoritesFile = File(context.filesDir, "favorites.json")

    private var favoritesInternal: MutableSet<String> = loadFavorites()

    val favorites: Set<String> get() = favoritesInternal.toSet()

    fun isFavorite(repoFullName: String): Boolean = favoritesInternal.contains(repoFullName)

    fun toggleFavorite(repoFullName: String): Boolean {
        return if (favoritesInternal.contains(repoFullName)) {
            favoritesInternal.remove(repoFullName)
            saveFavorites()
            false
        } else {
            favoritesInternal.add(repoFullName)
            saveFavorites()
            true
        }
    }

    private fun loadFavorites(): MutableSet<String> {
        return try {
            if (favoritesFile.exists()) {
                val json = favoritesFile.readText()
                val type = object : TypeToken<MutableSet<String>>() {}.type
                gson.fromJson<MutableSet<String>>(json, type) ?: mutableSetOf()
            } else {
                mutableSetOf()
            }
        } catch (_: Exception) {
            mutableSetOf()
        }
    }

    private fun saveFavorites() {
        try {
            favoritesFile.writeText(gson.toJson(favoritesInternal))
        } catch (_: Exception) {}
    }
}
