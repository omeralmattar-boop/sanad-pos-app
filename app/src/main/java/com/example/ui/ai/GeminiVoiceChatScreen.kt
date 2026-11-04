package com.example.ui.ai

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PartyType
import com.example.data.local.VoucherDirection
import com.example.data.remote.ai.GeminiAssistantService
import com.example.data.remote.ai.ParsedVoiceCommandAction
import com.example.data.remote.ai.VoiceCommandIntentType
import com.example.ui.components.SanadBadge
import com.example.ui.components.formatMoney
import com.example.ui.theme.SanadAmberContainer
import com.example.ui.theme.SanadAmberWarning
import com.example.ui.theme.SanadBlueContainer
import com.example.ui.theme.SanadBorder
import com.example.ui.theme.SanadCoralRed
import com.example.ui.theme.SanadElectricBlue
import com.example.ui.theme.SanadEmeraldContainer
import com.example.ui.theme.SanadEmeraldGreen
import com.example.ui.theme.SanadNavyDark
import com.example.ui.theme.SanadSurface
import com.example.ui.theme.SanadSurfaceElevated
import com.example.ui.theme.SanadTextMuted
import com.example.ui.theme.SanadTextPrimary
import com.example.ui.theme.SanadTextSecondary
import com.example.ui.viewmodel.SanadViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

data class AiChatMessage(
    val id: Long = System.nanoTime(),
    val isUser: Boolean,
    val text: String,
    val parsedVoiceAction: ParsedVoiceCommandAction? = null
)

