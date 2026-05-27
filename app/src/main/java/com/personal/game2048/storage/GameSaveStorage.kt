package com.personal.game2048.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GameSaveStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("g2048_save", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun hasSavedGame(): Boolean =
        prefs.getBoolean("g2048_save_active", false)

    fun saveGame(board: List<Int>, score: Int, elapsedSecs: Int) {
        prefs.edit()
            .putBoolean("g2048_save_active", true)
            .putString("g2048_save_board", gson.toJson(board))
            .putInt("g2048_save_score", score)
            .putInt("g2048_save_elapsed_secs", elapsedSecs)
            .apply()
    }

    fun loadGame(): Triple<List<Int>, Int, Int>? {
        if (!hasSavedGame()) return null
        val boardJson = prefs.getString("g2048_save_board", null) ?: return null
        val board: List<Int> = gson.fromJson(boardJson, object : TypeToken<List<Int>>() {}.type)
        val score = prefs.getInt("g2048_save_score", 0)
        val elapsed = prefs.getInt("g2048_save_elapsed_secs", 0)
        return Triple(board, score, elapsed)
    }

    fun clearSave() {
        prefs.edit()
            .putBoolean("g2048_save_active", false)
            .remove("g2048_save_board")
            .remove("g2048_save_score")
            .remove("g2048_save_elapsed_secs")
            .commit()
    }
}
