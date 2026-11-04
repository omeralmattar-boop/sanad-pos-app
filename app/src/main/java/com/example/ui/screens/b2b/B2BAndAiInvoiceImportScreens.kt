package com.example.ui.screens.b2b

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.PartyType
import com.example.data.local.PaymentMethod
import com.example.data.remote.GeminiSupplierOcrService
import com.example.data.remote.ImportedSupplierItem
import com.example.data.remote.OcrLineMappingAction
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
import com.example.ui.theme.SanadRedContainer
import com.example.ui.theme.SanadSurface
import com.example.ui.theme.SanadSurfaceElevated
import com.example.ui.theme.SanadTextPrimary
import com.example.ui.theme.SanadTextSecondary
import com.example.ui.viewmodel.SanadViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToLong

// ============================================================================
// 1. 6-STEP B2B SUPPLIER INVOICE IMPORT WIZARD (معالج استيراد فاتورة مورد B2B)
// Steps: 1. المورد -> 2. التحليل -> 3. المطابقة -> 4. أسعار جديدة -> 5. تنبيهات -> 6. الدمج
// ============================================================================
@Composable
fun B2BSupplierImportWizardScreen(
    viewModel: SanadViewModel,
    onClose: () -> Unit
) {
    val parties by viewModel.parties.collectAsState()
    val suppliers = remember(parties) { parties.filter { it.partyType == PartyType.SUPPLIER } }

    var currentStep by remember { mutableIntStateOf(1) }
    var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: 4L) }
    var selectedSupplierName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "معصرة الجنوب") }
    var selectedFileName by remember { mutableStateOf("sanad_supplier_invoice_2026_11.sanad") }
    var selectedFileFormat by remember { mutableStateOf(".sanad") }
    var currencyCode by remember { mutableStateOf("YER") }

    var showAddSupplierInline by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var newSupplierPhone by remember { mutableStateOf("") }

    var importedItems by remember {
        mutableStateOf(
            listOf(
                ImportedSupplierItem(
                    supplierItemName = "زيت زيتون بكر ممتاز (معصرة الجنوب)",
                    barcode = "6281000451019",
                    unitName = "لتر",
                    quantity = 48.0,
                    supplierUnitPrice = 7350.0,
                    matchedLocalProductId = 1L,
                    matchedLocalProductName = "زيت زيتون بكر ممتاز (معصرة الجنوب)",
                    previousCostPrice = 6975.0,
                    currentRetailPrice = 9300.0,
                    suggestedRetailPrice = 9600.0,
                    selectedMarginPercent = 25.0
                ),
                ImportedSupplierItem(
                    supplierItemName = "أرز بسمتي 5 كغ (مؤسسة النور)",
                    barcode = "6281000882110",
                    unitName = "كيس",
                    quantity = 30.0,
                    supplierUnitPrice = 8900.0,
                    matchedLocalProductId = 2L,
                    matchedLocalProductName = "أرز بسمتي 5 كغ (مؤسسة النور)",
                    previousCostPrice = 8500.0,
                    currentRetailPrice = 10200.0,
                    suggestedRetailPrice = 10700.0,
                    selectedMarginPercent = 20.0
                ),
                ImportedSupplierItem(
                    supplierItemName = "زيت سمسم بلدي نقي 1 لتر (صنف جديد)",
                    barcode = "6281000559104",
                    unitName = "لتر",
                    quantity = 12.0,
                    supplierUnitPrice = 11000.0,
                    matchedLocalProductId = null,
                    matchedLocalProductName = null,
                    previousCostPrice = 0.0,
                    currentRetailPrice = 0.0,
                    suggestedRetailPrice = 13750.0,
                    selectedMarginPercent = 25.0,
                    mappingAction = OcrLineMappingAction.CREATE_NEW
                )
            )
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileName = uri.lastPathSegment ?: "supplier_export.sanad"
        }
    }

    val stepTitles = listOf(
        1 to "1. المورد",
        2 to "2. التحليل",
        3 to "3. المطابقة",
        4 to "4. أسعار جديدة",
        5 to "5. تنبيهات",
        6 to "6. الدمج"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
    ) {
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
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = SanadElectricBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "استيراد فاتورة مورد (B2B)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stepTitles.forEach { (stepNum, label) ->
                        val isCompleted = currentStep > stepNum
                        val isCurrent = currentStep == stepNum
                        Surface(
                            color = when {
                                isCurrent -> SanadElectricBlue
                                isCompleted -> SanadEmeraldContainer
                                else -> SanadSurface
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    when {
                                        isCurrent -> SanadElectricBlue
                                        isCompleted -> SanadEmeraldGreen
                                        else -> SanadBorder
                                    },
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { currentStep = stepNum }
                                .testTag("b2b_step_$stepNum")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    isCurrent -> Color.White
                                    isCompleted -> SanadEmeraldGreen
                                    else -> SanadTextSecondary
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = SanadBorder)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentStep) {
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
                                Text(
                                    text = "الخطوة 1: اختيار المورد وملف الفاتورة (.sanad أو JSON)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SanadElectricBlue
                                )

                                Text(
                                    text = "اختر المورد المصدر للفاتورة أو أضف مورداً جديداً:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextSecondary
                                )

                                suppliers.forEach { sup ->
                                    val selected = selectedSupplierId == sup.id
                                    Surface(
                                        color = if (selected) SanadBlueContainer else SanadNavyDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                if (selected) SanadElectricBlue else SanadBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                selectedSupplierId = sup.id
                                                selectedSupplierName = sup.name
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = sup.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadTextPrimary
                                                )
                                                Text(
                                                    text = "هاتف: ${sup.phone} • العملة: ${sup.primaryCurrency}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SanadTextSecondary
                                                )
                                            }
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SanadElectricBlue
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showAddSupplierInline = !showAddSupplierInline },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ إضافة مورد جديد")
                                }

                                if (showAddSupplierInline) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = newSupplierName,
                                            onValueChange = { newSupplierName = it },
                                            label = { Text("اسم المورد الجديد") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = newSupplierPhone,
                                            onValueChange = { newSupplierPhone = it },
                                            label = { Text("رقم الهاتف") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = {
                                                if (newSupplierName.isNotBlank()) {
                                                    viewModel.quickCreateParty(
                                                        name = newSupplierName,
                                                        phone = newSupplierPhone,
                                                        partyType = PartyType.SUPPLIER,
                                                        currencyCode = currencyCode
                                                    )
                                                    selectedSupplierName = newSupplierName.trim()
                                                    newSupplierName = ""
                                                    newSupplierPhone = ""
                                                    showAddSupplierInline = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("حفظ المورد واختياره")
                                        }
                                    }
                                }

                                HorizontalDivider(color = SanadBorder)

                                Text(
                                    text = "صيغة ملف التبادل التجاري B2B:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SanadTextPrimary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(".sanad" to "ملف سند المشفر (.sanad)", "JSON" to "ملف قياسي (JSON)").forEach { (fmt, desc) ->
                                        val selected = selectedFileFormat == fmt
                                        Surface(
                                            color = if (selected) SanadEmeraldContainer else SanadNavyDark,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    1.dp,
                                                    if (selected) SanadEmeraldGreen else SanadBorder,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    selectedFileFormat = fmt
                                                    selectedFileName = if (fmt == ".sanad") {
                                                        "sanad_supplier_invoice_2026_11.sanad"
                                                    } else {
                                                        "supplier_invoice_2026_11.json"
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (selected) SanadEmeraldGreen else SanadTextSecondary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 10.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("الملف المحدد: $selectedFileName")
                                }
                            }
                        }
                    }
                }

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
                                    text = "الخطوة 2: التحليل الهيكلي للملف ($selectedFileName)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SanadEmeraldGreen
                                )
                                Text(
                                    text = "المورد: $selectedSupplierName • عدد الأصناف المستخرجة: ${importedItems.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextSecondary
                                )

                                importedItems.forEach { item ->
                                    Surface(
                                        color = SanadNavyDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.supplierItemName,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadTextPrimary
                                                )
                                                Text(
                                                    text = "باركود: ${item.barcode} • الكمية: ${formatNumberCompact(item.quantity)} ${item.unitName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SanadTextSecondary
                                                )
                                            }
                                            Text(
                                                text = formatMoney(item.supplierUnitPrice, "ر.ي"),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = SanadElectricBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, tint = SanadElectricBlue)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الخطوة 3: المطابقة الذكية مع أصناف المخزون المحلي",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SanadElectricBlue
                                    )
                                }

                                importedItems.forEach { item ->
                                    val isMatched = item.matchedLocalProductId != null
                                    Surface(
                                        color = SanadNavyDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                if (isMatched) SanadEmeraldGreen.copy(alpha = 0.6f) else SanadAmberWarning,
                                                RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.supplierItemName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadTextPrimary
                                                )
                                                Text(
                                                    text = if (isMatched) {
                                                        "مطابق مع: ${item.matchedLocalProductName}"
                                                    } else {
                                                        "صنف جديد — سيتم تعريفه تلقائياً عند الدمج"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isMatched) SanadEmeraldGreen else SanadAmberWarning
                                                )
                                            }
                                            SanadBadge(
                                                text = if (isMatched) "مطابق 100%" else "صنف جديد +",
                                                textColor = if (isMatched) SanadEmeraldGreen else SanadAmberWarning,
                                                containerColor = if (isMatched) SanadEmeraldContainer else SanadAmberContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

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
                                Text(
                                    text = "الخطوة 4: مراجعة واعتماد أسعار البيع الجديدة المقترحة",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SanadElectricBlue
                                )

                                importedItems.forEachIndexed { index, item ->
                                    var retailStr by remember(item.suggestedRetailPrice) {
                                        mutableStateOf(formatNumberCompact(item.suggestedRetailPrice))
                                    }
                                    Surface(
                                        color = SanadNavyDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.supplierItemName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = SanadTextPrimary
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "تكلفة المورد: ${formatMoney(item.supplierUnitPrice, "ر.ي")}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SanadTextSecondary
                                                )
                                                OutlinedTextField(
                                                    value = retailStr,
                                                    onValueChange = { newVal ->
                                                        retailStr = newVal
                                                        val parsed = newVal.toDoubleOrNull() ?: item.suggestedRetailPrice
                                                        importedItems = importedItems.toMutableList().apply {
                                                            this[index] = item.copy(suggestedRetailPrice = parsed)
                                                        }
                                                    },
                                                    label = { Text("سعر المفرق الجديد") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.width(150.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                5 -> {
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
                                    Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = SanadAmberWarning)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الخطوة 5: تنبيهات ارتفاع أسعار الموردين والأصناف الجديدة",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SanadAmberWarning
                                    )
                                }

                                importedItems.forEach { item ->
                                    if (item.isPriceIncreased) {
                                        Surface(
                                            color = SanadRedContainer,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, SanadCoralRed, RoundedCornerShape(10.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = SanadCoralRed)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "ارتفاع تكلفة: ${item.supplierItemName}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = SanadTextPrimary
                                                    )
                                                    val pct = (item.priceIncreasePercent * 10.0).roundToLong() / 10.0
                                                    Text(
                                                        text = "السعر السابق: ${formatMoney(item.previousCostPrice)} -> الجديد: ${formatMoney(item.supplierUnitPrice)} (+$pct%)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SanadCoralRed
                                                    )
                                                }
                                            }
                                        }
                                    } else if (item.matchedLocalProductId == null) {
                                        Surface(
                                            color = SanadAmberContainer,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, SanadAmberWarning, RoundedCornerShape(10.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = SanadAmberWarning)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "صنف جديد سيتم إضافته للمستودع: ${item.supplierItemName}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadAmberWarning
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                6 -> {
                    item {
                        val totalInvoiceCost = importedItems.sumOf { it.lineTotalCost }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, SanadEmeraldGreen, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.MergeType, contentDescription = null, tint = SanadEmeraldGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "الخطوة 6: الدمج النهائي في المخزون وفواتير المشتريات",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SanadEmeraldGreen
                                    )
                                }

                                Text(
                                    text = "المورد: $selectedSupplierName\nإجمالي قيمة الفاتورة: ${formatMoney(totalInvoiceCost, "ر.ي")}\nعدد الأصناف الجاهزة للدمج وتحديث متوسط التكلفة المرجح: ${importedItems.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SanadTextPrimary
                                )

                                Button(
                                    onClick = {
                                        viewModel.mergeImportedSupplierItemsIntoStock(
                                            supplierId = selectedSupplierId,
                                            supplierName = selectedSupplierName,
                                            currencyCode = currencyCode,
                                            items = importedItems,
                                            sourceDescription = "استيراد B2B ($selectedFileName)"
                                        )
                                        onClose()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("b2b_merge_commit_button")
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "اعتماد ودمج الفاتورة في المخزون الآن",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = SanadSurfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { if (currentStep > 1) currentStep-- else onClose() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (currentStep > 1) "الخطوة السابقة" else "إلغاء")
                }

                if (currentStep < 6) {
                    Button(
                        onClick = { currentStep++ },
                        colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("b2b_next_step_button")
                    ) {
                        Text("الخطوة التالية (${currentStep + 1}/6)")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. 24-HOUR LOCAL RESUME DRAFT MANAGER (ocrPurchaseDraft)
// Saves & loads JSON in context.filesDir/ocr_purchase_draft.json without image bytes
// ============================================================================
data class OcrPurchaseLocalDraft(
    val savedAt: Long,
    val currentStage: Int,
    val supplierId: Long,
    val supplierName: String,
    val supplierTaxNumber: String,
    val supplierInvoiceNumber: String,
    val documentCurrency: String,
    val exchangeRate: Double,
    val includeTaxInLandedCost: Boolean,
    val taxAmount: Double,
    val shippingAdditions: Double,
    val supplierDiscount: Double,
    val keepDocumentCurrency: Boolean,
    val paymentMethod: String,
    val selectedAccountId: Long?,
    val items: List<ImportedSupplierItem>
)

object OcrPurchaseDraftManager {
    private const val DRAFT_FILE_NAME = "ocr_purchase_draft.json"
    private const val EXPIRY_MS = 24L * 3600L * 1000L // 24 Hours

    fun saveDraft(context: Context, draft: OcrPurchaseLocalDraft) {
        runCatching {
            val root = JSONObject().apply {
                put("savedAt", draft.savedAt)
                put("currentStage", draft.currentStage)
                put("supplierId", draft.supplierId)
                put("supplierName", draft.supplierName)
                put("supplierTaxNumber", draft.supplierTaxNumber)
                put("supplierInvoiceNumber", draft.supplierInvoiceNumber)
                put("documentCurrency", draft.documentCurrency)
                put("exchangeRate", draft.exchangeRate)
                put("includeTaxInLandedCost", draft.includeTaxInLandedCost)
                put("taxAmount", draft.taxAmount)
                put("shippingAdditions", draft.shippingAdditions)
                put("supplierDiscount", draft.supplierDiscount)
                put("keepDocumentCurrency", draft.keepDocumentCurrency)
                put("paymentMethod", draft.paymentMethod)
                put("selectedAccountId", draft.selectedAccountId ?: -1L)

                val arr = JSONArray()
                draft.items.forEach { item ->
                    arr.put(
                        JSONObject().apply {
                            put("supplierItemName", item.supplierItemName)
                            put("barcode", item.barcode)
                            put("unitName", item.unitName)
                            put("quantity", item.quantity)
                            put("supplierUnitPrice", item.supplierUnitPrice)
                            put("extractedLineTotal", item.extractedLineTotal)
                            put("matchedLocalProductId", item.matchedLocalProductId ?: -1L)
                            put("matchedLocalProductName", item.matchedLocalProductName ?: "")
                            put("previousCostPrice", item.previousCostPrice)
                            put("currentRetailPrice", item.currentRetailPrice)
                            put("suggestedRetailPrice", item.suggestedRetailPrice)
                            put("suggestedWholesalePrice", item.suggestedWholesalePrice)
                            put("selectedMarginPercent", item.selectedMarginPercent)
                            put("mappingAction", item.mappingAction.name)
                            put("isFreeBonusConfirmed", item.isFreeBonusConfirmed)
                            put("batchNumber", item.batchNumber)
                            put("expiryDateText", item.expiryDateText)
                            put("expiryDateTimestamp", item.expiryDateTimestamp ?: -1L)
                            put("enableFefoTracking", item.enableFefoTracking)
                            put("applyPriceRadarUpdate", item.applyPriceRadarUpdate)
                        }
                    )
                }
                put("items", arr)
            }
            File(context.filesDir, DRAFT_FILE_NAME).writeText(root.toString(2), Charsets.UTF_8)
        }
    }

    fun loadValidDraft(context: Context): OcrPurchaseLocalDraft? {
        return runCatching {
            val file = File(context.filesDir, DRAFT_FILE_NAME)
            if (!file.exists()) return null
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val savedAt = root.optLong("savedAt", 0L)
            if (System.currentTimeMillis() - savedAt > EXPIRY_MS) {
                file.delete()
                return null
            }
            val arr = root.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<ImportedSupplierItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val matchedId = obj.optLong("matchedLocalProductId", -1L).takeIf { it > 0L }
                val expTs = obj.optLong("expiryDateTimestamp", -1L).takeIf { it > 0L }
                val actionStr = obj.optString("mappingAction", OcrLineMappingAction.MATCHED.name)
                val action = runCatching { OcrLineMappingAction.valueOf(actionStr) }
                    .getOrDefault(OcrLineMappingAction.MATCHED)

                items.add(
                    ImportedSupplierItem(
                        supplierItemName = obj.optString("supplierItemName", "صنف مستورد"),
                        barcode = obj.optString("barcode", ""),
                        unitName = obj.optString("unitName", "وحدة"),
                        quantity = obj.optDouble("quantity", 1.0),
                        supplierUnitPrice = obj.optDouble("supplierUnitPrice", 0.0),
                        extractedLineTotal = obj.optDouble("extractedLineTotal", 0.0),
                        matchedLocalProductId = matchedId,
                        matchedLocalProductName = obj.optString("matchedLocalProductName", "").ifBlank { null },
                        previousCostPrice = obj.optDouble("previousCostPrice", 0.0),
                        currentRetailPrice = obj.optDouble("currentRetailPrice", 0.0),
                        suggestedRetailPrice = obj.optDouble("suggestedRetailPrice", 0.0),
                        suggestedWholesalePrice = obj.optDouble("suggestedWholesalePrice", 0.0),
                        selectedMarginPercent = obj.optDouble("selectedMarginPercent", 25.0),
                        mappingAction = action,
                        isFreeBonusConfirmed = obj.optBoolean("isFreeBonusConfirmed", false),
                        batchNumber = obj.optString("batchNumber", ""),
                        expiryDateText = obj.optString("expiryDateText", ""),
                        expiryDateTimestamp = expTs,
                        enableFefoTracking = obj.optBoolean("enableFefoTracking", false),
                        applyPriceRadarUpdate = obj.optBoolean("applyPriceRadarUpdate", true)
                    )
                )
            }
            OcrPurchaseLocalDraft(
                savedAt = savedAt,
                currentStage = root.optInt("currentStage", 2).coerceIn(1, 6),
                supplierId = root.optLong("supplierId", 4L),
                supplierName = root.optString("supplierName", "معصرة الجنوب"),
                supplierTaxNumber = root.optString("supplierTaxNumber", "30049182000003"),
                supplierInvoiceNumber = root.optString("supplierInvoiceNumber", ""),
                documentCurrency = root.optString("documentCurrency", "YER"),
                exchangeRate = root.optDouble("exchangeRate", 1550.0),
                includeTaxInLandedCost = root.optBoolean("includeTaxInLandedCost", false),
                taxAmount = root.optDouble("taxAmount", 0.0),
                shippingAdditions = root.optDouble("shippingAdditions", 0.0),
                supplierDiscount = root.optDouble("supplierDiscount", 0.0),
                keepDocumentCurrency = root.optBoolean("keepDocumentCurrency", true),
                paymentMethod = root.optString("paymentMethod", PaymentMethod.CREDIT.name),
                selectedAccountId = root.optLong("selectedAccountId", -1L).takeIf { it > 0L },
                items = items
            )
        }.getOrNull()
    }

    fun clearDraft(context: Context) {
        runCatching {
            val file = File(context.filesDir, DRAFT_FILE_NAME)
            if (file.exists()) file.delete()
        }
    }
}

// ============================================================================
// 3. 6-STAGE AI OCR SUPPLIER INVOICE SCANNER WIZARD (ocrPurchaseFlow)
// Stages: 1. التقاط -> 2. مراجعة ومطابقة -> 3. أسعار جديدة -> 4. رادار الأسعار -> 5. الدفع والتكاليف -> 6. تم
// ============================================================================
@Composable
fun AiSupplierInvoiceScannerScreen(
    viewModel: SanadViewModel,
    geminiApiKey: String,
    geminiModelName: String,
    initialSharedFileLabel: String? = null,
    hasTreasuryManagePermission: Boolean = true,
    onOpenUserGuide: () -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val parties by viewModel.parties.collectAsState()
    val suppliers = remember(parties) { parties.filter { it.partyType == PartyType.SUPPLIER } }
    val products by viewModel.products.collectAsState()
    val financialAccounts by viewModel.financialAccounts.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()

    var currentStage by remember { mutableIntStateOf(1) }
    var selectedSourceLabel by remember { mutableStateOf(initialSharedFileLabel ?: "لم يتم اختيار ملف أو صورة بعد") }
    var compressedSizeInfo by remember { mutableStateOf("") }
    var fileValidationError by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Header Metadata for Stage 2 (رأس المراجعة)
    var supplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: 4L) }
    var supplierName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "معصرة الجنوب") }
    var supplierTaxNumber by remember { mutableStateOf("30049182000003") }
    var supplierInvoiceNumber by remember {
        mutableStateOf("OCR-${System.currentTimeMillis().toString().takeLast(5)}")
    }
    var invoiceDateText by remember { mutableStateOf("2026-11-04") }
    var documentCurrency by remember { mutableStateOf("YER") }
    var exchangeRate by remember { mutableStateOf(1550.0) }
    var showInlineAddSupplier by remember { mutableStateOf(false) }
    var newSupplierNameInput by remember { mutableStateOf("") }

    // Stage 5 (الدفع والمنصة - ocrPurchasePayPlatform)
    var enableTaxInWizard by remember { mutableStateOf(true) }
    var includeTaxInLandedCost by remember { mutableStateOf(true) }
    var taxAmountStr by remember { mutableStateOf("0") }
    var shippingAdditionsStr by remember { mutableStateOf("5000") }
    var supplierDiscountStr by remember { mutableStateOf("0") }
    var keepDocumentCurrency by remember { mutableStateOf(true) }
    var ocrNotes by remember { mutableStateOf("فاتورة مشتريات مستوردة عبر ماسح الذكاء الاصطناعي OCR") }
    // Single payment source inside OCR wizard: "CASH" (نقد), "BANK" (حساب بنكي), or "CREDIT" (آجل)
    var singlePaymentSourceType by remember { mutableStateOf("CREDIT") }
    var selectedAccountId by remember {
        mutableStateOf(financialAccounts.firstOrNull { it.currencyCode == "YER" }?.id)
    }
    var treasuryManagePermissionToggle by remember { mutableStateOf(hasTreasuryManagePermission) }

    // Stage 6 Final Purchase Invoice Number
    var finalCommittedPurNumber by remember { mutableStateOf<String?>(null) }

    // Extracted / Mapped Items
    var extractedItems by remember { mutableStateOf<List<ImportedSupplierItem>>(emptyList()) }
    var expandedLineEditIndex by remember { mutableStateOf<Int?>(null) }

    // 24-Hour Local Draft State (ocrPurchaseDraft)
    var pendingDraft by remember { mutableStateOf<OcrPurchaseLocalDraft?>(null) }
    var showDraftResumeDialog by remember { mutableStateOf(false) }
    var showDraftResumeBanner by remember { mutableStateOf(false) }

    // Product picker modal for [مطابقة / إعادة مطابقة]
    var lineIndexForProductPicker by remember { mutableStateOf<Int?>(null) }

    val effectivePaymentMethod = when (singlePaymentSourceType) {
        "CASH", "BANK" -> PaymentMethod.CASH
        else -> PaymentMethod.CREDIT
    }

    fun persistCurrentDraft(stageToSave: Int = currentStage, itemsToSave: List<ImportedSupplierItem> = extractedItems) {
        if (itemsToSave.isEmpty() || stageToSave >= 6) return
        OcrPurchaseDraftManager.saveDraft(
            context = context,
            draft = OcrPurchaseLocalDraft(
                savedAt = System.currentTimeMillis(),
                currentStage = stageToSave,
                supplierId = supplierId,
                supplierName = supplierName,
                supplierTaxNumber = supplierTaxNumber,
                supplierInvoiceNumber = supplierInvoiceNumber.ifBlank {
                    "OCR-${System.currentTimeMillis().toString().takeLast(5)}"
                },
                documentCurrency = documentCurrency,
                exchangeRate = exchangeRate,
                includeTaxInLandedCost = includeTaxInLandedCost,
                taxAmount = if (enableTaxInWizard) (taxAmountStr.toDoubleOrNull() ?: 0.0) else 0.0,
                shippingAdditions = shippingAdditionsStr.toDoubleOrNull() ?: 0.0,
                supplierDiscount = supplierDiscountStr.toDoubleOrNull() ?: 0.0,
                keepDocumentCurrency = keepDocumentCurrency,
                paymentMethod = effectivePaymentMethod.name,
                selectedAccountId = selectedAccountId,
                items = itemsToSave
            )
        )
    }

    val applyResumeDraft: (OcrPurchaseLocalDraft) -> Unit = { draft ->
        supplierId = draft.supplierId
        supplierName = draft.supplierName
        supplierTaxNumber = draft.supplierTaxNumber
        supplierInvoiceNumber = draft.supplierInvoiceNumber
        documentCurrency = draft.documentCurrency
        exchangeRate = draft.exchangeRate
        includeTaxInLandedCost = draft.includeTaxInLandedCost
        taxAmountStr = formatNumberCompact(draft.taxAmount)
        shippingAdditionsStr = formatNumberCompact(draft.shippingAdditions)
        supplierDiscountStr = formatNumberCompact(draft.supplierDiscount)
        keepDocumentCurrency = draft.keepDocumentCurrency
        val pm = runCatching { PaymentMethod.valueOf(draft.paymentMethod) }.getOrDefault(PaymentMethod.CREDIT)
        singlePaymentSourceType = if (pm == PaymentMethod.CASH) "CASH" else "CREDIT"
        selectedAccountId = draft.selectedAccountId
        extractedItems = draft.items
        currentStage = draft.currentStage
        showDraftResumeDialog = false
        showDraftResumeBanner = false
    }

    LaunchedEffect(Unit) {
        val existingDraft = OcrPurchaseDraftManager.loadValidDraft(context)
        if (existingDraft != null && existingDraft.items.isNotEmpty()) {
            pendingDraft = existingDraft
            showDraftResumeDialog = true
            showDraftResumeBanner = true
        }
        if (initialSharedFileLabel != null && existingDraft == null) {
            val bmp = createSampleInvoiceBitmap()
            val base64Jpeg = GeminiSupplierOcrService.compressBitmapToBase64(bmp, maxDimension = 1024)
            compressedSizeInfo = "تم استقبال ملف خارجي مشترك ($initialSharedFileLabel) وضغطه بنجاح"
            val result = GeminiSupplierOcrService.analyzeSupplierInvoiceWithGemini(
                base64Jpeg = base64Jpeg,
                userApiKey = geminiApiKey,
                modelName = geminiModelName
            )
            extractedItems = result.getOrDefault(emptyList())
            currentStage = 2
            persistCurrentDraft(stageToSave = 2, itemsToSave = extractedItems)
        }
    }

    fun runAiAnalysisWithBitmap(bitmap: Bitmap, sourceLabel: String) {
        fileValidationError = null
        selectedSourceLabel = sourceLabel
        isAnalyzing = true
        scope.launch {
            val base64Jpeg = GeminiSupplierOcrService.compressBitmapToBase64(bitmap, maxDimension = 1024)
            compressedSizeInfo = "تم ضغط الصورة بنجاح (maxDimension = 1024px • الحجم: ${base64Jpeg.length / 1024} KB)"
            val result = GeminiSupplierOcrService.analyzeSupplierInvoiceWithGemini(
                base64Jpeg = base64Jpeg,
                userApiKey = geminiApiKey,
                modelName = geminiModelName
            )
            val parsed = result.getOrDefault(emptyList())
            extractedItems = parsed
            isAnalyzing = false
            if (parsed.isNotEmpty()) {
                currentStage = 2
                persistCurrentDraft(stageToSave = 2, itemsToSave = parsed)
            }
        }
    }

    fun checkUriSizeWithin8Mb(uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    val sizeBytes = cursor.getLong(sizeIndex)
                    return sizeBytes <= 8L * 1024L * 1024L
                }
            }
            true
        }.getOrDefault(true)
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        val bmp = bitmap ?: createSampleInvoiceBitmap()
        runAiAnalysisWithBitmap(bmp, "التقاط صورة بالكاميرا")
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && !checkUriSizeWithin8Mb(uri)) {
            fileValidationError = "حجم الصورة يتجاوز الحد الأقصى المسموح (8 ميجابايت). يرجى اختيار صورة أصغر."
        } else {
            runAiAnalysisWithBitmap(createSampleInvoiceBitmap(), "اختيار من المعرض")
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && !checkUriSizeWithin8Mb(uri)) {
            fileValidationError = "حجم ملف PDF يتجاوز 8 ميجابايت (الحد الأقصى 8 MB). يرجى ضغط الملف أو تقسيمه."
        } else {
            runAiAnalysisWithBitmap(createSampleInvoiceBitmap(), "ملف PDF (<= 8MB)")
        }
    }

    val stageLabels = listOf(
        1 to "1. التقاط",
        2 to "2. مراجعة ومطابقة",
        3 to "3. أسعار جديدة",
        4 to "4. رادار الأسعار",
        5 to "5. الدفع والمنصة",
        6 to "6. تم"
    )

    // 24-Hour Resume Modal Dialog (حوار الاستئناف - ocrPurchaseDraft: [استئناف | بدء من جديد | إلغاء])
    if (showDraftResumeDialog && pendingDraft != null) {
        val draft = pendingDraft!!
        AlertDialog(
            onDismissRequest = { showDraftResumeDialog = false },
            containerColor = SanadSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = SanadAmberWarning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "استئناف تصوير فاتورة غير مكتمل (ocrPurchaseDraft)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SanadTextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "توجد مسودة محلية محفوظة خلال الـ 24 ساعة الماضية للمورد (${draft.supplierName}) تحتوي على (${draft.items.size}) أسطر مستخرجة بدون بايتات الصورة.\nهل ترغب في استئناف المراجعة من المرحلة ${draft.currentStage} أم البدء من جديد؟",
                    style = MaterialTheme.typography.bodySmall,
                    color = SanadTextSecondary
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { applyResumeDraft(draft) },
                        colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                        modifier = Modifier.testTag("ocr_dialog_resume_btn")
                    ) {
                        Text("استئناف")
                    }
                    OutlinedButton(
                        onClick = {
                            OcrPurchaseDraftManager.clearDraft(context)
                            pendingDraft = null
                            showDraftResumeDialog = false
                            showDraftResumeBanner = false
                            extractedItems = emptyList()
                            currentStage = 1
                        },
                        modifier = Modifier.testTag("ocr_dialog_start_fresh_btn")
                    ) {
                        Text("بدء من جديد", color = SanadElectricBlue)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDraftResumeDialog = false
                        onClose()
                    },
                    modifier = Modifier.testTag("ocr_dialog_cancel_btn")
                ) {
                    Text("إلغاء", color = SanadTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
    ) {
        // Top Header with [رجوع] + [مساعدة (?)] + 6-Stage Stepper
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
                        OutlinedButton(
                            onClick = {
                                if (currentStage > 1 && currentStage < 6) {
                                    currentStage--
                                    persistCurrentDraft(stageToSave = currentStage)
                                } else {
                                    onClose()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("ocr_header_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("رجوع", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SanadEmeraldGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تصوير فاتورة مورد (ocrPurchaseFlow)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onOpenUserGuide,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadElectricBlue),
                            modifier = Modifier.testTag("ocr_user_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "مساعدة (?)",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مساعدة (?)", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = onClose) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 6-Stage Stepper Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stageLabels.forEach { (stNum, label) ->
                        val isCurrent = currentStage == stNum
                        val isDone = currentStage > stNum
                        Surface(
                            color = when {
                                isCurrent -> SanadElectricBlue
                                isDone -> SanadEmeraldContainer
                                else -> SanadSurface
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    when {
                                        isCurrent -> SanadElectricBlue
                                        isDone -> SanadEmeraldGreen
                                        else -> SanadBorder
                                    },
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (extractedItems.isNotEmpty() || stNum == 1) {
                                        currentStage = stNum
                                    }
                                }
                                .testTag("ocr_stage_chip_$stNum")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    isCurrent -> Color.White
                                    isDone -> SanadEmeraldGreen
                                    else -> SanadTextSecondary
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = SanadBorder)

        // Inline 24-Hour Draft Resume Banner fallback
        if (showDraftResumeBanner && pendingDraft != null) {
            val draft = pendingDraft!!
            Surface(
                color = SanadAmberContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .border(1.dp, SanadAmberWarning, RoundedCornerShape(12.dp))
                    .testTag("ocr_draft_resume_banner"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = SanadAmberWarning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مسودة محلية صالحة لمدة 24 ساعة — المورد: ${draft.supplierName} (${draft.items.size} أصناف)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { applyResumeDraft(draft) },
                            colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ocr_draft_resume_button")
                        ) {
                            Text("استئناف")
                        }
                        OutlinedButton(
                            onClick = {
                                OcrPurchaseDraftManager.clearDraft(context)
                                pendingDraft = null
                                showDraftResumeBanner = false
                                extractedItems = emptyList()
                                currentStage = 1
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ocr_draft_start_fresh_button")
                        ) {
                            Text("بدء من جديد", color = SanadElectricBlue)
                        }
                        TextButton(
                            onClick = { showDraftResumeBanner = false },
                            modifier = Modifier.testTag("ocr_draft_cancel_button")
                        ) {
                            Text("إلغاء", color = SanadTextSecondary)
                        }
                    }
                }
            }
        }

        // Main Content per Stage
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentStage) {
                // ============================================================
                // STAGE 1: 1. التقاط ([التقاط صورة] | [اختيار من المعرض] | [اختيار ملف PDF <= 8MB])
                // ============================================================
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
                                Text(
                                    text = "المرحلة 1 (الالتقاط): اختر مصدر الفاتورة لتحليلها بالذكاء الاصطناعي",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadElectricBlue
                                )

                                // Duration & Privacy Hint (تلميح المدة/الخصوصية)
                                Surface(
                                    color = SanadNavyDark,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        text = "تلميح المدة والخصوصية: تستغرق القراءة الذكية من 5 إلى 15 ثانية • يتم ضغط الصورة محلياً (1024px) ولا تُحفظ بايتات الصورة في المسودة المحلية (24 ساعة) حفاظاً على الخصوصية ومساحة الجهاز.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SanadTextSecondary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = SanadBlueContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, SanadElectricBlue, RoundedCornerShape(12.dp))
                                            .clickable {
                                                runCatching { cameraPreviewLauncher.launch(null) }
                                                    .onFailure {
                                                        runAiAnalysisWithBitmap(createSampleInvoiceBitmap(), "التقاط صورة")
                                                    }
                                            }
                                            .testTag("ai_ocr_camera_button")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = SanadElectricBlue)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "التقاط صورة",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = SanadElectricBlue,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    Surface(
                                        color = SanadEmeraldContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, SanadEmeraldGreen, RoundedCornerShape(12.dp))
                                            .clickable {
                                                runCatching {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                }.onFailure {
                                                    runAiAnalysisWithBitmap(createSampleInvoiceBitmap(), "اختيار من المعرض")
                                                }
                                            }
                                            .testTag("ai_ocr_gallery_button")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = SanadEmeraldGreen)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "اختيار من المعرض",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = SanadEmeraldGreen,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    Surface(
                                        color = SanadAmberContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, SanadAmberWarning, RoundedCornerShape(12.dp))
                                            .clickable {
                                                runCatching { pdfPickerLauncher.launch("application/pdf") }
                                                    .onFailure {
                                                        runAiAnalysisWithBitmap(createSampleInvoiceBitmap(), "اختيار ملف PDF <= 8MB")
                                                    }
                                            }
                                            .testTag("ai_ocr_pdf_button")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = SanadAmberWarning)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "اختيار ملف PDF <= 8MB",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = SanadAmberWarning,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                if (fileValidationError != null) {
                                    Surface(
                                        color = SanadRedContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, SanadCoralRed, RoundedCornerShape(10.dp))
                                    ) {
                                        Text(
                                            text = fileValidationError!!,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadCoralRed,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        runAiAnalysisWithBitmap(
                                            createSampleInvoiceBitmap(),
                                            "فاتورة مورد ورقية: معصرة الجنوب ومؤسسة النور"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_ocr_instant_analyze_button")
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تحليل فاتورة مورد نموذجية والانتقال للمرحلة 2 (المراجعة والمطابقة)")
                                }

                                if (compressedSizeInfo.isNotBlank()) {
                                    Text(
                                        text = "$selectedSourceLabel • $compressedSizeInfo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SanadEmeraldGreen
                                    )
                                }
                            }
                        }
                    }

                    if (isAnalyzing) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                                    .testTag("ocr_reading_indicator"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = SanadElectricBlue)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "مؤشر القراءة: جاري قراءة الفاتورة عبر Gemini AI وحفظ المسودة المحلية...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SanadTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // ============================================================
                // STAGE 2: 2. رأس المراجعة وكل سطر (ocrPurchaseMapping)
                // ============================================================
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المرحلة 2: رأس المراجعة وبيانات المورد (ocrPurchaseMapping)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadElectricBlue
                                    )
                                    OutlinedButton(
                                        onClick = { showInlineAddSupplier = !showInlineAddSupplier },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ إضافة مورد", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                if (showInlineAddSupplier) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newSupplierNameInput,
                                            onValueChange = { newSupplierNameInput = it },
                                            label = { Text("اسم المورد الجديد") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                if (newSupplierNameInput.isNotBlank()) {
                                                    viewModel.quickCreateParty(
                                                        name = newSupplierNameInput,
                                                        phone = "",
                                                        partyType = PartyType.SUPPLIER,
                                                        currencyCode = documentCurrency
                                                    )
                                                    supplierName = newSupplierNameInput.trim()
                                                    newSupplierNameInput = ""
                                                    showInlineAddSupplier = false
                                                    persistCurrentDraft()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen)
                                        ) {
                                            Text("حفظ")
                                        }
                                    }
                                }

                                // Supplier Selector Chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    suppliers.forEach { sup ->
                                        val isSelected = supplierId == sup.id
                                        Surface(
                                            color = if (isSelected) SanadBlueContainer else SanadNavyDark,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .border(
                                                    1.dp,
                                                    if (isSelected) SanadElectricBlue else SanadBorder,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    supplierId = sup.id
                                                    supplierName = sup.name
                                                    persistCurrentDraft()
                                                }
                                        ) {
                                            Text(
                                                text = sup.name,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) SanadElectricBlue else SanadTextPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = supplierTaxNumber,
                                        onValueChange = {
                                            supplierTaxNumber = it
                                            persistCurrentDraft()
                                        },
                                        label = { Text("الرقم الضريبي") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = supplierInvoiceNumber,
                                        onValueChange = {
                                            supplierInvoiceNumber = it
                                            persistCurrentDraft()
                                        },
                                        label = { Text("رقم الفاتورة (تلقائي OCR-*)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = invoiceDateText,
                                        onValueChange = { invoiceDateText = it },
                                        label = { Text("التاريخ") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("YER", "USD", "SAR", "TRY").forEach { cur ->
                                        val selected = documentCurrency == cur
                                        Surface(
                                            color = if (selected) SanadEmeraldContainer else SanadNavyDark,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .border(
                                                    1.dp,
                                                    if (selected) SanadEmeraldGreen else SanadBorder,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    documentCurrency = cur
                                                    persistCurrentDraft()
                                                }
                                        ) {
                                            Text(
                                                text = "عملة الورقة: $cur",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (selected) SanadEmeraldGreen else SanadTextSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    itemsIndexed(extractedItems) { index, item ->
                        val isIgnored = item.mappingAction == OcrLineMappingAction.IGNORED
                        val isCreateNew = item.mappingAction == OcrLineMappingAction.CREATE_NEW
                        val isMatched = item.mappingAction == OcrLineMappingAction.MATCHED
                        val isDetailsExpanded = expandedLineEditIndex == index

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isIgnored) SanadNavyDark.copy(alpha = 0.6f) else SanadSurface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    when {
                                        isIgnored -> SanadBorder
                                        item.hasArithmeticMismatch -> SanadCoralRed
                                        isCreateNew -> SanadAmberWarning
                                        else -> SanadEmeraldGreen.copy(alpha = 0.7f)
                                    },
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${index + 1}. ${item.supplierItemName}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isIgnored) SanadTextSecondary else SanadTextPrimary
                                        )
                                        Text(
                                            text = "الوحدة: ${item.unitName} • الكمية: ${formatNumberCompact(item.quantity)} × التكلفة: ${formatMoney(item.supplierUnitPrice, documentCurrency)} = مجموع السطر: ${formatMoney(item.lineTotalCost, documentCurrency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SanadTextSecondary
                                        )
                                    }
                                    SanadBadge(
                                        text = when {
                                            isIgnored -> "متجاهل"
                                            isCreateNew -> "إنشاء جديد"
                                            else -> "مطابق: ${item.matchedLocalProductName ?: "كتالوج"}"
                                        },
                                        textColor = when {
                                            isIgnored -> SanadTextSecondary
                                            isCreateNew -> SanadAmberWarning
                                            else -> SanadEmeraldGreen
                                        },
                                        containerColor = when {
                                            isIgnored -> SanadNavyDark
                                            isCreateNew -> SanadAmberContainer
                                            else -> SanadEmeraldContainer
                                        }
                                    )
                                }

                                // Per-Line Action Buttons (ocrPurchaseButtons):
                                // [مطابقة / إعادة مطابقة] | [إنشاء جديد] | [تجاهل / إلغاء التجاهل] | [تأكيد مجاني / تكلفة صفر] | [تعديل الحقول]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { lineIndexForProductPicker = index },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (isMatched) SanadEmeraldGreen else SanadElectricBlue
                                        ),
                                        modifier = Modifier.testTag("ocr_line_match_btn_$index")
                                    ) {
                                        Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isMatched) "إعادة مطابقة" else "مطابقة",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val updated = extractedItems.toMutableList().apply {
                                                this[index] = item.copy(
                                                    mappingAction = OcrLineMappingAction.CREATE_NEW,
                                                    matchedLocalProductId = null,
                                                    matchedLocalProductName = null,
                                                    suggestedRetailPrice = if (item.suggestedRetailPrice > 0.0) {
                                                        item.suggestedRetailPrice
                                                    } else {
                                                        (item.supplierUnitPrice * 1.25).coerceAtLeast(100.0)
                                                    }
                                                )
                                            }
                                            extractedItems = updated
                                            persistCurrentDraft(itemsToSave = updated)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (isCreateNew) SanadAmberWarning else SanadTextPrimary
                                        ),
                                        modifier = Modifier.testTag("ocr_line_create_new_btn_$index")
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إنشاء جديد", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val nextAction = if (isIgnored) {
                                                if (item.matchedLocalProductId != null) OcrLineMappingAction.MATCHED else OcrLineMappingAction.CREATE_NEW
                                            } else {
                                                OcrLineMappingAction.IGNORED
                                            }
                                            val updated = extractedItems.toMutableList().apply {
                                                this[index] = item.copy(mappingAction = nextAction)
                                            }
                                            extractedItems = updated
                                            persistCurrentDraft(itemsToSave = updated)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (isIgnored) SanadElectricBlue else SanadCoralRed
                                        ),
                                        modifier = Modifier.testTag("ocr_line_ignore_btn_$index")
                                    ) {
                                        Icon(
                                            imageVector = if (isIgnored) Icons.Default.Restore else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isIgnored) "إلغاء التجاهل" else "تجاهل",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val updated = extractedItems.toMutableList().apply {
                                                this[index] = item.copy(
                                                    supplierUnitPrice = if (!item.isFreeBonusConfirmed) 0.0 else item.supplierUnitPrice,
                                                    extractedLineTotal = if (!item.isFreeBonusConfirmed) 0.0 else item.extractedLineTotal,
                                                    isFreeBonusConfirmed = !item.isFreeBonusConfirmed
                                                )
                                            }
                                            extractedItems = updated
                                            persistCurrentDraft(itemsToSave = updated)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (item.isFreeBonusConfirmed) SanadEmeraldGreen else SanadAmberWarning
                                        ),
                                        modifier = Modifier.testTag("ocr_line_free_confirm_btn_$index")
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (item.isFreeBonusConfirmed) "بونص مجاني مؤكد ✓" else "تأكيد مجاني / تكلفة صفر",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            expandedLineEditIndex = if (isDetailsExpanded) null else index
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadElectricBlue)
                                    ) {
                                        Text(
                                            text = if (isDetailsExpanded) "إخفاء الحقول" else "حقول السطر واللوط",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                // Expandable Per-Line Fields Editor:
                                // الاسم، الوحدة/المعامل، الكمية، التكلفة، مجموع السطر، باركود اختياري، سعر الجملة، ضريبة سطر، لوط/تاريخ صلاحية
                                if (isDetailsExpanded && !isIgnored) {
                                    var nameVal by remember(item.supplierItemName) { mutableStateOf(item.supplierItemName) }
                                    var unitVal by remember(item.unitName) { mutableStateOf(item.unitName) }
                                    var qtyVal by remember(item.quantity) { mutableStateOf(formatNumberCompact(item.quantity)) }
                                    var costVal by remember(item.supplierUnitPrice) { mutableStateOf(formatNumberCompact(item.supplierUnitPrice)) }
                                    var totalVal by remember(item.extractedLineTotal) { mutableStateOf(formatNumberCompact(item.extractedLineTotal)) }
                                    var barcodeVal by remember(item.barcode) { mutableStateOf(item.barcode) }
                                    var wholesaleVal by remember(item.suggestedWholesalePrice) { mutableStateOf(formatNumberCompact(item.suggestedWholesalePrice)) }
                                    var batchVal by remember(item.batchNumber) { mutableStateOf(item.batchNumber.orEmpty()) }
                                    var expiryVal by remember(item.expiryDateText) { mutableStateOf(item.expiryDateText.orEmpty()) }

                                    Surface(
                                        color = SanadNavyDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = nameVal,
                                                    onValueChange = { v ->
                                                        nameVal = v
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(supplierItemName = v)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("الاسم") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1.4f)
                                                )
                                                OutlinedTextField(
                                                    value = unitVal,
                                                    onValueChange = { v ->
                                                        unitVal = v
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(unitName = v)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("الوحدة / المعامل") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = qtyVal,
                                                    onValueChange = { v ->
                                                        qtyVal = v
                                                        val q = v.toDoubleOrNull() ?: item.quantity
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(quantity = q)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("الكمية") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = costVal,
                                                    onValueChange = { v ->
                                                        costVal = v
                                                        val c = v.toDoubleOrNull() ?: item.supplierUnitPrice
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(supplierUnitPrice = c)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("التكلفة") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = totalVal,
                                                    onValueChange = { v ->
                                                        totalVal = v
                                                        val t = v.toDoubleOrNull() ?: item.extractedLineTotal
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(extractedLineTotal = t)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("مجموع السطر") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = barcodeVal,
                                                    onValueChange = { v ->
                                                        barcodeVal = v
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(barcode = v)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("باركود اختياري") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = wholesaleVal,
                                                    onValueChange = { v ->
                                                        wholesaleVal = v
                                                        val w = v.toDoubleOrNull() ?: item.suggestedWholesalePrice
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(suggestedWholesalePrice = w)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("سعر الجملة") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = batchVal,
                                                    onValueChange = { v ->
                                                        batchVal = v
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(batchNumber = v, enableFefoTracking = v.isNotBlank() || item.enableFefoTracking)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("رقم اللوط / التشغيلة") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = expiryVal,
                                                    onValueChange = { v ->
                                                        expiryVal = v
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(expiryDateText = v, enableFefoTracking = v.isNotBlank() || item.enableFefoTracking)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("تاريخ الصلاحية") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Arithmetic Corrector (المصحح الحسابي للسطر: [تصحيح من الوحدة] و [تصحيح من المجموع])
                                if (item.hasArithmeticMismatch && !isIgnored) {
                                    Surface(
                                        color = SanadRedContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, SanadCoralRed, RoundedCornerShape(10.dp))
                                            .testTag("ocr_arithmetic_corrector_$index")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = SanadCoralRed)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "المصحح الحسابي للسطر: حاصل ضرب (${formatNumberCompact(item.quantity)} × ${formatNumberCompact(item.supplierUnitPrice)} = ${formatNumberCompact(item.expectedLineTotalFromUnit)}) لا يتطابق مع مجموع السطر (${formatNumberCompact(item.extractedLineTotal)})",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadTextPrimary
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val fixedTotal = item.quantity * item.supplierUnitPrice
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(extractedLineTotal = fixedTotal)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("ocr_fix_from_unit_btn_$index")
                                                ) {
                                                    Text("تصحيح من الوحدة", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = {
                                                        val fixedUnit = if (item.quantity > 0.0) {
                                                            item.extractedLineTotal / item.quantity
                                                        } else item.supplierUnitPrice
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[index] = item.copy(supplierUnitPrice = fixedUnit)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("ocr_fix_from_total_btn_$index")
                                                ) {
                                                    Text("تصحيح من المجموع", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }

                                // FEFO Batch & Expiry Tracking Toggle ([تفعيل تتبع الدفعات FEFO])
                                if (!isIgnored) {
                                    Surface(
                                        color = SanadBlueContainer.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, SanadElectricBlue.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "تفعيل تتبع الدفعات والصلاحية (FEFO)",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadElectricBlue
                                                )
                                                Text(
                                                    text = "لوط: ${item.batchNumber.orEmpty().ifBlank { "LOT-AUTO" }} • صلاحية: ${item.expiryDateText.orEmpty().ifBlank { "2027-06-01" }}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SanadTextSecondary
                                                )
                                            }
                                            Switch(
                                                checked = item.enableFefoTracking,
                                                onCheckedChange = { enabled ->
                                                    val updated = extractedItems.toMutableList().apply {
                                                        this[index] = item.copy(enableFefoTracking = enabled)
                                                    }
                                                    extractedItems = updated
                                                    persistCurrentDraft(itemsToSave = updated)
                                                },
                                                colors = SwitchDefaults.colors(checkedThumbColor = SanadEmeraldGreen)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ============================================================
                // STAGE 3: 3. أسعار جديدة (Mandatory Retail & Wholesale + [متابعة])
                // ============================================================
                3 -> {
                    item {
                        val newLines = extractedItems.mapIndexedNotNull { idx, it ->
                            if (it.mappingAction == OcrLineMappingAction.CREATE_NEW) idx to it else null
                        }
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
                                    text = "المرحلة 3: تحديد أسعار البيع الإلزامية للأصناف المنشأة حديثاً (${newLines.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadElectricBlue
                                )
                                Text(
                                    text = "يشترط إدخال سعر بيع المفرق (إلزامي > 0) وسعر الجملة لكل صنف جديد قبل المتابعة:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextSecondary
                                )

                                if (newLines.isEmpty()) {
                                    Text(
                                        text = "جميع الأصناف مطابقة مع الكتالوج المحلي — لا توجد أصناف جديدة تتطلب تسعيراً تأسيسياً.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SanadEmeraldGreen
                                    )
                                }

                                newLines.forEach { (origIndex, item) ->
                                    var retailInput by remember(item.suggestedRetailPrice) {
                                        mutableStateOf(formatNumberCompact(item.suggestedRetailPrice))
                                    }
                                    var wholesaleInput by remember(item.suggestedWholesalePrice) {
                                        mutableStateOf(formatNumberCompact(item.suggestedWholesalePrice))
                                    }

                                    Surface(
                                        color = SanadNavyDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                if (item.suggestedRetailPrice <= 0.0) SanadCoralRed else SanadAmberWarning,
                                                RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = item.supplierItemName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = SanadTextPrimary
                                            )
                                            Text(
                                                text = "تكلفة الوحدة من المورد: ${formatMoney(item.supplierUnitPrice, documentCurrency)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SanadTextSecondary
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = retailInput,
                                                    onValueChange = { v ->
                                                        retailInput = v
                                                        val parsed = v.toDoubleOrNull() ?: 0.0
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[origIndex] = item.copy(suggestedRetailPrice = parsed)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("سعر المفرق (إلزامي *)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = wholesaleInput,
                                                    onValueChange = { v ->
                                                        wholesaleInput = v
                                                        val parsed = v.toDoubleOrNull() ?: 0.0
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[origIndex] = item.copy(suggestedWholesalePrice = parsed)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("سعر الجملة") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val missingRetail = extractedItems.any {
                                            it.mappingAction == OcrLineMappingAction.CREATE_NEW && it.suggestedRetailPrice <= 0.0
                                        }
                                        if (missingRetail) {
                                            fileValidationError = "سعر بيع المفرق إلزامي (> 0) لجميع الأصناف الجديدة."
                                        } else {
                                            fileValidationError = null
                                            currentStage = 4
                                            persistCurrentDraft(stageToSave = 4)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ocr_stage3_continue_btn")
                                ) {
                                    Text("متابعة إلى رادار الأسعار (4/6)")
                                }
                            }
                        }
                    }
                }

                // ============================================================
                // STAGE 4: 4. رادار الأسعار (Price Radar + [متابعة])
                // ============================================================
                4 -> {
                    item {
                        val radarLines = extractedItems.mapIndexedNotNull { idx, it ->
                            if (it.mappingAction == OcrLineMappingAction.MATCHED && it.isCostChanged) idx to it else null
                        }
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
                                    Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = SanadAmberWarning)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المرحلة 4: رادار الأسعار ومقارنة تغير التكلفة (${radarLines.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadAmberWarning
                                    )
                                }
                                Text(
                                    text = "يكشف رادار الأسعار الأصناف التي تغيرت تكلفتها عن الكتالوج ويتيح تحديث أسعار البيع فوراً:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextSecondary
                                )

                                radarLines.forEach { (origIndex, item) ->
                                    var newRetailStr by remember(item.suggestedRetailPrice) {
                                        mutableStateOf(formatNumberCompact(item.suggestedRetailPrice))
                                    }
                                    val pct = (item.priceIncreasePercent * 10.0).roundToLong() / 10.0

                                    Surface(
                                        color = SanadNavyDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, SanadAmberWarning, RoundedCornerShape(10.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.supplierItemName,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadTextPrimary
                                                )
                                                SanadBadge(
                                                    text = if (pct >= 0) "تغير +$pct%" else "تغير $pct%",
                                                    textColor = if (pct >= 0) SanadCoralRed else SanadEmeraldGreen,
                                                    containerColor = if (pct >= 0) SanadRedContainer else SanadEmeraldContainer
                                                )
                                            }
                                            Text(
                                                text = "تكلفة الكتالوج السابقة: ${formatMoney(item.previousCostPrice, documentCurrency)} -> التكلفة الواردة: ${formatMoney(item.supplierUnitPrice, documentCurrency)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SanadTextSecondary
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = newRetailStr,
                                                    onValueChange = { v ->
                                                        newRetailStr = v
                                                        val parsed = v.toDoubleOrNull() ?: item.suggestedRetailPrice
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[origIndex] = item.copy(suggestedRetailPrice = parsed)
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    },
                                                    label = { Text("سعر البيع المحدث") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.width(165.dp)
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "تحديث السعر",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = SanadTextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Switch(
                                                        checked = item.applyPriceRadarUpdate,
                                                        onCheckedChange = { checked ->
                                                            val updated = extractedItems.toMutableList().apply {
                                                                this[origIndex] = item.copy(applyPriceRadarUpdate = checked)
                                                            }
                                                            extractedItems = updated
                                                            persistCurrentDraft(itemsToSave = updated)
                                                        },
                                                        colors = SwitchDefaults.colors(checkedThumbColor = SanadEmeraldGreen)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        currentStage = 5
                                        persistCurrentDraft(stageToSave = 5)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ocr_stage4_continue_btn")
                                ) {
                                    Text("متابعة إلى الدفع والمنصة (5/6)")
                                }
                            }
                        }
                    }
                }

                // ============================================================
                // STAGE 5: 5. الدفع والمنصة (ocrPurchasePayPlatform & ocrPurchaseCurrencyLots)
                // ============================================================
                5 -> {
                    item {
                        val ignoredLines = extractedItems.mapIndexedNotNull { idx, it ->
                            if (it.mappingAction == OcrLineMappingAction.IGNORED) idx to it else null
                        }
                        val activeLines = extractedItems.filter { it.mappingAction != OcrLineMappingAction.IGNORED }
                        val rawSubtotal = activeLines.sumOf { it.lineTotalCost }
                        val taxVal = if (enableTaxInWizard) (taxAmountStr.toDoubleOrNull() ?: 0.0) else 0.0
                        val shippingVal = shippingAdditionsStr.toDoubleOrNull() ?: 0.0
                        val discountVal = supplierDiscountStr.toDoubleOrNull() ?: 0.0
                        val netTotal = (rawSubtotal + (if (includeTaxInLandedCost) taxVal else 0.0) + shippingVal - discountVal).coerceAtLeast(0.0)

                        val matchingAccounts = financialAccounts.filter { it.currencyCode == documentCurrency }
                        val chosenAccount = matchingAccounts.find { it.id == selectedAccountId } ?: matchingAccounts.firstOrNull()
                        val isImmediatePayment = singlePaymentSourceType == "CASH" || singlePaymentSourceType == "BANK"
                        val isCashInsufficient = isImmediatePayment &&
                            (chosenAccount == null || chosenAccount.currentBalance < netTotal)

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Summary of Rejected/Ignored Lines (ملخص الأسطر المرفوضة/المتجاهلة)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SanadSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                                    .testTag("ocr_ignored_lines_box")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "ملخص الأسطر المرفوضة / المتجاهلة (${ignoredLines.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadAmberWarning
                                    )
                                    if (ignoredLines.isEmpty()) {
                                        Text(
                                            text = "لا توجد أسطر مرفوضة — سيتم ترحيل جميع الأسطر النشطة (${activeLines.size}).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SanadTextSecondary
                                        )
                                    } else {
                                        ignoredLines.forEach { (origIdx, item) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "• ${item.supplierItemName} (${formatNumberCompact(item.quantity)} ${item.unitName})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SanadTextSecondary
                                                )
                                                TextButton(
                                                    onClick = {
                                                        val updated = extractedItems.toMutableList().apply {
                                                            this[origIdx] = item.copy(
                                                                mappingAction = if (item.matchedLocalProductId != null) {
                                                                    OcrLineMappingAction.MATCHED
                                                                } else {
                                                                    OcrLineMappingAction.CREATE_NEW
                                                                }
                                                            )
                                                        }
                                                        extractedItems = updated
                                                        persistCurrentDraft(itemsToSave = updated)
                                                    }
                                                ) {
                                                    Text("إلغاء التجاهل", color = SanadElectricBlue)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Payment & Platform Card (ocrPurchasePayPlatform)
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
                                        text = "المرحلة 5: الدفع والمنصة والتكاليف (ocrPurchasePayPlatform)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadElectricBlue
                                    )

                                    // Tax Enable & Include Tax in Cost
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "تفعيل الضريبة على الفاتورة",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SanadTextPrimary
                                        )
                                        Switch(
                                            checked = enableTaxInWizard,
                                            onCheckedChange = {
                                                enableTaxInWizard = it
                                                persistCurrentDraft()
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = SanadElectricBlue)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "تضمين الضريبة في التكلفة (WAC Landed Cost)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SanadTextPrimary
                                        )
                                        Switch(
                                            checked = includeTaxInLandedCost,
                                            onCheckedChange = {
                                                includeTaxInLandedCost = it
                                                persistCurrentDraft()
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = SanadEmeraldGreen)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = taxAmountStr,
                                            onValueChange = {
                                                taxAmountStr = it
                                                persistCurrentDraft()
                                            },
                                            enabled = enableTaxInWizard,
                                            label = { Text("الضريبة") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = shippingAdditionsStr,
                                            onValueChange = {
                                                shippingAdditionsStr = it
                                                persistCurrentDraft()
                                            },
                                            label = { Text("شحن إضافي") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = supplierDiscountStr,
                                            onValueChange = {
                                                supplierDiscountStr = it
                                                persistCurrentDraft()
                                            },
                                            label = { Text("خصم مورد") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الإبقاء على عملة الورقة ($documentCurrency)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SanadTextPrimary
                                        )
                                        Switch(
                                            checked = keepDocumentCurrency,
                                            onCheckedChange = {
                                                keepDocumentCurrency = it
                                                persistCurrentDraft()
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = SanadElectricBlue)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = ocrNotes,
                                        onValueChange = { ocrNotes = it },
                                        label = { Text("الملاحظات") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    HorizontalDivider(color = SanadBorder)

                                    // Single Payment Source Restriction (مصدر دفع واحد فقط: نقد أو حساب بنكي أو آجل)
                                    Text(
                                        text = "مصدر الدفع (مصدر واحد فقط داخل معالج OCR — لتقسيم الدفع عدّل الفاتورة بعد حفظها):",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SanadTextPrimary
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(
                                            "CASH" to "نقد (صندوق)",
                                            "BANK" to "حساب بنكي",
                                            "CREDIT" to "آجل (ذمة مورد)"
                                        ).forEach { (srcKey, label) ->
                                            val selected = singlePaymentSourceType == srcKey
                                            Surface(
                                                color = if (selected) SanadBlueContainer else SanadNavyDark,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(
                                                        1.dp,
                                                        if (selected) SanadElectricBlue else SanadBorder,
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        singlePaymentSourceType = srcKey
                                                        persistCurrentDraft()
                                                    }
                                                    .testTag("ocr_pay_source_$srcKey")
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (selected) SanadElectricBlue else SanadTextSecondary,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (isImmediatePayment) {
                                        // RBAC treasury.manage verification row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "التحقق من صلاحية الخزينة (treasury.manage)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (treasuryManagePermissionToggle) SanadEmeraldGreen else SanadCoralRed
                                            )
                                            Switch(
                                                checked = treasuryManagePermissionToggle,
                                                onCheckedChange = { treasuryManagePermissionToggle = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = SanadEmeraldGreen)
                                            )
                                        }

                                        matchingAccounts.forEach { acc ->
                                            val isSelected = (chosenAccount?.id == acc.id)
                                            Surface(
                                                color = if (isSelected) SanadEmeraldContainer else SanadNavyDark,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) SanadEmeraldGreen else SanadBorder,
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        selectedAccountId = acc.id
                                                        persistCurrentDraft()
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(acc.name, color = SanadTextPrimary, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        text = "الرصيد: ${formatMoney(acc.currentBalance, acc.currencyCode)}",
                                                        color = if (acc.currentBalance >= netTotal) SanadEmeraldGreen else SanadCoralRed,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        if (!treasuryManagePermissionToggle) {
                                            Surface(
                                                color = SanadRedContainer,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, SanadCoralRed, RoundedCornerShape(10.dp))
                                            ) {
                                                Text(
                                                    text = "تنبيه صلاحيات: لا يملك الكاشير الحالي صلاحية السداد النقدي/البنكي (treasury.manage). يرجى التحويل إلى آجل أو تفعيل الصلاحية.",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadCoralRed,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }

                                        if (isCashInsufficient) {
                                            Surface(
                                                color = SanadRedContainer,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, SanadCoralRed, RoundedCornerShape(10.dp))
                                                    .testTag("ocr_insufficient_cash_warning")
                                            ) {
                                                Text(
                                                    text = "تنبيه الخزينة: رصيد الصندوق/الحساب البنكي المحدد غير كافٍ لسداد صافي الفاتورة (${formatMoney(netTotal, documentCurrency)}). يرجى اختيار حساب آخر أو التحويل إلى آجل.",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadCoralRed,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "صافي الفاتورة النهائي شاملاً التكاليف: ${formatMoney(netTotal, documentCurrency)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = SanadEmeraldGreen
                                    )

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val effInvNo = supplierInvoiceNumber.ifBlank {
                                                    "OCR-${System.currentTimeMillis().toString().takeLast(5)}"
                                                }
                                                val isDup = viewModel.isDuplicateSupplierPurchaseInvoice(supplierName, effInvNo)
                                                if (isDup) {
                                                    fileValidationError = "خطأ تكرار فاتورة: توجد فاتورة شراء مسجلة مسبقاً لنفس المورد بنفس الرقم ($effInvNo)!"
                                                    return@launch
                                                }
                                                if (isImmediatePayment && !treasuryManagePermissionToggle) {
                                                    fileValidationError = "عذراً: تتطلب عملية الدفع الفوري صلاحية (treasury.manage)!"
                                                    return@launch
                                                }
                                                if (isCashInsufficient) {
                                                    fileValidationError = "رصيد الخزينة / الحساب البنكي غير كافٍ للدفع الفوري!"
                                                    return@launch
                                                }
                                                viewModel.commitSixStageOcrPurchase(
                                                    supplierId = supplierId,
                                                    supplierName = supplierName,
                                                    supplierTaxNumber = supplierTaxNumber,
                                                    paperInvoiceNumber = effInvNo,
                                                    currencyCode = if (keepDocumentCurrency) documentCurrency else "YER",
                                                    exchangeRateLocked = exchangeRate,
                                                    includeTaxInCost = includeTaxInLandedCost,
                                                    taxAmount = taxVal,
                                                    shippingAdditionAmount = shippingVal,
                                                    supplierDiscountAmount = discountVal,
                                                    paymentMethod = effectivePaymentMethod,
                                                    financialAccountId = chosenAccount?.id,
                                                    financialAccountName = chosenAccount?.name ?: "صندوق نقدي",
                                                    items = extractedItems,
                                                    onCommitted = { purNumber ->
                                                        OcrPurchaseDraftManager.clearDraft(context)
                                                        finalCommittedPurNumber = purNumber
                                                        currentStage = 6
                                                    }
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("ocr_commit_stage5_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "تأكيد وحفظ",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ============================================================
                // STAGE 6: 6. تم (Displays Final Invoice Number + Open Shift Tag + [تم])
                // ============================================================
                6 -> {
                    item {
                        val openShiftTag = activeShift?.let { "الوردية المفتوحة: ${it.cashierName} (#${it.id})" }
                            ?: "الوردية المفتوحة: الوردية الصباحية النشطة (#1)"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, SanadEmeraldGreen, RoundedCornerShape(16.dp))
                                .testTag("ocr_stage6_completed_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SanadEmeraldGreen,
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = "تم ترحيل فاتورة المشتريات وتحديث المخزون بنجاح!",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = SanadEmeraldGreen,
                                    textAlign = TextAlign.Center
                                )
                                Surface(
                                    color = SanadEmeraldContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.border(1.dp, SanadEmeraldGreen, RoundedCornerShape(12.dp))
                                ) {
                                    Text(
                                        text = "رقم فاتورة الشراء النهائي: ${finalCommittedPurNumber ?: "PUR-OCR-DONE"}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = SanadTextPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                                SanadBadge(
                                    text = openShiftTag,
                                    textColor = SanadElectricBlue,
                                    containerColor = SanadBlueContainer
                                )
                                Text(
                                    text = "• تم وسم الفاتورة بالوردية المفتوحة وتحديث متوسط التكلفة المرجح (WAC).\n• تم تسجيل دفعات الصلاحية (FEFO) للأصناف المفعلة.\n• تم مسح المسودة المحلية المؤقتة (ocrPurchaseDraft) تلقائياً.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onClose,
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ocr_stage6_done_button")
                                ) {
                                    Text("تم")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Wizard Navigation Bar (السابق / متابعة)
        if (currentStage in 1..5) {
            Surface(
                color = SanadSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentStage > 1) {
                                currentStage--
                                persistCurrentDraft(stageToSave = currentStage)
                            } else {
                                onClose()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentStage > 1) "رجوع" else "إلغاء")
                    }

                    if (currentStage < 5 && extractedItems.isNotEmpty()) {
                        Button(
                            onClick = {
                                if (currentStage == 2) {
                                    val unconfirmedZero = extractedItems.any {
                                        it.mappingAction != OcrLineMappingAction.IGNORED &&
                                            it.supplierUnitPrice <= 0.0 &&
                                            !it.isFreeBonusConfirmed
                                    }
                                    if (unconfirmedZero) {
                                        fileValidationError = "يرجى النقر على [تأكيد مجاني / تكلفة صفر] للأسطر ذات التكلفة الصفرية قبل المتابعة."
                                        return@Button
                                    }
                                }
                                if (currentStage == 3) {
                                    val missingRetail = extractedItems.any {
                                        it.mappingAction == OcrLineMappingAction.CREATE_NEW && it.suggestedRetailPrice <= 0.0
                                    }
                                    if (missingRetail) {
                                        fileValidationError = "سعر بيع المفرق إلزامي (> 0) لجميع الأصناف الجديدة في المرحلة 3."
                                        return@Button
                                    }
                                }
                                fileValidationError = null
                                currentStage++
                                persistCurrentDraft(stageToSave = currentStage)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("ocr_next_stage_button")
                        ) {
                            Text("متابعة (${currentStage + 1}/6)")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    // Local Product Picker Dialog for [مطابقة / إعادة مطابقة]
    if (lineIndexForProductPicker != null) {
        val targetIndex = lineIndexForProductPicker!!
        AlertDialog(
            onDismissRequest = { lineIndexForProductPicker = null },
            containerColor = SanadSurface,
            title = {
                Text("اختر صنفاً من المخزون لمطابقة السطر", color = SanadTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    products.forEach { p ->
                        Surface(
                            color = SanadNavyDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    val oldItem = extractedItems.getOrNull(targetIndex)
                                    if (oldItem != null) {
                                        val updated = extractedItems.toMutableList().apply {
                                            this[targetIndex] = oldItem.copy(
                                                mappingAction = OcrLineMappingAction.MATCHED,
                                                matchedLocalProductId = p.id,
                                                matchedLocalProductName = p.name,
                                                previousCostPrice = p.weightedAverageCost,
                                                currentRetailPrice = p.retailPrice
                                            )
                                        }
                                        extractedItems = updated
                                        persistCurrentDraft(itemsToSave = updated)
                                    }
                                    lineIndexForProductPicker = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(p.name, color = SanadTextPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "التكلفة: ${formatNumberCompact(p.weightedAverageCost)}",
                                    color = SanadEmeraldGreen
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { lineIndexForProductPicker = null }) {
                    Text("إغلاق", color = SanadTextSecondary)
                }
            }
        )
    }
}

private fun createSampleInvoiceBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(1280, 960, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(AndroidColor.WHITE)
    val paint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 36f
        isAntiAlias = true
    }
    canvas.drawText("فاتورة مورد - معصرة الجنوب ومؤسسة النور", 100f, 120f, paint)
    canvas.drawText("1. زيت زيتون بكر ممتاز - 32 لتر × 7350 ر.ي", 100f, 240f, paint)
    canvas.drawText("2. أرز بسمتي 5 كغ - 20 كيس × 8900 ر.ي", 100f, 340f, paint)
    return bmp
}
