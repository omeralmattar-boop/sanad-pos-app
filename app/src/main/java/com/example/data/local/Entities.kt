package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ============================================================================
// 1. MULTI-CURRENCY ENGINE & EXCHANGE RATES (CurrencyAndRateEntity)
// ============================================================================
@Entity(tableName = "currencies")
data class CurrencyAndRateEntity(
    @PrimaryKey
    val code: String,               // "YER", "USD", "SAR", "TRY"
    val nameArabic: String,         // "ريال يمني", "دولار أمريكي", "ريال سعودي", "ليرة تركية"
    val symbol: String,             // "ر.ي", "$", "ر.س", "₺"
    val exchangeRateToBase: Double, // Rate relative to USD (1 USD = 1550 YER, 1 USD = 1.0 USD)
    val isBaseCurrency: Boolean = false,
    val isPreferredDisplay: Boolean = false,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

typealias CurrencyEntity = CurrencyAndRateEntity

// ============================================================================
// 2. INVENTORY PRODUCTS, MULTI-UNIT, PLU SCALE & COMPOSITE BUNDLES
//    (ProductEntity, ProductUnitEntity, CompositeComponentEntity)
// ============================================================================
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["category"]),
        Index(value = ["name"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val barcode: String,
    val thumbnailUri: String? = null,
    val transactionCurrency: String = "YER",
    val costPrice: Double,                  // سعر التكلفة
    val weightedAverageCost: Double,        // متوسط التكلفة المرجح
    val profitMarginPercent: Double,        // نسبة الربح %
    val wholesalePrice: Double,             // سعر الجملة
    val retailPrice: Double,                // سعر المفرق
    val availableQuantity: Double,          // الكمية المتاحة بالوحدة الأساسية
    val safetyThreshold: Double = 10.0,     // حد الأمان
    val baseUnitName: String = "لتر",       // اسم الوحدة الأساسية (لتر، كيس، كجم، حبة...)
    val secondaryUnits: List<ProductUnitConversion> = emptyList(), // الوحدات المتعددة ومعامل التحويل (مثل: 6 تنكة = 96 لتر، 4 كرتونة = 40 كيس)
    val soldByWeightPlu: Boolean = false,   // يباع بالوزن - ميزان PLU
    val pluPrefix: String = "21",           // بادئة باركود الميزان
    val taxType: TaxType = TaxType.ZERO,    // قياسية، صفرية، معفاة، مخصصة
    val customTaxPercent: Double = 0.0,
    val isCompositeBundle: Boolean = false, // هل هذا منتج تجميعي؟
    val bundleComponents: List<BundleComponentItem> = emptyList(),
    val trackBatchesAndExpiry: Boolean = false, // تتبع الدفعات / الصلاحية
    val nearestExpiryTimestamp: Long? = null,   // أقرب تاريخ صلاحية للتنبيه السريع (منتهي الصلاحية)
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Generates a formatted Arabic multi-unit breakdown string (e.g., "6 تنكة = 96 لتر" or "4 كرتونة = 40 كيس")
     */
    fun formattedMultiUnitBreakdown(): String? {
        val primarySec = secondaryUnits.firstOrNull { it.conversionFactor > 1.0 } ?: return null
        val secCount = availableQuantity / primarySec.conversionFactor
        val countFormatted = if (secCount % 1.0 == 0.0) {
            secCount.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", secCount)
        }
        val baseFormatted = if (availableQuantity % 1.0 == 0.0) {
            availableQuantity.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", availableQuantity)
        }
        return "$countFormatted ${primarySec.unitName} = $baseFormatted $baseUnitName"
    }
}

@Entity(
    tableName = "product_units",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productId"]), Index(value = ["barcode"])]
)
data class ProductUnitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val unitName: String,           // e.g., "تنكة", "كرتونة"
    val conversionFactor: Double,   // e.g., 16.0 (1 تنكة = 16 لتر), 10.0 (1 كرتونة = 10 كيس)
    val barcode: String = "",
    val retailPrice: Double = 0.0,
    val wholesalePrice: Double = 0.0
)

