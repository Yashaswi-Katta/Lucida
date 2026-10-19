package com.lucida.studio.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences
import org.json.JSONArray
import org.json.JSONObject

private val Context.lucidaSessionStore by preferencesDataStore(name = "lucida_session_v1")

data class SavedSession(
    val uri: String? = null,
    val matrix: List<Float> = emptyList(),
    val corners: List<Float> = emptyList(),
    val viewportWidth: Int = 0,
    val viewportHeight: Int = 0,
    val opacity: Float = 0.65f,
    val filter: String = "RAW_IMAGE",
    val tint: String = "AMBER_GOLD",
    val threshold: Int = 45,
    val solo: String = "ALL_BANDS",
    val grid: String = "NONE",
    val homography: Boolean = false
)

class SessionPreferencesRepo(context: Context) {
    private val store = context.applicationContext.lucidaSessionStore
    private val key = stringPreferencesKey("session")

    val sessions: Flow<SavedSession> = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { preferences ->
        val text = preferences[key]
        if (text == null) SavedSession() else decode(text)
    }

    suspend fun save(session: SavedSession) {
        val json = JSONObject().apply {
            put("schema", 1)
            put("uri", session.uri ?: JSONObject.NULL)
            put("matrix", JSONArray(session.matrix))
            put("corners", JSONArray(session.corners))
            put("viewportWidth", session.viewportWidth)
            put("viewportHeight", session.viewportHeight)
            put("opacity", session.opacity.toDouble())
            put("filter", session.filter)
            put("tint", session.tint)
            put("threshold", session.threshold)
            put("solo", session.solo)
            put("grid", session.grid)
            put("homography", session.homography)
        }
        store.edit { it[key] = json.toString() }
    }

    private fun decode(text: String): SavedSession = try {
        val json = JSONObject(text)
        if (json.optInt("schema", 0) != 1) SavedSession() else SavedSession(
            uri = if (json.isNull("uri")) null else json.optString("uri").takeIf { it.isNotBlank() },
            matrix = floats(json.optJSONArray("matrix"), 9),
            corners = floats(json.optJSONArray("corners"), 8),
            viewportWidth = json.optInt("viewportWidth").coerceAtLeast(0),
            viewportHeight = json.optInt("viewportHeight").coerceAtLeast(0),
            opacity = json.optDouble("opacity", 0.65).toFloat().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0.65f },
            filter = json.optString("filter", "RAW_IMAGE"),
            tint = json.optString("tint", "AMBER_GOLD"),
            threshold = json.optInt("threshold", 45).coerceIn(10, 120),
            solo = json.optString("solo", "ALL_BANDS"),
            grid = json.optString("grid", "NONE"),
            homography = json.optBoolean("homography")
        )
    } catch (_: org.json.JSONException) {
        SavedSession()
    }

    private fun floats(array: JSONArray?, count: Int): List<Float> {
        if (array == null || array.length() != count) return emptyList()
        return try {
            List(count) { array.getDouble(it).toFloat() }.takeIf { values -> values.all { it.isFinite() } } ?: emptyList()
        } catch (_: org.json.JSONException) {
            emptyList()
        }
    }
}
