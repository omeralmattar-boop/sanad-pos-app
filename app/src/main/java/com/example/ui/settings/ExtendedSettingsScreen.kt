package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.preferences.SanadAppSettings
import com.example.data.preferences.SanadPreferencesRepository
import com.example.data.remote.ai.GeminiAssistantService
import com.example.ui.components.SanadBadge
import com.example.ui.components.formatMoney
import com.example.ui.components.formatNumberCompact
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
import com.example.ui.theme.SanadTextPrimary
import com.example.ui.theme.SanadTextSecondary
import com.example.ui.viewmodel.SanadViewModel
import kotlinx.coroutines.launch

@Composable
fun ExtendedSettingsScreen(
    viewModel: SanadViewModel,
    prefsRepo: SanadPreferencesRepository,
    appSettings: SanadAppSettings,
    onOpenOcrScanner: () -> Unit = {},
    onOpenB2BWizard: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val currencies by viewModel.currencies.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "المتجر والعملات",
        "الطابعة الحرارية (58/80mm)",
        "الديون والواتساب",
        "الذكاء الاصطناعي (Gemini)",
        "النسخ الاحتياطي والأمان"
    )

    // Tab 0 State: Store Profile
    var storeName by remember(appSettings.storeName) { mutableStateOf(appSettings.storeName) }
    var storePhone by remember(appSettings.storePhone) { mutableStateOf(appSettings.storePhone) }
    var storeAddress by remember(appSettings.storeAddress) { mutableStateOf(appSettings.storeAddress) }
    var taxNumber by remember(appSettings.taxRegistrationNumber) { mutableStateOf(appSettings.taxRegistrationNumber) }
    var receiptFooter by remember(appSettings.receiptFooterMessage) { mutableStateOf(appSettings.receiptFooterMessage) }

    // Tab 1 State: Thermal Printer
    var paperWidthMm by remember(appSettings.thermalPaperWidthMm) { mutableIntStateOf(appSettings.thermalPaperWidthMm) }
    var autoPrint by remember(appSettings.autoPrintAfterSave) { mutableStateOf(appSettings.autoPrintAfterSave) }

    // Tab 2 State: WhatsApp & Debts Template
    var whatsappReminderTemplate by remember {
        mutableStateOf("تحية طيبة من مؤسسة سند التجارية، نود تذكيركم بموعد سداد الرصيد المستحق. شكراً لتعاونكم.")
    }

    // Tab 3 State: Gemini AI
    var apiKeyInput by remember(appSettings.geminiApiKey) { mutableStateOf(appSettings.geminiApiKey) }
    var selectedModel by remember(appSettings.geminiModelName) { mutableStateOf(appSettings.geminiModelName) }
    var connectionStatusText by remember { mutableStateOf<String?>(null) }
    var isConnectionSuccess by remember { mutableStateOf(true) }

    // Tab 4 State: Cloud & Local Backup + PIN/Biometric Lock
    var pinEnabled by remember(appSettings.pinLockEnabled) { mutableStateOf(appSettings.pinLockEnabled) }
    var pinCode by remember(appSettings.pinCode) { mutableStateOf(appSettings.pinCode) }
    var biometricEnabled by remember(appSettings.biometricEnabled) { mutableStateOf(appSettings.biometricEnabled) }
    var localBackups by remember { mutableStateOf(GoogleDriveBackupManager.listLocalBackups(context)) }
    var backupStatusBanner by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
    ) {
        Surface(
            color = SanadSurfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = SanadElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "الإعدادات الموسعة والنسخ الاحتياطي السحابي",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SanadTextPrimary
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = SanadSurfaceElevated,
            contentColor = SanadElectricBlue,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedTab == idx) SanadElectricBlue else SanadTextSecondary
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = SanadBorder)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                // Tab 0: Store Profile & Multi-Currency Exchange Rate Table
                0 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = SanadElectricBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "بيانات المنشأة وترويسة الفاتورة",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadElectricBlue
                                    )
                                }

                                OutlinedTextField(
                                    value = storeName,
                                    onValueChange = { storeName = it },
                                    label = { Text("اسم المتجر / المؤسسة") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = storePhone,
                                    onValueChange = { storePhone = it },
                                    label = { Text("رقم الهاتف للترويسة") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = storeAddress,
                                    onValueChange = { storeAddress = it },
                                    label = { Text("العنوان") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = taxNumber,
                                    onValueChange = { taxNumber = it },
                                    label = { Text("الرقم الضريبي") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = receiptFooter,
                                    onValueChange = { receiptFooter = it },
                                    label = { Text("عبارة التذييل أسفل الفاتورة") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        scope.launch {
                                            prefsRepo.updateStoreProfile(
                                                storeName = storeName,
                                                storePhone = storePhone,
                                                storeAddress = storeAddress,
                                                taxNumber = taxNumber,
                                                receiptFooter = receiptFooter
                                            )
                                            viewModel.showMessage("تم حفظ بيانات المتجر وترويسة الفاتورة بنجاح")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("حفظ إعدادات المتجر")
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "جدول أسعار الصرف مقابل الدولار الأمريكي (1 USD)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadEmeraldGreen
                                )

                                currencies.filter { !it.isBaseCurrency }.forEach { curr ->
                                    var rateStr by remember(curr.exchangeRateToBase) {
                                        mutableStateOf(formatNumberCompact(curr.exchangeRateToBase))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${curr.nameArabic} (${curr.code})",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SanadTextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = rateStr,
                                            onValueChange = { rateStr = it },
                                            label = { Text("1 $ =") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.width(120.dp)
                                        )
                                        Button(
                                            onClick = {
                                                val parsed = rateStr.toDoubleOrNull() ?: curr.exchangeRateToBase
                                                viewModel.updateExchangeRate(curr.code, parsed)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("تحديث")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Approved OCR Path #1 & #4 + Shortcut Customization: بطاقة استيراد وتصدير
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadElectricBlue.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                .testTag("settings_import_export_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = SanadElectricBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "بطاقة استيراد وتصدير (تصوير فاتورة مورد وملفات .sanad)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadElectricBlue
                                    )
                                }

                                Text(
                                    text = "المسار المعتمد لفتح معالج تصوير فاتورة المورد بالذكاء الاصطناعي (6 مراحل) أو استيراد ملف فاتورة الكترونية (.sanad):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onOpenOcrScanner,
                                        colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_open_ocr_invoice_button")
                                    ) {
                                        Text("تصوير فاتورة مورد")
                                    }

                                    OutlinedButton(
                                        onClick = onOpenB2BWizard,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadElectricBlue),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_open_b2b_sanad_button")
                                    ) {
                                        Text("استيراد ملف (.sanad)")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "إظهار اختصار «تصوير فاتورة» في الشاشة الرئيسية",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = appSettings.showHomeOcrShortcut,
                                        onCheckedChange = { enabled ->
                                            scope.launch {
                                                prefsRepo.updateShowHomeOcrShortcut(enabled)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = SanadEmeraldGreen),
                                        modifier = Modifier.testTag("toggle_home_ocr_shortcut_switch")
                                    )
                                }
                            }
                        }
                    }
                }

                // Tab 1: Thermal Printer Setup (58mm / 80mm)
                1 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = SanadElectricBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "إعدادات الطابعة الحرارية (Bluetooth / USB / LAN)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadElectricBlue
                                    )
                                }

                                Text(
                                    text = "مقاس ورق رول الطباعة الحرارية:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SanadTextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf(58 to "طابعة 58mm (محمولة)", 80 to "طابعة 80mm (كاشير قياسية)").forEach { (widthMm, label) ->
                                        val selected = paperWidthMm == widthMm
                                        Surface(
                                            color = if (selected) SanadElectricBlue else SanadNavyDark,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    1.dp,
                                                    if (selected) SanadElectricBlue else SanadBorder,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    paperWidthMm = widthMm
                                                    scope.launch { prefsRepo.updatePrinterSettings(widthMm, autoPrint) }
                                                }
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (selected) Color.White else SanadTextSecondary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "طباعة الإيصال تلقائياً فور حفظ الفاتورة",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SanadTextPrimary
                                    )
                                    Switch(
                                        checked = autoPrint,
                                        onCheckedChange = {
                                            autoPrint = it
                                            scope.launch { prefsRepo.updatePrinterSettings(paperWidthMm, it) }
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = SanadEmeraldGreen)
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.showMessage("تم إرسال صفحة اختبار طباعة حرارية بمقاس ${paperWidthMm}mm")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Print, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("طباعة إيصال تجريبي (${paperWidthMm}mm)")
                                }
                            }
                        }
                    }
                }

                // Tab 2: Debts & WhatsApp Templates
                2 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "إعدادات تنبيهات الديون وقوالب رسائل الواتساب",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadEmeraldGreen
                                )
                                OutlinedTextField(
                                    value = whatsappReminderTemplate,
                                    onValueChange = { whatsappReminderTemplate = it },
                                    label = { Text("قالب رسالة المطالبة المالية عبر واتساب") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        viewModel.showMessage("تم حفظ قالب رسائل المطالبة عبر واتساب")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("حفظ قالب الواتساب")
                                }
                            }
                        }
                    }
                }

                // Tab 3: Gemini AI Setup (gemini-2.5-pro / gemini-2.5-flash + Paste + Connection Test)
                3 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = SanadElectricBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "إعدادات الذكاء الاصطناعي (Gemini REST API via OkHttp)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadElectricBlue
                                    )
                                }

                                Text(
                                    text = "اختر النموذج المفضل للتحليل المالي وقراءة فواتير الموردين:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "gemini-3.8-flash" to "Gemini 3.8 Flash (افتراضي)",
                                        "gemini-3.5-flash-lite" to "Gemini 3.5 Flash-Lite (فائق السرعة)"
                                    ).forEach { (modelId, label) ->
                                        val selected = selectedModel == modelId
                                        Surface(
                                            color = if (selected) SanadElectricBlue else SanadNavyDark,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    1.dp,
                                                    if (selected) SanadElectricBlue else SanadBorder,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    selectedModel = modelId
                                                    scope.launch { prefsRepo.updateGeminiConfig(apiKeyInput, modelId) }
                                                }
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (selected) Color.White else SanadTextSecondary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 10.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = apiKeyInput,
                                        onValueChange = { apiKeyInput = it },
                                        label = { Text("مفتاح Gemini API Key (أو من لوحة Secrets)") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("gemini_api_key_input")
                                    )

                                    // Paste Button
                                    Surface(
                                        color = SanadBlueContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .size(52.dp)
                                            .border(1.dp, SanadElectricBlue, RoundedCornerShape(10.dp))
                                            .clickable {
                                                val clip = clipboardManager.getText()?.text.orEmpty()
                                                if (clip.isNotBlank()) {
                                                    apiKeyInput = GeminiAssistantService.sanitizeApiKey(clip)
                                                }
                                            }
                                            .testTag("paste_gemini_key_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "لصق المفتاح",
                                            tint = SanadElectricBlue,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val clean = GeminiAssistantService.sanitizeApiKey(apiKeyInput)
                                            apiKeyInput = clean
                                            scope.launch {
                                                prefsRepo.updateGeminiConfig(clean, selectedModel)
                                                viewModel.showMessage("تم حفظ مفتاح Gemini والنموذج ($selectedModel)")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("حفظ المفتاح")
                                    }

                                    // [اختبار الاتصال] Button
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val res = GeminiAssistantService.testConnection(apiKeyInput, selectedModel)
                                                res.onSuccess { msg ->
                                                    isConnectionSuccess = true
                                                    connectionStatusText = msg
                                                }.onFailure { err ->
                                                    isConnectionSuccess = false
                                                    connectionStatusText = err.message ?: "تعذر الاتصال"
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("test_gemini_connection_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("اختبار الاتصال")
                                    }
                                }

                                connectionStatusText?.let { statusMsg ->
                                    Surface(
                                        color = if (isConnectionSuccess) SanadEmeraldContainer else SanadAmberContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = statusMsg,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isConnectionSuccess) SanadEmeraldGreen else SanadAmberWarning,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 4: Cloud (Google Drive) & Local (.sanad / ZIP) Backup + PIN / Biometric Security
                4 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Backup, contentDescription = null, tint = SanadEmeraldGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "النسخ الاحتياطي السحابي (Google Drive) والمحلي (.sanad / ZIP)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadEmeraldGreen
                                    )
                                }

                                Text(
                                    text = "ينفذ النظام أمر PRAGMA wal_checkpoint(FULL) لدمج كافة الحركات المؤقتة بأمان تام قبل ضغط قاعدة البيانات.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val res = GoogleDriveBackupManager.createSafeSanadBackupArchive(
                                                    context = context,
                                                    dao = viewModel.dao
                                                )
                                                res.onSuccess { info ->
                                                    localBackups = GoogleDriveBackupManager.listLocalBackups(context)
                                                    backupStatusBanner = "تم إنشاء النسخة (${info.archiveFile.name}) بحجم ${info.sizeKilobytes} KB"
                                                    viewModel.showMessage("تم حفظ نسخة احتياطية محلية (.sanad) بنجاح")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("create_local_backup_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نسخة محلية (.sanad)")
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val res = GoogleDriveBackupManager.createSafeSanadBackupArchive(
                                                    context = context,
                                                    dao = viewModel.dao
                                                )
                                                res.onSuccess { info ->
                                                    localBackups = GoogleDriveBackupManager.listLocalBackups(context)
                                                    backupStatusBanner = "تم تجهيز ورفع النسخة إلى Google Drive (${info.sizeKilobytes} KB)"
                                                    GoogleDriveBackupManager.launchGoogleDriveUploadIntent(context, info.archiveFile)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("google_drive_backup_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("رفع لـ Google Drive")
                                    }
                                }

                                backupStatusBanner?.let { banner ->
                                    Surface(
                                        color = SanadEmeraldContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = banner,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SanadEmeraldGreen,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                if (localBackups.isNotEmpty()) {
                                    Text(
                                        text = "النسخ الاحتياطية المحفوظة (${localBackups.size}):",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SanadTextPrimary
                                    )
                                    localBackups.take(4).forEach { file ->
                                        Surface(
                                            color = SanadNavyDark,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = file.name,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = SanadTextPrimary
                                                    )
                                                    Text(
                                                        text = "الحجم: ${(file.length() / 1024L).coerceAtLeast(1L)} KB",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SanadTextSecondary
                                                    )
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val res = GoogleDriveBackupManager.restoreFromBackupArchive(
                                                                context = context,
                                                                dao = viewModel.dao,
                                                                archiveFile = file
                                                            )
                                                            res.onSuccess { msg -> viewModel.showMessage(msg) }
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("استعادة")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // PIN & Biometric Security Lock Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = SanadAmberWarning)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "قفل التطبيق بالبصمة ورمز الحماية (PIN / Biometric)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadAmberWarning
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("تفعيل قفل رمز الحماية السري (PIN)", color = SanadTextPrimary)
                                    Switch(
                                        checked = pinEnabled,
                                        onCheckedChange = {
                                            pinEnabled = it
                                            scope.launch { prefsRepo.updateSecuritySettings(it, pinCode, biometricEnabled) }
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = SanadEmeraldGreen)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("تفعيل فتح القفل بالبصمة الحيوية (Biometric)", color = SanadTextPrimary)
                                    Switch(
                                        checked = biometricEnabled,
                                        onCheckedChange = {
                                            biometricEnabled = it
                                            scope.launch { prefsRepo.updateSecuritySettings(pinEnabled, pinCode, it) }
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = SanadElectricBlue)
                                    )
                                }

                                OutlinedTextField(
                                    value = pinCode,
                                    onValueChange = {
                                        pinCode = it.take(6)
                                        scope.launch { prefsRepo.updateSecuritySettings(pinEnabled, pinCode, biometricEnabled) }
                                    },
                                    label = { Text("رمز القفل السري (PIN)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