@Composable
fun GeminiVoiceChatScreen(
    viewModel: SanadViewModel,
    geminiApiKey: String,
    geminiModelName: String,
    onModelChange: (String) -> Unit,
    onNavigateToInvoiceEditor: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val products by viewModel.products.collectAsState()
    val invoices by viewModel.completedInvoices.collectAsState()
    val parties by viewModel.parties.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val accounts by viewModel.financialAccounts.collectAsState()

    // Build Live Database Summary for Context-Aware Gemini Answers
    val liveSummary = remember(products, invoices, parties, expenses, accounts) {
        val totalSalesYer = invoices.sumOf { it.invoice.netTotalAmount }
        val totalProfitYer = invoices.sumOf { it.invoice.realizedProfitAmount }
        val receivablesYer = parties.filter { it.partyType == PartyType.CUSTOMER }.sumOf { abs(it.balanceInPrimaryCurrency) }
        val payablesYer = parties.filter { it.partyType == PartyType.SUPPLIER }.sumOf { abs(it.balanceInPrimaryCurrency) }
        val cashYer = accounts.filter { it.currencyCode == "YER" }.sumOf { it.currentBalance }
        "إجمالي المبيعات: ${formatMoney(totalSalesYer, "ر.ي")} | صافي الربح: ${formatMoney(totalProfitYer, "ر.ي")} | ديون لنا: ${formatMoney(receivablesYer, "ر.ي")} | ديون علينا: ${formatMoney(payablesYer, "ر.ي")} | السيولة بالريال: ${formatMoney(cashYer, "ر.ي")} | عدد الأصناف: ${products.size}"
    }

    var ttsEnabled by remember { mutableStateOf(true) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.forLanguageTag("ar")
            }
        }
        ttsEngine = instance
        onDispose {
            instance?.stop()
            instance?.shutdown()
        }
    }

    fun speakArabicReply(text: String) {
        if (ttsEnabled) {
            ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sanad_ai_tts")
        }
    }

    val messages = remember {
        mutableStateListOf(
            AiChatMessage(
                isUser = false,
                text = "مرحباً بك في المحاسب الصوتي الذكي (سند AI)!\nيمكنك التحدث أو الكتابة لإصدار (فاتورة مبيعات، فاتورة مشتريات، سند قبض/دفعة، أو مصروف يومي) أو السؤال عن الأرباح والديون والمخزون."
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    fun handleUserCommandOrQuestion(query: String, treatAsVoiceRouter: Boolean) {
        if (query.isBlank()) return
        messages.add(AiChatMessage(isUser = true, text = query))
        inputText = ""

        if (treatAsVoiceRouter ||
            query.startsWith("سجل ") ||
            query.startsWith("بيع ") ||
            query.startsWith("شراء ") ||
            query.contains("فاتورة") ||
            query.contains("مصروف") ||
            query.contains("واصل") ||
            query.contains("قبض")
        ) {
            val routed = GeminiAssistantService.routeUnifiedArabicVoiceCommand(query)
            val reply = "تم تحليل الأمر الصوتي عبر الموجه الشامل:\n• النوع: ${routed.intentType.arabicLabel}\n• التفاصيل: ${routed.summaryArabic}\nاضغط على (تنفيذ العملية فوراً) لاعتمادها في قاعدة البيانات."
            messages.add(
                AiChatMessage(
                    isUser = false,
                    text = reply,
                    parsedVoiceAction = routed
                )
            )
            speakArabicReply(routed.summaryArabic)
        } else {
            isThinking = true
            scope.launch {
                val reply = GeminiAssistantService.askFinancialAssistant(
                    userPrompt = query,
                    liveDatabaseSummary = liveSummary,
                    rawApiKey = geminiApiKey,
                    modelName = geminiModelName
                )
                messages.add(AiChatMessage(isUser = false, text = reply))
                isThinking = false
                speakArabicReply(reply)
            }
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                handleUserCommandOrQuestion(spoken, treatAsVoiceRouter = true)
            }
        }
    }

    fun executeParsedAction(action: ParsedVoiceCommandAction) {
        when (action.intentType) {
            VoiceCommandIntentType.SALES_INVOICE -> {
                val prod = products.firstOrNull { it.name.contains(action.itemName.take(8)) } ?: products.firstOrNull()
                if (prod != null) {
                    viewModel.executeVoiceInvoiceCommand(
                        isSale = true,
                        partyName = action.partyName,
                        product = prod,
                        quantity = action.quantity,
                        currencyCode = action.currencyCode
                    )
                    onNavigateToInvoiceEditor()
                }
            }
            VoiceCommandIntentType.PURCHASE_INVOICE -> {
                val prod = products.firstOrNull { it.name.contains(action.itemName.take(8)) } ?: products.firstOrNull()
                if (prod != null) {
                    viewModel.executeVoiceInvoiceCommand(
                        isSale = false,
                        partyName = action.partyName,
                        product = prod,
                        quantity = action.quantity,
                        currencyCode = action.currencyCode
                    )
                    onNavigateToInvoiceEditor()
                }
            }
            VoiceCommandIntentType.DEBT_VOUCHER -> {
                val party = parties.firstOrNull { it.name.contains(action.partyName) } ?: parties.firstOrNull()
                val acc = accounts.firstOrNull { it.currencyCode == action.currencyCode } ?: accounts.firstOrNull()
                viewModel.registerVoucher(
                    partyType = party?.partyType ?: PartyType.CUSTOMER,
                    partyId = party?.id ?: 1L,
                    partyName = party?.name ?: action.partyName,
                    direction = if (action.isReceiptIn) VoucherDirection.RECEIPT_IN else VoucherDirection.PAYMENT_OUT,
                    currencyCode = action.currencyCode,
                    lockedRate = if (action.currencyCode == "USD") 1.0 else 1550.0,
                    amount = action.amount,
                    accountId = acc?.id ?: 1L,
                    accountName = acc?.name ?: "الصندوق النقدي الرئيسي",
                    notes = "سند منفذ بالأمر الصوتي الذكي"
                )
            }
            VoiceCommandIntentType.DAILY_EXPENSE -> {
                val acc = accounts.firstOrNull { it.currencyCode == action.currencyCode } ?: accounts.firstOrNull()
                viewModel.registerExpense(
                    title = action.summaryArabic,
                    category = action.expenseCategory,
                    currencyCode = action.currencyCode,
                    lockedRate = if (action.currencyCode == "USD") 1.0 else 1550.0,
                    amount = action.amount,
                    accountId = acc?.id ?: 1L,
                    accountName = acc?.name ?: "الصندوق النقدي الرئيسي",
                    supplierPartyId = null,
                    supplierPartyName = null,
                    notes = "مصروف منفذ بالأمر الصوتي الذكي"
                )
            }
            VoiceCommandIntentType.FINANCIAL_QUERY -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
    ) {
        // Top Header with Model Switcher (gemini-2.5-pro / gemini-2.5-flash) & TTS Toggle
        Surface(
            color = SanadSurfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = SanadElectricBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "المحاسب الذكي والأمر الصوتي الشامل",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SanadTextPrimary
                            )
                            Text(
                                text = "موجه أوامر صوتية + تحليل مالي حي + نطق صوتي TTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SanadEmeraldGreen
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { ttsEnabled = !ttsEnabled }) {
                            Icon(
                                imageVector = if (ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "تبديل النطق الصوتي",
                                tint = if (ttsEnabled) SanadEmeraldGreen else SanadTextSecondary
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Model Toggle Strip + Live DB Summary Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("gemini-3.8-flash", "gemini-3.5-flash-lite").forEach { model ->
                            val selected = geminiModelName == model
                            Surface(
                                color = if (selected) SanadElectricBlue else SanadSurface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(1.dp, if (selected) SanadElectricBlue else SanadBorder, RoundedCornerShape(8.dp))
                                    .clickable { onModelChange(model) }
                            ) {
                                Text(
                                    text = model,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) Color.White else SanadTextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    SanadBadge(
                        text = "متصل بقاعدة البيانات الحية",
                        textColor = SanadEmeraldGreen,
                        containerColor = SanadEmeraldContainer
                    )
                }
            }
        }

        HorizontalDivider(color = SanadBorder)

        // Preset Arabic Voice Commands Strip for Instant Testing
        Surface(
            color = SanadSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = "أوامر صوتية فورية (انقر للتجربة أو اضغط زر الميكروفون 🎙️):",
                    style = MaterialTheme.typography.labelSmall,
                    color = SanadAmberWarning
                )
                Spacer(modifier = Modifier.height(6.dp))
                val quickVoiceCommands = listOf(
                    "بيع 4 زيت زيتون للعميل أحمد الخطيب",
                    "شراء 10 أرز بسمتي من مؤسسة النور",
                    "سجل سند قبض واصل من محمد العلي 155000 ريال",
                    "سجل مصروف كهرباء 45000 ريال",
                    "ما هو صافي الأرباح والديون المتأخرة اليوم؟"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickVoiceCommands.forEach { cmd ->
                        Surface(
                            color = SanadNavyDark,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    handleUserCommandOrQuestion(
                                        query = cmd,
                                        treatAsVoiceRouter = !cmd.contains("؟")
                                    )
                                }
                        ) {
                            Text(
                                text = cmd,
                                style = MaterialTheme.typography.labelMedium,
                                color = SanadTextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = SanadBorder)

        // Chat Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.Start else Arrangement.End
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) SanadBlueContainer else SanadSurface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .border(
                                1.dp,
                                if (msg.isUser) SanadElectricBlue else SanadBorder,
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SanadTextPrimary
                            )

                            msg.parsedVoiceAction?.let { action ->
                                Button(
                                    onClick = { executeParsedAction(action) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("execute_voice_action_button")
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تنفيذ العملية فوراً (${action.intentType.arabicLabel})",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = SanadElectricBlue,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "جاري تحليل البيانات المالية عبر Gemini...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SanadTextSecondary
                        )
                    }
                }
            }
        }

        // Bottom Voice Mic + Text Input Bar
        Surface(
            color = SanadSurfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Voice Mic Button 🎙️
                Surface(
                    color = SanadEmeraldGreen,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن لإصدار فاتورة أو سند أو مصروف...")
                            }
                            runCatching { speechRecognizerLauncher.launch(intent) }
                                .onFailure {
                                    handleUserCommandOrQuestion(
                                        "بيع 2 زيت زيتون للعميل أحمد الخطيب",
                                        treatAsVoiceRouter = true
                                    )
                                }
                        }
                        .testTag("voice_mic_trigger_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "الأمر الصوتي الشامل",
                            tint = Color.White
                        )
                    }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "اكتب أمراً محاسبياً أو اسأل المحاسب الذكي...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SanadTextMuted
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SanadElectricBlue,
                        unfocusedBorderColor = SanadBorder,
                        focusedContainerColor = SanadNavyDark,
                        unfocusedContainerColor = SanadNavyDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input")
                )

                Surface(
                    color = SanadElectricBlue,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            handleUserCommandOrQuestion(inputText, treatAsVoiceRouter = false)
                        }
                        .testTag("ai_chat_send_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
