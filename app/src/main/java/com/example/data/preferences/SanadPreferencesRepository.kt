package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sanadDataStore: DataStore<Preferences> by preferencesDataStore(name = "sanad_settings_prefs")

data class SanadAppSettings(
    val storeName: String = "مؤسسة سند التجارية الشاملة",
    val storePhone: String = "0944112233",
    val storeAddress: String = "المركز الرئيسي - الشارع التجاري العام",
    val taxRegistrationNumber: String = "TRN-900481726",
    val receiptFooterMessage: String = "شكراً لتعاملكم معنا - البضاعة المباعة ترد وتستبدل خلال 3 أيام",
    val thermalPaperWidthMm: Int = 80, // 58 or 80
    val autoPrintAfterSave: Boolean = true,
    val geminiApiKey: String = "",
    val geminiModelName: String = "gemini-3.8-flash",
    val pinLockEnabled: Boolean = false,
    val pinCode: String = "1234",
    val biometricEnabled: Boolean = false,
    val showHomeOcrShortcut: Boolean = true,
    val lastBackupTimestamp: Long = 0L
)

class SanadPreferencesRepository(private val context: Context) {

    private object Keys {
        val STORE_NAME = stringPreferencesKey("store_name")
        val STORE_PHONE = stringPreferencesKey("store_phone")
        val STORE_ADDRESS = stringPreferencesKey("store_address")
        val TAX_REG_NUMBER = stringPreferencesKey("tax_reg_number")
        val RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")
        val THERMAL_WIDTH = intPreferencesKey("thermal_width_mm")
        val AUTO_PRINT = booleanPreferencesKey("auto_print")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_CODE = stringPreferencesKey("pin_code")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val SHOW_HOME_OCR_SHORTCUT = booleanPreferencesKey("show_home_ocr_shortcut")
    }

    val settingsFlow: Flow<SanadAppSettings> = context.sanadDataStore.data.map { prefs ->
        SanadAppSettings(
            storeName = prefs[Keys.STORE_NAME] ?: "مؤسسة سند التجارية الشاملة",
            storePhone = prefs[Keys.STORE_PHONE] ?: "0944112233",
            storeAddress = prefs[Keys.STORE_ADDRESS] ?: "المركز الرئيسي - الشارع التجاري العام",
            taxRegistrationNumber = prefs[Keys.TAX_REG_NUMBER] ?: "TRN-900481726",
            receiptFooterMessage = prefs[Keys.RECEIPT_FOOTER] ?: "شكراً لتعاملكم معنا - البضاعة المباعة ترد وتستبدل خلال 3 أيام",
            thermalPaperWidthMm = prefs[Keys.THERMAL_WIDTH] ?: 80,
            autoPrintAfterSave = prefs[Keys.AUTO_PRINT] ?: true,
            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
            geminiModelName = prefs[Keys.GEMINI_MODEL] ?: "gemini-3.8-flash",
            pinLockEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            pinCode = prefs[Keys.PIN_CODE] ?: "1234",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            showHomeOcrShortcut = prefs[Keys.SHOW_HOME_OCR_SHORTCUT] ?: true
        )
    }

    suspend fun updateStoreProfile(
        storeName: String,
        storePhone: String,
        storeAddress: String,
        taxNumber: String,
        receiptFooter: String
    ) {
        context.sanadDataStore.edit { prefs ->
            prefs[Keys.STORE_NAME] = storeName
            prefs[Keys.STORE_PHONE] = storePhone
            prefs[Keys.STORE_ADDRESS] = storeAddress
            prefs[Keys.TAX_REG_NUMBER] = taxNumber
            prefs[Keys.RECEIPT_FOOTER] = receiptFooter
        }
    }

    suspend fun updatePrinterSettings(paperWidthMm: Int, autoPrint: Boolean) {
        context.sanadDataStore.edit { prefs ->
            prefs[Keys.THERMAL_WIDTH] = paperWidthMm
            prefs[Keys.AUTO_PRINT] = autoPrint
        }
    }

    suspend fun updateGeminiConfig(apiKey: String, modelName: String) {
        context.sanadDataStore.edit { prefs ->
            prefs[Keys.GEMINI_API_KEY] = apiKey.trim()
            prefs[Keys.GEMINI_MODEL] = modelName
        }
    }

    suspend fun updateSecuritySettings(pinEnabled: Boolean, pinCode: String, biometricEnabled: Boolean) {
        context.sanadDataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = pinEnabled
            prefs[Keys.PIN_CODE] = pinCode
            prefs[Keys.BIOMETRIC_ENABLED] = biometricEnabled
        }
    }

    suspend fun updateShowHomeOcrShortcut(show: Boolean) {
        context.sanadDataStore.edit { prefs ->
            prefs[Keys.SHOW_HOME_OCR_SHORTCUT] = show
        }
    }
}
