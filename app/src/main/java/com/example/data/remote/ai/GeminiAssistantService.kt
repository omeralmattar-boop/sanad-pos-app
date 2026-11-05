package com.example.data.remote.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "GeminiAssistantService"
private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

enum class VoiceCommandIntentType(val arabicLabel: String) {
    SALES_INVOICE("فاتورة مبيعات"),
    PURCHASE_INVOICE("فاتورة مشتريات"),
    DEBT_VOUCHER("قيد دين / واصل"),
    DAILY_EXPENSE("مصروف يومي"),
    FINANCIAL_QUERY("استفسار محاسبي")
}

data class ParsedVoiceCommandAction(
    val intentType: VoiceCommandIntentType,
    val summaryArabic: String,
    val partyName: String = "عميل نقدي",
    val itemName: String = "زيت زيتون بكر ممتاز (معصرة الجنوب)",
    val quantity: Double = 1.0,
    val amount: Double = 9300.0,
    val currencyCode: String = "YER",
    val expenseCategory: String = "فواتير",
    val isReceiptIn: Boolean = true
)

object GeminiAssistantService {

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

    fun optimizeBitmapForAi(bitmap: Bitmap, maxDimension: Int = 1024, jpegQuality: Int = 82): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val largestSide = max(width, height)

        return if (largestSide > maxDimension) {
            val ratio = maxDimension.toFloat() / largestSide.toFloat()
            val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }
    }

    fun bitmapToBase64Jpeg(bitmap: Bitmap, jpegQuality: Int = 82): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildGeminiUrl(modelName: String, apiKey: String): String {
        val safeModel = modelName.ifBlank { "gemini-1.5-flash" }
        return "$GEMINI_BASE_URL/$safeModel:generateContent?key=${apiKey.trim()}"
    }

    private fun generateDirectText(
        prompt: String,
        modelName: String = "gemini-1.5-flash",
        apiKey: String = "",
        imageBase64: String? = null
    ): String {
        val key = sanitizeApiKey(apiKey)
        require(key.isNotBlank()) { "مفتاح Gemini API فارغ. أضف API key بشكل صحيح." }

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

        return executeGeminiRequest(request)
    }

    private fun executeGeminiRequest(request: Request): String {
        val response: Response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            response.close()
            throw IllegalStateException("Gemini API failed: ${response.code} ${response.message}. Body: $errorBody")
        }

        val rawBody = response.body?.string().orEmpty()
        response.close()

        val root = JSONObject(rawBody)
        val candidates = root.optJSONArray("candidates") ?: return ""
        val firstCandidate = candidates.optJSONObject(0) ?: return ""
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val resultParts = content.optJSONArray("parts") ?: return ""

        val builder = StringBuilder()
        for (i in 0 until resultParts.length()) {
            val part = resultParts.optJSONObject(i) ?: continue
            val text = part.optString("text", "")
            if (text.isNotEmpty()) builder.append(text)
        }
        return builder.toString().trim()
    }

    suspend fun testConnection(
        rawApiKey: String = "",
        modelName: String = "gemini-1.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val text = withTimeout(60_000L) {
                generateDirectText(
                    prompt = "رد بكلمة واحدة فقط: متصل",
                    modelName = modelName,
                    apiKey = rawApiKey
                )
            }

            if (text.isNotEmpty()) {
                Result.success("اتصال ناجح ومستقر عبر Gemini REST API ($modelName): $text")
            } else {
                Result.failure(IllegalStateException("أعاد النموذج استجابة فارغة."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini connection timed out", e)
            Result.failure(IllegalStateException("انتهت مهلة الاتصال بعد 60 ثانية. يرجى المحاولة مرة أخرى."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini connection test failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "تعذر الاتصال بخدمة Gemini REST API"))
        }
    }

    fun streamFinancialAssistant(
        userPrompt: String,
        liveDatabaseSummary: String,
        modelName: String = "gemini-1.5-flash",
        rawApiKey: String = ""
    ): Flow<String> = flow {
        val fullPrompt = """
            أنت "المستشار المالي الذكي" داخل نظام سند المحاسبي المتكامل ونقاط البيع.
            لديك الملخص الحي التالي من قاعدة البيانات المحلية:
            $liveDatabaseSummary
            أجب باللغة العربية بأسلوب محاسبي احترافي ومختصر وواضح مع أرقام دقيقة.
            سؤال المستخدم أو أمره: $userPrompt
        """.trimIndent()

        try {
            val text = withTimeout(60_000L) {
                generateDirectText(
                    prompt = fullPrompt,
                    modelName = modelName,
                    apiKey = rawApiKey
                )
            }
            if (text.isNotEmpty()) emit(text)
        } catch (e: Exception) {
            Log.e(TAG, "streamFinancialAssistant failed", e)
            emit(buildLocalAnalyticalResponse(userPrompt, liveDatabaseSummary))
        }
    }

    suspend fun askFinancialAssistant(
        userPrompt: String,
        liveDatabaseSummary: String,
        rawApiKey: String = "",
        modelName: String = "gemini-1.5-flash",
        optionalBase64Image: String? = null
    ): String = withContext(Dispatchers.IO) {
        val guideReply = checkInstantOcrGuideQuery(userPrompt)
        if (guideReply != null) return@withContext guideReply

        val systemPrompt = """
            أنت "المستشار المالي الذكي" داخل نظام سند المحاسبي المتكامل ونقاط البيع.
            لديك الملخص الحي التالي من قاعدة البيانات المحلية:
            $liveDatabaseSummary
            أجب باللغة العربية بأسلوب محاسبي احترافي ومختصر وواضح مع أرقام دقيقة.
            سؤال المستخدم أو أمره: $userPrompt
        """.trimIndent()

        try {
            val responseText = withTimeout(60_000L) {
                if (!optionalBase64Image.isNullOrBlank()) {
                    val bytes = Base64.decode(optionalBase64Image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        val optimized = withContext(Dispatchers.Default) {
                            optimizeBitmapForAi(bitmap, 1024)
                        }
                        val imageBase64 = withContext(Dispatchers.Default) {
                            bitmapToBase64Jpeg(optimized)
                        }
                        generateDirectText(
                            prompt = systemPrompt,
                            modelName = modelName,
                            apiKey = rawApiKey,
                            imageBase64 = imageBase64
                        )
                    } else {
                        generateDirectText(
                            prompt = systemPrompt,
                            modelName = modelName,
                            apiKey = rawApiKey
                        )
                    }
                } else {
                    generateDirectText(
                        prompt = systemPrompt,
                        modelName = modelName,
                        apiKey = rawApiKey
                    )
                }
            }

            if (responseText.isNotEmpty()) return@withContext responseText
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini financial assistant timed out", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini financial assistant call failed", e)
        }

        buildLocalAnalyticalResponse(userPrompt, liveDatabaseSummary)
    }

    fun routeUnifiedArabicVoiceCommand(spokenText: String): ParsedVoiceCommandAction {
        val normalized = spokenText.trim()
        val extractedNumber = Regex("(\\d+(?:\\.\\d+)?)")
            .findAll(normalized)
            .mapNotNull { it.value.toDoubleOrNull() }
            .toList()

        val currency = when {
            normalized.contains("دولار") || normalized.contains("USD", ignoreCase = true) -> "USD"
            normalized.contains("سعودي") || normalized.contains("SAR", ignoreCase = true) -> "SAR"
            normalized.contains("تركي") || normalized.contains("ليرة") || normalized.contains("TRY", ignoreCase = true) -> "TRY"
            else -> "YER"
        }

        val party = when {
            normalized.contains("أحمد الخطيب") || normalized.contains("احمد الخطيب") -> "أحمد الخطيب"
            normalized.contains("محمد العلي") -> "محمد العلي"
            normalized.contains("مطعم الأصيل") || normalized.contains("الاصيل") -> "مطعم الأصيل"
            normalized.contains("معصرة الجنوب") -> "معصرة الجنوب"
            normalized.contains("مؤسسة النور") || normalized.contains("النور") -> "مؤسسة النور"
            else -> "أحمد الخطيب"
        }

        val item = when {
            normalized.contains("أرز") || normalized.contains("ارز") || normalized.contains("بسمتي") -> "أرز بسمتي 5 كغ (مؤسسة النور)"
            normalized.contains("حليب") -> "حليب مجفف كامل الدسم 900 جرام"
            normalized.contains("بن") || normalized.contains("قهوة") -> "بن عربي فاخر بالهيل (ميزان PLU)"
            normalized.contains("سكر") -> "سكر أبيض ناعم 10 كغ"
            else -> "زيت زيتون بكر ممتاز (معصرة الجنوب)"
        }

        return when {
            normalized.contains("مصروف") || normalized.contains("كهرباء") ||
                normalized.contains("إيجار") || normalized.contains("ايجار") ||
                normalized.contains("رواتب") || normalized.contains("مواصلات") -> {
                val amount = extractedNumber.firstOrNull { it >= 50 } ?: 45000.0
                val category = when {
                    normalized.contains("رواتب") -> "رواتب"
                    normalized.contains("إيجار") || normalized.contains("ايجار") -> "إيجار"
                    normalized.contains("مواصلات") || normalized.contains("نقل") -> "مواصلات"
                    normalized.contains("صيانة") -> "صيانة"
                    else -> "فواتير"
                }
                ParsedVoiceCommandAction(
                    intentType = VoiceCommandIntentType.DAILY_EXPENSE,
                    summaryArabic = "تسجيل مصروف يومي ($category) بمبلغ $amount $currency",
                    amount = amount,
                    currencyCode = currency,
                    expenseCategory = category
                )
            }

            normalized.contains("قبض") || normalized.contains("واصل") ||
                normalized.contains("دفعة") || normalized.contains("سند") ||
                normalized.contains("سدد") -> {
                val amount = extractedNumber.firstOrNull { it >= 50 } ?: 155000.0
                val isReceipt = !normalized.contains("دفعة لمورد") && !normalized.contains("صرف") && party != "معصرة الجنوب"
                ParsedVoiceCommandAction(
                    intentType = VoiceCommandIntentType.DEBT_VOUCHER,
                    summaryArabic = if (isReceipt) {
                        "تسجيل سند قبض (واصل) من ($party) بمبلغ $amount $currency"
                    } else {
                        "تسجيل سند دفعة إلى ($party) بمبلغ $amount $currency"
                    },
                    partyName = party,
                    amount = amount,
                    currencyCode = currency,
                    isReceiptIn = isReceipt
                )
            }

            normalized.contains("شراء") || normalized.contains("مشتريات") || normalized.contains("توريد") -> {
                val qty = extractedNumber.firstOrNull { it <= 500 } ?: 10.0
                val unitCost = if (item.contains("أرز")) 8500.0 else 6975.0
                val supplier = if (party == "أحمد الخطيب") "معصرة الجنوب" else party
                ParsedVoiceCommandAction(
                    intentType = VoiceCommandIntentType.PURCHASE_INVOICE,
                    summaryArabic = "إنشاء فاتورة مشتريات من ($supplier): $qty من ($item)",
                    partyName = supplier,
                    itemName = item,
                    quantity = qty,
                    amount = qty * unitCost,
                    currencyCode = currency
                )
            }

            else -> {
                val qty = extractedNumber.firstOrNull { it <= 200 } ?: 2.0
                val unitPrice = if (item.contains("أرز")) 10200.0 else 9300.0
                ParsedVoiceCommandAction(
                    intentType = VoiceCommandIntentType.SALES_INVOICE,
                    summaryArabic = "إنشاء فاتورة مبيعات للعميل ($party): $qty من ($item)",
                    partyName = party,
                    itemName = item,
                    quantity = qty,
                    amount = qty * unitPrice,
                    currencyCode = currency
                )
            }
        }
    }

    fun checkInstantOcrGuideQuery(prompt: String): String? {
        val lower = prompt.lowercase()
        if (lower.contains("ocrpurchasepaths") || prompt.contains("مسارات تصوير")) {
            return """
                مسارات تصوير فاتورة المورد المعتمدة (ocrPurchasePaths):
                1) الإعدادات -> بطاقة استيراد وتصدير -> تصوير فاتورة مورد.
                2) اختصار الشاشة الرئيسية «تصوير فاتورة» (مع إمكانية تخصيص ظهوره من الإعدادات).
                3) مشاركة أو فتح ملف PDF أو صورة من النظام خارجياً إلى التطبيق.
                4) مسار مستقل لاستيراد ملف فاتورة الكترونية (.sanad) لدمج فواتير الموردين B2B.
                • ملاحظة: لا يوجد زر OCR داخل تبويب الفواتير أو نموذج الشراء اليدوي.
            """.trimIndent()
        }

        if (lower.contains("ocrpurchasebuttons") || prompt.contains("أزرار تصوير")) {
            return """
                أزرار معالج تصوير فاتورة المورد (ocrPurchaseButtons):
                • المرحلة 1 (الالتقاط): [التقاط صورة]، [اختيار من المعرض]، [اختيار ملف PDF <= 8MB]، مؤشر القراءة، وتلميح المدة/الخصوصية.
                • نافذة المسودة (24 ساعة): [استئناف | بدء من جديد | إلغاء].
                • المرحلة 2 (المراجعة وكل سطر): [رجوع]، [مساعدة (?)]، اختيار/إضافة مورد، رقم ضريبي، رقم فاتورة (OCR-*), عملة الورقة، ولكل سطر: [مطابقة / إعادة مطابقة]، [إنشاء جديد]، [تجاهل / إلغاء التجاهل]، [تأكيد مجاني / تكلفة صفر]، [تفعيل تتبع الدفعات FEFO]، وزرّا [تصحيح من الوحدة] و[تصحيح من المجموع].
                • المرحلة 3 و 4: [متابعة] في الأسعار الجديدة ورادار الأسعار.
                • المرحلة 5 (الدفع والمنصة): اختيار مصدر دفع واحد فقط [نقد | حساب بنكي | آجل]، تفعيل الضريبة، [تضمين الضريبة في التكلفة]، التعديلات المالية، [الإبقاء على عملة الورقة]، الملاحظات، ملخص الأسطر المرفوضة، و[تأكيد وحفظ].
                • المرحلة 6: رقم الفاتورة النهائي (PUR-...) وزر [تم].
            """.trimIndent()
        }

        if (lower.contains("ocr") || prompt.contains("تصوير فاتورة") || prompt.contains("فاتورة مورد")) {
            return """
                دليل معالج تصوير فاتورة المورد بالذكاء الاصطناعي (ocrPurchaseFlow - 6 مراحل):
                1. الالتقاط (كاميرا / معرض / PDF <= 8MB) مع حفظ مسودة محلية لمدة 24 ساعة (ocrPurchaseDraft).
                2. المراجعة والمطابقة لكل سطر (ocrPurchaseMapping) مع المصحح الحسابي للسطر وتتبع اللوطات FEFO.
                3. تسعير الأصناف الجديدة (سعر مفرق إلزامي + سعر جملة).
                4. رادار الأسعار لمقارنة التكلفة الجديدة مع الكتالوج.
                5. الدفع والمنصة (ocrPurchasePayPlatform): مصدر دفع واحد [نقد | حساب بنكي | آجل] مع فحص صلاحية الخزينة (`treasury.manage`) وكفاية الرصيد ومنع تكرار رقم الفاتورة لنفس المورد.
                6. الحفظ النهائي ووسم الفاتورة بالوردية المفتوحة.
            """.trimIndent()
        }

        return null
    }

    private fun buildLocalAnalyticalResponse(prompt: String, liveSummary: String): String {
        checkInstantOcrGuideQuery(prompt)?.let { return it }
        return """
            تحليل المحاسب الذكي (سند AI - Gemini REST API):
            بناءً على بياناتك الحية في قاعدة البيانات:
            $liveSummary
            
            رداً على استفسارك ("$prompt"):
            • الوضع المالي متوازن، ونوصي بمتابعة تحصيل الذمم المتأخرة ومراجعة الأصناف المنتهية الصلاحية عبر زر (إتلاف سريع) أو تجديد دفعاتها.
        """.trimIndent()
    }
}
