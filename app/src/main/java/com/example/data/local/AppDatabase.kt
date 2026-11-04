package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        CurrencyAndRateEntity::class,
        ProductEntity::class,
        ProductUnitEntity::class,
        CompositeComponentEntity::class,
        ProductBatchEntity::class,
        StockMovementEntity::class,
        InventoryCountSessionEntity::class,
        InventoryCountLineEntity::class,
        FinancialAccountEntity::class,
        PartyEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        SaleInvoiceEntity::class,
        SaleItemEntity::class,
        LedgerEntryEntity::class,
        ExpenseEntity::class,
        ShiftEntity::class,
        JournalEntryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(SanadTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sanadDao(): SanadDao

    companion object {
        const val DATABASE_NAME = "sanad_erp_production.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val seedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                seedScope.launch {
                    ensureRealisticArabicSeedData(instance.sanadDao())
                }
                instance
            }
        }

        /**
         * Populates realistic Arabic multi-currency Sanad Accounting seed data matching the screenshots:
         * - Products: زيت زيتون (6 تنكة = 96 لتر، 9,300 ر.ي، هامش 25%، تكلفة 6,975 ر.ي)، أرز بسمتي 5 كغ (4 كرتونة = 40 كيس)
         * - Customers ("لنا"): أحمد الخطيب، محمد العلي، مطعم الأصيل
         * - Suppliers ("علينا"): معصرة الجنوب، مؤسسة النور
         * - Financial Accounts: الصندوق النقدي الرئيسي، شام كاش USD، بنك بيمو، بنك بيبلوس، الخزينة المركزية
         */
        suspend fun ensureRealisticArabicSeedData(dao: SanadDao) {
            val existingCurrencies = dao.getCurrenciesSnapshot()
            if (existingCurrencies.isNotEmpty()) return

            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            // 1. Seed Multi-Currency Exchange Rate Table (1 $ = 1,550 ر.ي)
            val currencies = listOf(
                CurrencyAndRateEntity(
                    code = "USD",
                    nameArabic = "دولار أمريكي",
                    symbol = "$",
                    exchangeRateToBase = 1.0,
                    isBaseCurrency = true,
                    isPreferredDisplay = false,
                    lastUpdatedTimestamp = now
                ),
                CurrencyAndRateEntity(
                    code = "YER",
                    nameArabic = "ريال يمني",
                    symbol = "ر.ي",
                    exchangeRateToBase = 1550.0,
                    isBaseCurrency = false,
                    isPreferredDisplay = true,
                    lastUpdatedTimestamp = now
                ),
                CurrencyAndRateEntity(
                    code = "SAR",
                    nameArabic = "ريال سعودي",
                    symbol = "ر.س",
                    exchangeRateToBase = 3.75,
                    isBaseCurrency = false,
                    isPreferredDisplay = false,
                    lastUpdatedTimestamp = now
                ),
                CurrencyAndRateEntity(
                    code = "TRY",
                    nameArabic = "ليرة تركية",
                    symbol = "₺",
                    exchangeRateToBase = 34.50,
                    isBaseCurrency = false,
                    isPreferredDisplay = false,
                    lastUpdatedTimestamp = now
                )
            )
            dao.upsertCurrencies(currencies)

            // 2. Seed Financial Accounts (الصندوق النقدي، شام كاش USD، بنك بيمو، بنك بيبلوس، الخزينة)
            val accounts = listOf(
                FinancialAccountEntity(
                    id = 1,
                    name = "الصندوق النقدي الرئيسي",
                    accountType = AccountType.CASH_BOX,
                    currencyCode = "YER",
                    currentBalance = 4_850_000.0,
                    isDefaultForCurrency = true,
                    isVault = false,
                    sortOrder = 1,
                    notes = "الصندوق النقدي اليومي للمبيعات والمقبوضات بالريال"
                ),
                FinancialAccountEntity(
                    id = 2,
                    name = "محفظة شام كاش (Sham Cash USD)",
                    accountType = AccountType.DIGITAL_WALLET,
                    currencyCode = "USD",
                    currentBalance = 2_650.0,
                    isDefaultForCurrency = true,
                    isVault = false,
                    sortOrder = 2,
                    accountNumberOrIban = "SHAM-USD-88412",
                    notes = "محفظة شام كاش الإلكترونية للتحويلات بالدولار"
                ),
                FinancialAccountEntity(
                    id = 3,
                    name = "بنك بيمو السعودي الفرنسي",
                    accountType = AccountType.BANK,
                    currencyCode = "USD",
                    currentBalance = 8_400.0,
                    isDefaultForCurrency = false,
                    isVault = false,
                    sortOrder = 3,
                    accountNumberOrIban = "BEMO-004918273",
                    notes = "حساب التجارة والاستيراد - بنك بيمو"
                ),
                FinancialAccountEntity(
                    id = 4,
                    name = "بنك بيبلوس (Byblos Bank)",
                    accountType = AccountType.BANK,
                    currencyCode = "SAR",
                    currentBalance = 19_500.0,
                    isDefaultForCurrency = true,
                    isVault = false,
                    sortOrder = 4,
                    accountNumberOrIban = "BYB-99102834",
                    notes = "حساب الاعتمادات والحوالات التجارية"
                ),
                FinancialAccountEntity(
                    id = 5,
                    name = "صندوق الليرة التركية TRY",
                    accountType = AccountType.CASH_BOX,
                    currencyCode = "TRY",
                    currentBalance = 32_000.0,
                    isDefaultForCurrency = true,
                    isVault = false,
                    sortOrder = 5,
                    notes = "صندوق المصاريف والمشتريات بالليرة التركية"
                ),
                FinancialAccountEntity(
                    id = 6,
                    name = "الخزينة المركزية (Vault YER)",
                    accountType = AccountType.VAULT,
                    currencyCode = "YER",
                    currentBalance = 14_200_000.0,
                    isDefaultForCurrency = false,
                    isVault = true,
                    sortOrder = 6,
                    notes = "الخزينة الرئيسية لترحيل فائض الصناديق والورديات"
                )
            )
            dao.insertFinancialAccounts(accounts)

            // 3. Seed Inventory Products (زيت زيتون: 6 تنكة = 96 لتر، 9,300 ر.ي، هامش 25%، تكلفة 6,975 ر.ي | أرز بسمتي 5 كغ: 4 كرتونة = 40 كيس)
            val products = listOf(
                ProductEntity(
                    id = 1,
                    name = "زيت زيتون بكر ممتاز (معصرة الجنوب)",
                    category = "زيوت وسمن",
                    barcode = "6281000451019",
                    transactionCurrency = "YER",
                    costPrice = 6975.0,
                    weightedAverageCost = 6975.0,
                    profitMarginPercent = 25.0,
                    wholesalePrice = 8600.0,
                    retailPrice = 9300.0,
                    availableQuantity = 96.0, // 6 تنكة = 96 لتر
                    safetyThreshold = 16.0,
                    baseUnitName = "لتر",
                    secondaryUnits = listOf(
                        ProductUnitConversion(
                            unitName = "تنكة",
                            conversionFactor = 16.0,
                            barcode = "6281000451026",
                            retailPrice = 148800.0,
                            wholesalePrice = 137600.0
                        )
                    ),
                    trackBatchesAndExpiry = true,
                    nearestExpiryTimestamp = now + (180 * dayMs)
                ),
                ProductEntity(
                    id = 2,
                    name = "أرز بسمتي 5 كغ (مؤسسة النور)",
                    category = "بقوليات وحبوب",
                    barcode = "6281000882110",
                    transactionCurrency = "YER",
                    costPrice = 8500.0,
                    weightedAverageCost = 8500.0,
                    profitMarginPercent = 20.0,
                    wholesalePrice = 9800.0,
                    retailPrice = 10200.0,
                    availableQuantity = 40.0, // 4 كرتونة = 40 كيس
                    safetyThreshold = 10.0,
                    baseUnitName = "كيس",
                    secondaryUnits = listOf(
                        ProductUnitConversion(
                            unitName = "كرتونة",
                            conversionFactor = 10.0,
                            barcode = "6281000882127",
                            retailPrice = 102000.0,
                            wholesalePrice = 98000.0
                        )
                    ),
                    trackBatchesAndExpiry = false
                ),
                ProductEntity(
                    id = 3,
                    name = "حليب مجفف كامل الدسم 900 جرام",
                    category = "ألبان ومعلبات",
                    barcode = "6281000991041",
                    transactionCurrency = "YER",
                    costPrice = 5400.0,
                    weightedAverageCost = 5400.0,
                    profitMarginPercent = 20.0,
                    wholesalePrice = 6100.0,
                    retailPrice = 6500.0,
                    availableQuantity = 24.0,
                    safetyThreshold = 12.0,
                    baseUnitName = "علبة",
                    secondaryUnits = listOf(
                        ProductUnitConversion(
                            unitName = "كرتونة",
                            conversionFactor = 12.0,
                            barcode = "6281000991058",
                            retailPrice = 76000.0,
                            wholesalePrice = 72500.0
                        )
                    ),
                    trackBatchesAndExpiry = true,
                    nearestExpiryTimestamp = now - (4 * dayMs) // منتهي الصلاحية لتجربة التنبيه الأحمر والإتلاف السريع
                ),
                ProductEntity(
                    id = 4,
                    name = "بن عربي فاخر بالهيل (ميزان PLU)",
                    category = "مشروبات وقهوة",
                    barcode = "2100004005008",
                    transactionCurrency = "YER",
                    costPrice = 9500.0,
                    weightedAverageCost = 9500.0,
                    profitMarginPercent = 31.5,
                    wholesalePrice = 11500.0,
                    retailPrice = 12500.0,
                    availableQuantity = 45.0,
                    safetyThreshold = 10.0,
                    baseUnitName = "كجم",
                    soldByWeightPlu = true,
                    pluPrefix = "21"
                ),
                ProductEntity(
                    id = 5,
                    name = "سكر أبيض ناعم 10 كغ",
                    category = "بقوليات وحبوب",
                    barcode = "6281000334015",
                    transactionCurrency = "YER",
                    costPrice = 11000.0,
                    weightedAverageCost = 11000.0,
                    profitMarginPercent = 18.0,
                    wholesalePrice = 12400.0,
                    retailPrice = 13000.0,
                    availableQuantity = 60.0,
                    safetyThreshold = 15.0,
                    baseUnitName = "كيس"
                ),
                ProductEntity(
                    id = 6,
                    name = "سلة التوفير العائلية (منتج تجميعي)",
                    category = "عروض وسلال",
                    barcode = "6281000999993",
                    transactionCurrency = "YER",
                    costPrice = 33450.0,
                    weightedAverageCost = 33450.0,
                    profitMarginPercent = 22.0,
                    wholesalePrice = 39000.0,
                    retailPrice = 40800.0,
                    availableQuantity = 20.0,
                    safetyThreshold = 5.0,
                    baseUnitName = "سلة",
                    isCompositeBundle = true,
                    bundleComponents = listOf(
                        BundleComponentItem(1, "زيت زيتون بكر ممتاز (معصرة الجنوب)", 2.0, 6975.0),
                        BundleComponentItem(2, "أرز بسمتي 5 كغ (مؤسسة النور)", 1.0, 8500.0),
                        BundleComponentItem(5, "سكر أبيض ناعم 10 كغ", 1.0, 11000.0)
                    )
                )
            )
            dao.insertProducts(products)

            // Seed Relational ProductUnitEntity & CompositeComponentEntity tables
            dao.insertProductUnits(
                listOf(
                    ProductUnitEntity(
                        productId = 1,
                        unitName = "تنكة",
                        conversionFactor = 16.0,
                        barcode = "6281000451026",
                        retailPrice = 148800.0,
                        wholesalePrice = 137600.0
                    ),
                    ProductUnitEntity(
                        productId = 2,
                        unitName = "كرتونة",
                        conversionFactor = 10.0,
                        barcode = "6281000882127",
                        retailPrice = 102000.0,
                        wholesalePrice = 98000.0
                    ),
                    ProductUnitEntity(
                        productId = 3,
                        unitName = "كرتونة",
                        conversionFactor = 12.0,
                        barcode = "6281000991058",
                        retailPrice = 76000.0,
                        wholesalePrice = 72500.0
                    )
                )
            )

            dao.insertCompositeComponents(
                listOf(
                    CompositeComponentEntity(
                        parentProductId = 6,
                        componentProductId = 1,
                        componentProductName = "زيت زيتون بكر ممتاز (معصرة الجنوب)",
                        requiredQuantity = 2.0,
                        unitCost = 6975.0
                    ),
                    CompositeComponentEntity(
                        parentProductId = 6,
                        componentProductId = 2,
                        componentProductName = "أرز بسمتي 5 كغ (مؤسسة النور)",
                        requiredQuantity = 1.0,
                        unitCost = 8500.0
                    ),
                    CompositeComponentEntity(
                        parentProductId = 6,
                        componentProductId = 5,
                        componentProductName = "سكر أبيض ناعم 10 كغ",
                        requiredQuantity = 1.0,
                        unitCost = 11000.0
                    )
                )
            )

            dao.insertProductBatches(
                listOf(
                    ProductBatchEntity(
                        productId = 1,
                        batchNumber = "BATCH-OLIVE-26A",
                        expiryDateTimestamp = now + (180 * dayMs),
                        quantity = 96.0,
                        costPrice = 6975.0
                    ),
                    ProductBatchEntity(
                        productId = 3,
                        batchNumber = "BATCH-MILK-EXP01",
                        expiryDateTimestamp = now - (4 * dayMs),
                        quantity = 24.0,
                        costPrice = 5400.0
                    )
                )
            )

            // 4. Seed Parties, Customers & Suppliers matching the exact names in the prompt:
            // العملاء: أحمد الخطيب، محمد العلي، مطعم الأصيل
            // الموردون: معصرة الجنوب، مؤسسة النور
            val parties = listOf(
                PartyEntity(
                    id = 1,
                    name = "أحمد الخطيب",
                    phone = "0944112233",
                    partyType = PartyType.CUSTOMER,
                    primaryCurrency = "YER",
                    balanceInPrimaryCurrency = 465_000.0, // لنا
                    balanceInUsdEquivalent = 300.0,
                    dueDateTimestamp = now - (12 * dayMs), // متأخر
                    address = "الحي التجاري - شارع الجلاء",
                    notes = "عميل مفرق وجملة - حساب لنا"
                ),
                PartyEntity(
                    id = 2,
                    name = "محمد العلي",
                    phone = "0933556677",
                    partyType = PartyType.CUSTOMER,
                    primaryCurrency = "YER",
                    balanceInPrimaryCurrency = 232_500.0, // لنا
                    balanceInUsdEquivalent = 150.0,
                    dueDateTimestamp = now + (7 * dayMs),
                    address = "المنطقة الشرقية",
                    notes = "عميل جملة منتظم"
                ),
                PartyEntity(
                    id = 3,
                    name = "مطعم الأصيل",
                    phone = "0911889900",
                    partyType = PartyType.CUSTOMER,
                    primaryCurrency = "USD",
                    balanceInPrimaryCurrency = 420.0, // لنا بالدولار
                    balanceInUsdEquivalent = 420.0,
                    dueDateTimestamp = now - (5 * dayMs), // متأخر
                    address = "ساحة المطاعم المركزية",
                    notes = "توريد دوري لزيت الزيتون والأرز البسمتي"
                ),
                PartyEntity(
                    id = 4,
                    name = "معصرة الجنوب",
                    phone = "0955224466",
                    partyType = PartyType.SUPPLIER,
                    primaryCurrency = "YER",
                    balanceInPrimaryCurrency = -1_085_000.0, // علينا
                    balanceInUsdEquivalent = -700.0,
                    dueDateTimestamp = now - (3 * dayMs),
                    address = "طريق الجنوب العام",
                    notes = "المورد الرئيسي لزيت الزيتون البكر الممتاز"
                ),
                PartyEntity(
                    id = 5,
                    name = "مؤسسة النور",
                    phone = "0966778899",
                    partyType = PartyType.SUPPLIER,
                    primaryCurrency = "USD",
                    balanceInPrimaryCurrency = -950.0, // علينا بالدولار
                    balanceInUsdEquivalent = -950.0,
                    dueDateTimestamp = now + (15 * dayMs),
                    address = "المنطقة الحرة - مستودع 14",
                    notes = "مورد الأرز البسمتي والمواد الغذائية المستوردة"
                ),
                PartyEntity(
                    id = 6,
                    name = "حساب جاري الشريك (عمليات خاصة)",
                    phone = "0999001122",
                    partyType = PartyType.PRIVATE,
                    primaryCurrency = "YER",
                    balanceInPrimaryCurrency = 310_000.0,
                    balanceInUsdEquivalent = 200.0,
                    dueDateTimestamp = now + (30 * dayMs),
                    address = "الإدارة العامة",
                    notes = "ذمم وسندات خاصة خارج المبيعات"
                )
            )
            dao.insertParties(parties)

            dao.insertCustomers(
                listOf(
                    CustomerEntity(
                        id = 1,
                        name = "أحمد الخطيب",
                        phone = "0944112233",
                        currencyCode = "YER",
                        receivableBalance = 465_000.0,
                        receivableUsdEquivalent = 300.0,
                        isOverdue = true,
                        dueDateTimestamp = now - (12 * dayMs),
                        address = "الحي التجاري - شارع الجلاء"
                    ),
                    CustomerEntity(
                        id = 2,
                        name = "محمد العلي",
                        phone = "0933556677",
                        currencyCode = "YER",
                        receivableBalance = 232_500.0,
                        receivableUsdEquivalent = 150.0,
                        isOverdue = false,
                        dueDateTimestamp = now + (7 * dayMs),
                        address = "المنطقة الشرقية"
                    ),
                    CustomerEntity(
                        id = 3,
                        name = "مطعم الأصيل",
                        phone = "0911889900",
                        currencyCode = "USD",
                        receivableBalance = 420.0,
                        receivableUsdEquivalent = 420.0,
                        isOverdue = true,
                        dueDateTimestamp = now - (5 * dayMs),
                        address = "ساحة المطاعم المركزية"
                    )
                )
            )

            dao.insertSuppliers(
                listOf(
                    SupplierEntity(
                        id = 4,
                        name = "معصرة الجنوب",
                        phone = "0955224466",
                        currencyCode = "YER",
                        payableBalance = 1_085_000.0,
                        payableUsdEquivalent = 700.0,
                        isOverdue = true,
                        dueDateTimestamp = now - (3 * dayMs),
                        address = "طريق الجنوب العام"
                    ),
                    SupplierEntity(
                        id = 5,
                        name = "مؤسسة النور",
                        phone = "0966778899",
                        currencyCode = "USD",
                        payableBalance = 950.0,
                        payableUsdEquivalent = 950.0,
                        isOverdue = false,
                        dueDateTimestamp = now + (15 * dayMs),
                        address = "المنطقة الحرة - مستودع 14"
                    )
                )
            )

            // 5. Seed Active Shift (إدارة الورديات متعددة الجيوب)
            dao.insertShift(
                ShiftEntity(
                    id = 1,
                    cashierName = "أحمد الخطيب (الوردية الصباحية)",
                    openingSource = ShiftOpeningSource.FROM_CASHBOX,
                    primaryCashAccountId = 1,
                    primaryCashAccountName = "الصندوق النقدي الرئيسي",
                    currencyCode = "YER",
                    openingCustodyBalance = 250_000.0,
                    lockedSecondaryAccountIds = listOf(2L, 3L),
                    totalCashSales = 740_000.0,
                    totalCashReturns = 18_600.0,
                    totalReceiptsIn = 155_000.0,
                    totalPaymentsOut = 77_500.0,
                    totalExpensesOut = 45_000.0,
                    expectedClosingBalance = 1_003_900.0,
                    isOpen = true,
                    openedAt = now - (6 * 3600_000L),
                    zReportNotes = "الوردية مفتوحة - العهدة لا تغير الأرصدة الرئيسية حتى إغلاق Z-Report"
                )
            )

            // 6. Seed Last 7 Days Completed Sales Invoices + 1 Held Invoice (الفواتير المعلقة)
            val customerNames = listOf("أحمد الخطيب", "محمد العلي", "مطعم الأصيل")
            for (dayOffset in 6 downTo 0) {
                val invTime = now - (dayOffset * dayMs) - 3600_000L
                val qtyOil = (2 + (dayOffset % 4)).toDouble()
                val qtyRice = (1 + (dayOffset % 3)).toDouble()
                val line1Total = qtyOil * 9300.0
                val line2Total = qtyRice * 10200.0
                val sub = line1Total + line2Total
                val costTotal = (qtyOil * 6975.0) + (qtyRice * 8500.0)
                val profit = sub - costTotal
                val invNumber = "INV-2026-100${7 - dayOffset}"
                val custIndex = dayOffset % customerNames.size

                val invId = dao.insertInvoice(
                    SaleInvoiceEntity(
                        invoiceNumber = invNumber,
                        paperBookNumber = "BK-${500 + (7 - dayOffset)}",
                        mode = InvoiceMode.SALE,
                        status = InvoiceStatus.COMPLETED,
                        pricingTier = PricingTier.RETAIL,
                        partyId = (custIndex + 1).toLong(),
                        partyName = customerNames[custIndex],
                        currencyCode = "YER",
                        lockedExchangeRate = 1550.0,
                        subtotalAmount = sub,
                        netTotalAmount = sub,
                        netTotalBaseEquivalent = sub / 1550.0,
                        totalCostAmount = costTotal,
                        realizedProfitAmount = profit,
                        paymentMethod = PaymentMethod.CASH,
                        paidAmount = sub,
                        financialAccountId = 1L,
                        financialAccountName = "الصندوق النقدي الرئيسي",
                        shiftId = 1L,
                        notes = "فاتورة بيع معتمدة",
                        timestamp = invTime
                    )
                )

                dao.insertInvoiceLines(
                    listOf(
                        SaleItemEntity(
                            invoiceId = invId,
                            productId = 1L,
                            productName = "زيت زيتون بكر ممتاز (معصرة الجنوب)",
                            barcode = "6281000451019",
                            selectedUnitName = "لتر",
                            unitConversionFactor = 1.0,
                            quantity = qtyOil,
                            baseUnitQuantityDeducted = qtyOil,
                            unitPrice = 9300.0,
                            unitCost = 6975.0,
                            lineTotal = line1Total
                        ),
                        SaleItemEntity(
                            invoiceId = invId,
                            productId = 2L,
                            productName = "أرز بسمتي 5 كغ (مؤسسة النور)",
                            barcode = "6281000882110",
                            selectedUnitName = "كيس",
                            unitConversionFactor = 1.0,
                            quantity = qtyRice,
                            baseUnitQuantityDeducted = qtyRice,
                            unitPrice = 10200.0,
                            unitCost = 8500.0,
                            lineTotal = line2Total
                        )
                    )
                )
            }

            // Seed 1 Held Invoice (الفواتير المعلقة) for مطعم الأصيل
            val heldInvId = dao.insertInvoice(
                SaleInvoiceEntity(
                    invoiceNumber = "HOLD-2026-01",
                    paperBookNumber = "BK-519",
                    mode = InvoiceMode.SALE,
                    status = InvoiceStatus.HELD,
                    pricingTier = PricingTier.WHOLESALE,
                    partyId = 3L,
                    partyName = "مطعم الأصيل",
                    currencyCode = "YER",
                    lockedExchangeRate = 1550.0,
                    subtotalAmount = 235_600.0,
                    netTotalAmount = 235_600.0,
                    netTotalBaseEquivalent = 235_600.0 / 1550.0,
                    totalCostAmount = 196_600.0,
                    realizedProfitAmount = 39_000.0,
                    paymentMethod = PaymentMethod.CREDIT,
                    remainingCreditAmount = 235_600.0,
                    notes = "طلبية مطعم الأصيل معلقة بانتظار تأكيد الاستلام",
                    timestamp = now - 1800_000L
                )
            )
            dao.insertInvoiceLines(
                listOf(
                    SaleItemEntity(
                        invoiceId = heldInvId,
                        productId = 1L,
                        productName = "زيت زيتون بكر ممتاز (معصرة الجنوب)",
                        barcode = "6281000451026",
                        selectedUnitName = "تنكة",
                        unitConversionFactor = 16.0,
                        quantity = 1.0,
                        baseUnitQuantityDeducted = 16.0,
                        unitPrice = 137_600.0,
                        unitCost = 111_600.0,
                        lineTotal = 137_600.0
                    ),
                    SaleItemEntity(
                        invoiceId = heldInvId,
                        productId = 2L,
                        productName = "أرز بسمتي 5 كغ (مؤسسة النور)",
                        barcode = "6281000882127",
                        selectedUnitName = "كرتونة",
                        unitConversionFactor = 10.0,
                        quantity = 1.0,
                        baseUnitQuantityDeducted = 10.0,
                        unitPrice = 98_000.0,
                        unitCost = 85_000.0,
                        lineTotal = 98_000.0
                    )
                )
            )

            // 7. Seed Vouchers / Ledger Entries (سند قبض من أحمد الخطيب & سند دفعة لمعصرة الجنوب)
            dao.insertLedgerEntries(
                listOf(
                    LedgerEntryEntity(
                        voucherNumber = "VCH-2026-501",
                        partyType = PartyType.CUSTOMER,
                        partyId = 1L,
                        partyName = "أحمد الخطيب",
                        direction = VoucherDirection.RECEIPT_IN,
                        currencyCode = "YER",
                        lockedExchangeRate = 1550.0,
                        amount = 155_000.0,
                        baseUsdEquivalent = 100.0,
                        financialAccountId = 1L,
                        financialAccountName = "الصندوق النقدي الرئيسي",
                        shiftId = 1L,
                        notes = "سند قبض نقدي من الحساب الجاري",
                        timestamp = now - (2 * dayMs)
                    ),
                    LedgerEntryEntity(
                        voucherNumber = "VCH-2026-502",
                        partyType = PartyType.SUPPLIER,
                        partyId = 4L,
                        partyName = "معصرة الجنوب",
                        direction = VoucherDirection.PAYMENT_OUT,
                        currencyCode = "YER",
                        lockedExchangeRate = 1550.0,
                        amount = 310_000.0,
                        baseUsdEquivalent = 200.0,
                        financialAccountId = 1L,
                        financialAccountName = "الصندوق النقدي الرئيسي",
                        shiftId = 1L,
                        notes = "دفعة نقدية لحساب معصرة الجنوب",
                        timestamp = now - dayMs
                    )
                )
            )

            // 8. Seed Multi-Currency Expenses (المصاريف)
            dao.insertExpenses(
                listOf(
                    ExpenseEntity(
                        title = "فواتير كهرباء وإنترنت الصالة",
                        category = "فواتير",
                        currencyCode = "YER",
                        lockedExchangeRate = 1550.0,
                        amount = 45_000.0,
                        baseUsdEquivalent = 45_000.0 / 1550.0,
                        financialAccountId = 1L,
                        financialAccountName = "الصندوق النقدي الرئيسي",
                        shiftId = 1L,
                        notes = "سداد فاتورة الشهر الحالي",
                        timestamp = now - (3 * 3600_000L)
                    ),
                    ExpenseEntity(
                        title = "إيجار المستودع الرئيسي الشهري",
                        category = "إيجار",
                        currencyCode = "USD",
                        lockedExchangeRate = 1.0,
                        amount = 250.0,
                        baseUsdEquivalent = 250.0,
                        financialAccountId = 2L,
                        financialAccountName = "محفظة شام كاش (Sham Cash USD)",
                        notes = "تحويل إيجار المستودع عبر شام كاش",
                        timestamp = now - (3 * dayMs)
                    ),
                    ExpenseEntity(
                        title = "نقل وشحن بضائع مؤسسة النور",
                        category = "مواصلات",
                        currencyCode = "TRY",
                        lockedExchangeRate = 34.50,
                        amount = 1_725.0,
                        baseUsdEquivalent = 50.0,
                        financialAccountId = 5L,
                        financialAccountName = "صندوق الليرة التركية TRY",
                        supplierPartyId = 5L,
                        supplierPartyName = "مؤسسة النور",
                        notes = "أجور شحن وتفريغ",
                        timestamp = now - (4 * dayMs)
                    )
                )
            )

            // 9. Seed Initial Accounting Journal Entries for Trial Balance & Double-Entry Export
            dao.insertJournalEntries(
                listOf(
                    JournalEntryEntity(
                        entryNumber = "JE-OPEN-001",
                        description = "قيد افتتاحي - الصندوق النقدي الرئيسي والمخزون",
                        debitAccountCode = "1101",
                        debitAccountName = "الصندوق النقدي الرئيسي",
                        creditAccountCode = "3101",
                        creditAccountName = "رأس المال العامل",
                        transactionCurrency = "YER",
                        lockedExchangeRate = 1550.0,
                        amountInCurrency = 4_850_000.0,
                        amountInBaseUsd = 4_850_000.0 / 1550.0,
                        referenceType = "OPENING",
                        referenceId = 1L,
                        timestamp = now - (7 * dayMs)
                    ),
                    JournalEntryEntity(
                        entryNumber = "JE-INV-1007",
                        description = "فاتورة بيع نقدي - أحمد الخطيب",
                        debitAccountCode = "1101",
                        debitAccountName = "الصندوق النقدي الرئيسي",
                        creditAccountCode = "4101",
                        creditAccountName = "إيرادات المبيعات",
                        transactionCurrency = "YER",
                        lockedExchangeRate = 1550.0,
                        amountInCurrency = 28_800.0,
                        amountInBaseUsd = 28_800.0 / 1550.0,
                        referenceType = "INVOICE",
                        referenceId = 7L,
                        timestamp = now - 3600_000L
                    )
                )
            )
        }
    }
}
