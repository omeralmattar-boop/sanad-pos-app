package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountType
import com.example.data.local.AppDatabase
import com.example.data.local.BundleComponentItem
import com.example.data.local.CompositeComponentEntity
import com.example.data.local.CurrencyAndRateEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.FinancialAccountEntity
import com.example.data.local.InventorySessionWithLines
import com.example.data.local.InvoiceMode
import com.example.data.local.InvoiceStatus
import com.example.data.local.InvoiceWithLines
import com.example.data.local.JournalEntryEntity
import com.example.data.local.LedgerEntryEntity
import com.example.data.local.PartyEntity
import com.example.data.local.PartyType
import com.example.data.local.PaymentMethod
import com.example.data.local.PricingTier
import com.example.data.local.ProductEntity
import com.example.data.local.ProductUnitConversion
import com.example.data.local.ProductUnitEntity
import com.example.data.local.SaleInvoiceEntity
import com.example.data.local.SaleItemEntity
import com.example.data.local.ShiftEntity
import com.example.data.local.ShiftOpeningSource
import com.example.data.local.SplitPaymentEntry
import com.example.data.local.StockMovementEntity
import com.example.data.local.StockMovementType
import com.example.data.local.SupplierEntity
import com.example.data.local.TaxType
import com.example.data.local.VoucherDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

// ============================================================================
// INTERACTIVE INVOICE CART LINE DRAFT
// ============================================================================
data class DraftInvoiceItem(
    val lineKey: Long = System.nanoTime(),
    val productId: Long,
    val productName: String,
    val barcode: String,
    val baseUnitName: String,
    val availableUnits: List<ProductUnitConversion>,
    val selectedUnitName: String,
    val unitConversionFactor: Double = 1.0,
    val quantity: Double = 1.0,
    val unitPrice: Double,
    val unitCost: Double,
    val discountAmount: Double = 0.0,
    val taxPercent: Double = 0.0
) {
    val lineSubtotal: Double
        get() = (quantity * unitPrice) - discountAmount

    val lineTaxAmount: Double
        get() = lineSubtotal * (taxPercent / 100.0)

    val lineTotal: Double
        get() = lineSubtotal + lineTaxAmount

    val baseQuantityDeducted: Double
        get() = quantity * unitConversionFactor
}

// ============================================================================
// UNIFIED INVOICE EDITOR UI STATE
// ============================================================================
data class InvoiceEditorState(
    val mode: InvoiceMode = InvoiceMode.SALE,
    val pricingTier: PricingTier = PricingTier.RETAIL,
    val selectedPartyId: Long? = null,
    val selectedPartyName: String = "عميل نقدي",
    val searchQuery: String = "",
    val items: List<DraftInvoiceItem> = emptyList(),
    val currencyCode: String = "YER",
    val lockedExchangeRate: Double = 1550.0, // 1 $ = 1,550 ر.ي
    val isRateLocked: Boolean = true,
    val includeVat: Boolean = false,
    val vatPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val additionAmount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val selectedAccountId: Long? = 1L,
    val selectedAccountName: String = "الصندوق النقدي الرئيسي",
    val paidAmountInput: Double? = null, // null means full netTotal when CASH
    val splitPayments: List<SplitPaymentEntry> = emptyList(),
    val paperBookNumber: String = "BK-508",
    val invoiceDateTimestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    // Return Safeguards
    val isStandaloneReturn: Boolean = true,
    val showReturnSafeguardBanner: Boolean = true,
    val linkedOriginalInvoiceNumber: String? = null,
    // Closed Shift Lock (isShiftClosed = true)
    val editingExistingInvoiceId: Long? = null,
    val isLockedByClosedShift: Boolean = false,
    val lockedShiftNotice: String? = null,
    // B2B (.sanad) Export Ready Banner after Sales Invoice Save
    val lastSavedSaleInvoiceNumber: String? = null,
    val lastSavedB2BSanadJson: String? = null,
    // Landscape Aronium Grid Toggle
    val showAroniumProductGrid: Boolean = true,
    val statusMessage: String? = null
) {
    val subtotalAmount: Double
        get() = items.sumOf { it.lineSubtotal }

    val calculatedVatAmount: Double
        get() = if (includeVat && vatPercent > 0.0) {
            (subtotalAmount - discountAmount + additionAmount).coerceAtLeast(0.0) * (vatPercent / 100.0)
        } else {
            items.sumOf { it.lineTaxAmount }
        }

    val netTotalAmount: Double
        get() = (subtotalAmount - discountAmount + additionAmount + calculatedVatAmount).coerceAtLeast(0.0)

    val netTotalUsdEquivalent: Double
        get() = if (lockedExchangeRate > 0.0) netTotalAmount / lockedExchangeRate else netTotalAmount

    val totalCostAmount: Double
        get() = items.sumOf { it.baseQuantityDeducted * it.unitCost }

    val realizedProfitAmount: Double
        get() = netTotalAmount - totalCostAmount

    val effectivePaidAmount: Double
        get() = when (paymentMethod) {
            PaymentMethod.CASH -> netTotalAmount
            PaymentMethod.CREDIT -> paidAmountInput ?: 0.0
            PaymentMethod.SPLIT -> splitPayments.sumOf { it.amount }
        }

    val remainingCreditAmount: Double
        get() = (netTotalAmount - effectivePaidAmount).coerceAtLeast(0.0)
}

typealias InvoiceEditorViewModel = SanadViewModel

class SanadViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val dao = database.sanadDao()

    // Reactive Database Streams
    val currencies: StateFlow<List<CurrencyAndRateEntity>> = dao.observeCurrencies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = dao.observeActiveProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = dao.observeAllStockMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryCountSessions: StateFlow<List<InventorySessionWithLines>> = dao.observeInventoryCountSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialAccounts: StateFlow<List<FinancialAccountEntity>> = dao.observeFinancialAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parties: StateFlow<List<PartyEntity>> = dao.observeParties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = dao.observeCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = dao.observeSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedInvoices: StateFlow<List<InvoiceWithLines>> = dao.observeCompletedInvoicesWithLines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heldInvoices: StateFlow<List<InvoiceWithLines>> = dao.observeHeldInvoicesWithLines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vouchers: StateFlow<List<LedgerEntryEntity>> = dao.observeLedgerEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = dao.observeExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeShift: StateFlow<ShiftEntity?> = dao.observeActiveShift()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allShifts: StateFlow<List<ShiftEntity>> = dao.observeAllShifts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntryEntity>> = dao.observeJournalEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Invoice Editor State
    private val _invoiceState = MutableStateFlow(InvoiceEditorState())
    val invoiceState: StateFlow<InvoiceEditorState> = _invoiceState.asStateFlow()

    // Snackbar / Toast Feedback
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun showMessage(msg: String) {
        _toastMessage.value = msg
    }

    // ========================================================================
    // MODULE 1: UNIFIED INVOICE ENGINE & ARONIUM POS ACTIONS
    // ========================================================================

    fun setInvoiceMode(mode: InvoiceMode) {
        _invoiceState.update { current ->
            val defaultPartyName = when (mode) {
                InvoiceMode.SALE, InvoiceMode.SALE_RETURN, InvoiceMode.QUOTATION -> "عميل نقدي"
                InvoiceMode.PURCHASE, InvoiceMode.PURCHASE_RETURN -> "معصرة الجنوب"
                InvoiceMode.DAMAGE, InvoiceMode.HOLD -> "إتلاف مخزني"
            }
            val defaultPartyId = when (mode) {
                InvoiceMode.PURCHASE, InvoiceMode.PURCHASE_RETURN -> 4L
                else -> null
            }
            current.copy(
                mode = mode,
                selectedPartyId = defaultPartyId,
                selectedPartyName = defaultPartyName,
                isStandaloneReturn = mode == InvoiceMode.SALE_RETURN || mode == InvoiceMode.PURCHASE_RETURN,
                showReturnSafeguardBanner = true,
                linkedOriginalInvoiceNumber = null,
                editingExistingInvoiceId = null,
                isLockedByClosedShift = false,
                lockedShiftNotice = null
            )
        }
    }

    fun setPricingTier(tier: PricingTier) {
        _invoiceState.update { current ->
            val updatedItems = current.items.map { item ->
                val prod = products.value.find { it.id == item.productId }
                if (prod != null) {
                    val secUnit = prod.secondaryUnits.find { it.unitName == item.selectedUnitName }
                    val newPrice = if (secUnit != null) {
                        if (tier == PricingTier.WHOLESALE && secUnit.wholesalePrice > 0) secUnit.wholesalePrice else secUnit.retailPrice
                    } else {
                        if (tier == PricingTier.WHOLESALE) prod.wholesalePrice else prod.retailPrice
                    }
                    item.copy(unitPrice = newPrice)
                } else {
                    item
                }
            }
            current.copy(pricingTier = tier, items = updatedItems)
        }
    }

    fun selectInvoiceParty(party: PartyEntity?) {
        _invoiceState.update {
            it.copy(
                selectedPartyId = party?.id,
                selectedPartyName = party?.name ?: "عميل نقدي"
            )
        }
    }

    fun setInvoiceCurrency(code: String) {
        val currency = currencies.value.find { it.code == code }
        val rate = currency?.exchangeRateToBase ?: 1550.0
        val matchingAccount = financialAccounts.value.firstOrNull { it.currencyCode == code }
        _invoiceState.update {
            it.copy(
                currencyCode = code,
                lockedExchangeRate = rate,
                selectedAccountId = matchingAccount?.id,
                selectedAccountName = matchingAccount?.name ?: ""
            )
        }
    }

    fun updateLockedExchangeRate(newRate: Double) {
        if (newRate > 0) {
            _invoiceState.update { it.copy(lockedExchangeRate = newRate) }
        }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _invoiceState.update { current ->
            val partyId = if (method == PaymentMethod.CREDIT && current.selectedPartyId == null) {
                1L // Default to أحمد الخطيب when switching to Credit if none selected
            } else {
                current.selectedPartyId
            }
            val partyName = if (method == PaymentMethod.CREDIT && current.selectedPartyId == null) {
                "أحمد الخطيب"
            } else {
                current.selectedPartyName
            }
            current.copy(
                paymentMethod = method,
                selectedPartyId = partyId,
                selectedPartyName = partyName
            )
        }
    }

    fun selectFinancialAccount(account: FinancialAccountEntity) {
        _invoiceState.update {
            it.copy(
                selectedAccountId = account.id,
                selectedAccountName = account.name
            )
        }
    }

    /**
     * Contextual Warning Banner Instant Action:
     * [+ إضافة صندوق / حساب بـ YER] creates and selects the cash box without leaving the invoice.
     */
    fun createAndSelectCashBoxForCurrency(currencyCode: String, customName: String? = null) {
        viewModelScope.launch {
            val arabicLabel = when (currencyCode) {
                "YER" -> "الصندوق النقدي - ريال يمني"
                "USD" -> "صندوق الدولار النقدي USD"
                "SAR" -> "صندوق الريال السعودي SAR"
                "TRY" -> "صندوق الليرة التركية TRY"
                else -> "صندوق نقدي ($currencyCode)"
            }
            val finalName = customName?.takeIf { it.isNotBlank() } ?: arabicLabel
            val newAcc = FinancialAccountEntity(
                name = finalName,
                accountType = AccountType.CASH_BOX,
                currencyCode = currencyCode,
                currentBalance = 0.0,
                isDefaultForCurrency = true,
                sortOrder = financialAccounts.value.size + 1,
                notes = "تم إنشاؤه فورياً من محرر الفواتير"
            )
            val newId = dao.insertFinancialAccount(newAcc)
            _invoiceState.update {
                it.copy(
                    selectedAccountId = newId,
                    selectedAccountName = finalName
                )
            }
            showMessage("تم إنشاء وتحديد ($finalName) بنجاح")
        }
    }

    fun addProductToInvoice(product: ProductEntity, customQuantity: Double = 1.0, selectedUnit: ProductUnitConversion? = null) {
        _invoiceState.update { current ->
            val isPurchaseOrDamage = current.mode == InvoiceMode.PURCHASE ||
                current.mode == InvoiceMode.PURCHASE_RETURN ||
                current.mode == InvoiceMode.DAMAGE
            val unitName = selectedUnit?.unitName ?: product.baseUnitName
            val factor = selectedUnit?.conversionFactor ?: 1.0
            val defaultPrice = when {
                isPurchaseOrDamage -> product.weightedAverageCost * factor
                selectedUnit != null -> if (current.pricingTier == PricingTier.WHOLESALE && selectedUnit.wholesalePrice > 0) {
                    selectedUnit.wholesalePrice
                } else {
                    selectedUnit.retailPrice
                }
                current.pricingTier == PricingTier.WHOLESALE -> product.wholesalePrice
                else -> product.retailPrice
            }

            // Convert price if invoice currency differs from product currency
            val prodRate = currencies.value.find { it.code == product.transactionCurrency }?.exchangeRateToBase ?: 1550.0
            val invRate = current.lockedExchangeRate.takeIf { it > 0 } ?: 1550.0
            val convertedPrice = if (product.transactionCurrency == current.currencyCode) {
                defaultPrice
            } else {
                (defaultPrice / prodRate) * invRate
            }
            val convertedCost = if (product.transactionCurrency == current.currencyCode) {
                product.weightedAverageCost
            } else {
                (product.weightedAverageCost / prodRate) * invRate
            }

            val existingIndex = current.items.indexOfFirst {
                it.productId == product.id && it.selectedUnitName == unitName
            }

            val newItems = if (existingIndex >= 0) {
                current.items.toMutableList().apply {
                    val old = this[existingIndex]
                    this[existingIndex] = old.copy(quantity = old.quantity + customQuantity)
                }
            } else {
                current.items + DraftInvoiceItem(
                    productId = product.id,
                    productName = product.name,
                    barcode = selectedUnit?.barcode?.ifBlank { product.barcode } ?: product.barcode,
                    baseUnitName = product.baseUnitName,
                    availableUnits = product.secondaryUnits,
                    selectedUnitName = unitName,
                    unitConversionFactor = factor,
                    quantity = customQuantity,
                    unitPrice = (convertedPrice * 100.0).roundToLong() / 100.0,
                    unitCost = convertedCost
                )
            }
            current.copy(items = newItems, searchQuery = "")
        }
    }

    /**
     * Handles Barcode Scanning including Electronic Weight-Scale PLU Barcodes (prefix "21")
     * Format: 21 + 5-digit item code + 5-digit weight in grams + 1 check digit
     */
    fun onBarcodeOrSearchSubmitted(codeOrQuery: String) {
        val trimmed = codeOrQuery.trim()
        if (trimmed.isEmpty()) return

        // 1. Check PLU Weight Barcode (13 digits starting with 21)
        if (trimmed.length == 13 && trimmed.startsWith("21")) {
            val weightGrams = trimmed.substring(7, 12).toDoubleOrNull() ?: 1000.0
            val weightKg = weightGrams / 1000.0
            val pluProduct = products.value.firstOrNull {
                it.soldByWeightPlu && (it.barcode == trimmed || it.barcode.startsWith("21"))
            } ?: products.value.firstOrNull { it.soldByWeightPlu }

            if (pluProduct != null) {
                addProductToInvoice(pluProduct, customQuantity = weightKg)
                showMessage("ميزان PLU: تم إضافة ${pluProduct.name} بوزن $weightKg ${pluProduct.baseUnitName}")
                return
            }
        }

        // 2. Check Secondary Unit Barcode
        for (prod in products.value) {
            val matchedSecUnit = prod.secondaryUnits.firstOrNull { it.barcode == trimmed }
            if (matchedSecUnit != null) {
                addProductToInvoice(prod, customQuantity = 1.0, selectedUnit = matchedSecUnit)
                showMessage("تم إدارج (${prod.name}) بالوحدة: ${matchedSecUnit.unitName}")
                return
            }
        }

        // 3. Check Base Product Barcode or Name
        val matchedProduct = products.value.firstOrNull {
            it.barcode.equals(trimmed, ignoreCase = true) || it.name.contains(trimmed, ignoreCase = true)
        }
        if (matchedProduct != null) {
            addProductToInvoice(matchedProduct, customQuantity = 1.0)
        } else {
            showMessage("لم يتم العثور على صنف مطابق للباركود: $trimmed")
        }
    }

    fun updateLineQuantity(lineKey: Long, newQuantity: Double) {
        if (_invoiceState.value.isLockedByClosedShift) {
            showMessage("قفل الوردية المغلقة: لا يمكن تعديل فاتورة تنتمي إلى وردية مغلقة (isShiftClosed = true)")
            return
        }
        if (newQuantity <= 0.0) {
            removeLineItem(lineKey)
            return
        }
        _invoiceState.update { current ->
            current.copy(
                items = current.items.map {
                    if (it.lineKey == lineKey) it.copy(quantity = newQuantity) else it
                }
            )
        }
    }

    fun updateLineUnitPrice(lineKey: Long, newPrice: Double) {
        if (_invoiceState.value.isLockedByClosedShift) {
            showMessage("قفل الوردية المغلقة: لا يمكن تعديل فاتورة تنتمي إلى وردية مغلقة (isShiftClosed = true)")
            return
        }
        _invoiceState.update { current ->
            current.copy(
                items = current.items.map {
                    if (it.lineKey == lineKey) it.copy(unitPrice = newPrice.coerceAtLeast(0.0)) else it
                }
            )
        }
    }

    /**
     * Smart Rule 1: Reverse Line Total Calculation (الحساب العكسي للسطر)
     * Editing [مجموع السطر Line Total] directly calculates unitPrice = lineTotal / quantity automatically.
     */
    fun updateLineTotalReversely(lineKey: Long, newLineTotal: Double) {
        if (_invoiceState.value.isLockedByClosedShift) {
            showMessage("قفل الوردية المغلقة: لا يمكن تعديل فاتورة تنتمي إلى وردية مغلقة (isShiftClosed = true)")
            return
        }
        _invoiceState.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.lineKey != lineKey) return@map item
                    val safeQty = item.quantity.coerceAtLeast(0.0001)
                    val subtotalBeforeTax = if (item.taxPercent > 0.0) {
                        newLineTotal.coerceAtLeast(0.0) / (1.0 + item.taxPercent / 100.0)
                    } else {
                        newLineTotal.coerceAtLeast(0.0)
                    }
                    val calculatedUnitPrice = ((subtotalBeforeTax + item.discountAmount) / safeQty).coerceAtLeast(0.0)
                    val roundedUnitPrice = (calculatedUnitPrice * 100.0).roundToLong() / 100.0
                    item.copy(unitPrice = roundedUnitPrice)
                }
            )
        }
        showMessage("الحساب العكسي للسطر: تم احتساب سعر الوحدة تلقائياً من مجموع السطر")
    }

    /**
     * Smart Rule 2: Automatic Discount from Final Net Total (الخصم التلقائي من الإجمالي)
     * Editing [الإجمالي النهائي] downwards automatically computes the difference as an invoice discount.
     */
    fun applyAutoDiscountFromFinalNetTotal(newTargetNetTotal: Double) {
        if (_invoiceState.value.isLockedByClosedShift) {
            showMessage("قفل الوردية المغلقة: لا يمكن تعديل فاتورة تنتمي إلى وردية مغلقة (isShiftClosed = true)")
            return
        }
        _invoiceState.update { current ->
            val grossBeforeDiscount = current.subtotalAmount + current.additionAmount
            val targetBeforeVat = if (current.includeVat && current.vatPercent > 0.0) {
                newTargetNetTotal.coerceAtLeast(0.0) / (1.0 + current.vatPercent / 100.0)
            } else {
                newTargetNetTotal.coerceAtLeast(0.0) - current.items.sumOf { it.lineTaxAmount }
            }
            val autoDiscount = (grossBeforeDiscount - targetBeforeVat).coerceAtLeast(0.0)
            val roundedDiscount = (autoDiscount * 100.0).roundToLong() / 100.0
            current.copy(discountAmount = roundedDiscount)
        }
        showMessage("الخصم التلقائي: تم احتساب الفرق كخصم على الفاتورة تلقائياً")
    }

    /**
     * Smart Rule 3: One-Click Conversion from Quotation (عرض سعر) to Sales Invoice (فاتورة بيع)
     */
    fun convertQuotationToSaleInvoice() {
        _invoiceState.update { current ->
            current.copy(
                mode = InvoiceMode.SALE,
                notes = if (current.notes.isBlank()) "محولة من عرض سعر" else "${current.notes} (محولة من عرض سعر)"
            )
        }
        showMessage("تم تحويل عرض السعر إلى فاتورة بيع جاهزة للحفظ والخصم المخزني")
    }

    /**
     * Unlocks a closed-shift recalled invoice ONLY by creating a brand-new copy (preserving the original closed-shift record)
     */
    fun unlockShiftLockForNewCopy() {
        _invoiceState.update { current ->
            current.copy(
                editingExistingInvoiceId = null,
                isLockedByClosedShift = false,
                lockedShiftNotice = null,
                paperBookNumber = "BK-${(510..999).random()}"
            )
        }
        showMessage("تم فتح نسخة جديدة مستقلة عن الوردية المغلقة")
    }

    fun clearLastSavedB2BExport() {
        _invoiceState.update {
            it.copy(lastSavedSaleInvoiceNumber = null, lastSavedB2BSanadJson = null)
        }
    }

    /**
     * Smart Rule 4: B2B (.sanad / JSON) Export Builder for sharing Sales Invoice with Buyer
     */
    fun buildCurrentOrLastB2BSanadJson(): Pair<String, String> {
        val state = _invoiceState.value
        if (!state.lastSavedB2BSanadJson.isNullOrBlank() && !state.lastSavedSaleInvoiceNumber.isNullOrBlank()) {
            return state.lastSavedSaleInvoiceNumber to state.lastSavedB2BSanadJson
        }
        val invNum = "INV-2026-${(System.currentTimeMillis() / 1000) % 100000}"
        val json = generateSanadB2BJson(
            invoiceNumber = invNum,
            partyName = state.selectedPartyName,
            currencyCode = state.currencyCode,
            exchangeRate = state.lockedExchangeRate,
            items = state.items,
            discountAmount = state.discountAmount,
            additionAmount = state.additionAmount,
            vatAmount = state.calculatedVatAmount,
            netTotal = state.netTotalAmount
        )
        return invNum to json
    }

    private fun generateSanadB2BJson(
        invoiceNumber: String,
        partyName: String,
        currencyCode: String,
        exchangeRate: Double,
        items: List<DraftInvoiceItem>,
        discountAmount: Double,
        additionAmount: Double,
        vatAmount: Double,
        netTotal: Double
    ): String {
        val itemsJson = items.joinToString(",\n    ") { item ->
            """
            {
              "name": "${item.productName.replace("\"", "\\\"")}",
              "barcode": "${item.barcode}",
              "unit": "${item.selectedUnitName}",
              "conversionFactor": ${item.unitConversionFactor},
              "quantity": ${item.quantity},
              "unitPrice": ${item.unitPrice},
              "lineTotal": ${item.lineTotal}
            }
            """.trimIndent()
        }
        return """
        {
          "format": "sanad.b2b.v2",
          "supplierName": "مؤسسة سند التجارية الشاملة",
          "buyerName": "${partyName.replace("\"", "\\\"")}",
          "invoiceNumber": "$invoiceNumber",
          "currencyCode": "$currencyCode",
          "lockedExchangeRate": $exchangeRate,
          "discountAmount": $discountAmount,
          "shippingAdditionAmount": $additionAmount,
          "vatAmount": $vatAmount,
          "netTotalAmount": $netTotal,
          "timestamp": ${System.currentTimeMillis()},
          "items": [
            $itemsJson
          ]
        }
        """.trimIndent()
    }

    fun cycleLineUnit(lineKey: Long) {
        if (_invoiceState.value.isLockedByClosedShift) {
            showMessage("قفل الوردية المغلقة: لا يمكن تعديل فاتورة تنتمي إلى وردية مغلقة (isShiftClosed = true)")
            return
        }
        _invoiceState.update { current ->
            val updated = current.items.map { item ->
                if (item.lineKey != lineKey) return@map item
                val prod = products.value.find { it.id == item.productId } ?: return@map item
                val allOptions = listOf(
                    ProductUnitConversion(
                        unitName = prod.baseUnitName,
                        conversionFactor = 1.0,
                        barcode = prod.barcode,
                        retailPrice = prod.retailPrice,
                        wholesalePrice = prod.wholesalePrice
                    )
                ) + prod.secondaryUnits
                val idx = allOptions.indexOfFirst { it.unitName == item.selectedUnitName }
                val next = allOptions[(idx + 1) % allOptions.size]
                val nextPrice = if (current.pricingTier == PricingTier.WHOLESALE && next.wholesalePrice > 0) {
                    next.wholesalePrice
                } else {
                    next.retailPrice
                }
                item.copy(
                    selectedUnitName = next.unitName,
                    unitConversionFactor = next.conversionFactor,
                    unitPrice = nextPrice
                )
            }
            current.copy(items = updated)
        }
    }

    fun removeLineItem(lineKey: Long) {
        if (_invoiceState.value.isLockedByClosedShift) {
            showMessage("قفل الوردية المغلقة: لا يمكن حذف بنود فاتورة تنتمي إلى وردية مغلقة (isShiftClosed = true)")
            return
        }
        _invoiceState.update { current ->
            current.copy(items = current.items.filterNot { it.lineKey == lineKey })
        }
    }

    fun updateFinancialAdjustments(
        includeVat: Boolean,
        vatPercent: Double,
        discountAmount: Double,
        additionAmount: Double
    ) {
        if (_invoiceState.value.isLockedByClosedShift) {
            showMessage("قفل الوردية المغلقة: لا يمكن تعديل فاتورة تنتمي إلى وردية مغلقة")
            return
        }
        _invoiceState.update {
            it.copy(
                includeVat = includeVat,
                vatPercent = vatPercent,
                discountAmount = discountAmount,
                additionAmount = additionAmount
            )
        }
    }

    fun updateInvoiceMetadata(paperBookNumber: String, notes: String) {
        _invoiceState.update {
            it.copy(paperBookNumber = paperBookNumber, notes = notes)
        }
    }

    fun updatePaidAmountForCredit(amount: Double) {
        _invoiceState.update { it.copy(paidAmountInput = amount) }
    }

    fun dismissReturnSafeguardBanner() {
        _invoiceState.update { it.copy(showReturnSafeguardBanner = false) }
    }

    fun toggleAroniumProductGrid() {
        _invoiceState.update { it.copy(showAroniumProductGrid = !it.showAroniumProductGrid) }
    }

    /**
     * Links a Return Invoice via QR Code Scan to an Original Completed Invoice
     */
    fun linkReturnInvoiceByNumber(invoiceNumber: String) {
        viewModelScope.launch {
            val found = dao.getInvoiceByNumberWithLines(invoiceNumber)
                ?: dao.getLastCompletedInvoiceWithLines()
            if (found != null) {
                val restoredDrafts = found.lines.map { line ->
                    val prod = products.value.find { it.id == line.productId }
                    DraftInvoiceItem(
                        productId = line.productId,
                        productName = line.productName,
                        barcode = line.barcode,
                        baseUnitName = prod?.baseUnitName ?: line.selectedUnitName,
                        availableUnits = prod?.secondaryUnits ?: emptyList(),
                        selectedUnitName = line.selectedUnitName,
                        unitConversionFactor = line.unitConversionFactor,
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        unitCost = line.unitCost
                    )
                }
                _invoiceState.update {
                    it.copy(
                        selectedPartyId = found.invoice.partyId,
                        selectedPartyName = found.invoice.partyName,
                        currencyCode = found.invoice.currencyCode,
                        lockedExchangeRate = found.invoice.lockedExchangeRate,
                        items = restoredDrafts,
                        isStandaloneReturn = false,
                        linkedOriginalInvoiceNumber = found.invoice.invoiceNumber,
                        notes = "مرتجع مرتبط بالفاتورة الأصلية رقم ${found.invoice.invoiceNumber}"
                    )
                }
                showMessage("تم ربط المرتجع بالفاتورة الأصلية (${found.invoice.invoiceNumber}) واسترجاع الأصناف")
            } else {
                showMessage("لم يتم العثور على فاتورة بهذا الرقم")
            }
        }
    }

    /**
     * Loads the Last Completed Invoice ([آخر فاتورة]) into the editor for inspection or re-printing
     */
    fun loadLastCompletedInvoice() {
        viewModelScope.launch {
            val last = dao.getLastCompletedInvoiceWithLines()
            if (last == null) {
                showMessage("لا توجد فواتير سابقة مسجلة")
                return@launch
            }
            restoreInvoiceIntoEditor(last, keepAsNewCopy = false)
            showMessage("تم استعراض آخر فاتورة: ${last.invoice.invoiceNumber}")
        }
    }

    /**
     * Resumes a Held Invoice ([الفواتير المعلقة]) into the active editor and removes the hold record
     */
    fun resumeHeldInvoice(held: InvoiceWithLines) {
        viewModelScope.launch {
            restoreInvoiceIntoEditor(held, keepAsNewCopy = false)
            dao.deleteInvoiceById(held.invoice.id)
            showMessage("تم استئناف الفاتورة المعلقة (${held.invoice.invoiceNumber})")
        }
    }

    /**
     * Recalls any archived/completed invoice from Loyverse Reports Archive into the Unified Invoice Editor
     * and checks if its shift is closed (isShiftClosed = true).
     */
    fun recallCompletedInvoiceToEditor(record: InvoiceWithLines) {
        viewModelScope.launch {
            restoreInvoiceIntoEditor(record, keepAsNewCopy = false)
        }
    }

    /**
     * Smart Rule 5: Prevents deleting an archived invoice if it belongs to a closed shift (isShiftClosed = true)
     */
    fun deleteArchivedInvoiceWithShiftCheck(record: InvoiceWithLines) {
        viewModelScope.launch {
            val shiftId = record.invoice.shiftId
            val shift = if (shiftId != null) dao.getShiftById(shiftId) else null
            val isShiftClosed = shift != null && !shift.isOpen
            if (isShiftClosed) {
                showMessage("ممنوع الحذف: الفاتورة (${record.invoice.invoiceNumber}) تنتمي إلى وردية مغلقة (Z-Report #${shift?.id})")
                return@launch
            }
            dao.deleteInvoiceById(record.invoice.id)
            showMessage("تم حذف الفاتورة (${record.invoice.invoiceNumber}) بنجاح")
        }
    }

    /**
     * Executes a Unified Voice Command to populate a Sales or Purchase Invoice in the editor
     */
    fun executeVoiceInvoiceCommand(
        isSale: Boolean,
        partyName: String,
        product: ProductEntity,
        quantity: Double,
        currencyCode: String
    ) {
        setInvoiceMode(if (isSale) InvoiceMode.SALE else InvoiceMode.PURCHASE)
        setInvoiceCurrency(currencyCode)
        val matchedParty = parties.value.firstOrNull {
            it.name.contains(partyName, ignoreCase = true)
        }
        if (matchedParty != null) {
            selectInvoiceParty(matchedParty)
        } else if (partyName.isNotBlank()) {
            _invoiceState.update { it.copy(selectedPartyName = partyName) }
        }
        addProductToInvoice(product = product, customQuantity = quantity.coerceAtLeast(1.0))
        showMessage("الأمر الصوتي الذكي: تم تجهيز فاتورة (${product.name} × ${quantity.toInt()}) للطرف ($partyName)")
    }

    private suspend fun restoreInvoiceIntoEditor(record: InvoiceWithLines, keepAsNewCopy: Boolean) {
        val inv = record.invoice
        val shift = if (inv.shiftId != null) dao.getShiftById(inv.shiftId) else null
        val isShiftClosed = !keepAsNewCopy && inv.status == InvoiceStatus.COMPLETED &&
            inv.mode != InvoiceMode.HOLD && inv.mode != InvoiceMode.QUOTATION &&
            (shift != null && !shift.isOpen)

        val drafts = record.lines.map { line ->
            val prod = products.value.find { it.id == line.productId }
            DraftInvoiceItem(
                productId = line.productId,
                productName = line.productName,
                barcode = line.barcode,
                baseUnitName = prod?.baseUnitName ?: line.selectedUnitName,
                availableUnits = prod?.secondaryUnits ?: emptyList(),
                selectedUnitName = line.selectedUnitName,
                unitConversionFactor = line.unitConversionFactor,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                unitCost = line.unitCost
            )
        }
        _invoiceState.update {
            it.copy(
                mode = if (inv.mode == InvoiceMode.HOLD) InvoiceMode.SALE else inv.mode,
                pricingTier = inv.pricingTier,
                selectedPartyId = inv.partyId,
                selectedPartyName = inv.partyName,
                currencyCode = inv.currencyCode,
                lockedExchangeRate = inv.lockedExchangeRate,
                discountAmount = inv.discountAmount,
                additionAmount = inv.additionAmount,
                includeVat = inv.vatPercent > 0.0,
                vatPercent = inv.vatPercent,
                paymentMethod = inv.paymentMethod,
                selectedAccountId = inv.financialAccountId,
                selectedAccountName = inv.financialAccountName,
                paperBookNumber = if (keepAsNewCopy) "BK-${(500..999).random()}" else inv.paperBookNumber,
                notes = inv.notes,
                items = drafts,
                editingExistingInvoiceId = if (keepAsNewCopy) null else inv.id,
                isLockedByClosedShift = isShiftClosed,
                lockedShiftNotice = if (isShiftClosed) {
                    "هذه الفاتورة (${inv.invoiceNumber}) مقفلة لأنها تنتمي إلى وردية مغلقة (isShiftClosed = true). لا يمكن تعديلها أو حذفها حفاظاً على مطابقة تقرير Z-Report."
                } else null
            )
        }
        if (isShiftClosed) {
            showMessage("تنبيه: الفاتورة (${inv.invoiceNumber}) مقفلة لانتمائها إلى وردية مغلقة")
        }
    }

    /**
     * Holds the current invoice ([تعليق الفاتورة]) or commits it permanently ([حفظ وطباعة] / [حفظ وفاتورة جديدة])
     */
    fun commitCurrentInvoice(holdOnly: Boolean = false, printReceipt: Boolean = false) {
        val state = _invoiceState.value
        if (state.isLockedByClosedShift) {
            showMessage("ممنوع التعديل: الفاتورة تنتمي إلى وردية مغلقة (isShiftClosed = true). اضغط (نسخة جديدة) لإنشاء فاتورة منفصلة.")
            return
        }
        if (state.items.isEmpty()) {
            showMessage("الرجاء إضافة صنف واحد على الأقل إلى الفاتورة")
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prefix = when {
                holdOnly -> "HOLD"
                state.mode == InvoiceMode.SALE -> "INV"
                state.mode == InvoiceMode.PURCHASE -> "PUR"
                state.mode == InvoiceMode.SALE_RETURN -> "SRET"
                state.mode == InvoiceMode.PURCHASE_RETURN -> "PRET"
                state.mode == InvoiceMode.DAMAGE -> "DMG"
                state.mode == InvoiceMode.QUOTATION -> "QUO"
                else -> "INV"
            }
            val invoiceNumber = "$prefix-2026-${(now / 1000) % 100000}"

            // Prepare B2B .sanad JSON export if this is a Sales Invoice
            val b2bExportJson = if (!holdOnly && state.mode == InvoiceMode.SALE) {
                generateSanadB2BJson(
                    invoiceNumber = invoiceNumber,
                    partyName = state.selectedPartyName,
                    currencyCode = state.currencyCode,
                    exchangeRate = state.lockedExchangeRate,
                    items = state.items,
                    discountAmount = state.discountAmount,
                    additionAmount = state.additionAmount,
                    vatAmount = state.calculatedVatAmount,
                    netTotal = state.netTotalAmount
                )
            } else null

            val invoiceEntity = SaleInvoiceEntity(
                invoiceNumber = invoiceNumber,
                paperBookNumber = state.paperBookNumber,
                mode = state.mode,
                status = if (holdOnly) InvoiceStatus.HELD else InvoiceStatus.COMPLETED,
                pricingTier = state.pricingTier,
                partyId = state.selectedPartyId,
                partyName = state.selectedPartyName,
                currencyCode = state.currencyCode,
                lockedExchangeRate = state.lockedExchangeRate,
                subtotalAmount = state.subtotalAmount,
                discountAmount = state.discountAmount,
                additionAmount = state.additionAmount,
                vatPercent = if (state.includeVat) state.vatPercent else 0.0,
                vatAmount = state.calculatedVatAmount,
                netTotalAmount = state.netTotalAmount,
                netTotalBaseEquivalent = state.netTotalUsdEquivalent,
                totalCostAmount = state.totalCostAmount,
                realizedProfitAmount = state.realizedProfitAmount,
                paymentMethod = state.paymentMethod,
                paidAmount = state.effectivePaidAmount,
                remainingCreditAmount = state.remainingCreditAmount,
                financialAccountId = state.selectedAccountId,
                financialAccountName = state.selectedAccountName,
                splitPayments = state.splitPayments,
                linkedOriginalInvoiceNumber = state.linkedOriginalInvoiceNumber,
                isStandaloneReturn = state.isStandaloneReturn,
                notes = state.notes,
                timestamp = now
            )

            val lineEntities = state.items.map { item ->
                SaleItemEntity(
                    invoiceId = 0,
                    productId = item.productId,
                    productName = item.productName,
                    barcode = item.barcode,
                    selectedUnitName = item.selectedUnitName,
                    unitConversionFactor = item.unitConversionFactor,
                    quantity = item.quantity,
                    baseUnitQuantityDeducted = item.baseQuantityDeducted,
                    unitPrice = item.unitPrice,
                    unitCost = item.unitCost,
                    discountAmount = item.discountAmount,
                    taxPercent = item.taxPercent,
                    taxAmount = item.lineTaxAmount,
                    lineTotal = item.lineTotal
                )
            }

            dao.commitUnifiedInvoiceTransaction(invoiceEntity, lineEntities)

            // Reset Editor for Next Invoice while preserving B2B (.sanad) export banner if Sales Invoice
            _invoiceState.update { current ->
                InvoiceEditorState(
                    mode = current.mode,
                    pricingTier = current.pricingTier,
                    currencyCode = current.currencyCode,
                    lockedExchangeRate = current.lockedExchangeRate,
                    selectedAccountId = current.selectedAccountId,
                    selectedAccountName = current.selectedAccountName,
                    paperBookNumber = "BK-${(510..999).random()}",
                    lastSavedSaleInvoiceNumber = if (b2bExportJson != null) invoiceNumber else null,
                    lastSavedB2BSanadJson = b2bExportJson,
                    showAroniumProductGrid = current.showAroniumProductGrid
                )
            }

            val msg = when {
                holdOnly -> "تم تعليق الفاتورة ($invoiceNumber) وحفظها في قائمة الفواتير المعلقة"
                state.mode == InvoiceMode.QUOTATION -> "تم حفظ عرض السعر ($invoiceNumber) بدون التأثير على المخزون أو الصندوق"
                printReceipt -> "تم حفظ الفاتورة ($invoiceNumber) وإرسالها للطباعة الحرارية بنجاح"
                else -> "تم حفظ الفاتورة ($invoiceNumber) وفتح فاتورة جديدة"
            }
            showMessage(msg)
        }
    }

    /**
     * Quick Customer/Supplier Creation directly from Invoice Editor (+) button
     */
    fun quickCreateParty(name: String, phone: String, partyType: PartyType, currencyCode: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newParty = PartyEntity(
                name = name.trim(),
                phone = phone.trim(),
                partyType = partyType,
                primaryCurrency = currencyCode
            )
            val id = dao.insertParty(newParty)
            if (partyType == PartyType.CUSTOMER) {
                dao.insertCustomer(
                    CustomerEntity(
                        id = id,
                        name = name.trim(),
                        phone = phone.trim(),
                        currencyCode = currencyCode
                    )
                )
            } else if (partyType == PartyType.SUPPLIER) {
                dao.insertSupplier(
                    SupplierEntity(
                        id = id,
                        name = name.trim(),
                        phone = phone.trim(),
                        currencyCode = currencyCode
                    )
                )
            }
            _invoiceState.update {
                it.copy(selectedPartyId = id, selectedPartyName = name.trim())
            }
            showMessage("تمت إضافة (${name.trim()}) وتحديده في الفاتورة")
        }
    }

    // ========================================================================
    // MODULE 2: ADVANCED INVENTORY CONTROL & NEW PRODUCT BUILDER
    // ========================================================================

    fun saveNewOrEditedProduct(
        editingProductId: Long? = null,
        name: String,
        category: String,
        barcode: String,
        transactionCurrency: String,
        costPrice: Double,
        profitMarginPercent: Double,
        wholesalePrice: Double,
        retailPrice: Double,
        availableQuantity: Double,
        safetyThreshold: Double,
        baseUnitName: String,
        secondaryUnits: List<ProductUnitConversion>,
        soldByWeightPlu: Boolean,
        taxType: TaxType,
        customTaxPercent: Double,
        isCompositeBundle: Boolean,
        bundleComponents: List<BundleComponentItem>,
        trackBatchesAndExpiry: Boolean,
        nearestExpiryTimestamp: Long?
    ) {
        if (name.isBlank()) {
            showMessage("يرجى إدخال اسم الصنف")
            return
        }
        viewModelScope.launch {
            val finalBarcode = barcode.ifBlank { "6281000${(100000..999999).random()}" }
            val product = ProductEntity(
                id = editingProductId ?: 0L,
                name = name.trim(),
                category = category.ifBlank { "عام" },
                barcode = finalBarcode,
                transactionCurrency = transactionCurrency,
                costPrice = costPrice,
                weightedAverageCost = costPrice,
                profitMarginPercent = profitMarginPercent,
                wholesalePrice = wholesalePrice,
                retailPrice = retailPrice,
                availableQuantity = availableQuantity,
                safetyThreshold = safetyThreshold,
                baseUnitName = baseUnitName.ifBlank { "وحدة" },
                secondaryUnits = secondaryUnits,
                soldByWeightPlu = soldByWeightPlu,
                taxType = taxType,
                customTaxPercent = customTaxPercent,
                isCompositeBundle = isCompositeBundle,
                bundleComponents = bundleComponents,
                trackBatchesAndExpiry = trackBatchesAndExpiry,
                nearestExpiryTimestamp = nearestExpiryTimestamp
            )

            val relationalUnits = secondaryUnits.map {
                ProductUnitEntity(
                    productId = editingProductId ?: 0L,
                    unitName = it.unitName,
                    conversionFactor = it.conversionFactor,
                    barcode = it.barcode,
                    retailPrice = it.retailPrice,
                    wholesalePrice = it.wholesalePrice
                )
            }

            val relationalComponents = bundleComponents.map {
                CompositeComponentEntity(
                    parentProductId = editingProductId ?: 0L,
                    componentProductId = it.componentProductId,
                    componentProductName = it.componentProductName,
                    requiredQuantity = it.requiredQuantity,
                    unitCost = it.unitCost
                )
            }

            val savedId = dao.saveProductWithUnitsAndComponents(
                product = product,
                units = relationalUnits,
                components = relationalComponents
            )

            if (editingProductId == null && availableQuantity > 0) {
                val rate = currencies.value.find { it.code == transactionCurrency }?.exchangeRateToBase ?: 1550.0
                dao.insertStockMovement(
                    StockMovementEntity(
                        productId = savedId,
                        productName = product.name,
                        movementType = StockMovementType.OPENING_STOCK,
                        quantityDelta = availableQuantity,
                        balanceAfter = availableQuantity,
                        unitName = product.baseUnitName,
                        unitCostAtMovement = costPrice,
                        unitPriceAtMovement = retailPrice,
                        currencyCode = transactionCurrency,
                        exchangeRateLocked = rate,
                        reasonOrNotes = "رصيد افتتاحي عند تعريف الصنف"
                    )
                )
            }

            showMessage("تم حفظ الصنف (${product.name}) في المستودع بنجاح")
        }
    }

    /**
     * 1-Click Fast Write-off ([إتلاف سريع]) from Rich Stock Card
     */
    fun triggerFastWriteOff(product: ProductEntity, quantity: Double, reason: String) {
        if (quantity <= 0) return
        viewModelScope.launch {
            dao.performFastDamageWriteOff(
                productId = product.id,
                quantityToDestroy = quantity,
                reason = reason.ifBlank { "إتلاف سريع - انتهاء صلاحية / تلف" }
            )
            showMessage("تم تسجيل إتلاف سريع لـ $quantity ${product.baseUnitName} من (${product.name})")
        }
    }

    /**
     * Physical Inventory Count Session ([بدء جلسة جرد])
     */
    fun commitInventoryCount(
        sessionTitle: String,
        categoryFilter: String,
        countedQuantities: Map<Long, Double>,
        notes: String
    ) {
        viewModelScope.launch {
            dao.commitPhysicalInventoryCountSession(
                sessionTitle = sessionTitle.ifBlank { "جلسة جرد مخزني دوري" },
                categoryFilter = categoryFilter,
                countedItems = countedQuantities,
                notes = notes
            )
            showMessage("تم اعتماد جلسة الجرد الفعلي وتسوية الفائض والعجز بنجاح")
        }
    }

    // ========================================================================
    // MODULE 3 & 4 HELPERS (TREASURY, SHIFTS, VOUCHERS, EXPENSES, CURRENCIES)
    // ========================================================================

    fun updateExchangeRate(currencyCode: String, newRateToBase: Double) {
        if (newRateToBase <= 0) return
        viewModelScope.launch {
            dao.updateCurrencyRate(currencyCode, newRateToBase)
            if (_invoiceState.value.currencyCode == currencyCode) {
                _invoiceState.update { it.copy(lockedExchangeRate = newRateToBase) }
            }
            showMessage("تم تحديث سعر الصرف ($currencyCode = $newRateToBase)")
        }
    }

    fun registerVoucher(
        partyType: PartyType,
        partyId: Long,
        partyName: String,
        direction: VoucherDirection,
        currencyCode: String,
        lockedRate: Double,
        amount: Double,
        accountId: Long,
        accountName: String,
        notes: String
    ) {
        if (amount <= 0) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val vch = LedgerEntryEntity(
                voucherNumber = "VCH-2026-${(now / 1000) % 100000}",
                partyType = partyType,
                partyId = partyId,
                partyName = partyName,
                direction = direction,
                currencyCode = currencyCode,
                lockedExchangeRate = lockedRate,
                amount = amount,
                baseUsdEquivalent = if (lockedRate > 0) amount / lockedRate else amount,
                financialAccountId = accountId,
                financialAccountName = accountName,
                notes = notes,
                timestamp = now
            )
            dao.commitVoucherTransaction(vch)
            showMessage("تم حفظ السند (${vch.voucherNumber}) وتثبيت سعر الصرف")
        }
    }

    fun registerExpense(
        title: String,
        category: String,
        currencyCode: String,
        lockedRate: Double,
        amount: Double,
        accountId: Long,
        accountName: String,
        supplierPartyId: Long?,
        supplierPartyName: String?,
        notes: String
    ) {
        if (title.isBlank() || amount <= 0) return
        viewModelScope.launch {
            val exp = ExpenseEntity(
                title = title.trim(),
                category = category,
                currencyCode = currencyCode,
                lockedExchangeRate = lockedRate,
                amount = amount,
                baseUsdEquivalent = if (lockedRate > 0) amount / lockedRate else amount,
                financialAccountId = accountId,
                financialAccountName = accountName,
                supplierPartyId = supplierPartyId,
                supplierPartyName = supplierPartyName,
                notes = notes
            )
            dao.commitExpenseTransaction(exp)
            showMessage("تم تسجيل المصروف (${exp.title}) بنجاح")
        }
    }

    fun transferToVault(sourceAccountId: Long, targetVaultId: Long, amount: Double, notes: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            dao.commitVaultTransferTransaction(sourceAccountId, targetVaultId, amount, notes)
            showMessage("تم ترحيل المبلغ إلى الخزينة بنجاح")
        }
    }

    fun reconcileAccountBalance(accountId: Long, actualBalance: Double, reason: String) {
        viewModelScope.launch {
            dao.commitAccountReconciliationTransaction(accountId, actualBalance, reason)
            showMessage("تم تسوية رصيد الحساب المالي وتوليد قيد التسوية")
        }
    }

    fun openNewShift(
        cashierName: String,
        openingSource: ShiftOpeningSource,
        primaryAccountId: Long,
        primaryAccountName: String,
        currencyCode: String,
        openingCustody: Double,
        secondaryAccountIds: List<Long>
    ) {
        viewModelScope.launch {
            val current = dao.getActiveShiftSnapshot()
            if (current != null) {
                dao.updateShift(current.copy(isOpen = false, closedAt = System.currentTimeMillis()))
            }
            dao.insertShift(
                ShiftEntity(
                    cashierName = cashierName.ifBlank { "الكاشير المناوب" },
                    openingSource = openingSource,
                    primaryCashAccountId = primaryAccountId,
                    primaryCashAccountName = primaryAccountName,
                    currencyCode = currencyCode,
                    openingCustodyBalance = openingCustody,
                    lockedSecondaryAccountIds = secondaryAccountIds,
                    expectedClosingBalance = openingCustody,
                    isOpen = true
                )
            )
            showMessage("تم فتح الوردية الجديدة باسم ($cashierName)")
        }
    }

    fun closeActiveShiftZReport(actualCounted: Double, notes: String) {
        viewModelScope.launch {
            val current = dao.getActiveShiftSnapshot() ?: return@launch
            val variance = actualCounted - current.expectedClosingBalance
            dao.updateShift(
                current.copy(
                    actualCountedClosingBalance = actualCounted,
                    varianceAmount = variance,
                    isOpen = false,
                    closedAt = System.currentTimeMillis(),
                    zReportNotes = notes
                )
            )
            showMessage("تم إغلاق الوردية وإصدار تقرير Z-Report بنجاح")
        }
    }

    /**
     * Merges B2B or AI OCR Imported Supplier Invoice Items into Inventory Stock,
     * updates Weighted Average Cost & Suggested Retail Prices, and creates an atomic Purchase Invoice.
     */
    fun mergeImportedSupplierItemsIntoStock(
        supplierId: Long,
        supplierName: String,
        currencyCode: String,
        items: List<com.example.data.remote.ImportedSupplierItem>,
        sourceDescription: String
    ) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val rate = currencies.value.find { it.code == currencyCode }?.exchangeRateToBase ?: 1550.0
            val purchaseLines = mutableListOf<SaleItemEntity>()

            for (imported in items) {
                val existing = imported.matchedLocalProductId?.let { dao.getProductById(it) }
                    ?: dao.findProductByBarcode(imported.barcode)

                val productId = if (existing != null) {
                    // Update retail price to suggested retail price while commitUnifiedInvoiceTransaction updates WAC & stock
                    dao.updateProduct(
                        existing.copy(
                            retailPrice = imported.suggestedRetailPrice,
                            wholesalePrice = (imported.suggestedRetailPrice * 0.94).roundToLong().toDouble()
                        )
                    )
                    existing.id
                } else {
                    val newProd = ProductEntity(
                        name = imported.supplierItemName,
                        category = "مستورد B2B / AI",
                        barcode = imported.barcode.ifBlank { "6281000${(100000..999999).random()}" },
                        transactionCurrency = currencyCode,
                        costPrice = imported.supplierUnitPrice,
                        weightedAverageCost = imported.supplierUnitPrice,
                        profitMarginPercent = imported.selectedMarginPercent,
                        wholesalePrice = (imported.suggestedRetailPrice * 0.94).roundToLong().toDouble(),
                        retailPrice = imported.suggestedRetailPrice,
                        availableQuantity = 0.0, // Will be added by commitUnifiedInvoiceTransaction below
                        baseUnitName = imported.unitName
                    )
                    dao.insertProduct(newProd)
                }

                purchaseLines.add(
                    SaleItemEntity(
                        invoiceId = 0,
                        productId = productId,
                        productName = imported.supplierItemName,
                        barcode = imported.barcode,
                        selectedUnitName = imported.unitName,
                        unitConversionFactor = 1.0,
                        quantity = imported.quantity,
                        baseUnitQuantityDeducted = imported.quantity,
                        unitPrice = imported.supplierUnitPrice,
                        unitCost = imported.supplierUnitPrice,
                        lineTotal = imported.lineTotalCost
                    )
                )
            }

            val totalCost = purchaseLines.sumOf { it.lineTotal }
            val purchaseInvoice = SaleInvoiceEntity(
                invoiceNumber = "B2B-2026-${(now / 1000) % 100000}",
                paperBookNumber = "B2B-IMP",
                mode = InvoiceMode.PURCHASE,
                status = InvoiceStatus.COMPLETED,
                pricingTier = PricingTier.WHOLESALE,
                partyId = supplierId,
                partyName = supplierName,
                currencyCode = currencyCode,
                lockedExchangeRate = rate,
                subtotalAmount = totalCost,
                netTotalAmount = totalCost,
                netTotalBaseEquivalent = if (rate > 0) totalCost / rate else totalCost,
                totalCostAmount = totalCost,
                paymentMethod = PaymentMethod.CREDIT,
                paidAmount = 0.0,
                remainingCreditAmount = totalCost,
                notes = sourceDescription,
                timestamp = now
            )

            dao.commitUnifiedInvoiceTransaction(purchaseInvoice, purchaseLines)
            showMessage("تم دمج ${items.size} أصناف من ($supplierName) وتحديث المخزون ومتوسط التكلفة بنجاح")
        }
    }

    /**
     * Checks if a Supplier Purchase Invoice with the same supplier name and invoice number already exists
     */
    suspend fun isDuplicateSupplierPurchaseInvoice(supplierName: String, invoiceRef: String): Boolean {
        if (invoiceRef.isBlank()) return false
        return dao.findDuplicatePurchaseInvoice(supplierName.trim(), invoiceRef.trim()) != null
    }

    /**
     * Commits Stage 6 of the 6-Stage AI Supplier Invoice Scanner (ocrPurchaseFlow)
     * including Tax in Cost, Shipping Additions, Supplier Discount, FEFO batches, and Price Radar updates.
     */
    fun commitSixStageOcrPurchase(
        supplierId: Long?,
        supplierName: String,
        supplierTaxNumber: String,
        paperInvoiceNumber: String,
        currencyCode: String,
        exchangeRateLocked: Double,
        includeTaxInCost: Boolean,
        taxAmount: Double,
        shippingAdditionAmount: Double,
        supplierDiscountAmount: Double,
        paymentMethod: PaymentMethod,
        financialAccountId: Long?,
        financialAccountName: String,
        items: List<com.example.data.remote.ImportedSupplierItem>,
        onCommitted: (String) -> Unit
    ) {
        val activeItems = items.filter { it.mappingAction != com.example.data.remote.OcrLineMappingAction.IGNORED }
        if (activeItems.isEmpty()) {
            showMessage("لا توجد أسطر مفعّلة للاستيراد بعد استبعاد الأسطر المتجاهلة")
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val finalInvNum = paperInvoiceNumber.trim().ifBlank { "OCR-${(now / 1000) % 100000}" }
            val purchaseLines = mutableListOf<SaleItemEntity>()

            for (imported in activeItems) {
                val isMatch = imported.mappingAction == com.example.data.remote.OcrLineMappingAction.MATCHED
                val existing = if (isMatch) {
                    imported.matchedLocalProductId?.let { dao.getProductById(it) }
                        ?: dao.findProductByBarcode(imported.barcode)
                } else null

                val productId = if (existing != null) {
                    val updatedRetail = if (imported.applyPriceRadarUpdate && imported.suggestedRetailPrice > 0) {
                        imported.suggestedRetailPrice
                    } else {
                        existing.retailPrice
                    }
                    val updatedWholesale = if (imported.applyPriceRadarUpdate && imported.suggestedWholesalePrice > 0) {
                        imported.suggestedWholesalePrice
                    } else {
                        existing.wholesalePrice
                    }
                    dao.updateProduct(
                        existing.copy(
                            retailPrice = updatedRetail,
                            wholesalePrice = updatedWholesale,
                            trackBatchesAndExpiry = existing.trackBatchesAndExpiry || imported.enableFefoTracking
                        )
                    )
                    existing.id
                } else {
                    val newProd = ProductEntity(
                        name = imported.supplierItemName,
                        category = "مستورد AI OCR",
                        barcode = imported.barcode.ifBlank { "6281000${(100000..999999).random()}" },
                        transactionCurrency = currencyCode,
                        costPrice = if (imported.isFreeBonusConfirmed) 0.0 else imported.supplierUnitPrice,
                        weightedAverageCost = if (imported.isFreeBonusConfirmed) 0.0 else imported.supplierUnitPrice,
                        profitMarginPercent = imported.selectedMarginPercent,
                        wholesalePrice = imported.suggestedWholesalePrice.takeIf { it > 0 }
                            ?: (imported.suggestedRetailPrice * 0.94).roundToLong().toDouble(),
                        retailPrice = imported.suggestedRetailPrice.coerceAtLeast(1.0),
                        availableQuantity = 0.0,
                        baseUnitName = imported.unitName,
                        trackBatchesAndExpiry = imported.enableFefoTracking,
                        nearestExpiryTimestamp = imported.expiryDateTimestamp
                    )
                    dao.insertProduct(newProd)
                }

                val effectiveUnitCost = if (imported.isFreeBonusConfirmed) 0.0 else imported.supplierUnitPrice
                val effectiveLineTotal = if (imported.isFreeBonusConfirmed) 0.0 else imported.lineTotalCost

                purchaseLines.add(
                    SaleItemEntity(
                        invoiceId = 0,
                        productId = productId,
                        productName = imported.supplierItemName,
                        barcode = imported.barcode,
                        selectedUnitName = imported.unitName,
                        unitConversionFactor = 1.0,
                        quantity = imported.quantity,
                        baseUnitQuantityDeducted = imported.quantity,
                        unitPrice = effectiveUnitCost,
                        unitCost = effectiveUnitCost,
                        lineTotal = effectiveLineTotal,
                        batchNumber = if (imported.enableFefoTracking) imported.batchNumber?.ifBlank { "LOT-${now % 10000}" } else null,
                        expiryDateTimestamp = if (imported.enableFefoTracking) {
                            imported.expiryDateTimestamp ?: (now + 180L * 24L * 3600L * 1000L)
                        } else null
                    )
                )
            }

            val subtotal = purchaseLines.sumOf { it.lineTotal }
            val effectiveVat = if (includeTaxInCost) taxAmount else 0.0
            val netTotal = (subtotal + shippingAdditionAmount + effectiveVat - supplierDiscountAmount).coerceAtLeast(0.0)
            val paid = if (paymentMethod == PaymentMethod.CASH) netTotal else 0.0
            val remaining = (netTotal - paid).coerceAtLeast(0.0)

            val purchaseInvoice = SaleInvoiceEntity(
                invoiceNumber = "PUR-$finalInvNum",
                paperBookNumber = finalInvNum,
                mode = InvoiceMode.PURCHASE,
                status = InvoiceStatus.COMPLETED,
                pricingTier = PricingTier.WHOLESALE,
                partyId = supplierId,
                partyName = supplierName.ifBlank { "مورد عام" },
                currencyCode = currencyCode,
                lockedExchangeRate = exchangeRateLocked,
                subtotalAmount = subtotal,
                discountAmount = supplierDiscountAmount,
                additionAmount = shippingAdditionAmount,
                vatPercent = if (subtotal > 0 && effectiveVat > 0) (effectiveVat / subtotal) * 100.0 else 0.0,
                vatAmount = effectiveVat,
                netTotalAmount = netTotal,
                netTotalBaseEquivalent = if (exchangeRateLocked > 0) netTotal / exchangeRateLocked else netTotal,
                totalCostAmount = netTotal,
                paymentMethod = paymentMethod,
                paidAmount = paid,
                remainingCreditAmount = remaining,
                financialAccountId = financialAccountId,
                financialAccountName = financialAccountName,
                notes = "استيراد فاتورة مورد بالذكاء الاصطناعي (رقم ضريبي: ${supplierTaxNumber.ifBlank { "غير محدد" }})",
                timestamp = now
            )

            dao.commitUnifiedInvoiceTransaction(purchaseInvoice, purchaseLines)
            showMessage("تم اعتماد فاتورة الشراء (${purchaseInvoice.invoiceNumber}) وتحديث المخزون ومتوسط التكلفة المرجح بنجاح")
            onCommitted(purchaseInvoice.invoiceNumber)
        }
    }
}


