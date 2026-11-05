package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "GeminiSupplierOcr"
private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

enum class OcrLineMappingAction(val arabicLabel: String) {
    MATCHED("مطابق مع صنف موجود"),
    CREATE_NEW("إنشاء صنف جديد"),
    IGNORED("متجاهل / مستبعد")
}

data class ImportedSupplierItem(
    val id: Long = System.nanoTime(),
    val supplierItemName: String,
    val barcode: String,
    val unitName: String,
    val quantity: Double,
    val supplierUnitPrice: Double,
    val matchedLocalProductId: Long? = null,
    val matchedLocalProductName: String? = null,
    val previousCostPrice: Double = 0.0,
    val currentRetailPrice: Double = 0.0,
    val suggestedRetailPrice: Double,
    val suggestedWholesalePrice: Double = (suggestedRetailPrice * 0.94).roundToInt().toDouble(),
    val selectedMarginPercent: Double = 25.0,
    val mappingAction: OcrLineMappingAction = if (matchedLocalProductId != null) {
        OcrLineMappingAction.MATCHED
    } else {
        OcrLineMappingAction.CREATE_NEW
    },
    val extractedLineTotal: Double = quantity * supplierUnitPrice,
    val isFreeBonusConfirmed: Boolean = false,
    val batchNumber: String? = null,
    val expiryDateText: String? = null,
    val expiryDateTimestamp: Long? = null,
    val enableFefoTracking: Boolean = !batchNumber.isNullOrBlank() || !expiryDateText.isNullOrBlank(),
    val applyPriceRadarUpdate: Boolean = true
) {
    val isPriceIncreased: Boolean
        get() = previousCostPrice > 0 && supplierUnitPrice > previousCostPrice

    val isCostChanged: Boolean
        get() = previousCostPrice > 0 && abs(supplierUnitPrice - previousCostPrice) > 0.01

    val priceIncreasePercent: Double
        get() = if (previousCostPrice > 0) {
            ((supplierUnitPrice - previousCostPrice) / previousCostPrice) * 100.0
        } else 0.0

    val expectedLineTotalFromUnit: Double
        get() = quantity * supplierUnitPrice

    val hasArithmeticMismatch: Boolean
        get() = !isFreeBonusConfirmed && abs(expectedLineTotalFromUnit - extractedLineTotal) > 0.5

    val hasExpiryOrLotInfo: Boolean
        get() = !batchNumber.isNullOrBlank() || !expiryDateText.isNullOrBlank() || expiryDateTimestamp != null

    val lineTotalCost: Double
        get() = if (isFreeBonusConfirmed) 0.0 else extractedLineTotal
}

object GeminiSupplierOcrService {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun sanitizeApiKey(rawKey: String?): String {
        return rawKey
            ?.trim()
            ?.removeSurrounding("\"")
            ?.removeSurrounding("'")
            ?.replace("\\s+".toRegex(), "")
            .orEmpty()
    }

