package com.example.muzzio.data

import android.content.Context
import com.example.muzzio.data.models.Playlist
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PlaylistStorage(context: Context) {

    private val prefs = context.getSharedPreferences("playlists", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): MutableList<Playlist> {
        val json = prefs.getString("data", "[]") ?: "[]"
        val type = object : TypeToken<MutableList<Playlist>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveAll(list: MutableList<Playlist>) {
        prefs.edit().putString("data", gson.toJson(list)).apply()
    }

    fun addPlaylist(playlist: Playlist) {
        val list = getAll()
        list.add(playlist)
        saveAll(list)
    }

    fun updatePlaylist(playlist: Playlist) {
        val list = getAll()
        val index = list.indexOfFirst { it.id == playlist.id }
        if (index != -1) {
            list[index] = playlist
            saveAll(list)
        }
    }

    fun deletePlaylist(id: String) {
        val list = getAll().filter { it.id != id }.toMutableList()
        saveAll(list)
    }
}
