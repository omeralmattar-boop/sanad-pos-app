package com.example.ui.screens.invoices

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.CurrencyAndRateEntity
import com.example.data.local.FinancialAccountEntity
import com.example.data.local.InvoiceMode
import com.example.data.local.InvoiceWithLines
import com.example.data.local.PartyEntity
import com.example.data.local.PartyType
import com.example.data.local.PaymentMethod
import com.example.data.local.PricingTier
import com.example.data.local.ProductEntity
import com.example.ui.components.BarcodeScannerSimulationDialog
import com.example.ui.components.DirectNumpadDialog
import com.example.ui.components.SanadBadge
import com.example.ui.components.formatArabicDate
import com.example.ui.components.formatMoney
import com.example.ui.components.formatNumberCompact
import com.example.ui.components.formatShortDate
import com.example.ui.theme.AroniumTileDiscount
import com.example.ui.theme.AroniumTileHold
import com.example.ui.theme.AroniumTilePayment
import com.example.ui.theme.AroniumTileVoid
import com.example.ui.theme.SanadAmberContainer
import com.example.ui.theme.SanadAmberWarning
import com.example.ui.theme.SanadBlueContainer
import com.example.ui.theme.SanadBorder
import com.example.ui.theme.SanadCoralRed
import com.example.ui.theme.SanadElectricBlue
import com.example.ui.theme.SanadEmeraldContainer
import com.example.ui.theme.SanadEmeraldGreen
import com.example.ui.theme.SanadNavyDark
import com.example.ui.theme.SanadNavyDeep
import com.example.ui.theme.SanadRedContainer
import com.example.ui.theme.SanadSurface
import com.example.ui.theme.SanadSurfaceElevated
import com.example.ui.theme.SanadTextMuted
import com.example.ui.theme.SanadTextPrimary
import com.example.ui.theme.SanadTextSecondary
import com.example.ui.viewmodel.DraftInvoiceItem
import com.example.ui.viewmodel.InvoiceEditorState
import com.example.ui.viewmodel.SanadViewModel

import android.content.Intent
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext

private enum class LineNumpadFieldType {
    QUANTITY,
    UNIT_PRICE,
    LINE_TOTAL_REVERSE
}

private data class NumpadTarget(
    val lineKey: Long,
    val fieldType: LineNumpadFieldType,
    val productName: String,
    val currentValue: Double,
    val unitOrSymbol: String
)

