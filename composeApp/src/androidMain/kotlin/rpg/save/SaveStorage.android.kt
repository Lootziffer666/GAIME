package rpg.save

import android.content.Context

/**
 * Holds the application [Context] needed for Android save storage. Set once from
 * `MainActivity.onCreate` (with `applicationContext`, so there is no leak). Until
 * it is set, save/load are no-ops returning null.
 */
object SaveStorageContext {
    @Volatile
    var appContext: Context? = null
}

private const val PREFS_NAME = "gaime_save"
private const val KEY_SAVE = "save_json"

actual fun saveGame(json: String) {
    val ctx = SaveStorageContext.appContext ?: return
    runCatching {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVE, json)
            .apply()
    }
}

actual fun loadGame(): String? {
    val ctx = SaveStorageContext.appContext ?: return null
    return runCatching {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SAVE, null)
    }.getOrNull()
}