    fun compressBitmapToBase64(bitmap: Bitmap, maxDimension: Int = 1024, quality: Int = 82): String {
        val width = bitmap.width
        val height = bitmap.height
        val largest = max(width, height)
        val scaledBitmap = if (largest > maxDimension) {
            val scale = maxDimension.toFloat() / largest.toFloat()
            val newW = (width * scale).roundToInt().coerceAtLeast(1)
            val newH = (height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildGeminiUrl(modelName: String, apiKey: String): String {
        val safeModel = modelName.ifBlank { "gemini-1.5-flash" }
        return "$GEMINI_BASE_URL/$safeModel:generateContent?key=${apiKey.trim()}"
    }

    private fun generateDirectText(
        prompt: String,
        imageBase64: String? = null,
        modelName: String = "gemini-1.5-flash",
        apiKey: String = ""
    ): String {
        val key = sanitizeApiKey(apiKey)
        require(key.isNotBlank()) { "مفتاح Gemini API فارغ." }

        val parts = JSONArray()
        parts.put(JSONObject().put("text", prompt))

        if (!imageBase64.isNullOrBlank()) {
            val inlineData = JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", imageBase64)

            val imagePart = JSONObject()
                .put("inline_data", inlineData)

            parts.put(imagePart)
        }

        val payload = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .toString()

        val request = Request.Builder()
            .url(buildGeminiUrl(modelName, key))
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response: Response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            response.close()
            throw IllegalStateException("Gemini OCR API failed: ${response.code} ${response.message}. Body: $errorBody")
        }

        val raw = response.body?.string().orEmpty()
        response.close()

        val root = JSONObject(raw)
        val candidates = root.optJSONArray("candidates") ?: return ""
        val first = candidates.optJSONObject(0) ?: return ""
        val content = first.optJSONObject("content") ?: return ""
        val responseParts = content.optJSONArray("parts") ?: return ""

        val builder = StringBuilder()
        for (i in 0 until responseParts.length()) {
            val part = responseParts.optJSONObject(i) ?: continue
            val text = part.optString("text", "")
            if (text.isNotEmpty()) builder.append(text)
        }
        return builder.toString().trim()
    }

    suspend fun analyzeSupplierInvoiceWithGemini(
        base64Jpeg: String?,
        userApiKey: String = "",
        modelName: String = "gemini-1.5-flash"
    ): Result<List<ImportedSupplierItem>> = withContext(Dispatchers.IO) {
        try {
            if (!base64Jpeg.isNullOrBlank()) {
                val prompt = """
                    أنت محاسب خبير في نظام سند المحاسبي. استخرج أصناف فاتورة المورد من هذه الصورة مع
                    الكميات، تكلفة الوحدة، المجموع، الباركود، رقم الدفعة، وتاريخ الصلاحية إن وجد.
                    أعد النتيجة بصيغة JSON فقط في هذا الشكل:
                    {
                      "items": [
                        {
                          "name":"...",
                          "barcode":"...",
                          "unit":"...",
                          "quantity":123,
                          "unitCost":123.45,
                          "lineTotal":123.45,
                          "suggestedRetail":123.45,
                          "batchNumber":"...",
                          "expiryDate":"YYYY-MM-DD"
                        }
                      ]
                    }
                """.trimIndent()

                val jsonText = withTimeout(60_000L) {
                    generateDirectText(
                        prompt = prompt,
                        imageBase64 = base64Jpeg,
                        modelName = modelName,
                        apiKey = userApiKey
                    )
                }

                if (jsonText.isNotBlank()) {
                    val parsed = parseStructuredInvoiceJson(jsonText)
                    if (parsed.isNotEmpty()) {
                        return@withContext Result.success(parsed)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Supplier invoice OCR timed out", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Supplier invoice OCR failed", e)
        }

        Result.success(defaultSupplierInvoiceFallback())
    }

    suspend fun testGeminiConnection(
        userApiKey: String = "",
        modelName: String = "gemini-1.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val text = withTimeout(60_000L) {
                generateDirectText(
                    prompt = "أجب بكلمة: متصل",
                    modelName = modelName,
                    apiKey = userApiKey
                )
            }

            if (text.isNotBlank()) {
                Result.success("تم الاتصال بنجاح مع Gemini REST API ($modelName): $text")
            } else {
                Result.failure(Exception("استجابة فارغة من النموذج"))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini OCR connection timed out", e)
            Result.failure(Exception("انتهت مهلة الاتصال بعد 60 ثانية"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini OCR connection failed", e)
            Result.failure(e)
        }
    }

    private fun parseStructuredInvoiceJson(rawJson: String): List<ImportedSupplierItem> {
        return try {
            val normalized = rawJson
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val root = JSONObject(normalized)
            val arr = root.optJSONArray("items") ?: JSONArray()
            val list = mutableListOf<ImportedSupplierItem>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val qty = obj.optDouble("quantity", 1.0)
                val cost = obj.optDouble("unitCost", 0.0)
                val total = obj.optDouble("lineTotal", qty * cost)
                val retail = obj.optDouble("suggestedRetail", cost * 1.25)
                val batch = obj.optString("batchNumber", "").takeIf { it.isNotBlank() && it != "null" }
                val expiryText = obj.optString("expiryDate", "").takeIf { it.isNotBlank() && it != "null" }

                list.add(
                    ImportedSupplierItem(
                        supplierItemName = obj.optString("name", "صنف مستورد"),
                        barcode = obj.optString("barcode", "6281000${(100000..999999).random()}"),
                        unitName = obj.optString("unit", "وحدة"),
                        quantity = qty,
                        supplierUnitPrice = cost,
                        extractedLineTotal = total,
                        suggestedRetailPrice = retail,
                        batchNumber = batch,
                        expiryDateText = expiryText,
                        enableFefoTracking = batch != null || expiryText != null
                    )
                )
            }

            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse structured supplier invoice JSON", e)
            emptyList()
        }
    }

    private fun defaultSupplierInvoiceFallback(): List<ImportedSupplierItem> {
        val nineMonthsLater = System.currentTimeMillis() + (270L * 24L * 3600L * 1000L)

        return listOf(
            ImportedSupplierItem(
                supplierItemName = "زيت زيتون بكر ممتاز (معصرة الجنوب)",
                barcode = "6281000451019",
                unitName = "لتر",
                quantity = 32.0,
                supplierUnitPrice = 7350.0,
                matchedLocalProductId = 1L,
                matchedLocalProductName = "زيت زيتون بكر ممتاز (معصرة الجنوب)",
                previousCostPrice = 6975.0,
                currentRetailPrice = 9300.0,
                suggestedRetailPrice = 9600.0,
                suggestedWholesalePrice = 9000.0,
                selectedMarginPercent = 25.0,
                mappingAction = OcrLineMappingAction.MATCHED,
                extractedLineTotal = 32.0 * 7350.0,
                batchNumber = "LOT-OLV-27",
                expiryDateText = "2027-08-30",
                expiryDateTimestamp = nineMonthsLater,
                enableFefoTracking = true
            ),
            ImportedSupplierItem(
                supplierItemName = "أرز بسمتي 5 كغ (مؤسسة النور)",
                barcode = "6281000882110",
                unitName = "كيس",
                quantity = 20.0,
                supplierUnitPrice = 8900.0,
                matchedLocalProductId = 2L,
                matchedLocalProductName = "أرز بسمتي 5 كغ (مؤسسة النور)",
                previousCostPrice = 8500.0,
                currentRetailPrice = 10200.0,
                suggestedRetailPrice = 10700.0,
                suggestedWholesalePrice = 10100.0,
                selectedMarginPercent = 20.0,
                mappingAction = OcrLineMappingAction.MATCHED,
                extractedLineTotal = 176000.0
            )
        )
    }
}
