package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

object LicenseManager {

    private const val PREFS_NAME = "paramoteur_security_license_prefs"
    private const val KEY_ACTIVATION_CODE = "activated_code"
    private const val KEY_ACTIVATION_EXPIRY = "activation_expiry_epoch_ms"
    private const val KEY_IS_MASTER = "is_master_developer"
    private const val KEY_INSTALL_UUID = "install_device_uuid"
    private const val KEY_PILOT_NAME = "pilot_name"

    // Secret salt strictly for signature generation (tamper-proof)
    private const val SECRET_SALT = "PARAMOTEUR_SECURE_SALT_COMP_2026_X99F"

    // Master developer unlock codes (always valid on any device)
    val MASTER_CODES = listOf(
        "PARAMASTER2026",
        "DEV-UNLOCK-MASTER",
        "PARAMOTEUR-SUPER-ADMIN"
    )

    enum class LicenseDuration(val code: String, val label: String, val days: Int) {
        SEVEN_DAYS("7D", "7 Jours (Test rapide)", 7),
        THIRTY_DAYS("30D", "30 Jours (1 Mois)", 30),
        NINETY_DAYS("90D", "90 Jours (3 Mois)", 90),
        ONE_YEAR("1Y", "1 An (Saison)", 365),
        UNLIMITED("MAX", "Illimité / Permanent", 3650)
    }

    data class LicenseStatus(
        val isActivated: Boolean,
        val isMasterDeveloper: Boolean,
        val expiryDateMs: Long,
        val daysRemaining: Int,
        val pilotName: String,
        val licenseTypeLabel: String
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns a stable, unique 8-character device ID formatted as PM-XXXX-XXXX
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val prefs = getPrefs(context)
        var installUuid = prefs.getString(KEY_INSTALL_UUID, null)

        if (installUuid == null) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                null
            }

