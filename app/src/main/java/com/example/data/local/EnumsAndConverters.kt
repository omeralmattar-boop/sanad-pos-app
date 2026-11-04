package com.example.data.local

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// ============================================================================
// CORE DOMAIN ENUMS FOR SANAD ERP & ARONIUM POS
// ============================================================================

enum class InvoiceMode(val arabicLabel: String) {
    SALE("فاتورة بيع جديدة"),
    PURCHASE("فاتورة شراء جديدة"),
    SALE_RETURN("فاتورة مرتجع بيع"),
    PURCHASE_RETURN("فاتورة مرتجع شراء"),
    DAMAGE("تسجيل تلف"),
    QUOTATION("عرض سعر"),
    HOLD("فاتورة معلقة");

    companion object {
        // Aliases for seamless compatibility
        val SALES get() = SALE
        val SALES_RETURN get() = SALE_RETURN
        val DAMAGE_WRITEOFF get() = DAMAGE
    }
}

enum class InvoiceStatus(val arabicLabel: String) {
    COMPLETED("مكتملة"),
    HELD("معلقة"),
    VOIDED("ملغاة")
}

enum class PricingTier(val arabicLabel: String) {
    RETAIL("مفرق"),
    WHOLESALE("جملة")
}

enum class PaymentMethod(val arabicLabel: String) {
    CASH("نقداً"),
    CREDIT("دين / آجل"),
    SPLIT("دفع متعدد")
}

enum class TaxType(val arabicLabel: String, val defaultRatePercent: Double) {
    STANDARD("قياسية", 15.0),
    ZERO("صفرية", 0.0),
    EXEMPT("معفاة", 0.0),
    CUSTOM("مخصصة", 5.0)
}

enum class PartyType(val arabicLabel: String) {
    CUSTOMER("عميل"),
    SUPPLIER("مورد"),
    PRIVATE("خاص")
}

enum class AccountType(val arabicLabel: String) {
    CASH_BOX("صندوق نقدي"),
    DIGITAL_WALLET("محفظة رقمية"),
    BANK("حساب بنكي"),
    VAULT("الخزينة الرئيسية")
}

enum class VoucherDirection(val arabicLabel: String) {
    PAYMENT_OUT("دفعة - يخرج من الصندوق"),
    RECEIPT_IN("قبضة - يدخل إلى الصندوق")
}

enum class StockMovementType(val arabicLabel: String) {
    OPENING_STOCK("رصيد افتتاحي"),
    SALE("بيع"),
    PURCHASE("شراء"),
    SALE_RETURN("مرتجع بيع"),
    PURCHASE_RETURN("مرتجع شراء"),
    DAMAGE_WRITEOFF("إتلاف / تلف"),
    INVENTORY_COUNT_SURPLUS("فائض جرد"),
    INVENTORY_COUNT_DEFICIT("عجز جرد"),
    MANUAL_ADJUSTMENT("تسوية مخزنية"),
    BUNDLE_DEDUCTION("خصم مكون تجميعي"),
    B2B_IMPORT("استيراد مورد B2B / AI")
}

enum class ShiftOpeningSource(val arabicLabel: String) {
    FROM_CASHBOX("من الصندوق"),
    FROM_PREVIOUS_SHIFT("استلام من السابق")
}

// Embedded/JSON Helper Models for Multi-Unit, Bundles, and Split Payments
data class ProductUnitConversion(
    val unitName: String,           // e.g., "تنكة", "كرتونة"
    val conversionFactor: Double,   // e.g., 16.0 (6 تنكة = 96 لتر), 10.0 (4 كرتونة = 40 كيس)
    val barcode: String = "",
    val retailPrice: Double = 0.0,
    val wholesalePrice: Double = 0.0
)

data class BundleComponentItem(
    val componentProductId: Long,
    val componentProductName: String,
    val requiredQuantity: Double,
    val unitCost: Double
)

data class SplitPaymentEntry(
    val accountId: Long,
    val accountName: String,
    val currencyCode: String,
    val amount: Double,
    val exchangeRateToBase: Double
)

class SanadTypeConverters {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val unitListType = Types.newParameterizedType(List::class.java, ProductUnitConversion::class.java)
    private val unitListAdapter = moshi.adapter<List<ProductUnitConversion>>(unitListType)

    private val bundleListType = Types.newParameterizedType(List::class.java, BundleComponentItem::class.java)
    private val bundleListAdapter = moshi.adapter<List<BundleComponentItem>>(bundleListType)