@Entity(
    tableName = "composite_components",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentProductId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentProductId"]), Index(value = ["componentProductId"])]
)
data class CompositeComponentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentProductId: Long,
    val componentProductId: Long,
    val componentProductName: String,
    val requiredQuantity: Double,
    val unitCost: Double
)

@Entity(
    tableName = "product_batches",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productId"]), Index(value = ["expiryDateTimestamp"])]
)
data class ProductBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val batchNumber: String,
    val expiryDateTimestamp: Long,
    val quantity: Double,
    val costPrice: Double,
    val currencyCode: String = "YER",
    val receivedTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productId"]), Index(value = ["timestamp"])]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val movementType: StockMovementType,
    val quantityDelta: Double,      // +ve for inflow, -ve for outflow/damage
    val balanceAfter: Double,
    val unitName: String,
    val unitCostAtMovement: Double,
    val unitPriceAtMovement: Double,
    val currencyCode: String,
    val exchangeRateLocked: Double,
    val referenceInvoiceId: Long? = null,
    val reasonOrNotes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ============================================================================
// 3. LOYVERSE-GRADE INVENTORY COUNT SESSIONS (جلسات الجرد الفعلي)
// ============================================================================
@Entity(tableName = "inventory_count_sessions")
data class InventoryCountSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionTitle: String,
    val categoryFilter: String = "الكل",
    val status: String = "COMPLETED",
    val totalSurplusValueYer: Double = 0.0,
    val totalDeficitValueYer: Double = 0.0,
    val netVarianceValueYer: Double = 0.0,
    val notes: String = "",
    val createdBy: String = "المشرف العام",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory_count_lines",
    foreignKeys = [
        ForeignKey(
            entity = InventoryCountSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"]), Index(value = ["productId"])]
)
data class InventoryCountLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val expectedQty: Double,
    val countedQty: Double,
    val varianceQty: Double,
    val unitCostYer: Double,
    val varianceValueYer: Double
)

// ============================================================================
// 4. FINANCIAL ACCOUNTS (TREASURY, CASH BOXES, DIGITAL WALLETS, BANKS)
// ============================================================================
@Entity(
    tableName = "financial_accounts",
    indices = [Index(value = ["currencyCode"]), Index(value = ["sortOrder"])]
)
data class FinancialAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,               // e.g., "الصندوق النقدي الرئيسي", "شام كاش USD", "بنك بيمو", "بنك بيبلوس"
    val accountType: AccountType,
    val currencyCode: String,       // "YER", "USD", "SAR", "TRY"
    val currentBalance: Double,
    val isDefaultForCurrency: Boolean = false,
    val isVault: Boolean = false,
    val sortOrder: Int = 0,
    val accountNumberOrIban: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val lastReconciledTimestamp: Long? = null
)

// ============================================================================
// 5. PARTIES & DEDICATED CUSTOMER / SUPPLIER / LEDGER ENTITIES
//    (PartyEntity, CustomerEntity, SupplierEntity, LedgerEntryEntity)
// ============================================================================
@Entity(
    tableName = "parties",
    indices = [Index(value = ["partyType"]), Index(value = ["name"]), Index(value = ["phone"])]
)
data class PartyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,               // e.g., "أحمد الخطيب", "محمد العلي", "مطعم الأصيل", "معصرة الجنوب", "مؤسسة النور"
    val phone: String,
    val partyType: PartyType,       // CUSTOMER (عميل - لنا), SUPPLIER (مورد - علينا), PRIVATE (خاص)
    val primaryCurrency: String = "YER",
    val balanceInPrimaryCurrency: Double = 0.0,
    val balanceInUsdEquivalent: Double = 0.0,
    val creditLimitYer: Double = 5_000_000.0,
    val dueDateTimestamp: Long? = null,
    val lastTransactionTimestamp: Long = System.currentTimeMillis(),
    val address: String = "",
    val taxNumber: String = "",
    val notes: String = ""
)