            val rawBase = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                androidId
            } else {
                UUID.randomUUID().toString()
            }

            installUuid = rawBase
            prefs.edit().putString(KEY_INSTALL_UUID, installUuid).apply()
        }

        // Hash the base to produce a clean 8-character ID
        val digest = sha256("$installUuid-$SECRET_SALT")
        val part1 = digest.substring(0, 4).uppercase(Locale.ROOT)
        val part2 = digest.substring(4, 8).uppercase(Locale.ROOT)
        return "PM-$part1-$part2"
    }

    /**
     * Checks current activation status
     */
    fun checkStatus(context: Context): LicenseStatus {
        val prefs = getPrefs(context)
        val isMaster = prefs.getBoolean(KEY_IS_MASTER, false)
        if (isMaster) {
            return LicenseStatus(
                isActivated = true,
                isMasterDeveloper = true,
                expiryDateMs = Long.MAX_VALUE,
                daysRemaining = 9999,
                pilotName = "Développeur / Concepteur",
                licenseTypeLabel = "Accès Développeur Master"
            )
        }

        val code = prefs.getString(KEY_ACTIVATION_CODE, null)
        val expiryMs = prefs.getLong(KEY_ACTIVATION_EXPIRY, 0L)
        val pilotName = prefs.getString(KEY_PILOT_NAME, "") ?: ""

        if (code.isNullOrBlank() || expiryMs <= 0L) {
            return LicenseStatus(
                isActivated = false,
                isMasterDeveloper = false,
                expiryDateMs = 0L,
                daysRemaining = 0,
                pilotName = "",
                licenseTypeLabel = "Non activée"
            )
        }

        val now = System.currentTimeMillis()
        if (now > expiryMs) {
            return LicenseStatus(
                isActivated = false,
                isMasterDeveloper = false,
                expiryDateMs = expiryMs,
                daysRemaining = 0,
                pilotName = pilotName,
                licenseTypeLabel = "Licence expirée"
            )
        }

        val daysRemaining = (((expiryMs - now) / (1000L * 60 * 60 * 24))).toInt().coerceAtLeast(0) + 1
        return LicenseStatus(
            isActivated = true,
            isMasterDeveloper = false,
            expiryDateMs = expiryMs,
            daysRemaining = daysRemaining,
            pilotName = pilotName,
            licenseTypeLabel = "Licence active ($daysRemaining j restants)"
        )
    }

    /**
     * Activates the app using a key or master code
     */
    fun activate(context: Context, enteredCode: String, pilotName: String = ""): Pair<Boolean, String> {
        val trimmed = enteredCode.trim().uppercase(Locale.ROOT)
        if (trimmed.isBlank()) {
            return Pair(false, "Veuillez saisir un code d'activation.")
        }

        // Check if Master Code
        if (MASTER_CODES.any { it.equals(trimmed, ignoreCase = true) }) {
            val prefs = getPrefs(context)
            prefs.edit()
                .putBoolean(KEY_IS_MASTER, true)
                .putString(KEY_ACTIVATION_CODE, trimmed)
                .putLong(KEY_ACTIVATION_EXPIRY, Long.MAX_VALUE)
                .putString(KEY_PILOT_NAME, if (pilotName.isNotBlank()) pilotName else "Développeur")
                .apply()
            return Pair(true, "Application déverrouillée en mode Développeur Master !")
        }

        val deviceId = getDeviceId(context)
        val validationResult = verifyKey(deviceId, trimmed)
        if (!validationResult.isValid) {
            return Pair(false, validationResult.errorMessage)
        }

        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_IS_MASTER, false)
            .putString(KEY_ACTIVATION_CODE, trimmed)
            .putLong(KEY_ACTIVATION_EXPIRY, validationResult.expiryTimestampMs)
            .putString(KEY_PILOT_NAME, pilotName)
            .apply()

        return Pair(true, "Application activée avec succès ! (Valide jusqu'au ${formatDate(validationResult.expiryTimestampMs)})")
    }

    /**
     * Reset / Lock the app
     */
    fun resetActivation(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit()
            .remove(KEY_ACTIVATION_CODE)
            .remove(KEY_ACTIVATION_EXPIRY)
            .remove(KEY_IS_MASTER)
            .remove(KEY_PILOT_NAME)
            .apply()
    }

    /**
     * Generates a license key for a given device and duration (used in Admin Generator)
     */
    fun generateKey(targetDeviceId: String, duration: LicenseDuration, customDays: Int? = null): String {
        val cleanDevId = targetDeviceId.trim().uppercase(Locale.ROOT)
        val days = customDays ?: duration.days
        val now = System.currentTimeMillis()
        val expiryMs = now + (days.toLong() * 24L * 60L * 60L * 1000L)
        // Store expiry as days since epoch (hex encoded) for compactness
        val expiryDaysSinceEpoch = (expiryMs / (1000L * 60 * 60 * 24)).toInt()
        val expiryHex = Integer.toHexString(expiryDaysSinceEpoch).uppercase(Locale.ROOT)

        val rawData = "$cleanDevId:${duration.code}:$expiryHex:$SECRET_SALT"
        val signature = sha256(rawData).substring(0, 8).uppercase(Locale.ROOT)

        return "ACT-${duration.code}-$expiryHex-$signature"
    }

    data class KeyValidationResult(
        val isValid: Boolean,
        val expiryTimestampMs: Long = 0L,
        val errorMessage: String = ""
    )

    fun verifyKey(deviceId: String, key: String): KeyValidationResult {
        val cleanDevId = deviceId.trim().uppercase(Locale.ROOT)
        val cleanKey = key.trim().uppercase(Locale.ROOT)

        val parts = cleanKey.split("-")
        if (parts.size != 4 || parts[0] != "ACT") {
            return KeyValidationResult(false, 0L, "Format de clé invalide (doit commencer par ACT-).")
        }

        val durationCode = parts[1]
        val expiryHex = parts[2]
        val providedSig = parts[3]

        val expiryDays = try {
            expiryHex.toInt(16)
        } catch (e: Exception) {
            return KeyValidationResult(false, 0L, "Date de validité invalide dans la clé.")
        }

        val expiryTimestampMs = expiryDays.toLong() * 24L * 60L * 60L * 1000L

        // Check if expired
        if (System.currentTimeMillis() > expiryTimestampMs) {
            return KeyValidationResult(false, expiryTimestampMs, "Cette clé d'activation a expiré.")
        }

        // Recompute expected signature
        val rawData = "$cleanDevId:$durationCode:$expiryHex:$SECRET_SALT"
        val expectedSig = sha256(rawData).substring(0, 8).uppercase(Locale.ROOT)

        if (providedSig != expectedSig) {
            return KeyValidationResult(false, 0L, "Clé d'activation non valide pour cet appareil ($cleanDevId).")
        }

        return KeyValidationResult(true, expiryTimestampMs, "")
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun formatDate(epochMs: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(java.util.Date(epochMs))
    }
}
