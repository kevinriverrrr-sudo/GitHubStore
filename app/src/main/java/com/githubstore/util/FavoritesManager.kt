package com.githubstore.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

class FavoritesManager(context: Context) {
    private val gson = Gson()
    private val favoritesFile = File(context.filesDir, "favorites.json")

    @Volatile
    private var favoritesInternal: MutableSet<String> = Collections.synchronizedSet(loadFavorites())

    val favorites: Set<String> get() = synchronized(favoritesInternal) { favoritesInternal.toSet() }

    fun isFavorite(repoFullName: String): Boolean = synchronized(favoritesInternal) {
        favoritesInternal.contains(repoFullName)
    }

    fun toggleFavorite(repoFullName: String): Boolean = synchronized(favoritesInternal) {
        return if (favoritesInternal.contains(repoFullName)) {
            favoritesInternal.remove(repoFullName)
            saveFavoritesAsync()
            false
        } else {
            favoritesInternal.add(repoFullName)
            saveFavoritesAsync()
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

    private fun saveFavoritesAsync() {
        try {
            Thread {
                try {
                    val json = synchronized(favoritesInternal) { gson.toJson(favoritesInternal) }
                    favoritesFile.writeText(json)
                } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {}
    }
}
