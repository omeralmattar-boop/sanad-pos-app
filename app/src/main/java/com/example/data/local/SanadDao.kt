package com.example.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

// ============================================================================
// RELATION MODELS FOR REACTIVE UI & REPORTS
// ============================================================================
data class InvoiceWithLines(
    @Embedded val invoice: SaleInvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val lines: List<SaleItemEntity>
)

data class ProductWithRelations(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val units: List<ProductUnitEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentProductId"
    )
    val compositeComponents: List<CompositeComponentEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val batches: List<ProductBatchEntity>
)

data class ProductWithBatches(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val batches: List<ProductBatchEntity>
)

data class InventorySessionWithLines(
    @Embedded val session: InventoryCountSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val lines: List<InventoryCountLineEntity>
)

@Dao
interface SanadDao {

    // ------------------------------------------------------------------------
    // WAL CHECKPOINT FOR SAFE BACKUP & RESTORE (.sanad / ZIP / Google Drive)
    // ------------------------------------------------------------------------
    @RawQuery
    suspend fun rawWalCheckpoint(query: SupportSQLiteQuery): Int

    suspend fun performWalCheckpointFull(): Int {
        return rawWalCheckpoint(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))
    }

    // ------------------------------------------------------------------------
    // 1. CURRENCIES & EXCHANGE RATES (CurrencyAndRateEntity)
    // ------------------------------------------------------------------------
    @Query("SELECT * FROM currencies ORDER BY isBaseCurrency DESC, code ASC")
    fun observeCurrencies(): Flow<List<CurrencyAndRateEntity>>

    @Query("SELECT * FROM currencies ORDER BY isBaseCurrency DESC, code ASC")
    suspend fun getCurrenciesSnapshot(): List<CurrencyAndRateEntity>

    @Query("SELECT * FROM currencies WHERE code = :code LIMIT 1")
    suspend fun getCurrencyByCode(code: String): CurrencyAndRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrencies(currencies: List<CurrencyAndRateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrency(currency: CurrencyAndRateEntity)

    @Query("UPDATE currencies SET exchangeRateToBase = :newRate, lastUpdatedTimestamp = :timestamp WHERE code = :code")
    suspend fun updateCurrencyRate(code: String, newRate: Double, timestamp: Long = System.currentTimeMillis())

    // ------------------------------------------------------------------------
    // 2. PRODUCTS, MULTI-UNITS, COMPOSITE COMPONENTS, BATCHES & MOVEMENTS
    // ------------------------------------------------------------------------
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY id ASC")
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Transaction
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY id ASC")
    fun observeProductsWithRelations(): Flow<List<ProductWithRelations>>

    @Transaction
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY id ASC")
    fun observeProductsWithBatches(): Flow<List<ProductWithBatches>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun findProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>): List<Long>

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductUnits(units: List<ProductUnitEntity>)

    @Query("DELETE FROM product_units WHERE productId = :productId")
    suspend fun deleteUnitsForProduct(productId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompositeComponents(components: List<CompositeComponentEntity>)

    @Query("SELECT * FROM composite_components WHERE parentProductId = :parentProductId")
    suspend fun getCompositeComponentsForProduct(parentProductId: Long): List<CompositeComponentEntity>

    @Query("DELETE FROM composite_components WHERE parentProductId = :parentProductId")
    suspend fun deleteCompositeComponentsForProduct(parentProductId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductBatches(batches: List<ProductBatchEntity>)

    @Update
    suspend fun updateProductBatch(batch: ProductBatchEntity)

    @Query("DELETE FROM product_batches WHERE id = :batchId")
    suspend fun deleteProductBatchById(batchId: Long)

    @Query("SELECT * FROM product_batches WHERE productId = :productId AND quantity > 0 ORDER BY expiryDateTimestamp ASC, id ASC")
    suspend fun getBatchesForProductSortedByExpiry(productId: Long): List<ProductBatchEntity>

    @Query("SELECT * FROM product_batches WHERE productId = :productId ORDER BY expiryDateTimestamp ASC")
    fun observeBatchesForProduct(productId: Long): Flow<List<ProductBatchEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun observeAllStockMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun observeProductMovements(productId: Long): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryCountSession(session: InventoryCountSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryCountLines(lines: List<InventoryCountLineEntity>)

    @Transaction
    @Query("SELECT * FROM inventory_count_sessions ORDER BY timestamp DESC")
    fun observeInventoryCountSessions(): Flow<List<InventorySessionWithLines>>

    /**
     * Saves a Product along with its relational ProductUnitEntity and CompositeComponentEntity rows
     */
    @Transaction
    suspend fun saveProductWithUnitsAndComponents(
        product: ProductEntity,
        units: List<ProductUnitEntity>,
        components: List<CompositeComponentEntity>
    ): Long {
        val productId = insertProduct(product)
        deleteUnitsForProduct(productId)
        if (units.isNotEmpty()) {
            insertProductUnits(units.map { it.copy(productId = productId) })
        }
        deleteCompositeComponentsForProduct(productId)
        if (components.isNotEmpty()) {
            insertCompositeComponents(components.map { it.copy(parentProductId = productId) })
        }
        return productId
    }

    // ------------------------------------------------------------------------
    // 3. FINANCIAL ACCOUNTS (TREASURY, CASH BOXES, WALLETS, BANKS)
    // ------------------------------------------------------------------------
    @Query("SELECT * FROM financial_accounts WHERE isActive = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeFinancialAccounts(): Flow<List<FinancialAccountEntity>>

    @Query("SELECT * FROM financial_accounts WHERE isActive = 1 ORDER BY sortOrder ASC, id ASC")
    suspend fun getFinancialAccountsSnapshot(): List<FinancialAccountEntity>

    @Query("SELECT * FROM financial_accounts WHERE id = :accountId LIMIT 1")
    suspend fun getFinancialAccountById(accountId: Long): FinancialAccountEntity?

    @Query("SELECT * FROM financial_accounts WHERE currencyCode = :currencyCode AND isActive = 1 ORDER BY isDefaultForCurrency DESC, sortOrder ASC")
    suspend fun getAccountsByCurrency(currencyCode: String): List<FinancialAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialAccount(account: FinancialAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialAccounts(accounts: List<FinancialAccountEntity>): List<Long>

    @Update
    suspend fun updateFinancialAccount(account: FinancialAccountEntity)

    @Query("UPDATE financial_accounts SET currentBalance = currentBalance + :delta WHERE id = :accountId")
    suspend fun adjustFinancialAccountBalance(accountId: Long, delta: Double)

    @Query("UPDATE financial_accounts SET sortOrder = :sortOrder WHERE id = :accountId")
    suspend fun updateAccountSortOrder(accountId: Long, sortOrder: Int)

    // ------------------------------------------------------------------------
    // 4. PARTIES, CUSTOMERS, SUPPLIERS & LEDGER ENTRIES (الذمم والسندات)
    // ------------------------------------------------------------------------
    @Query("SELECT * FROM parties ORDER BY lastTransactionTimestamp DESC, id ASC")
    fun observeParties(): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE partyType = :type ORDER BY abs(balanceInPrimaryCurrency) DESC")
    fun observePartiesByType(type: PartyType): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :partyId LIMIT 1")
    suspend fun getPartyById(partyId: Long): PartyEntity?

    @Query("SELECT * FROM parties WHERE name = :name LIMIT 1")
    suspend fun getPartyByName(name: String): PartyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: PartyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParties(parties: List<PartyEntity>): List<Long>

    @Update
    suspend fun updateParty(party: PartyEntity)

    @Query("SELECT * FROM customers ORDER BY receivableBalance DESC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Query("SELECT * FROM suppliers ORDER BY payableBalance DESC")
    fun observeSuppliers(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Query(
        """
        UPDATE parties 
        SET balanceInPrimaryCurrency = balanceInPrimaryCurrency + :deltaPrimary,
            balanceInUsdEquivalent = balanceInUsdEquivalent + :deltaUsd,
            lastTransactionTimestamp = :timestamp
        WHERE id = :partyId
        """
    )
    suspend fun adjustPartyBalance(
        partyId: Long,
        deltaPrimary: Double,
        deltaUsd: Double,
        timestamp: Long = System.currentTimeMillis()
    )

    // ------------------------------------------------------------------------
    // 5. UNIFIED INVOICES & HELD INVOICES (SaleInvoiceEntity & SaleItemEntity)
    // ------------------------------------------------------------------------
    @Transaction
    @Query("SELECT * FROM invoices WHERE status = 'COMPLETED' AND mode != 'HOLD' ORDER BY timestamp DESC")
    fun observeCompletedInvoicesWithLines(): Flow<List<InvoiceWithLines>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE status = 'HELD' OR mode = 'HOLD' ORDER BY timestamp DESC")
    fun observeHeldInvoicesWithLines(): Flow<List<InvoiceWithLines>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE status = 'COMPLETED' AND mode != 'HOLD' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastCompletedInvoiceWithLines(): InvoiceWithLines?

    @Transaction
    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumberWithLines(invoiceNumber: String): InvoiceWithLines?

    @Transaction
    @Query("SELECT * FROM invoices WHERE partyId = :partyId AND status = 'COMPLETED' ORDER BY timestamp DESC")
    fun observePartyInvoices(partyId: Long): Flow<List<InvoiceWithLines>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: SaleInvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceLines(lines: List<SaleItemEntity>)

    @Query("DELETE FROM invoices WHERE id = :invoiceId")
    suspend fun deleteInvoiceById(invoiceId: Long)

    // ------------------------------------------------------------------------
    // 6. LEDGER ENTRIES (VOUCHERS) & MULTI-CURRENCY EXPENSES
    // ------------------------------------------------------------------------
    @Query("SELECT * FROM ledger_entries ORDER BY timestamp DESC")
    fun observeVouchers(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries ORDER BY timestamp DESC")
    fun observeLedgerEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE partyId = :partyId ORDER BY timestamp DESC")
    fun observePartyVouchers(partyId: Long): Flow<List<LedgerEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: LedgerEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<LedgerEntryEntity>)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    // ------------------------------------------------------------------------
    // 7. ANALYTICAL SHIFTS & DOUBLE-ENTRY JOURNAL
    // ------------------------------------------------------------------------
    @Query("SELECT * FROM shifts WHERE isOpen = 1 ORDER BY openedAt DESC LIMIT 1")
    fun observeActiveShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE isOpen = 1 ORDER BY openedAt DESC LIMIT 1")
    suspend fun getActiveShiftSnapshot(): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE id = :shiftId LIMIT 1")
    suspend fun getShiftById(shiftId: Long): ShiftEntity?

    @Query(
        """
        SELECT * FROM invoices 
        WHERE mode = 'PURCHASE' 
          AND status = 'COMPLETED' 
          AND partyName = :partyName 
          AND (paperBookNumber = :invoiceRef OR invoiceNumber = :invoiceRef)
        LIMIT 1
        """
    )
    suspend fun findDuplicatePurchaseInvoice(partyName: String, invoiceRef: String): SaleInvoiceEntity?

    @Query("SELECT * FROM shifts ORDER BY openedAt DESC")
    fun observeAllShifts(): Flow<List<ShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC, id DESC")
    fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntries(entries: List<JournalEntryEntity>)

    // ========================================================================
    // ATOMIC FINANCIAL & INVENTORY TRANSACTIONS (@Transaction)
    // ========================================================================

    /**
     * Executes an atomic invoice commit across all Unified Invoice Modes:
     * 1. SALE (فاتورة بيع - with FEFO batch deduction)
     * 2. PURCHASE (فاتورة شراء - with Landed Cost WAC including Tax, Shipping & Supplier Discount)
     * 3. SALE_RETURN (مرتجع بيع)
     * 4. PURCHASE_RETURN (مرتجع شراء)
     * 5. DAMAGE (تسجيل تلف)
     * 6. QUOTATION (عرض سعر - does not affect stock or treasury)
     * 7. HOLD (الفواتير المعلقة)
     */
    @Transaction
    suspend fun commitUnifiedInvoiceTransaction(
        invoice: SaleInvoiceEntity,
        lines: List<SaleItemEntity>
    ): Long {
        val now = invoice.timestamp
        val activeShift = getActiveShiftSnapshot()
        val isHeld = invoice.status == InvoiceStatus.HELD || invoice.mode == InvoiceMode.HOLD
        val isQuotation = invoice.mode == InvoiceMode.QUOTATION

        val finalInvoice = if (activeShift != null && invoice.shiftId == null) {
            invoice.copy(
                shiftId = activeShift.id,
                status = if (isHeld) InvoiceStatus.HELD else InvoiceStatus.COMPLETED
            )
        } else {
            invoice.copy(status = if (isHeld) InvoiceStatus.HELD else InvoiceStatus.COMPLETED)
        }

        val invoiceId = insertInvoice(finalInvoice)
        val boundLines = lines.map { it.copy(invoiceId = invoiceId) }
        insertInvoiceLines(boundLines)

        // If Held Invoice (فاتورة معلقة) or Quotation (عرض سعر), do not mutate stock, treasury, or party balances
        if (isHeld || isQuotation) {
            return invoiceId
        }

        // Net invoice-level landed cost adjustments for Purchase WAC (Shipping + VAT - Supplier Discount)
        val netPurchaseAdjustments = finalInvoice.additionAmount + finalInvoice.vatAmount - finalInvoice.discountAmount
        val totalLinesSubtotal = boundLines.sumOf { it.lineTotal }.takeIf { it > 0.0 } ?: 1.0

        // 1. Update Inventory Stock & Weighted Average Cost + Deduct Composite Bundle Components + FEFO
        for (line in boundLines) {
            val product = getProductById(line.productId) ?: continue
            val baseQty = line.baseUnitQuantityDeducted

            when (finalInvoice.mode) {
                InvoiceMode.SALE -> {
                    val relationalComponents = getCompositeComponentsForProduct(product.id)
                    val bundleList = if (relationalComponents.isNotEmpty()) {
                        relationalComponents.map {
                            BundleComponentItem(
                                componentProductId = it.componentProductId,
                                componentProductName = it.componentProductName,
                                requiredQuantity = it.requiredQuantity,
                                unitCost = it.unitCost
                            )
                        }
                    } else {
                        product.bundleComponents
                    }

                    if (product.isCompositeBundle && bundleList.isNotEmpty()) {
                        // Automatically deduct component quantities upon sale of a composite bundle
                        for (comp in bundleList) {
                            val compProd = getProductById(comp.componentProductId) ?: continue
                            val compDeduction = comp.requiredQuantity * line.quantity
                            val newCompBalance = (compProd.availableQuantity - compDeduction).coerceAtLeast(0.0)
                            updateProduct(compProd.copy(availableQuantity = newCompBalance))
                            insertStockMovement(
                                StockMovementEntity(
                                    productId = compProd.id,
                                    productName = compProd.name,
                                    movementType = StockMovementType.BUNDLE_DEDUCTION,
                                    quantityDelta = -compDeduction,
                                    balanceAfter = newCompBalance,
                                    unitName = compProd.baseUnitName,
                                    unitCostAtMovement = compProd.weightedAverageCost,
                                    unitPriceAtMovement = 0.0,
                                    currencyCode = finalInvoice.currencyCode,
                                    exchangeRateLocked = finalInvoice.lockedExchangeRate,
                                    referenceInvoiceId = invoiceId,
                                    reasonOrNotes = "خصم مكون تجميعي لصنف: ${product.name}",
                                    timestamp = now
                                )
                            )
                        }
                    } else {
                        // FEFO (First Expired, First Out - الأقرب انتهاءً يُصرف أولاً)
                        var fefoNote = ""
                        var updatedNearestExpiry = product.nearestExpiryTimestamp
                        if (product.trackBatchesAndExpiry) {
                            val activeBatches = getBatchesForProductSortedByExpiry(product.id)
                            var remainingToDeduct = baseQty
                            val consumedBatches = mutableListOf<String>()
                            for (batch in activeBatches) {
                                if (remainingToDeduct <= 0.0) break
                                val deductFromThis = minOf(batch.quantity, remainingToDeduct)
                                val leftInBatch = batch.quantity - deductFromThis
                                remainingToDeduct -= deductFromThis
                                consumedBatches.add("${batch.batchNumber} (${deductFromThis.toInt()})")
                                if (leftInBatch <= 0.0001) {
                                    deleteProductBatchById(batch.id)
                                } else {
                                    updateProductBatch(batch.copy(quantity = leftInBatch))
                                }
                            }
                            val remainingBatches = getBatchesForProductSortedByExpiry(product.id)
                            updatedNearestExpiry = remainingBatches.firstOrNull()?.expiryDateTimestamp
                            if (consumedBatches.isNotEmpty()) {
                                fefoNote = " [صرف FEFO: ${consumedBatches.joinToString("، ")}]"
                            }
                        }

                        val newBalance = product.availableQuantity - baseQty
                        updateProduct(
                            product.copy(
                                availableQuantity = newBalance,
                                nearestExpiryTimestamp = updatedNearestExpiry
                            )
                        )
                        insertStockMovement(
                            StockMovementEntity(
                                productId = product.id,
                                productName = product.name,
                                movementType = StockMovementType.SALE,
                                quantityDelta = -baseQty,
                                balanceAfter = newBalance,
                                unitName = line.selectedUnitName,
                                unitCostAtMovement = line.unitCost,
                                unitPriceAtMovement = line.unitPrice,
                                currencyCode = finalInvoice.currencyCode,
                                exchangeRateLocked = finalInvoice.lockedExchangeRate,
                                referenceInvoiceId = invoiceId,
                                reasonOrNotes = "فاتورة بيع رقم ${finalInvoice.invoiceNumber}$fefoNote",
                                timestamp = now
                            )
                        )
                    }
                }

                InvoiceMode.PURCHASE -> {
                    val oldQty = product.availableQuantity.coerceAtLeast(0.0)
                    val newQty = oldQty + baseQty
                    // Include proportional share of VAT, Shipping/Additions, and Supplier Discount in Landed Cost WAC
                    val lineWeight = if (boundLines.size == 1) 1.0 else (line.lineTotal / totalLinesSubtotal)
                    val allocatedAdjustment = netPurchaseAdjustments * lineWeight
                    val effectiveLandedLineTotal = (line.lineTotal + allocatedAdjustment).coerceAtLeast(0.0)

                    val incomingUnitCostPerBase = if (baseQty > 0.0) {
                        effectiveLandedLineTotal / baseQty
                    } else if (line.unitConversionFactor > 0.0) {
                        line.unitPrice / line.unitConversionFactor
                    } else {
                        line.unitPrice
                    }

                    val newWeightedCost = if (newQty > 0) {
                        ((oldQty * product.weightedAverageCost) + (baseQty * incomingUnitCostPerBase)) / newQty
                    } else {
                        incomingUnitCostPerBase
                    }
                    val margin = product.profitMarginPercent
                    val updatedRetail = if (margin > 0) {
                        newWeightedCost * (1.0 + margin / 100.0)
                    } else {
                        product.retailPrice
                    }
                    val enableTrackBatches = product.trackBatchesAndExpiry ||
                        (!line.batchNumber.isNullOrBlank() && line.expiryDateTimestamp != null)

                    if (!line.batchNumber.isNullOrBlank() && line.expiryDateTimestamp != null) {
                        insertProductBatches(
                            listOf(
                                ProductBatchEntity(
                                    productId = product.id,
                                    batchNumber = line.batchNumber,
                                    expiryDateTimestamp = line.expiryDateTimestamp,
                                    quantity = baseQty,
                                    costPrice = incomingUnitCostPerBase,
                                    currencyCode = finalInvoice.currencyCode,
                                    receivedTimestamp = now
                                )
                            )
                        )
                    }
                    val earliestBatchExpiry = getBatchesForProductSortedByExpiry(product.id)
                        .firstOrNull()?.expiryDateTimestamp
                        ?: line.expiryDateTimestamp
                        ?: product.nearestExpiryTimestamp

                    updateProduct(
                        product.copy(
                            availableQuantity = product.availableQuantity + baseQty,
                            costPrice = incomingUnitCostPerBase,
                            weightedAverageCost = newWeightedCost,
                            retailPrice = updatedRetail,
                            trackBatchesAndExpiry = enableTrackBatches,
                            nearestExpiryTimestamp = earliestBatchExpiry
                        )
                    )
                    insertStockMovement(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            movementType = StockMovementType.PURCHASE,
                            quantityDelta = baseQty,
                            balanceAfter = product.availableQuantity + baseQty,
                            unitName = line.selectedUnitName,
                            unitCostAtMovement = incomingUnitCostPerBase,
                            unitPriceAtMovement = line.unitPrice,
                            currencyCode = finalInvoice.currencyCode,
                            exchangeRateLocked = finalInvoice.lockedExchangeRate,
                            referenceInvoiceId = invoiceId,
                            reasonOrNotes = "فاتورة شراء رقم ${finalInvoice.invoiceNumber} (تكلفة صافية شاملة الشحن/الضريبة/الخصم)",
                            timestamp = now
                        )
                    )
                }

                InvoiceMode.SALE_RETURN -> {
                    val newBalance = product.availableQuantity + baseQty
                    updateProduct(product.copy(availableQuantity = newBalance))
                    insertStockMovement(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            movementType = StockMovementType.SALE_RETURN,
                            quantityDelta = baseQty,
                            balanceAfter = newBalance,
                            unitName = line.selectedUnitName,
                            unitCostAtMovement = line.unitCost,
                            unitPriceAtMovement = line.unitPrice,
                            currencyCode = finalInvoice.currencyCode,
                            exchangeRateLocked = finalInvoice.lockedExchangeRate,
                            referenceInvoiceId = invoiceId,
                            reasonOrNotes = "مرتجع بيع رقم ${finalInvoice.invoiceNumber}",
                            timestamp = now
                        )
                    )
                }

                InvoiceMode.PURCHASE_RETURN -> {
                    val newBalance = product.availableQuantity - baseQty
                    updateProduct(product.copy(availableQuantity = newBalance))
                    insertStockMovement(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            movementType = StockMovementType.PURCHASE_RETURN,
                            quantityDelta = -baseQty,
                            balanceAfter = newBalance,
                            unitName = line.selectedUnitName,
                            unitCostAtMovement = line.unitCost,
                            unitPriceAtMovement = line.unitPrice,
                            currencyCode = finalInvoice.currencyCode,
                            exchangeRateLocked = finalInvoice.lockedExchangeRate,
                            referenceInvoiceId = invoiceId,
                            reasonOrNotes = "مرتجع شراء رقم ${finalInvoice.invoiceNumber}",
                            timestamp = now
                        )
                    )
                }

                InvoiceMode.DAMAGE, InvoiceMode.HOLD, InvoiceMode.QUOTATION -> {
                    val newBalance = product.availableQuantity - baseQty
                    updateProduct(product.copy(availableQuantity = newBalance))
                    insertStockMovement(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            movementType = StockMovementType.DAMAGE_WRITEOFF,
                            quantityDelta = -baseQty,
                            balanceAfter = newBalance,
                            unitName = line.selectedUnitName,
                            unitCostAtMovement = line.unitCost,
                            unitPriceAtMovement = 0.0,
                            currencyCode = finalInvoice.currencyCode,
                            exchangeRateLocked = finalInvoice.lockedExchangeRate,
                            referenceInvoiceId = invoiceId,
                            reasonOrNotes = finalInvoice.notes.ifBlank { "تسجيل تلف مخزني رقم ${finalInvoice.invoiceNumber}" },
                            timestamp = now
                        )
                    )
                }
            }
        }

        // 2. Update Financial Accounts (Cash Boxes / Split Payments) & Active Shift Sub-ledger
        if (finalInvoice.mode != InvoiceMode.DAMAGE && finalInvoice.mode != InvoiceMode.QUOTATION) {
            val isCashInflow = finalInvoice.mode == InvoiceMode.SALE || finalInvoice.mode == InvoiceMode.PURCHASE_RETURN
            val sign = if (isCashInflow) 1.0 else -1.0

            when (finalInvoice.paymentMethod) {
                PaymentMethod.CASH -> {
                    val accId = finalInvoice.financialAccountId
                    if (accId != null && finalInvoice.paidAmount > 0) {
                        adjustFinancialAccountBalance(accId, sign * finalInvoice.paidAmount)
                    }
                }
                PaymentMethod.SPLIT -> {
                    for (split in finalInvoice.splitPayments) {
                        if (split.amount > 0) {
                            adjustFinancialAccountBalance(split.accountId, sign * split.amount)
                        }
                    }
                    if (finalInvoice.remainingCreditAmount > 0 && finalInvoice.partyId != null) {
                        val partySign = if (finalInvoice.mode == InvoiceMode.SALE || finalInvoice.mode == InvoiceMode.PURCHASE_RETURN) 1.0 else -1.0
                        val usdDelta = if (finalInvoice.lockedExchangeRate > 0) {
                            (finalInvoice.remainingCreditAmount / finalInvoice.lockedExchangeRate) * partySign
                        } else 0.0
                        adjustPartyBalance(
                            partyId = finalInvoice.partyId,
                            deltaPrimary = finalInvoice.remainingCreditAmount * partySign,
                            deltaUsd = usdDelta,
                            timestamp = now
                        )
                    }
                }
                PaymentMethod.CREDIT -> {
                    if (finalInvoice.paidAmount > 0 && finalInvoice.financialAccountId != null) {
                        adjustFinancialAccountBalance(finalInvoice.financialAccountId, sign * finalInvoice.paidAmount)
                    }
                    if (finalInvoice.remainingCreditAmount > 0 && finalInvoice.partyId != null) {
                        val partySign = if (finalInvoice.mode == InvoiceMode.SALE || finalInvoice.mode == InvoiceMode.PURCHASE_RETURN) 1.0 else -1.0
                        val usdDelta = if (finalInvoice.lockedExchangeRate > 0) {
                            (finalInvoice.remainingCreditAmount / finalInvoice.lockedExchangeRate) * partySign
                        } else 0.0
                        adjustPartyBalance(
                            partyId = finalInvoice.partyId,
                            deltaPrimary = finalInvoice.remainingCreditAmount * partySign,
                            deltaUsd = usdDelta,
                            timestamp = now
                        )
                    }
                }
            }

            if (activeShift != null && finalInvoice.paidAmount > 0) {
                val updatedShift = when (finalInvoice.mode) {
                    InvoiceMode.SALE -> activeShift.copy(
                        totalCashSales = activeShift.totalCashSales + finalInvoice.paidAmount,
                        expectedClosingBalance = activeShift.expectedClosingBalance + finalInvoice.paidAmount
                    )
                    InvoiceMode.SALE_RETURN -> activeShift.copy(
                        totalCashReturns = activeShift.totalCashReturns + finalInvoice.paidAmount,
                        expectedClosingBalance = activeShift.expectedClosingBalance - finalInvoice.paidAmount
                    )
                    else -> activeShift
                }
                updateShift(updatedShift)
            }
        }

        // 3. Record Double-Entry Accounting Journal Entry
        val (debitCode, debitName, creditCode, creditName) = when (finalInvoice.mode) {
            InvoiceMode.SALE -> listOf(
                "1101", finalInvoice.financialAccountName.ifBlank { "ذمم العملاء / النقدية" },
                "4101", "إيرادات المبيعات"
            )
            InvoiceMode.PURCHASE -> listOf(
                "1301", "المخزون السلعي",
                "2101", finalInvoice.financialAccountName.ifBlank { "ذمم الموردين / النقدية" }
            )
            InvoiceMode.SALE_RETURN -> listOf(
                "4102", "مردودات المبيعات",
                "1101", finalInvoice.financialAccountName.ifBlank { "ذمم العملاء / النقدية" }
            )
            InvoiceMode.PURCHASE_RETURN -> listOf(
                "2101", finalInvoice.financialAccountName.ifBlank { "ذمم الموردين / النقدية" },
                "1301", "المخزون السلعي"
            )
            InvoiceMode.DAMAGE, InvoiceMode.HOLD, InvoiceMode.QUOTATION -> listOf(
                "5205", "خسائر تلف البضاعة",
                "1301", "المخزون السلعي"
            )
        }

        insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-${finalInvoice.invoiceNumber}",
                description = "${finalInvoice.mode.arabicLabel} - ${finalInvoice.invoiceNumber} (${finalInvoice.partyName})",
                debitAccountCode = debitCode,
                debitAccountName = debitName,
                creditAccountCode = creditCode,
                creditAccountName = creditName,
                transactionCurrency = finalInvoice.currencyCode,
                lockedExchangeRate = finalInvoice.lockedExchangeRate,
                amountInCurrency = finalInvoice.netTotalAmount,
                amountInBaseUsd = finalInvoice.netTotalBaseEquivalent,
                referenceType = "INVOICE",
                referenceId = invoiceId,
                timestamp = now
            )
        )

        return invoiceId
    }

    /**
     * Fast 1-Click Write-off (إتلاف سريع) directly from Rich Stock Card
     */
    @Transaction
    suspend fun performFastDamageWriteOff(
        productId: Long,
        quantityToDestroy: Double,
        reason: String
    ): Long? {
        val product = getProductById(productId) ?: return null
        val currency = getCurrencyByCode(product.transactionCurrency)
        val rate = currency?.exchangeRateToBase ?: 1550.0
        val totalLossCost = quantityToDestroy * product.weightedAverageCost
        val totalLossUsd = if (rate > 0) totalLossCost / rate else 0.0
        val now = System.currentTimeMillis()
        val invNumber = "DMG-${now % 100000}"

        val damageInvoice = SaleInvoiceEntity(
            invoiceNumber = invNumber,
            mode = InvoiceMode.DAMAGE,
            status = InvoiceStatus.COMPLETED,
            partyName = "إتلاف مخزني سريع",
            currencyCode = product.transactionCurrency,
            lockedExchangeRate = rate,
            subtotalAmount = totalLossCost,
            netTotalAmount = totalLossCost,
            netTotalBaseEquivalent = totalLossUsd,
            totalCostAmount = totalLossCost,
            realizedProfitAmount = -totalLossCost,
            notes = reason,
            timestamp = now
        )

        val damageLine = SaleItemEntity(
            invoiceId = 0,
            productId = product.id,
            productName = product.name,
            barcode = product.barcode,
            selectedUnitName = product.baseUnitName,
            unitConversionFactor = 1.0,
            quantity = quantityToDestroy,
            baseUnitQuantityDeducted = quantityToDestroy,
            unitPrice = product.weightedAverageCost,
            unitCost = product.weightedAverageCost,
            lineTotal = totalLossCost
        )

        return commitUnifiedInvoiceTransaction(damageInvoice, listOf(damageLine))
    }

    /**
     * Atomic Voucher Registration (تسجيل دفعة / قبضة) with Rate Lock
     */
    @Transaction
    suspend fun commitVoucherTransaction(voucher: LedgerEntryEntity): Long {
        val activeShift = getActiveShiftSnapshot()
        val finalVoucher = if (activeShift != null && voucher.shiftId == null) {
            voucher.copy(shiftId = activeShift.id)
        } else {
            voucher
        }
        val voucherId = insertVoucher(finalVoucher)

        val cashDelta = if (finalVoucher.direction == VoucherDirection.RECEIPT_IN) {
            finalVoucher.amount
        } else {
            -finalVoucher.amount
        }
        adjustFinancialAccountBalance(finalVoucher.financialAccountId, cashDelta)

        val partyDelta = if (finalVoucher.direction == VoucherDirection.RECEIPT_IN) {
            -finalVoucher.amount
        } else {
            finalVoucher.amount
        }
        val partyUsdDelta = if (finalVoucher.direction == VoucherDirection.RECEIPT_IN) {
            -finalVoucher.baseUsdEquivalent
        } else {
            finalVoucher.baseUsdEquivalent
        }
        adjustPartyBalance(
            partyId = finalVoucher.partyId,
            deltaPrimary = partyDelta,
            deltaUsd = partyUsdDelta,
            timestamp = finalVoucher.timestamp
        )

        if (activeShift != null) {
            val updatedShift = if (finalVoucher.direction == VoucherDirection.RECEIPT_IN) {
                activeShift.copy(
                    totalReceiptsIn = activeShift.totalReceiptsIn + finalVoucher.amount,
                    expectedClosingBalance = activeShift.expectedClosingBalance + finalVoucher.amount
                )
            } else {
                activeShift.copy(
                    totalPaymentsOut = activeShift.totalPaymentsOut + finalVoucher.amount,
                    expectedClosingBalance = activeShift.expectedClosingBalance - finalVoucher.amount
                )
            }
            updateShift(updatedShift)
        }

        val (debitCode, debitName, creditCode, creditName) = if (finalVoucher.direction == VoucherDirection.RECEIPT_IN) {
            listOf("1101", finalVoucher.financialAccountName, "1201", "حساب ${finalVoucher.partyName}")
        } else {
            listOf("2101", "حساب ${finalVoucher.partyName}", "1101", finalVoucher.financialAccountName)
        }

        insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-${finalVoucher.voucherNumber}",
                description = "${finalVoucher.direction.arabicLabel} - ${finalVoucher.partyName} (${finalVoucher.notes})",
                debitAccountCode = debitCode,
                debitAccountName = debitName,
                creditAccountCode = creditCode,
                creditAccountName = creditName,
                transactionCurrency = finalVoucher.currencyCode,
                lockedExchangeRate = finalVoucher.lockedExchangeRate,
                amountInCurrency = finalVoucher.amount,
                amountInBaseUsd = finalVoucher.baseUsdEquivalent,
                referenceType = "VOUCHER",
                referenceId = voucherId,
                timestamp = finalVoucher.timestamp
            )
        )

        return voucherId
    }

    /**
     * Atomic Multi-Currency Expense Registration (مصروف جديد)
     */
    @Transaction
    suspend fun commitExpenseTransaction(expense: ExpenseEntity): Long {
        val activeShift = getActiveShiftSnapshot()
        val finalExpense = if (activeShift != null && expense.shiftId == null) {
            expense.copy(shiftId = activeShift.id)
        } else {
            expense
        }
        val expenseId = insertExpense(finalExpense)

        adjustFinancialAccountBalance(finalExpense.financialAccountId, -finalExpense.amount)

        if (activeShift != null) {
            updateShift(
                activeShift.copy(
                    totalExpensesOut = activeShift.totalExpensesOut + finalExpense.amount,
                    expectedClosingBalance = activeShift.expectedClosingBalance - finalExpense.amount
                )
            )
        }

        insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-EXP-$expenseId",
                description = "مصروف (${finalExpense.category}): ${finalExpense.title}",
                debitAccountCode = "5101",
                debitAccountName = "مصروفات - ${finalExpense.category}",
                creditAccountCode = "1101",
                creditAccountName = finalExpense.financialAccountName,
                transactionCurrency = finalExpense.currencyCode,
                lockedExchangeRate = finalExpense.lockedExchangeRate,
                amountInCurrency = finalExpense.amount,
                amountInBaseUsd = finalExpense.baseUsdEquivalent,
                referenceType = "EXPENSE",
                referenceId = expenseId,
                timestamp = finalExpense.timestamp
            )
        )

        return expenseId
    }

    /**
     * Treasury Micro-Action 1: Vault Transfer (ترحيل للخزينة)
     */
    @Transaction
    suspend fun commitVaultTransferTransaction(
        sourceAccountId: Long,
        targetVaultAccountId: Long,
        amount: Double,
        notes: String
    ) {
        val sourceAcc = getFinancialAccountById(sourceAccountId) ?: return
        val targetAcc = getFinancialAccountById(targetVaultAccountId) ?: return
        val currency = getCurrencyByCode(sourceAcc.currencyCode)
        val rate = currency?.exchangeRateToBase ?: 1.0
        val now = System.currentTimeMillis()

        adjustFinancialAccountBalance(sourceAccountId, -amount)
        adjustFinancialAccountBalance(targetVaultAccountId, amount)

        insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-TRF-${now % 100000}",
                description = "ترحيل للخزينة من ${sourceAcc.name} إلى ${targetAcc.name} ($notes)",
                debitAccountCode = "1105",
                debitAccountName = targetAcc.name,
                creditAccountCode = "1101",
                creditAccountName = sourceAcc.name,
                transactionCurrency = sourceAcc.currencyCode,
                lockedExchangeRate = rate,
                amountInCurrency = amount,
                amountInBaseUsd = if (rate > 0) amount / rate else amount,
                referenceType = "VAULT_TRANSFER",
                referenceId = sourceAccountId,
                timestamp = now
            )
        )
    }

    /**
     * Treasury Micro-Action 2: Balance Reconciliation (تسوية الحساب)
     */
    @Transaction
    suspend fun commitAccountReconciliationTransaction(
        accountId: Long,
        actualBalance: Double,
        reason: String
    ) {
        val account = getFinancialAccountById(accountId) ?: return
        val diff = actualBalance - account.currentBalance
        if (abs(diff) < 0.001) return

        val currency = getCurrencyByCode(account.currencyCode)
        val rate = currency?.exchangeRateToBase ?: 1.0
        val now = System.currentTimeMillis()

        updateFinancialAccount(
            account.copy(
                currentBalance = actualBalance,
                lastReconciledTimestamp = now
            )
        )

        val isSurplus = diff > 0
        insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-REC-${now % 100000}",
                description = "تسوية رصيد ${account.name}: $reason",
                debitAccountCode = if (isSurplus) "1101" else "5209",
                debitAccountName = if (isSurplus) account.name else "فروقات وعجز الصناديق",
                creditAccountCode = if (isSurplus) "4209" else "1101",
                creditAccountName = if (isSurplus) "فائض تسوية الصناديق" else account.name,
                transactionCurrency = account.currencyCode,
                lockedExchangeRate = rate,
                amountInCurrency = abs(diff),
                amountInBaseUsd = if (rate > 0) abs(diff) / rate else abs(diff),
                referenceType = "RECONCILIATION",
                referenceId = accountId,
                timestamp = now
            )
        )
    }

    /**
     * Physical Inventory Count Reconciliation (اعتماد جلسة الجرد الفعلي وتسوية الفائض والعجز)
     */
    @Transaction
    suspend fun commitPhysicalInventoryCountSession(
        sessionTitle: String,
        categoryFilter: String,
        countedItems: Map<Long, Double>,
        notes: String
    ): Long {
        val now = System.currentTimeMillis()
        var totalSurplusYer = 0.0
        var totalDeficitYer = 0.0
        val linesToInsert = mutableListOf<InventoryCountLineEntity>()

        for ((productId, countedQty) in countedItems) {
            val product = getProductById(productId) ?: continue
            val expectedQty = product.availableQuantity
            val varianceQty = countedQty - expectedQty
            val varianceVal = varianceQty * product.weightedAverageCost

            if (varianceVal > 0) {
                totalSurplusYer += varianceVal
            } else if (varianceVal < 0) {
                totalDeficitYer += abs(varianceVal)
            }

            linesToInsert.add(
                InventoryCountLineEntity(
                    sessionId = 0,
                    productId = product.id,
                    productName = product.name,
                    unitName = product.baseUnitName,
                    expectedQty = expectedQty,
                    countedQty = countedQty,
                    varianceQty = varianceQty,
                    unitCostYer = product.weightedAverageCost,
                    varianceValueYer = varianceVal
                )
            )

            if (abs(varianceQty) > 0.0001) {
                updateProduct(product.copy(availableQuantity = countedQty))
                insertStockMovement(
                    StockMovementEntity(
                        productId = product.id,
                        productName = product.name,
                        movementType = if (varianceQty > 0) StockMovementType.INVENTORY_COUNT_SURPLUS else StockMovementType.INVENTORY_COUNT_DEFICIT,
                        quantityDelta = varianceQty,
                        balanceAfter = countedQty,
                        unitName = product.baseUnitName,
                        unitCostAtMovement = product.weightedAverageCost,
                        unitPriceAtMovement = product.retailPrice,
                        currencyCode = product.transactionCurrency,
                        exchangeRateLocked = 1550.0,
                        reasonOrNotes = "جلسة جرد: $sessionTitle",
                        timestamp = now
                    )
                )
            }
        }

        val sessionId = insertInventoryCountSession(
            InventoryCountSessionEntity(
                sessionTitle = sessionTitle,
                categoryFilter = categoryFilter,
                status = "COMPLETED",
                totalSurplusValueYer = totalSurplusYer,
                totalDeficitValueYer = totalDeficitYer,
                netVarianceValueYer = totalSurplusYer - totalDeficitYer,
                notes = notes,
                timestamp = now
            )
        )

        insertInventoryCountLines(linesToInsert.map { it.copy(sessionId = sessionId) })
        return sessionId
    }
}