@Composable
fun UnifiedInvoiceEditorScreen(
    viewModel: SanadViewModel,
    onOpenNewProductModal: () -> Unit,
    onOpenAiAssistant: () -> Unit = {},
    onOpenUserGuide: () -> Unit = {},
    onCloseScreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.invoiceState.collectAsState()
    val products by viewModel.products.collectAsState()
    val parties by viewModel.parties.collectAsState()
    val currencies by viewModel.currencies.collectAsState()
    val accounts by viewModel.financialAccounts.collectAsState()
    val heldInvoices by viewModel.heldInvoices.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var searchQuery by remember { mutableStateOf("") }
    var numpadTarget by remember { mutableStateOf<NumpadTarget?>(null) }
    var showNetTotalAutoDiscountNumpad by remember { mutableStateOf(false) }
    var showRateLockEditor by remember { mutableStateOf(false) }
    var showHeldInvoicesSheet by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showReturnQrScanner by remember { mutableStateOf(false) }
    var showQuickAddPartyDialog by remember { mutableStateOf(false) }

    val activeSymbol = currencies.find { it.code == state.currencyCode }?.symbol ?: "ر.ي"

    val shareB2BSanadPayload: () -> Unit = {
        val (invNum, jsonContent) = viewModel.buildCurrentOrLastB2BSanadJson()
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "ملف فاتورة سند B2B ($invNum.sanad)")
            putExtra(Intent.EXTRA_TEXT, jsonContent)
        }
        context.startActivity(Intent.createChooser(sendIntent, "تصدير ومشاركة فاتورة B2B (.sanad)"))
        viewModel.showMessage("تم توليد ملف B2B ($invNum.sanad) جاهز للمشاركة مع المشتري")
    }

    val executeKeyboardShortcut: (String) -> Unit = { fKey ->
        when (fKey) {
            "F2" -> showBarcodeScanner = true
            "F3" -> {
                val nextTier = if (state.pricingTier == PricingTier.RETAIL) PricingTier.WHOLESALE else PricingTier.RETAIL
                viewModel.setPricingTier(nextTier)
            }
            "F4" -> viewModel.commitCurrentInvoice(holdOnly = true)
            "F5" -> showHeldInvoicesSheet = true
            "F6" -> viewModel.setPaymentMethod(PaymentMethod.CASH)
            "F7" -> viewModel.setPaymentMethod(PaymentMethod.CREDIT)
            "F8" -> showNetTotalAutoDiscountNumpad = true
            "F10" -> viewModel.commitCurrentInvoice(holdOnly = false, printReceipt = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.F2 -> { executeKeyboardShortcut("F2"); true }
                        Key.F3 -> { executeKeyboardShortcut("F3"); true }
                        Key.F4 -> { executeKeyboardShortcut("F4"); true }
                        Key.F5 -> { executeKeyboardShortcut("F5"); true }
                        Key.F6 -> { executeKeyboardShortcut("F6"); true }
                        Key.F7 -> { executeKeyboardShortcut("F7"); true }
                        Key.F8 -> { executeKeyboardShortcut("F8"); true }
                        Key.F10 -> { executeKeyboardShortcut("F10"); true }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Top Header Bar: Title + (?) User Guide + Smart AI Robot Icon + Close (X) Button
        InvoiceEditorTopBar(
            mode = state.mode,
            onModeSelected = { viewModel.setInvoiceMode(it) },
            onOpenAiAssistant = onOpenAiAssistant,
            onOpenUserGuide = onOpenUserGuide,
            onClose = onCloseScreen
        )

        // 1b. Hardware & Touch Keyboard Shortcuts Strip (F2–F8 & F10 للدفع والحفظ السريع)
        Surface(
            color = SanadSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "F2" to "F2 باركود",
                    "F3" to "F3 مفرق/جملة",
                    "F4" to "F4 تعليق",
                    "F5" to "F5 المعلقة",
                    "F6" to "F6 نقدي",
                    "F7" to "F7 آجل",
                    "F8" to "F8 خصم",
                    "F10" to "F10 دفع وحفظ سريع"
                ).forEach { (keyCode, label) ->
                    val isF10 = keyCode == "F10"
                    Surface(
                        color = if (isF10) SanadEmeraldContainer else SanadNavyDark,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (isF10) SanadEmeraldGreen else SanadBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { executeKeyboardShortcut(keyCode) }
                            .testTag("invoice_shortcut_$keyCode")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isF10) SanadEmeraldGreen else SanadTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 2. Main Content: Portrait Vertical Editor OR Landscape Aronium Split-Screen POS
        Box(modifier = Modifier.weight(1f)) {
            if (isLandscape) {
                LandscapeAroniumPosLayout(
                    state = state,
                    products = products,
                    parties = parties,
                    currencies = currencies,
                    accounts = accounts,
                    heldCount = heldInvoices.size,
                    activeSymbol = activeSymbol,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onBarcodeSubmitted = {
                        viewModel.onBarcodeOrSearchSubmitted(it)
                        searchQuery = ""
                    },
                    onOpenCameraScanner = { showBarcodeScanner = true },
                    onProductClicked = { viewModel.addProductToInvoice(it) },
                    onOpenNewProductModal = onOpenNewProductModal,
                    onNumpadRequested = { numpadTarget = it },
                    onEditFinalNetTotalDiscount = { showNetTotalAutoDiscountNumpad = true },
                    onCycleUnit = { viewModel.cycleLineUnit(it) },
                    onRemoveLine = { viewModel.removeLineItem(it) },
                    onHoldInvoice = { viewModel.commitCurrentInvoice(holdOnly = true) },
                    onShowHeldInvoices = { showHeldInvoicesSheet = true },
                    onLoadLastInvoice = { viewModel.loadLastCompletedInvoice() },
                    onToggleGrid = { viewModel.toggleAroniumProductGrid() },
                    viewModel = viewModel
                )
            } else {
                PortraitUnifiedInvoiceContent(
                    state = state,
                    products = products,
                    parties = parties,
                    currencies = currencies,
                    accounts = accounts,
                    heldCount = heldInvoices.size,
                    activeSymbol = activeSymbol,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onBarcodeSubmitted = {
                        viewModel.onBarcodeOrSearchSubmitted(it)
                        searchQuery = ""
                    },
                    onOpenCameraScanner = { showBarcodeScanner = true },
                    onOpenReturnQrScanner = { showReturnQrScanner = true },
                    onProductClicked = {
                        viewModel.addProductToInvoice(it)
                        searchQuery = ""
                    },
                    onOpenNewProductModal = onOpenNewProductModal,
                    onOpenQuickAddParty = { showQuickAddPartyDialog = true },
                    onNumpadRequested = { numpadTarget = it },
                    onCycleUnit = { viewModel.cycleLineUnit(it) },
                    onRemoveLine = { viewModel.removeLineItem(it) },
                    onHoldInvoice = { viewModel.commitCurrentInvoice(holdOnly = true) },
                    onShowHeldInvoices = { showHeldInvoicesSheet = true },
                    onLoadLastInvoice = { viewModel.loadLastCompletedInvoice() },
                    onEditRateLock = { showRateLockEditor = true },
                    onExportB2BSanad = shareB2BSanadPayload,
                    viewModel = viewModel
                )
            }
        }

        // 3. Sticky Bottom Footer: Dual-Currency Subtotals + Clickable Net Total (Auto Discount) + Action Buttons
        StickyDualCurrencyInvoiceFooter(
            state = state,
            activeSymbol = activeSymbol,
            onEditNetTotalForDiscount = { showNetTotalAutoDiscountNumpad = true },
            onConvertQuotationToSale = { viewModel.convertQuotationToSaleInvoice() },
            onExportB2BSanad = shareB2BSanadPayload,
            onSaveAndPrint = { viewModel.commitCurrentInvoice(holdOnly = false, printReceipt = true) },
            onSaveAndNew = { viewModel.commitCurrentInvoice(holdOnly = false, printReceipt = false) }
        )
    }

    // ========================================================================
    // INTERACTIVE DIALOGS & MODALS
    // ========================================================================

    // Direct Numpad Dialog for tapping on Quantity, Unit Price, OR Line Total (Reverse Calculation)
    numpadTarget?.let { target ->
        val titleText = when (target.fieldType) {
            LineNumpadFieldType.QUANTITY -> "تعديل الكمية"
            LineNumpadFieldType.UNIT_PRICE -> "تعديل سعر الوحدة"
            LineNumpadFieldType.LINE_TOTAL_REVERSE -> "تعديل مجموع السطر (حساب عكسي للسعر)"
        }
        DirectNumpadDialog(
            title = titleText,
            subtitle = target.productName,
            initialValue = target.currentValue,
            currencySymbol = target.unitOrSymbol,
            onDismiss = { numpadTarget = null },
            onConfirm = { newValue ->
                when (target.fieldType) {
                    LineNumpadFieldType.QUANTITY -> viewModel.updateLineQuantity(target.lineKey, newValue)
                    LineNumpadFieldType.UNIT_PRICE -> viewModel.updateLineUnitPrice(target.lineKey, newValue)
                    LineNumpadFieldType.LINE_TOTAL_REVERSE -> viewModel.updateLineTotalReversely(target.lineKey, newValue)
                }
                numpadTarget = null
            }
        )
    }

    // Direct Numpad Dialog for Automatic Discount from Final Net Total (الخصم التلقائي من الإجمالي)
    if (showNetTotalAutoDiscountNumpad) {
        DirectNumpadDialog(
            title = "تعديل الإجمالي النهائي (خصم تلقائي)",
            subtitle = "أدخل الإجمالي النهائي المطلوب ليُحتسب الفرق كخصم فاتورة تلقائياً",
            initialValue = state.netTotalAmount,
            currencySymbol = activeSymbol,
            onDismiss = { showNetTotalAutoDiscountNumpad = false },
            onConfirm = { newTargetTotal ->
                viewModel.applyAutoDiscountFromFinalNetTotal(newTargetTotal)
                showNetTotalAutoDiscountNumpad = false
            }
        )
    }

    // Exchange Rate Lock Editor Dialog
    if (showRateLockEditor) {
        DirectNumpadDialog(
            title = "تثبيت سعر الصرف (Rate Lock)",
            subtitle = "1 $ = كم ${state.currencyCode} على هذه الفاتورة؟",
            initialValue = state.lockedExchangeRate,
            currencySymbol = activeSymbol,
            onDismiss = { showRateLockEditor = false },
            onConfirm = { newRate ->
                viewModel.updateLockedExchangeRate(newRate)
                showRateLockEditor = false
            }
        )
    }

    // Camera Barcode & PLU Weight Scanner Dialog
    if (showBarcodeScanner) {
        val barcodePresets = buildList {
            products.forEach { prod ->
                add(prod.barcode to "${prod.name} (${prod.baseUnitName})")
                prod.secondaryUnits.forEach { sec ->
                    if (sec.barcode.isNotBlank()) {
                        add(sec.barcode to "${prod.name} - وحدة ${sec.unitName}")
                    }
                }
            }
            add("2100004015005" to "باركود ميزان PLU: بن عربي فاخر (وزن 1.500 كجم)")
        }
        BarcodeScannerSimulationDialog(
            title = "مسح باركود الصنف (الكاميرا / PLU)",
            presets = barcodePresets,
            onDismiss = { showBarcodeScanner = false },
            onScanned = { code ->
                viewModel.onBarcodeOrSearchSubmitted(code)
                showBarcodeScanner = false
            }
        )
    }

    // Return Invoice QR Code Scanner Dialog
    if (showReturnQrScanner) {
        BarcodeScannerSimulationDialog(
            title = "مسح QR الفاتورة الأصلية للربط بالمرتجع",
            presets = listOf(
                "INV-2026-1007" to "فاتورة بيع رقم INV-2026-1007 (أحمد الخطيب)",
                "INV-2026-1006" to "فاتورة بيع رقم INV-2026-1006 (محمد العلي)",
                "INV-2026-1005" to "فاتورة بيع رقم INV-2026-1005 (مطعم الأصيل)"
            ),
            onDismiss = { showReturnQrScanner = false },
            onScanned = { invNum ->
                viewModel.linkReturnInvoiceByNumber(invNum)
                showReturnQrScanner = false
            }
        )
    }

    // Held Invoices List Dialog ([الفواتير المعلقة])
    if (showHeldInvoicesSheet) {
        HeldInvoicesModalDialog(
            heldInvoices = heldInvoices,
            onDismiss = { showHeldInvoicesSheet = false },
            onResumeHeld = { held ->
                viewModel.resumeHeldInvoice(held)
                showHeldInvoicesSheet = false
            }
        )
    }

    // Quick Add Customer / Supplier Dialog (+)
    if (showQuickAddPartyDialog) {
        val isSupplierMode = state.mode == InvoiceMode.PURCHASE || state.mode == InvoiceMode.PURCHASE_RETURN
        QuickAddPartyModal(
            defaultType = if (isSupplierMode) PartyType.SUPPLIER else PartyType.CUSTOMER,
            defaultCurrency = state.currencyCode,
            onDismiss = { showQuickAddPartyDialog = false },
            onConfirm = { name, phone, partyType ->
                viewModel.quickCreateParty(name, phone, partyType, state.currencyCode)
                showQuickAddPartyDialog = false
            }
        )
    }
}

@Composable
private fun InvoiceEditorTopBar(
    mode: InvoiceMode,
    onModeSelected: (InvoiceMode) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenUserGuide: () -> Unit,
    onClose: () -> Unit
) {
    // 6 Unified Invoice Modes including Quotation (عرض سعر)
    val activeModes = listOf(
        InvoiceMode.SALE,
        InvoiceMode.PURCHASE,
        InvoiceMode.SALE_RETURN,
        InvoiceMode.PURCHASE_RETURN,
        InvoiceMode.DAMAGE,
        InvoiceMode.QUOTATION
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SanadSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = when (mode) {
                        InvoiceMode.SALE, InvoiceMode.QUOTATION -> SanadElectricBlue
                        InvoiceMode.PURCHASE -> SanadEmeraldGreen
                        InvoiceMode.SALE_RETURN, InvoiceMode.PURCHASE_RETURN -> SanadAmberWarning
                        InvoiceMode.DAMAGE, InvoiceMode.HOLD -> SanadCoralRed
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mode.arabicLabel,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SanadTextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // (?) Interactive User Guide Button
                IconButton(
                    onClick = onOpenUserGuide,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SanadBlueContainer)
                        .border(1.dp, SanadElectricBlue.copy(alpha = 0.6f), CircleShape)
                        .testTag("invoice_user_guide_help_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "دليل الاستخدام (?)",
                        tint = SanadElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Signature Smart AI Robot Icon
                Surface(
                    color = SanadBlueContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(1.dp, SanadElectricBlue.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable { onOpenAiAssistant() }
                        .testTag("invoice_ai_robot_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "المساعد الذكي AI",
                            tint = SanadElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "سند AI",
                            style = MaterialTheme.typography.labelLarge,
                            color = SanadElectricBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SanadSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = SanadTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 6 Unified Invoice Mode Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activeModes.forEach { tabMode ->
                val isSelected = mode == tabMode
                val accent = when (tabMode) {
                    InvoiceMode.SALE -> SanadElectricBlue
                    InvoiceMode.PURCHASE -> SanadEmeraldGreen
                    InvoiceMode.SALE_RETURN, InvoiceMode.PURCHASE_RETURN -> SanadAmberWarning
                    InvoiceMode.DAMAGE, InvoiceMode.HOLD -> SanadCoralRed
                    InvoiceMode.QUOTATION -> SanadElectricBlue
                }
                Surface(
                    color = if (isSelected) accent.copy(alpha = 0.2f) else SanadSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accent else SanadBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onModeSelected(tabMode) }
                        .testTag("mode_tab_${tabMode.name}")
                ) {
                    Text(
                        text = tabMode.arabicLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) accent else SanadTextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = SanadBorder)
    }
}

@Composable
private fun PortraitUnifiedInvoiceContent(
    state: InvoiceEditorState,
    products: List<ProductEntity>,
    parties: List<PartyEntity>,
    currencies: List<CurrencyAndRateEntity>,
    accounts: List<FinancialAccountEntity>,
    heldCount: Int,
    activeSymbol: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBarcodeSubmitted: (String) -> Unit,
    onOpenCameraScanner: () -> Unit,
    onOpenReturnQrScanner: () -> Unit,
    onProductClicked: (ProductEntity) -> Unit,
    onOpenNewProductModal: () -> Unit,
    onOpenQuickAddParty: () -> Unit,
    onNumpadRequested: (NumpadTarget) -> Unit,
    onCycleUnit: (Long) -> Unit,
    onRemoveLine: (Long) -> Unit,
    onHoldInvoice: () -> Unit,
    onShowHeldInvoices: () -> Unit,
    onLoadLastInvoice: () -> Unit,
    onEditRateLock: () -> Unit,
    onExportB2BSanad: () -> Unit,
    viewModel: SanadViewModel
) {
    var expandTaxCard by remember { mutableStateOf(false) }
    var expandAdjustmentsCard by remember { mutableStateOf(false) }
    var showPartyDropdown by remember { mutableStateOf(false) }

    val isReturnMode = state.mode == InvoiceMode.SALE_RETURN || state.mode == InvoiceMode.PURCHASE_RETURN
    val isSaleOrSaleReturn = state.mode == InvoiceMode.SALE || state.mode == InvoiceMode.SALE_RETURN
    val isDamageMode = state.mode == InvoiceMode.DAMAGE

    val matchingAccountsForCurrency = accounts.filter { it.currencyCode == state.currencyCode }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Control Strip: [تعليق الفاتورة] | [الفواتير المعلقة (counter)] | [آخر فاتورة]
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // تعليق الفاتورة
                Surface(
                    color = SanadSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                        .clickable { onHoldInvoice() }
                        .testTag("hold_invoice_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PauseCircleOutline,
                            contentDescription = null,
                            tint = SanadAmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تعليق الفاتورة",
                            style = MaterialTheme.typography.labelLarge,
                            color = SanadTextPrimary,
                            maxLines = 1
                        )
                    }
                }

                // الفواتير المعلقة (مع العداد)
                Surface(
                    color = if (heldCount > 0) SanadAmberContainer else SanadSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .border(
                            1.dp,
                            if (heldCount > 0) SanadAmberWarning else SanadBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onShowHeldInvoices() }
                        .testTag("held_invoices_list_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الفواتير المعلقة",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (heldCount > 0) SanadAmberWarning else SanadTextPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (heldCount > 0) SanadAmberWarning else SanadBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = heldCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (heldCount > 0) SanadNavyDark else SanadTextPrimary
                            )
                        }
                    }
                }

                // آخر فاتورة
                Surface(
                    color = SanadSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                        .clickable { onLoadLastInvoice() }
                        .testTag("last_invoice_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SanadElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "آخر فاتورة",
                            style = MaterialTheme.typography.labelLarge,
                            color = SanadTextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 2. Returns Safeguard Banner: "مرتجع حر (غير مرتبط)" + Dismissible Notice + [مسح QR الفاتورة]
        if (isReturnMode) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanadSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SanadAmberWarning.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SanadBadge(
                                text = if (state.isStandaloneReturn) {
                                    "مرتجع حر (غير مرتبط)"
                                } else {
                                    "مرتجع مرتبط: ${state.linkedOriginalInvoiceNumber}"
                                },
                                textColor = SanadAmberWarning,
                                containerColor = SanadAmberContainer
                            )

                            OutlinedButton(
                                onClick = onOpenReturnQrScanner,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = SanadElectricBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مسح QR الفاتورة",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SanadElectricBlue
                                )
                            }
                        }

                        AnimatedVisibility(visible = state.showReturnSafeguardBanner) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SanadAmberContainer.copy(alpha = 0.55f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = SanadAmberWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "يمكنك مسح رمز QR الفاتورة الأصلية لاسترجاع أصنافها وأسعارها المثبتة تلقائياً، أو المتابعة كمرتجع حر.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissReturnSafeguardBanner() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إخفاء التنبيه",
                                        tint = SanadTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Pricing Tier Toggle [مفرق / جملة] + Customer/Supplier Selector with (+) Button
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SanadSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSaleOrSaleReturn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "نمط التسعير:",
                                style = MaterialTheme.typography.titleSmall,
                                color = SanadTextSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SanadNavyDark)
                                    .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                                    .padding(3.dp)
                            ) {
                                PricingTier.entries.forEach { tier ->
                                    val selected = state.pricingTier == tier
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) SanadElectricBlue else Color.Transparent)
                                            .clickable { viewModel.setPricingTier(tier) }
                                            .padding(horizontal = 18.dp, vertical = 6.dp)
                                            .testTag("pricing_tier_${tier.name}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tier.arabicLabel,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (selected) Color.White else SanadTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!isDamageMode) {
                        val isSupplier = state.mode == InvoiceMode.PURCHASE || state.mode == InvoiceMode.PURCHASE_RETURN
                        val filteredParties = parties.filter {
                            if (isSupplier) it.partyType == PartyType.SUPPLIER else it.partyType == PartyType.CUSTOMER
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = SanadNavyDark,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                                        .clickable { showPartyDropdown = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isSupplier) "المورد" else "العميل",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SanadTextSecondary
                                            )
                                            Text(
                                                text = state.selectedPartyName,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = SanadTextPrimary
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = "اختيار",
                                            tint = SanadElectricBlue
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showPartyDropdown,
                                    onDismissRequest = { showPartyDropdown = false },
                                    modifier = Modifier.background(SanadSurfaceElevated)
                                ) {
                                    if (!isSupplier) {
                                        DropdownMenuItem(
                                            text = { Text("عميل نقدي (مباشر)", color = SanadTextPrimary) },
                                            onClick = {
                                                viewModel.selectInvoiceParty(null)
                                                showPartyDropdown = false
                                            }
                                        )
                                    }
                                    filteredParties.forEach { party ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "${party.name} (${party.phone})",
                                                    color = SanadTextPrimary
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectInvoiceParty(party)
                                                showPartyDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Quick (+) Party Button
                            Surface(
                                color = SanadBlueContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(1.dp, SanadElectricBlue, RoundedCornerShape(10.dp))
                                    .clickable { onOpenQuickAddParty() }
                                    .testTag("quick_add_party_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "إضافة عميل أو مورد جديد",
                                        tint = SanadElectricBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Smart Item Entry: Barcode Camera Scanner + Search Input + Quick (+) Product Button
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SanadSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    text = "امسح الباركود أو اكتب اسم الصنف...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SanadTextMuted
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = SanadElectricBlue
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = onOpenCameraScanner) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "مسح باركود بالكاميرا",
                                        tint = SanadEmeraldGreen
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { onBarcodeSubmitted(searchQuery) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SanadElectricBlue,
                                unfocusedBorderColor = SanadBorder,
                                focusedContainerColor = SanadNavyDark,
                                unfocusedContainerColor = SanadNavyDark,
                                focusedTextColor = SanadTextPrimary,
                                unfocusedTextColor = SanadTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("invoice_item_search_input")
                        )

                        // Quick (+) New Product Creation Button
                        Surface(
                            color = SanadEmeraldContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .size(52.dp)
                                .border(1.dp, SanadEmeraldGreen, RoundedCornerShape(12.dp))
                                .clickable { onOpenNewProductModal() }
                                .testTag("invoice_quick_new_product_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "تعريف صنف جديد",
                                    tint = SanadEmeraldGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fast Horizontal Product Chips / Suggestions
                    val suggestions = remember(products, searchQuery) {
                        if (searchQuery.isBlank()) {
                            products
                        } else {
                            products.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.barcode.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { prod ->
                            Surface(
                                color = SanadNavyDark,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                                    .clickable { onProductClicked(prod) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = SanadTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formatMoney(prod.retailPrice, "ر.ي"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SanadElectricBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Interactive Line-Item Table (Tapping directly on [Quantity] or [Unit Price] opens DirectNumpadDialog)
        item {
            InteractiveInvoiceLinesCard(
                items = state.items,
                activeSymbol = activeSymbol,
                onNumpadRequested = onNumpadRequested,
                onCycleUnit = onCycleUnit,
                onRemoveLine = onRemoveLine
            )
        }

        // 6. Collapsible VAT & Financial Adjustments Cards
        item {
            CollapsibleTaxAndAdjustmentsSection(
                state = state,
                activeSymbol = activeSymbol,
                expandTaxCard = expandTaxCard,
                onToggleTaxCard = { expandTaxCard = !expandTaxCard },
                expandAdjustmentsCard = expandAdjustmentsCard,
                onToggleAdjustmentsCard = { expandAdjustmentsCard = !expandAdjustmentsCard },
                viewModel = viewModel
            )
        }

        // 7. Multi-Currency Rate Lock Engine Card [عملة المعاملة: 1 $ = 1,550 ر.ي]
        item {
            MultiCurrencyRateLockCard(
                state = state,
                currencies = currencies,
                onCurrencySelected = { viewModel.setInvoiceCurrency(it) },
                onEditRateLock = onEditRateLock
            )
        }

        // 8. Payment Methods [نقداً | دين (آجل) | دفع متعدد] + Dynamic Cashbox Safeguard Banner
        if (!isDamageMode) {
            item {
                PaymentMethodAndCashboxSafeguardCard(
                    state = state,
                    matchingAccounts = matchingAccountsForCurrency,
                    activeSymbol = activeSymbol,
                    onPaymentMethodSelected = { viewModel.setPaymentMethod(it) },
                    onAccountSelected = { viewModel.selectFinancialAccount(it) },
                    onCreateCashboxForCurrency = {
                        viewModel.createAndSelectCashBoxForCurrency(state.currencyCode)
                    },
                    onPaidAmountChange = { viewModel.updatePaidAmountForCredit(it) }
                )
            }
        }

        // 9. Reference Paper Book Number (رقم الدفتر الورقي), Date, and Notes
        item {
            InvoiceMetadataCard(
                state = state,
                onMetadataChanged = { bookNum, notes ->
                    viewModel.updateInvoiceMetadata(bookNum, notes)
                }
            )
        }
    }
}

@Composable
private fun InteractiveInvoiceLinesCard(
    items: List<DraftInvoiceItem>,
    activeSymbol: String,
    onNumpadRequested: (NumpadTarget) -> Unit,
    onCycleUnit: (Long) -> Unit,
    onRemoveLine: (Long) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SanadSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جدول أصناف الفاتورة (${items.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SanadTextPrimary
                )
                Text(
                    text = "انقر على مربع الكمية أو السعر للتعديل المباشر",
                    style = MaterialTheme.typography.labelSmall,
                    color = SanadElectricBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SanadNavyDark)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الصنف / الوحدة",
                    style = MaterialTheme.typography.labelMedium,
                    color = SanadTextSecondary,
                    modifier = Modifier.weight(2.1f)
                )
                Text(
                    text = "الكمية",
                    style = MaterialTheme.typography.labelMedium,
                    color = SanadTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "سعر الوحدة",
                    style = MaterialTheme.typography.labelMedium,
                    color = SanadTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.3f)
                )
                Text(
                    text = "الإجمالي",
                    style = MaterialTheme.typography.labelMedium,
                    color = SanadTextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.3f)
                )
                Spacer(modifier = Modifier.width(28.dp))
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم يتم إدراج أصناف بعد — اختر صنفاً من الشريط أعلاه أو امسح الباركود",
                        style = MaterialTheme.typography.bodySmall,
                        color = SanadTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Item Name & Unit Pill
                        Column(modifier = Modifier.weight(2.1f)) {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SanadTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SanadNavyDark)
                                    .clickable { onCycleUnit(item.lineKey) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.selectedUnitName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SanadEmeraldGreen
                                )
                                if (item.availableUnits.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "تبديل الوحدة",
                                        tint = SanadEmeraldGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        // Tappable Quantity Box (No +/- buttons, opens DirectNumpadDialog)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SanadNavyDark)
                                .border(1.dp, SanadElectricBlue.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .clickable {
                                    onNumpadRequested(
                                        NumpadTarget(
                                            lineKey = item.lineKey,
                                            fieldType = LineNumpadFieldType.QUANTITY,
                                            productName = item.productName,
                                            currentValue = item.quantity,
                                            unitOrSymbol = item.selectedUnitName
                                        )
                                    )
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatNumberCompact(item.quantity),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SanadElectricBlue
                            )
                        }

                        // Tappable Unit Price Box (opens DirectNumpadDialog)
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SanadNavyDark)
                                .border(1.dp, SanadBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    onNumpadRequested(
                                        NumpadTarget(
                                            lineKey = item.lineKey,
                                            fieldType = LineNumpadFieldType.UNIT_PRICE,
                                            productName = item.productName,
                                            currentValue = item.unitPrice,
                                            unitOrSymbol = activeSymbol
                                        )
                                    )
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatMoney(item.unitPrice),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SanadTextPrimary
                            )
                        }

                        // Tappable Line Total Box (Reverse Line Calculation: unitPrice = lineTotal / quantity)
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SanadEmeraldContainer.copy(alpha = 0.45f))
                                .border(1.dp, SanadEmeraldGreen.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                .clickable {
                                    onNumpadRequested(
                                        NumpadTarget(
                                            lineKey = item.lineKey,
                                            fieldType = LineNumpadFieldType.LINE_TOTAL_REVERSE,
                                            productName = "${item.productName} (حساب عكسي للسعر من المجموع)",
                                            currentValue = item.lineTotal,
                                            unitOrSymbol = activeSymbol
                                        )
                                    )
                                }
                                .padding(vertical = 8.dp)
                                .testTag("line_total_reverse_box_${item.productId}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatMoney(item.lineTotal),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SanadEmeraldGreen,
                                textAlign = TextAlign.Center
                            )
                        }

                        IconButton(
                            onClick = { onRemoveLine(item.lineKey) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "حذف السطر",
                                tint = SanadCoralRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = SanadBorder.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun CollapsibleTaxAndAdjustmentsSection(
    state: InvoiceEditorState,
    activeSymbol: String,
    expandTaxCard: Boolean,
    onToggleTaxCard: () -> Unit,
    expandAdjustmentsCard: Boolean,
    onToggleAdjustmentsCard: () -> Unit,
    viewModel: SanadViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Collapsible Card 1: إضافة ضريبة القيمة المضافة (0%)
        Card(
            colors = CardDefaults.cardColors(containerColor = SanadSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SanadBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTaxCard() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.includeVat,
                            onCheckedChange = { checked ->
                                viewModel.updateFinancialAdjustments(
                                    includeVat = checked,
                                    vatPercent = if (checked && state.vatPercent == 0.0) 15.0 else state.vatPercent,
                                    discountAmount = state.discountAmount,
                                    additionAmount = state.additionAmount
                                )
                            },
                            colors = CheckboxDefaults.colors(checkedColor = SanadElectricBlue)
                        )
                        Text(
                            text = "إضافة ضريبة القيمة المضافة (${formatNumberCompact(state.vatPercent)}%)",
                            style = MaterialTheme.typography.titleSmall,
                            color = SanadTextPrimary
                        )
                    }
                    Icon(
                        imageVector = if (expandTaxCard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = SanadTextSecondary
                    )
                }

                AnimatedVisibility(visible = expandTaxCard) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.0, 5.0, 15.0).forEach { rate ->
                            val selected = state.vatPercent == rate && state.includeVat
                            Surface(
                                color = if (selected) SanadBlueContainer else SanadNavyDark,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (selected) SanadElectricBlue else SanadBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.updateFinancialAdjustments(
                                            includeVat = rate > 0.0,
                                            vatPercent = rate,
                                            discountAmount = state.discountAmount,
                                            additionAmount = state.additionAmount
                                        )
                                    }
                            ) {
                                Text(
                                    text = "ضريبة ${formatNumberCompact(rate)}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) SanadElectricBlue else SanadTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Collapsible Card 2: تعديلات مالية (إضافات وخصومات)
        Card(
            colors = CardDefaults.cardColors(containerColor = SanadSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SanadBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAdjustmentsCard() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تعديلات مالية (إضافات وخصومات)",
                        style = MaterialTheme.typography.titleSmall,
                        color = SanadTextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.discountAmount > 0 || state.additionAmount > 0) {
                            SanadBadge(
                                text = "خصم: ${formatMoney(state.discountAmount)} | إضافة: ${formatMoney(state.additionAmount)}",
                                textColor = SanadAmberWarning,
                                containerColor = SanadAmberContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Icon(
                            imageVector = if (expandAdjustmentsCard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = SanadTextSecondary
                        )
                    }
                }

                AnimatedVisibility(visible = expandAdjustmentsCard) {
                    var discountText by remember(state.discountAmount) {
                        mutableStateOf(if (state.discountAmount == 0.0) "" else formatNumberCompact(state.discountAmount))
                    }
                    var additionText by remember(state.additionAmount) {
                        mutableStateOf(if (state.additionAmount == 0.0) "" else formatNumberCompact(state.additionAmount))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = {
                                discountText = it
                                viewModel.updateFinancialAdjustments(
                                    includeVat = state.includeVat,
                                    vatPercent = state.vatPercent,
                                    discountAmount = it.toDoubleOrNull() ?: 0.0,
                                    additionAmount = state.additionAmount
                                )
                            },
                            label = { Text("خصم على الفاتورة ($activeSymbol)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = additionText,
                            onValueChange = {
                                additionText = it
                                viewModel.updateFinancialAdjustments(
                                    includeVat = state.includeVat,
                                    vatPercent = state.vatPercent,
                                    discountAmount = state.discountAmount,
                                    additionAmount = it.toDoubleOrNull() ?: 0.0
                                )
                            },
                            label = { Text("إضافات / نقل ($activeSymbol)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiCurrencyRateLockCard(
    state: InvoiceEditorState,
    currencies: List<CurrencyAndRateEntity>,
    onCurrencySelected: (String) -> Unit,
    onEditRateLock: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SanadSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SanadEmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "عملة المعاملة وقفل سعر الصرف (Rate Lock)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SanadTextPrimary
                    )
                }

                Surface(
                    color = SanadEmeraldContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, SanadEmeraldGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onEditRateLock() }
                ) {
                    Text(
                        text = "1 $ = ${formatMoney(state.lockedExchangeRate)} ${if (state.currencyCode == "USD") "ر.ي" else state.currencyCode}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = SanadEmeraldGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val codes = if (currencies.isNotEmpty()) {
                    currencies.map { it.code to it.symbol }
                } else {
                    listOf("YER" to "ر.ي", "USD" to "$", "SAR" to "ر.س", "TRY" to "₺")
                }
                codes.forEach { (code, sym) ->
                    val selected = state.currencyCode == code
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
                            .clickable { onCurrencySelected(code) }
                            .testTag("invoice_currency_$code")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$code ($sym)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) Color.White else SanadTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodAndCashboxSafeguardCard(
    state: InvoiceEditorState,
    matchingAccounts: List<FinancialAccountEntity>,
    activeSymbol: String,
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
    onAccountSelected: (FinancialAccountEntity) -> Unit,
    onCreateCashboxForCurrency: () -> Unit,
    onPaidAmountChange: (Double) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SanadSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "طريقة الدفع والصندوق المالي",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = SanadTextPrimary
            )

            // Payment Method Segmented Selector: [نقداً | دين / آجل | دفع متعدد]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    val selected = state.paymentMethod == method
                    val accent = when (method) {
                        PaymentMethod.CASH -> SanadEmeraldGreen
                        PaymentMethod.CREDIT -> SanadAmberWarning
                        PaymentMethod.SPLIT -> SanadElectricBlue
                    }
                    Surface(
                        color = if (selected) accent.copy(alpha = 0.2f) else SanadNavyDark,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) accent else SanadBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onPaymentMethodSelected(method) }
                            .testTag("payment_method_${method.name}")
                    ) {
                        Text(
                            text = method.arabicLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) accent else SanadTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Contextual Warning Banner when no financial account matches state.currencyCode
            if (matchingAccounts.isEmpty()) {
                Surface(
                    color = SanadAmberContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SanadAmberWarning, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = SanadAmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تنبيه: لا يوجد صندوق أو حساب مالي مسجل بعملة (${state.currencyCode})!",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = SanadTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onCreateCashboxForCurrency,
                            colors = ButtonDefaults.buttonColors(containerColor = SanadAmberWarning),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_cashbox_for_currency_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = SanadNavyDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ إضافة صندوق / حساب بـ ${state.currencyCode}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SanadNavyDark
                            )
                        }
                    }
                }
            } else {
                // Cashbox Selection Chips + Inline [+ إضافة صندوق / حساب بـ YER] button
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الحساب المالي المستلم / الدافع (${state.currencyCode}):",
                            style = MaterialTheme.typography.labelMedium,
                            color = SanadTextSecondary
                        )
                        TextButton(
                            onClick = onCreateCashboxForCurrency,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("inline_add_cashbox_button")
                        ) {
                            Text(
                                text = "+ إضافة صندوق / حساب بـ ${state.currencyCode}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SanadElectricBlue
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        matchingAccounts.forEach { acc ->
                            val isSelected = state.selectedAccountId == acc.id
                            Surface(
                                color = if (isSelected) SanadEmeraldContainer else SanadNavyDark,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isSelected) SanadEmeraldGreen else SanadBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onAccountSelected(acc) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = if (isSelected) SanadEmeraldGreen else SanadTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) SanadEmeraldGreen else SanadTextPrimary
                                        )
                                        Text(
                                            text = "الرصيد: ${formatMoney(acc.currentBalance, acc.currencyCode)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SanadTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.paymentMethod == PaymentMethod.CREDIT) {
                var paidText by remember(state.paidAmountInput) {
                    mutableStateOf(
                        if ((state.paidAmountInput ?: 0.0) == 0.0) "" else formatNumberCompact(state.paidAmountInput ?: 0.0)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = paidText,
                        onValueChange = {
                            paidText = it
                            onPaidAmountChange(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("الدفعة المقدمة النقدية ($activeSymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = SanadAmberContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "المتبقي ذمة (دين آجل)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SanadAmberWarning
                            )
                            Text(
                                text = formatMoney(state.remainingCreditAmount, activeSymbol),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SanadAmberWarning
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceMetadataCard(
    state: InvoiceEditorState,
    onMetadataChanged: (String, String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SanadSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.paperBookNumber,
                    onValueChange = { onMetadataChanged(it, state.notes) },
                    label = { Text("الرقم المرجعي (رقم الدفتر الورقي)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("paper_book_number_input")
                )

                OutlinedTextField(
                    value = formatShortDate(state.invoiceDateTimestamp),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("تاريخ الفاتورة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = { onMetadataChanged(state.paperBookNumber, it) },
                label = { Text("ملاحظات الفاتورة / شروط التسليم...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoice_notes_input")
            )
        }
    }
}

/**
 * Landscape Aronium POS Split-Screen View:
 * 40% Right-hand Invoice Table + Emerald Total Card & 60% Left-hand Compact Product Grid (108.dp tiles) + Aronium Color-Coded Action Buttons
 */
@Composable
private fun LandscapeAroniumPosLayout(
    state: InvoiceEditorState,
    products: List<ProductEntity>,
    parties: List<PartyEntity>,
    currencies: List<CurrencyAndRateEntity>,
    accounts: List<FinancialAccountEntity>,
    heldCount: Int,
    activeSymbol: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBarcodeSubmitted: (String) -> Unit,
    onOpenCameraScanner: () -> Unit,
    onProductClicked: (ProductEntity) -> Unit,
    onOpenNewProductModal: () -> Unit,
    onNumpadRequested: (NumpadTarget) -> Unit,
    onEditFinalNetTotalDiscount: () -> Unit,
    onCycleUnit: (Long) -> Unit,
    onRemoveLine: (Long) -> Unit,
    onHoldInvoice: () -> Unit,
    onShowHeldInvoices: () -> Unit,
    onLoadLastInvoice: () -> Unit,
    onToggleGrid: () -> Unit,
    viewModel: SanadViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Right Pane (40% in RTL): Invoice Table + Emerald Total Card
        Column(
            modifier = Modifier
                .weight(if (state.showAroniumProductGrid) 0.42f else 1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Emerald Total Summary Banner (Clickable for Auto-Discount from Final Net Total)
            Surface(
                color = SanadEmeraldContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, SanadEmeraldGreen, RoundedCornerShape(14.dp))
                    .clickable { onEditFinalNetTotalDiscount() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "صافي الفاتورة (${state.pricingTier.arabicLabel}) • انقر لخصم تلقائي",
                            style = MaterialTheme.typography.labelMedium,
                            color = SanadEmeraldGreen
                        )
                        Text(
                            text = formatMoney(state.netTotalAmount, activeSymbol),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "المعادل بالدولار",
                            style = MaterialTheme.typography.labelSmall,
                            color = SanadTextSecondary
                        )
                        Text(
                            text = formatMoney(state.netTotalUsdEquivalent, "$"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanadElectricBlue
                        )
                    }
                }
            }

            // Scrollable Line Items
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                InteractiveInvoiceLinesCard(
                    items = state.items,
                    activeSymbol = activeSymbol,
                    onNumpadRequested = onNumpadRequested,
                    onCycleUnit = onCycleUnit,
                    onRemoveLine = onRemoveLine
                )
            }

            // Toggle Grid Button
            OutlinedButton(
                onClick = onToggleGrid,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.GridView, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.showAroniumProductGrid) "إخفاء شبكة الأصناف (Aronium)" else "إظهار شبكة الأصناف (Aronium)"
                )
            }
        }

        // Left Pane (60% in RTL): Compact 108.dp Product Grid + Aronium Fast Action Buttons
        if (state.showAroniumProductGrid) {
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Bar + Fast Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("بحث سريع أو باركود...", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = onOpenCameraScanner) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = SanadEmeraldGreen
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = onOpenNewProductModal,
                        colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("صنف")
                    }
                }

                // 108.dp Compact Product Tiles Grid
                val filtered = products.filter {
                    searchQuery.isBlank() ||
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.barcode.contains(searchQuery, ignoreCase = true)
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 115.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { prod ->
                        Surface(
                            color = SanadSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(108.dp)
                                .border(1.dp, SanadBorder, RoundedCornerShape(12.dp))
                                .clickable { onProductClicked(prod) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SanadBadge(
                                        text = "متوفر: ${formatNumberCompact(prod.availableQuantity)}",
                                        textColor = SanadEmeraldGreen,
                                        containerColor = SanadEmeraldContainer
                                    )
                                }
                                Text(
                                    text = prod.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadTextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatMoney(prod.retailPrice, "ر.ي"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = SanadElectricBlue
                                )
                            }
                        }
                    }
                }

                // Aronium Color-Coded Fast Action Buttons Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AroniumFastTile(
                        label = "دفع نقدي فوري",
                        color = AroniumTilePayment,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.setPaymentMethod(PaymentMethod.CASH)
                            viewModel.commitCurrentInvoice(holdOnly = false, printReceipt = true)
                        }
                    )
                    AroniumFastTile(
                        label = "تعليق ($heldCount)",
                        color = AroniumTileHold,
                        modifier = Modifier.weight(1f),
                        onClick = onHoldInvoice
                    )
                    AroniumFastTile(
                        label = "معلقة",
                        color = AroniumTileDiscount,
                        modifier = Modifier.weight(1f),
                        onClick = onShowHeldInvoices
                    )
                    AroniumFastTile(
                        label = "آخر فاتورة",
                        color = AroniumTileVoid,
                        modifier = Modifier.weight(1f),
                        onClick = onLoadLastInvoice
                    )
                }
            }
        }
    }
}

@Composable
private fun AroniumFastTile(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(44.dp)
            .border(1.dp, color, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = color,
                maxLines = 1
            )
        }
    }
}

/**
 * Sticky Dual-Currency Invoice Footer pinned at the bottom of the Invoice Editor
 * Supports clicking directly on [الإجمالي النهائي] to trigger Automatic Discount from Net Total.
 */
@Composable
private fun StickyDualCurrencyInvoiceFooter(
    state: InvoiceEditorState,
    activeSymbol: String,
    onEditNetTotalForDiscount: () -> Unit,
    onConvertQuotationToSale: () -> Unit,
    onExportB2BSanad: () -> Unit,
    onSaveAndPrint: () -> Unit,
    onSaveAndNew: () -> Unit
) {
    Surface(
        color = SanadSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SanadBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Dual-Currency Subtotals & Clickable Net Total Bar (Auto-Discount)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEditNetTotalForDiscount() }
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                        .testTag("editable_final_net_total_box")
                ) {
                    Text(
                        text = buildString {
                            append("المجموع الفرعي: ${formatMoney(state.subtotalAmount, activeSymbol)}")
                            if (state.discountAmount > 0.0) {
                                append(" | خصم: ${formatMoney(state.discountAmount, activeSymbol)}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SanadTextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "الإجمالي النهائي: ",
                            style = MaterialTheme.typography.titleMedium,
                            color = SanadTextPrimary
                        )
                        Text(
                            text = formatMoney(state.netTotalAmount, activeSymbol),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = SanadEmeraldGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SanadBadge(
                            text = "خصم تلقائي ✎",
                            textColor = SanadAmberWarning,
                            containerColor = SanadAmberContainer
                        )
                    }
                }

                // Equivalent in Secondary/Base Currency
                Surface(
                    color = SanadBlueContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, SanadElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "الإجمالي بالعملة المقابلة",
                            style = MaterialTheme.typography.labelSmall,
                            color = SanadTextSecondary
                        )
                        val secondaryDisplay = if (state.currencyCode == "USD") {
                            formatMoney(state.netTotalAmount * 1550.0, "ر.ي")
                        } else {
                            formatMoney(state.netTotalUsdEquivalent, "$")
                        }
                        Text(
                            text = secondaryDisplay,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SanadElectricBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Action Buttons: [حفظ وطباعة] & [حفظ وفاتورة جديدة] + Contextual Quotation / B2B Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.mode == InvoiceMode.QUOTATION) {
                    Button(
                        onClick = onConvertQuotationToSale,
                        colors = ButtonDefaults.buttonColors(containerColor = SanadAmberWarning),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("footer_convert_quotation_button")
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp), tint = SanadNavyDark)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تحويل لبيع",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SanadNavyDark
                        )
                    }
                }

                Button(
                    onClick = onSaveAndPrint,
                    enabled = !state.isLockedByClosedShift,
                    colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_and_print_invoice_button")
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.mode == InvoiceMode.QUOTATION) "حفظ عرض السعر" else "حفظ وطباعة",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = onSaveAndNew,
                    enabled = !state.isLockedByClosedShift,
                    colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_and_new_invoice_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حفظ وفاتورة جديدة",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (state.mode == InvoiceMode.SALE && (state.items.isNotEmpty() || state.lastSavedB2BSanadJson != null)) {
                    OutlinedButton(
                        onClick = onExportB2BSanad,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadElectricBlue),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("footer_export_b2b_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("B2B", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeldInvoicesModalDialog(
    heldInvoices: List<InvoiceWithLines>,
    onDismiss: () -> Unit,
    onResumeHeld: (InvoiceWithLines) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SanadSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SanadBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الفواتير المعلقة (${heldInvoices.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SanadAmberWarning
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (heldInvoices.isEmpty()) {
                    Text(
                        text = "لا توجد فواتير معلقة حالياً.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SanadTextSecondary,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(heldInvoices, key = { it.invoice.id }) { held ->
                            Surface(
                                color = SanadSurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SanadBorder, RoundedCornerShape(12.dp))
                                    .clickable { onResumeHeld(held) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${held.invoice.invoiceNumber} - ${held.invoice.partyName}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadTextPrimary
                                        )
                                        Text(
                                            text = "${held.lines.size} أصناف • ${formatArabicDate(held.invoice.timestamp)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SanadTextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatMoney(held.invoice.netTotalAmount, held.invoice.currencyCode),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadAmberWarning
                                        )
                                        Text(
                                            text = "انقر للاستئناف",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SanadElectricBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddPartyModal(
    defaultType: PartyType,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, PartyType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(defaultType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SanadSurface,
        title = {
            Text(
                text = "إضافة ${selectedType.arabicLabel} جديد سريعاً",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SanadTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(PartyType.CUSTOMER, PartyType.SUPPLIER).forEach { type ->
                        val selected = selectedType == type
                        Surface(
                            color = if (selected) SanadElectricBlue else SanadNavyDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = type }
                        ) {
                            Text(
                                text = type.arabicLabel,
                                color = if (selected) Color.White else SanadTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (واتساب)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "عملة الحساب الافتراضية: $defaultCurrency",
                    style = MaterialTheme.typography.bodySmall,
                    color = SanadTextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone, selectedType) },
                colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen)
            ) {
                Text("حفظ واختيار")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = SanadTextSecondary)
            }
        }
    )
}