@Entity(
    tableName = "customers",
    indices = [Index(value = ["name"]), Index(value = ["phone"])]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,               // أحمد الخطيب، محمد العلي، مطعم الأصيل
    val phone: String,
    val currencyCode: String = "YER",
    val receivableBalance: Double = 0.0, // لنا (موجب)
    val receivableUsdEquivalent: Double = 0.0,
    val creditLimit: Double = 5_000_000.0,
    val isOverdue: Boolean = false,
    val dueDateTimestamp: Long? = null,
    val address: String = "",
    val notes: String = "",
    val lastTransactionTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["name"]), Index(value = ["phone"])]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,               // معصرة الجنوب، مؤسسة النور
    val phone: String,
    val currencyCode: String = "YER",
    val payableBalance: Double = 0.0,    // علينا
    val payableUsdEquivalent: Double = 0.0,
    val isOverdue: Boolean = false,
    val dueDateTimestamp: Long? = null,
    val address: String = "",
    val taxNumber: String = "",
    val notes: String = "",
    val lastTransactionTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ledger_entries",
    indices = [
        Index(value = ["voucherNumber"], unique = true),
        Index(value = ["partyId"]),
        Index(value = ["financialAccountId"]),
        Index(value = ["timestamp"])
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val voucherNumber: String,          // e.g., "VCH-2026-501"
    val partyType: PartyType,           // PRIVATE, CUSTOMER, SUPPLIER
    val partyId: Long,
    val partyName: String,
    val direction: VoucherDirection,    // PAYMENT_OUT (دفعة - يخرج من الصندوق), RECEIPT_IN (قبضة - يدخل إلى الصندوق)
    val currencyCode: String,
    val lockedExchangeRate: Double,     // يثبت سعر الصرف على سند الدفعة/القبضة ولا يتغير بعد التسجيل
    val amount: Double,
    val baseUsdEquivalent: Double,
    val financialAccountId: Long,
    val financialAccountName: String,
    val shiftId: Long? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

typealias VoucherEntity = LedgerEntryEntity

// ============================================================================
// 6. UNIFIED INVOICES & LINE ITEMS (SaleInvoiceEntity & SaleItemEntity)
//    Supports SALE, PURCHASE, SALE_RETURN, PURCHASE_RETURN, DAMAGE, HOLD
// ============================================================================
@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["mode"]),
        Index(value = ["status"]),
        Index(value = ["partyId"]),
        Index(value = ["timestamp"])
    ]
)
data class SaleInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,          // e.g., "INV-2026-1001"
    val paperBookNumber: String = "",   // الرقم المرجعي (رقم الدفتر الورقي)
    val mode: InvoiceMode,              // SALE, PURCHASE, SALE_RETURN, PURCHASE_RETURN, DAMAGE, HOLD
    val status: InvoiceStatus = InvoiceStatus.COMPLETED, // COMPLETED, HELD, VOIDED
    val pricingTier: PricingTier = PricingTier.RETAIL,   // نمط التسعير (مفرق / جملة)
    val partyId: Long? = null,
    val partyName: String = "عميل نقدي",
    val currencyCode: String = "YER",
    val lockedExchangeRate: Double = 1550.0, // سعر الصرف المثبت تاريخياً (Rate Lock)
    val baseCurrencyCode: String = "USD",
    val subtotalAmount: Double,         // المجموع الفرعي
    val discountAmount: Double = 0.0,   // خصومات
    val additionAmount: Double = 0.0,   // إضافات (التعديلات المالية)
    val vatPercent: Double = 0.0,       // ضريبة القيمة المضافة
    val vatAmount: Double = 0.0,
    val vatLandedCost: Double = 0.0,    // ضريبة القيمة المضافة المضمنة في تكلفة الشراء (Landed Cost VAT)
    val netTotalAmount: Double,         // الإجمالي بعملة المعاملة
    val netTotalBaseEquivalent: Double, // الإجمالي بالعملة المقابلة (العملتين)
    val totalCostAmount: Double = 0.0,
    val realizedProfitAmount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH, // نقداً، دين آجل، دفع متعدد
    val paidAmount: Double = 0.0,
    val remainingCreditAmount: Double = 0.0,
    val financialAccountId: Long? = null,
    val financialAccountName: String = "",
    val splitPayments: List<SplitPaymentEntry> = emptyList(),
    val linkedOriginalInvoiceId: Long? = null,
    val linkedOriginalInvoiceNumber: String? = null,
    val isStandaloneReturn: Boolean = false,
    val shiftId: Long? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