    private val splitListType = Types.newParameterizedType(List::class.java, SplitPaymentEntry::class.java)
    private val splitListAdapter = moshi.adapter<List<SplitPaymentEntry>>(splitListType)

    private val longListType = Types.newParameterizedType(List::class.java, Long::class.javaObjectType)
    private val longListAdapter = moshi.adapter<List<Long>>(longListType)

    @TypeConverter
    fun fromInvoiceMode(value: InvoiceMode): String = value.name

    @TypeConverter
    fun toInvoiceMode(value: String): InvoiceMode = when (value) {
        "SALES" -> InvoiceMode.SALE
        "SALES_RETURN" -> InvoiceMode.SALE_RETURN
        "DAMAGE_WRITEOFF" -> InvoiceMode.DAMAGE
        else -> runCatching { InvoiceMode.valueOf(value) }.getOrDefault(InvoiceMode.SALE)
    }

    @TypeConverter
    fun fromInvoiceStatus(value: InvoiceStatus): String = value.name

    @TypeConverter
    fun toInvoiceStatus(value: String): InvoiceStatus =
        runCatching { InvoiceStatus.valueOf(value) }.getOrDefault(InvoiceStatus.COMPLETED)

    @TypeConverter
    fun fromPricingTier(value: PricingTier): String = value.name

    @TypeConverter
    fun toPricingTier(value: String): PricingTier =
        runCatching { PricingTier.valueOf(value) }.getOrDefault(PricingTier.RETAIL)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod =
        runCatching { PaymentMethod.valueOf(value) }.getOrDefault(PaymentMethod.CASH)

    @TypeConverter
    fun fromTaxType(value: TaxType): String = value.name

    @TypeConverter
    fun toTaxType(value: String): TaxType =
        runCatching { TaxType.valueOf(value) }.getOrDefault(TaxType.ZERO)

    @TypeConverter
    fun fromPartyType(value: PartyType): String = value.name

    @TypeConverter
    fun toPartyType(value: String): PartyType =
        runCatching { PartyType.valueOf(value) }.getOrDefault(PartyType.CUSTOMER)

    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType =
        runCatching { AccountType.valueOf(value) }.getOrDefault(AccountType.CASH_BOX)

    @TypeConverter
    fun fromVoucherDirection(value: VoucherDirection): String = value.name

    @TypeConverter
    fun toVoucherDirection(value: String): VoucherDirection =
        runCatching { VoucherDirection.valueOf(value) }.getOrDefault(VoucherDirection.RECEIPT_IN)

    @TypeConverter
    fun fromStockMovementType(value: StockMovementType): String = value.name

    @TypeConverter
    fun toStockMovementType(value: String): StockMovementType =
        runCatching { StockMovementType.valueOf(value) }.getOrDefault(StockMovementType.MANUAL_ADJUSTMENT)

    @TypeConverter
    fun fromShiftOpeningSource(value: ShiftOpeningSource): String = value.name

    @TypeConverter
    fun toShiftOpeningSource(value: String): ShiftOpeningSource =
        runCatching { ShiftOpeningSource.valueOf(value) }.getOrDefault(ShiftOpeningSource.FROM_CASHBOX)

    @TypeConverter
    fun fromUnitConversions(list: List<ProductUnitConversion>?): String =
        unitListAdapter.toJson(list ?: emptyList())

    @TypeConverter
    fun toUnitConversions(json: String?): List<ProductUnitConversion> =
        if (json.isNullOrBlank()) emptyList() else runCatching { unitListAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())

    @TypeConverter
    fun fromBundleComponents(list: List<BundleComponentItem>?): String =
        bundleListAdapter.toJson(list ?: emptyList())

    @TypeConverter
    fun toBundleComponents(json: String?): List<BundleComponentItem> =
        if (json.isNullOrBlank()) emptyList() else runCatching { bundleListAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())

    @TypeConverter
    fun fromSplitPayments(list: List<SplitPaymentEntry>?): String =
        splitListAdapter.toJson(list ?: emptyList())

    @TypeConverter
    fun toSplitPayments(json: String?): List<SplitPaymentEntry> =
        if (json.isNullOrBlank()) emptyList() else runCatching { splitListAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())

    @TypeConverter
    fun fromLongList(list: List<Long>?): String =
        longListAdapter.toJson(list ?: emptyList())

    @TypeConverter
    fun toLongList(json: String?): List<Long> =
        if (json.isNullOrBlank()) emptyList() else runCatching { longListAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())
}
