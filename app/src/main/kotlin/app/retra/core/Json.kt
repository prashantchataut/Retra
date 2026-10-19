package app.retra.core

import kotlinx.serialization.json.Json

/** The single JSON configuration used for all app files and the catalog. */
val RetraJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
    coerceInputValues = true
    prettyPrint = false
}