typealias InvoiceEntity = SaleInvoiceEntity

@Entity(
    tableName = "invoice_lines",
    foreignKeys = [
        ForeignKey(
            entity = SaleInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"]), Index(value = ["productId"])]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val productName: String,
    val barcode: String,
    val selectedUnitName: String,
    val unitConversionFactor: Double = 1.0,
    val quantity: Double,
    val baseUnitQuantityDeducted: Double,
    val unitPrice: Double,
    val unitCost: Double,
    val discountAmount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val lineTotal: Double,
    val batchNumber: String? = null,
    val expiryDateTimestamp: Long? = null
)

typealias InvoiceLineEntity = SaleItemEntity

// ============================================================================
// 7. MULTI-CURRENCY EXPENSES (ExpenseEntity)
// ============================================================================
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["currencyCode"]),
        Index(value = ["category"]),
        Index(value = ["timestamp"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,               // رواتب | إيجار | فواتير | تسويق | مواصلات | صيانة | أخرى
    val currencyCode: String,           // USD, TRY, YER, SAR
    val lockedExchangeRate: Double,
    val amount: Double,
    val baseUsdEquivalent: Double,
    val financialAccountId: Long,
    val financialAccountName: String,
    val supplierPartyId: Long? = null,
    val supplierPartyName: String? = null,
    val shiftId: Long? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ============================================================================
// 8. ANALYTICAL SHIFT MANAGEMENT & Z-REPORT (ShiftEntity)
// ============================================================================
@Entity(
    tableName = "shifts",
    indices = [Index(value = ["isOpen"]), Index(value = ["openedAt"])]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cashierName: String,
    val openingSource: ShiftOpeningSource, // من الصندوق / استلام من السابق
    val primaryCashAccountId: Long,
    val primaryCashAccountName: String,
    val currencyCode: String = "YER",
    val openingCustodyBalance: Double,     // الرصيد الافتتاحي
    val lockedSecondaryAccountIds: List<Long> = emptyList(), // الحسابات المالية المقفلة ضمن الوردية
    val totalCashSales: Double = 0.0,
    val totalCashReturns: Double = 0.0,
    val totalReceiptsIn: Double = 0.0,
    val totalPaymentsOut: Double = 0.0,
    val totalExpensesOut: Double = 0.0,
    val expectedClosingBalance: Double = 0.0,
    val actualCountedClosingBalance: Double? = null,
    val varianceAmount: Double = 0.0,
    val isOpen: Boolean = true,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val zReportNotes: String = ""
)

// ============================================================================
// 9. DOUBLE-ENTRY ACCOUNTING JOURNAL (JournalEntryEntity)
// ============================================================================
@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["referenceType", "referenceId"]), Index(value = ["timestamp"])]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryNumber: String,
    val description: String,
    val debitAccountCode: String,
    val debitAccountName: String,
    val creditAccountCode: String,
    val creditAccountName: String,
    val transactionCurrency: String,
    val lockedExchangeRate: Double,
    val amountInCurrency: Double,
    val amountInBaseUsd: Double,
    val referenceType: String,
    val referenceId: Long,
    val timestamp: Long = System.currentTimeMillis()
)
