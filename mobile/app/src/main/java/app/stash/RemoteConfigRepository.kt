package app.stash

import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object RemoteConfigRepository {

    fun fetchConfig(onComplete: (RemoteConfigResult) -> Unit) {
        val apps = FirebaseApp.getApps(null)
        if (apps.isEmpty()) {
            onComplete(RemoteConfigResult(taskValidator = false, appData = AppDataConfig(), offers = emptyList()))
            return
        }

        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)

            remoteConfig.setDefaultsAsync(
                mapOf(
                    "task_validator" to false,
                    "app_data" to "{\"country\":\"ZZ\",\"locale\":\"en\"}",
                    "settings" to "{\"version\":1,\"offers\":[],\"fallbackOffers\":[],\"countries\":{}}"
                )
            )

            remoteConfig.fetchAndActivate().addOnCompleteListener {
                val validator = remoteConfig.getBoolean("task_validator")
                val appDataRaw = remoteConfig.getString("app_data")
                val settingsRaw = remoteConfig.getString("settings")

                val appData = parseAppData(appDataRaw)
                val offers = parseOffers(settingsRaw, appData)

                onComplete(
                    RemoteConfigResult(
                        taskValidator = validator,
                        appData = appData,
                        offers = offers
                    )
                )
            }
        } catch (_: Exception) {
            onComplete(RemoteConfigResult(taskValidator = false, appData = AppDataConfig(), offers = emptyList()))
        }
    }

    private fun parseAppData(raw: String): AppDataConfig {
        return try {
            val obj = JSONObject(raw)
            val country = obj.optString("country", "ZZ").uppercase(Locale.US)
            val locale = obj.optString("locale", "en").lowercase(Locale.US)
            AppDataConfig(country = country, locale = locale)
        } catch (_: Exception) {
            AppDataConfig()
        }
    }

    private fun parseOffers(settingsRaw: String, appData: AppDataConfig): List<OfferItem> {
        if (settingsRaw.isBlank()) return emptyList()
        return try {
            val root = JSONObject(settingsRaw)
            val countriesObj = root.optJSONObject("countries")
            val countryKey = appData.country.uppercase(Locale.US)

            var offersArray: JSONArray? = null

            if (countriesObj != null && countriesObj.has(countryKey)) {
                val countryBlock = countriesObj.optJSONObject(countryKey)
                val countryOffers = countryBlock?.optJSONArray("offers")
                val countryFallback = countryBlock?.optJSONArray("fallbackOffers")
                offersArray = when {
                    countryOffers != null && countryOffers.length() > 0 -> countryOffers
                    countryFallback != null && countryFallback.length() > 0 -> countryFallback
                    else -> null
                }
            }

            if (offersArray == null) {
                val rootOffers = root.optJSONArray("offers")
                val rootFallback = root.optJSONArray("fallbackOffers")
                offersArray = when {
                    rootOffers != null && rootOffers.length() > 0 -> rootOffers
                    rootFallback != null && rootFallback.length() > 0 -> rootFallback
                    else -> JSONArray()
                }
            }

            val result = mutableListOf<OfferItem>()
            val targetLocale = appData.locale.lowercase(Locale.US)

            for (i in 0 until offersArray.length()) {
                val item = offersArray.optJSONObject(i) ?: continue
                val defaultParams = item.optJSONObject("parameters")
                val localesObj = item.optJSONObject("locales")
                val localizedObj = localesObj?.optJSONObject(targetLocale)
                    ?: localesObj?.optJSONObject(Locale.getDefault().language.lowercase(Locale.US))
                val localizedParams = localizedObj?.optJSONObject("parameters")

                val providerName = localizedObj?.optString("providerName")?.takeIf { it.isNotBlank() }
                    ?: item.optString("providerName", "Offer")
                val ctaText = localizedObj?.optString("ctaText")?.takeIf { it.isNotBlank() }
                    ?: item.optString("ctaText", "Apply")

                fun param(key: String): String {
                    return localizedParams?.optString(key)?.takeIf { it.isNotBlank() }
                        ?: defaultParams?.optString(key)?.takeIf { it.isNotBlank() }
                        ?: ""
                }

                result.add(
                    OfferItem(
                        id = item.optString("id", i.toString()),
                        providerName = providerName,
                        logoUrl = item.optString("logoUrl", ""),
                        url = item.optString("url", ""),
                        openMode = item.optString("openMode", "webview"),
                        currency = item.optString("currency", ""),
                        rating = item.optDouble("rating", 4.8),
                        priority = item.optInt("priority", 99),
                        isFeatured = item.optBoolean("isFeatured", false),
                        ctaText = ctaText,
                        amount = param("amount"),
                        term = param("term"),
                        interestRate = param("interestRate"),
                        processingTime = param("processingTime"),
                        approvalChance = param("approvalChance")
                    )
                )
            }

            result.sortedWith(
                compareByDescending<OfferItem> { it.isFeatured }
                    .thenBy { it.priority }
                    .thenByDescending { it.rating }
            )
        } catch (_: Exception) {
            emptyList()
        }
    }
}
