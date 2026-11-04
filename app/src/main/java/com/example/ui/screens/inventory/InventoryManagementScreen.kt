package com.example.ui.screens.inventory

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.BundleComponentItem
import com.example.data.local.ProductEntity
import com.example.data.local.ProductUnitConversion
import com.example.data.local.StockMovementEntity
import com.example.data.local.TaxType
import com.example.ui.components.BarcodeScannerSimulationDialog
import com.example.ui.components.SanadBadge
import com.example.ui.components.formatArabicDate
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
import com.example.ui.theme.SanadTextMuted
import com.example.ui.theme.SanadTextPrimary
import com.example.ui.theme.SanadTextSecondary
import com.example.ui.viewmodel.SanadViewModel
import kotlin.math.roundToLong

@Composable
fun InventoryManagementScreen(
    viewModel: SanadViewModel,
    externalOpenNewProductDialog: Boolean = false,
    onDismissExternalNewProductDialog: () -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val currencies by viewModel.currencies.collectAsState()
    val movements by viewModel.stockMovements.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var showNewProductScreen by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var fastWriteOffTarget by remember { mutableStateOf<ProductEntity?>(null) }
    var showInventoryCountDialog by remember { mutableStateOf(false) }
    var showItemHistoryDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val yerRate = currencies.find { it.code == "YER" }?.exchangeRateToBase ?: 1550.0
    val now = System.currentTimeMillis()

    val categories = remember(products) {
        listOf("الكل", "منتهي الصلاحية", "نواقص الحد الآمن") +
            products.map { it.category }.distinct()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory, now) {
        products.filter { prod ->
            val matchesQuery = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.barcode.contains(searchQuery, ignoreCase = true) ||
                prod.category.contains(searchQuery, ignoreCase = true)

            val matchesCat = when (selectedCategory) {
                "الكل" -> true
                "منتهي الصلاحية" -> prod.nearestExpiryTimestamp != null && prod.nearestExpiryTimestamp < now
                "نواقص الحد الآمن" -> prod.availableQuantity <= prod.safetyThreshold
                else -> prod.category == selectedCategory
            }
            matchesQuery && matchesCat
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
    ) {
        // 1. Top Action Header: [+ إضافة صنف] | [بدء جلسة جرد] | [حركة المادة]
        Surface(
            color = SanadSurfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // [+ إضافة صنف]
                    Button(
                        onClick = {
                            editingProduct = null
                            showNewProductScreen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("add_new_product_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ إضافة صنف",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }

                    // [بدء جلسة جرد]
                    Surface(
                        color = SanadSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .border(1.dp, SanadEmeraldGreen, RoundedCornerShape(10.dp))
                            .clickable { showInventoryCountDialog = true }
                            .testTag("start_inventory_count_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = SanadEmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "بدء جلسة جرد",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SanadEmeraldGreen,
                                maxLines = 1
                            )
                        }
                    }

                    // [حركة المادة]
                    Surface(
                        color = SanadSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                            .clickable { showItemHistoryDialog = true }
                            .testTag("item_history_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = SanadAmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "حركة المادة",
                                style = MaterialTheme.typography.labelLarge,
                                color = SanadTextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Preferred Currency Exchange Rate Bar (1 $ = 1,550 ر.ي) + Valuation Summary
                val totalInventoryCostYer = products.sumOf { it.availableQuantity * it.weightedAverageCost }
                Surface(
                    color = SanadSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "سعر الصرف المعتمد: ",
                                style = MaterialTheme.typography.labelMedium,
                                color = SanadTextSecondary
                            )
                            Text(
                                text = "1 $ = ${formatMoney(yerRate, "ر.ي")}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SanadEmeraldGreen
                            )
                        }
                        Text(
                            text = "قيمة المخزون: ${formatMoney(totalInventoryCostYer, "ر.ي")}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = SanadElectricBlue
                        )
                    }
                }

                // Search Bar with Camera Barcode Scanner + Category Filter Chips
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث باسم الصنف أو الباركود أو الفئة...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SanadTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SanadElectricBlue)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showBarcodeScanner = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح الباركود بالكاميرا",
                                tint = SanadEmeraldGreen
                            )
                        }
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
                        .fillMaxWidth()
                        .testTag("inventory_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = selectedCategory == cat
                        val isExpiredFilter = cat == "منتهي الصلاحية"
                        Surface(
                            color = when {
                                selected && isExpiredFilter -> SanadRedContainer
                                selected -> SanadElectricBlue
                                else -> SanadSurface
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    when {
                                        selected && isExpiredFilter -> SanadCoralRed
                                        selected -> SanadElectricBlue
                                        else -> SanadBorder
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelLarge,
                                color = when {
                                    selected && isExpiredFilter -> SanadCoralRed
                                    selected -> Color.White
                                    else -> SanadTextSecondary
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = SanadBorder)

        // 2. Rich Stock Cards List (بطاقات الأصناف الكحلية)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredProducts, key = { it.id }) { product ->
                RichStockCard(
                    product = product,
                    currentTimeMs = now,
                    onEditClicked = {
                        editingProduct = product
                        showNewProductScreen = true
                    },
                    onFastWriteOffClicked = {
                        fastWriteOffTarget = product
                    }
                )
            }
        }
    }

    // ========================================================================
    // MODALS: NEW/EDIT PRODUCT SCREEN, FAST WRITE-OFF, INVENTORY COUNT, HISTORY
    // ========================================================================

    if (showNewProductScreen || externalOpenNewProductDialog) {
        NewOrEditProductModalScreen(
            existingProduct = editingProduct,
            allProducts = products,
            onDismiss = {
                showNewProductScreen = false
                editingProduct = null
                onDismissExternalNewProductDialog()
            },
            onSaveProduct = { name, category, barcode, currency, cost, margin, wholesale, retail, qty, safety, baseUnit, secUnits, isPlu, taxType, customTax, isBundle, bundleComps, trackExpiry, expiryTs ->
                viewModel.saveNewOrEditedProduct(
                    editingProductId = editingProduct?.id,
                    name = name,
                    category = category,
                    barcode = barcode,
                    transactionCurrency = currency,
                    costPrice = cost,
                    profitMarginPercent = margin,
                    wholesalePrice = wholesale,
                    retailPrice = retail,
                    availableQuantity = qty,
                    safetyThreshold = safety,
                    baseUnitName = baseUnit,
                    secondaryUnits = secUnits,
                    soldByWeightPlu = isPlu,
                    taxType = taxType,
                    customTaxPercent = customTax,
                    isCompositeBundle = isBundle,
                    bundleComponents = bundleComps,
                    trackBatchesAndExpiry = trackExpiry,
                    nearestExpiryTimestamp = expiryTs
                )
                showNewProductScreen = false
                editingProduct = null
                onDismissExternalNewProductDialog()
            }
        )
    }

    // 1-Click Fast Write-off Modal ([إتلاف سريع])
    fastWriteOffTarget?.let { targetProduct ->
        FastWriteOffDialog(
            product = targetProduct,
            onDismiss = { fastWriteOffTarget = null },
            onConfirmWriteOff = { qty, reason ->
                viewModel.triggerFastWriteOff(targetProduct, qty, reason)
                fastWriteOffTarget = null
            }
        )
    }

    // Physical Inventory Count Session Modal ([بدء جلسة جرد])
    if (showInventoryCountDialog) {
        PhysicalInventoryCountDialog(
            products = products,
            onDismiss = { showInventoryCountDialog = false },
            onCommitCount = { title, countedMap, notes ->
                viewModel.commitInventoryCount(title, "الكل", countedMap, notes)
                showInventoryCountDialog = false
            }
        )
    }

    // Item Movement History Modal ([حركة المادة])
    if (showItemHistoryDialog) {
        ItemMovementHistoryDialog(
            movements = movements,
            onDismiss = { showItemHistoryDialog = false }
        )
    }

    // Barcode Scanner for Inventory Search
    if (showBarcodeScanner) {
        BarcodeScannerSimulationDialog(
            title = "بحث بالباركود في المخزون",
            presets = products.map { it.barcode to it.name },
            onDismiss = { showBarcodeScanner = false },
            onScanned = { code ->
                searchQuery = code
                showBarcodeScanner = false
            }
        )
    }
}

/**
 * Rich Stock Card (بطاقة الصنف الكحلية) matching exact Sanad specifications:
 * - Item Name, Category, Barcode
 * - Retail Price in Electric Blue (9,300 ر.ي)
 * - Green Profit Margin Badge (هامش 25%)
 * - Weighted Average Cost (تكلفة 6,975 ر.ي)
 * - Green Availability Badge (متوفر: 96)
 * - Multi-Unit Breakdown (6 تنكة = 96 لتر / 4 كرتونة = 40 كيس)
 * - Red Expiry Badge (منتهي الصلاحية)
 * - 1-click [إتلاف سريع (Fast Write-off)] button
 */
@Composable
private fun RichStockCard(
    product: ProductEntity,
    currentTimeMs: Long,
    onEditClicked: () -> Unit,
    onFastWriteOffClicked: () -> Unit
) {
    val isExpired = product.nearestExpiryTimestamp != null && product.nearestExpiryTimestamp < currentTimeMs
    val isLowStock = product.availableQuantity <= product.safetyThreshold
    val sym = if (product.transactionCurrency == "USD") "$" else "ر.ي"
    val multiUnitBreakdown = product.formattedMultiUnitBreakdown()

    Card(
        colors = CardDefaults.cardColors(containerColor = SanadSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isExpired) SanadCoralRed.copy(alpha = 0.7f) else SanadBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onEditClicked() }
            .testTag("stock_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Product Name, Category, Barcode & Retail Price in Electric Blue + Profit Margin Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                        if (product.soldByWeightPlu) {
                            Spacer(modifier = Modifier.width(6.dp))
                            SanadBadge(
                                text = "ميزان PLU",
                                textColor = SanadAmberWarning,
                                containerColor = SanadAmberContainer
                            )
                        }
                        if (product.isCompositeBundle) {
                            Spacer(modifier = Modifier.width(6.dp))
                            SanadBadge(
                                text = "منتج تجميعي",
                                textColor = SanadElectricBlue,
                                containerColor = SanadBlueContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${product.category} • باركود: ${product.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SanadTextSecondary
                    )
                }

                // Retail Price in Electric Blue (9,300 ر.ي) & Green Profit Margin Badge (هامش 25%)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(product.retailPrice, sym),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SanadElectricBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SanadBadge(
                            text = "هامش ${formatNumberCompact(product.profitMarginPercent)}%",
                            textColor = SanadEmeraldGreen,
                            containerColor = SanadEmeraldContainer
                        )
                        SanadBadge(
                            text = "تكلفة ${formatMoney(product.weightedAverageCost, sym)}",
                            textColor = SanadTextSecondary,
                            containerColor = SanadNavyDark
                        )
                    }
                }
            }

            HorizontalDivider(color = SanadBorder.copy(alpha = 0.6f))

            // Bottom Row: Availability Badge (متوفر: 96), Multi-Unit Breakdown (6 تنكة = 96 لتر), Expiry Badge & [إتلاف سريع]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Green Availability Badge (متوفر: 96)
                    SanadBadge(
                        text = "متوفر: ${formatNumberCompact(product.availableQuantity)}",
                        textColor = if (isLowStock) SanadAmberWarning else SanadEmeraldGreen,
                        containerColor = if (isLowStock) SanadAmberContainer else SanadEmeraldContainer
                    )

                    // Multi-Unit Breakdown (6 تنكة = 96 لتر / 4 كرتونة = 40 كيس)
                    if (multiUnitBreakdown != null) {
                        SanadBadge(
                            text = multiUnitBreakdown,
                            textColor = SanadElectricBlue,
                            containerColor = SanadBlueContainer
                        )
                    }

                    // Red Expiry Badge (منتهي الصلاحية)
                    if (isExpired) {
                        SanadBadge(
                            text = "منتهي الصلاحية",
                            textColor = SanadCoralRed,
                            containerColor = SanadRedContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 1-Click [إتلاف سريع] Button
                    Surface(
                        color = SanadRedContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, SanadCoralRed.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .clickable { onFastWriteOffClicked() }
                            .testTag("fast_writeoff_button_${product.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = SanadCoralRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إتلاف سريع",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SanadCoralRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onEditClicked,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الصنف",
                            tint = SanadTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dynamic "New/Edit Product" Screen (صنف جديد) matching the exact Sanad design:
 * - Item Name, Barcode input with Camera scan & Barcode generator
 * - Weight-Scale PLU Switch (يباع بالوزن - ميزان PLU)
 * - Category, Tax Type Selector (قياسية | صفرية | معفاة | مخصصة)
 * - Bundled/Composite Product Switch (هل هذا منتج تجميعي؟)
 * - Bidirectional Pricing Matrix: Transaction Currency, Cost Price, Profit Margin % (auto-calculates price when edited), Wholesale Price, Retail Price
 * - Available Quantity, Safety Threshold
 * - "وحدات القياس" Card with Base Unit Name + "+ إضافة وحدة"
 * - "تتبع الدفعات / الصلاحية" toggle
 */
@Composable
fun NewOrEditProductModalScreen(
    existingProduct: ProductEntity?,
    allProducts: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSaveProduct: (
        name: String,
        category: String,
        barcode: String,
        currency: String,
        cost: Double,
        margin: Double,
        wholesale: Double,
        retail: Double,
        qty: Double,
        safety: Double,
        baseUnit: String,
        secUnits: List<ProductUnitConversion>,
        isPlu: Boolean,
        taxType: TaxType,
        customTax: Double,
        isBundle: Boolean,
        bundleComps: List<BundleComponentItem>,
        trackExpiry: Boolean,
        expiryTs: Long?
    ) -> Unit
) {
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var barcode by remember { mutableStateOf(existingProduct?.barcode ?: "") }
    var soldByWeightPlu by remember { mutableStateOf(existingProduct?.soldByWeightPlu ?: false) }
    var category by remember { mutableStateOf(existingProduct?.category ?: "زيوت وسمن") }
    var taxType by remember { mutableStateOf(existingProduct?.taxType ?: TaxType.ZERO) }
    var customTaxText by remember {
        mutableStateOf(formatNumberCompact(existingProduct?.customTaxPercent ?: 5.0))
    }
    var isCompositeBundle by remember { mutableStateOf(existingProduct?.isCompositeBundle ?: false) }
    var transactionCurrency by remember { mutableStateOf(existingProduct?.transactionCurrency ?: "YER") }

    // Bidirectional Pricing Matrix State
    var costText by remember {
        mutableStateOf(if (existingProduct != null) formatNumberCompact(existingProduct.costPrice) else "6975")
    }
    var marginText by remember {
        mutableStateOf(if (existingProduct != null) formatNumberCompact(existingProduct.profitMarginPercent) else "25")
    }
    var wholesaleText by remember {
        mutableStateOf(if (existingProduct != null) formatNumberCompact(existingProduct.wholesalePrice) else "8600")
    }
    var retailText by remember {
        mutableStateOf(if (existingProduct != null) formatNumberCompact(existingProduct.retailPrice) else "9300")
    }

    // Stock & Multi-Unit Builder State
    var availableQtyText by remember {
        mutableStateOf(if (existingProduct != null) formatNumberCompact(existingProduct.availableQuantity) else "96")
    }
    var safetyThresholdText by remember {
        mutableStateOf(if (existingProduct != null) formatNumberCompact(existingProduct.safetyThreshold) else "16")
    }
    var baseUnitName by remember { mutableStateOf(existingProduct?.baseUnitName ?: "لتر") }
    var secondaryUnits by remember {
        mutableStateOf(
            existingProduct?.secondaryUnits ?: listOf(
                ProductUnitConversion(
                    unitName = "تنكة",
                    conversionFactor = 16.0,
                    barcode = "6281000451026",
                    retailPrice = 148800.0,
                    wholesalePrice = 137600.0
                )
            )
        )
    }

    var bundleComponents by remember {
        mutableStateOf(existingProduct?.bundleComponents ?: emptyList())
    }
    var trackBatchesAndExpiry by remember {
        mutableStateOf(existingProduct?.trackBatchesAndExpiry ?: false)
    }
    var markAsExpiredDemo by remember {
        mutableStateOf(
            existingProduct?.nearestExpiryTimestamp != null &&
                existingProduct.nearestExpiryTimestamp < System.currentTimeMillis()
        )
    }

    // New Secondary Unit inline inputs
    var showAddUnitRow by remember { mutableStateOf(false) }
    var newUnitName by remember { mutableStateOf("") }
    var newUnitFactorText by remember { mutableStateOf("10") }
    var newUnitBarcode by remember { mutableStateOf("") }
    var newUnitRetailText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = SanadNavyDark,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Surface(
                    color = SanadSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = SanadElectricBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (existingProduct == null) "صنف جديد" else "تعديل بيانات الصنف",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = SanadTextPrimary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                        }
                    }
                }

                HorizontalDivider(color = SanadBorder)

                // Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Basic Product Info & Barcode Generator
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
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("اسم الصنف *") },
                                placeholder = { Text("مثال: زيت زيتون بكر ممتاز / أرز بسمتي 5 كغ") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("product_name_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = barcode,
                                    onValueChange = { barcode = it },
                                    label = { Text("الباركود") },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val prefix = if (soldByWeightPlu) "21000" else "62810"
                                                barcode = "$prefix${(10000000..99999999).random()}"
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "توليد باركود تلقائي",
                                                tint = SanadElectricBlue
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { category = it },
                                    label = { Text("الفئة / التصنيف") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }

                            // Weight-Scale PLU Switch (يباع بالوزن - ميزان PLU)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SanadNavyDark)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Scale,
                                        contentDescription = null,
                                        tint = SanadAmberWarning
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "يباع بالوزن (ميزان PLU)",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadTextPrimary
                                        )
                                        Text(
                                            text = "تفكيك باركود الميزان الإلكتروني تلقائياً إلى أوزان عشرية أو بيع بالمبلغ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SanadTextSecondary
                                        )
                                    }
                                }
                                Switch(
                                    checked = soldByWeightPlu,
                                    onCheckedChange = { soldByWeightPlu = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = SanadAmberWarning)
                                )
                            }

                            // Tax Type Selector (قياسية | صفرية | معفاة | مخصصة)
                            Text(
                                text = "نوع الضريبة:",
                                style = MaterialTheme.typography.labelLarge,
                                color = SanadTextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TaxType.entries.forEach { tType ->
                                    val selected = taxType == tType
                                    Surface(
                                        color = if (selected) SanadElectricBlue else SanadNavyDark,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                1.dp,
                                                if (selected) SanadElectricBlue else SanadBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { taxType = tType }
                                    ) {
                                        Text(
                                            text = tType.arabicLabel,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (selected) Color.White else SanadTextSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            if (taxType == TaxType.CUSTOM) {
                                OutlinedTextField(
                                    value = customTaxText,
                                    onValueChange = { customTaxText = it },
                                    label = { Text("نسبة الضريبة المخصصة %") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Composite/Bundled Product Switch (هل هذا منتج تجميعي؟)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SanadNavyDark)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "هل هذا منتج تجميعي؟",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadTextPrimary
                                    )
                                    Text(
                                        text = "يخصم كميات المكونات الفرعية تلقائياً عند البيع دون الحاجة لمخزون منفصل",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SanadTextSecondary
                                    )
                                }
                                Switch(
                                    checked = isCompositeBundle,
                                    onCheckedChange = {
                                        isCompositeBundle = it
                                        if (it && bundleComponents.isEmpty() && allProducts.size >= 2) {
                                            bundleComponents = allProducts.take(2).map { p ->
                                                BundleComponentItem(
                                                    componentProductId = p.id,
                                                    componentProductName = p.name,
                                                    requiredQuantity = 1.0,
                                                    unitCost = p.weightedAverageCost
                                                )
                                            }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = SanadElectricBlue)
                                )
                            }
                        }
                    }

                    // 2. Bidirectional Pricing Matrix Card (الحساب التبادلي التلقائي بين التكلفة ونسبة الربح % وسعر المفرق)
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مصفوفة التسعير التبادلية وعملة المعاملة",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadEmeraldGreen
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("YER", "USD", "SAR").forEach { curr ->
                                        val selected = transactionCurrency == curr
                                        Surface(
                                            color = if (selected) SanadElectricBlue else SanadNavyDark,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.clickable { transactionCurrency = curr }
                                        ) {
                                            Text(
                                                text = curr,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (selected) Color.White else SanadTextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Cost Price (سعر التكلفة)
                                OutlinedTextField(
                                    value = costText,
                                    onValueChange = { newCostStr ->
                                        costText = newCostStr
                                        val cost = newCostStr.toDoubleOrNull() ?: 0.0
                                        val margin = marginText.toDoubleOrNull() ?: 0.0
                                        if (cost > 0) {
                                            val calcRetail = (cost * (1.0 + margin / 100.0)).roundToLong().toDouble()
                                            val calcWholesale = (cost * (1.0 + (margin * 0.7) / 100.0)).roundToLong().toDouble()
                                            retailText = formatNumberCompact(calcRetail)
                                            wholesaleText = formatNumberCompact(calcWholesale)
                                        }
                                    },
                                    label = { Text("سعر التكلفة") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("product_cost_input")
                                )

                                // Profit Margin % (نسبة الربح % - auto-calculates retail price when edited)
                                OutlinedTextField(
                                    value = marginText,
                                    onValueChange = { newMarginStr ->
                                        marginText = newMarginStr
                                        val cost = costText.toDoubleOrNull() ?: 0.0
                                        val margin = newMarginStr.toDoubleOrNull() ?: 0.0
                                        if (cost > 0) {
                                            val calcRetail = (cost * (1.0 + margin / 100.0)).roundToLong().toDouble()
                                            val calcWholesale = (cost * (1.0 + (margin * 0.7) / 100.0)).roundToLong().toDouble()
                                            retailText = formatNumberCompact(calcRetail)
                                            wholesaleText = formatNumberCompact(calcWholesale)
                                        }
                                    },
                                    label = { Text("نسبة الربح %") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("product_margin_input")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Wholesale Price (سعر الجملة)
                                OutlinedTextField(
                                    value = wholesaleText,
                                    onValueChange = { wholesaleText = it },
                                    label = { Text("سعر الجملة") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )

                                // Retail Price (سعر المفرق - auto-calculates margin % when edited)
                                OutlinedTextField(
                                    value = retailText,
                                    onValueChange = { newRetailStr ->
                                        retailText = newRetailStr
                                        val cost = costText.toDoubleOrNull() ?: 0.0
                                        val retail = newRetailStr.toDoubleOrNull() ?: 0.0
                                        if (cost > 0 && retail > 0) {
                                            val calcMargin = (((retail - cost) / cost) * 1000.0).roundToLong() / 10.0
                                            marginText = formatNumberCompact(calcMargin)
                                        }
                                    },
                                    label = { Text("سعر المفرق") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("product_retail_input")
                                )
                            }
                        }
                    }

                    // 3. Stock & Multi-Unit Builder Card ("وحدات القياس" مع "+ إضافة وحدة")
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
                                text = "المخزون ووحدات القياس المتعددة",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SanadElectricBlue
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = availableQtyText,
                                    onValueChange = { availableQtyText = it },
                                    label = { Text("الكمية المتاحة") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = safetyThresholdText,
                                    onValueChange = { safetyThresholdText = it },
                                    label = { Text("حد الأمان") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = baseUnitName,
                                    onValueChange = { baseUnitName = it },
                                    label = { Text("الوحدة الأساسية") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(color = SanadBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "وحدات القياس الفرعية / الكبرى (${secondaryUnits.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = SanadTextPrimary
                                )

                                TextButton(
                                    onClick = { showAddUnitRow = !showAddUnitRow },
                                    modifier = Modifier.testTag("add_secondary_unit_button")
                                ) {
                                    Text(
                                        text = "+ إضافة وحدة",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = SanadEmeraldGreen
                                    )
                                }
                            }

                            secondaryUnits.forEachIndexed { idx, unit ->
                                Surface(
                                    color = SanadNavyDark,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SanadBorder, RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "1 ${unit.unitName} = ${formatNumberCompact(unit.conversionFactor)} $baseUnitName",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = SanadElectricBlue
                                            )
                                            Text(
                                                text = "سعر المفرق: ${formatMoney(unit.retailPrice)} • باركود: ${unit.barcode.ifBlank { "تلقائي" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SanadTextSecondary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                secondaryUnits = secondaryUnits.filterIndexed { i, _ -> i != idx }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "حذف الوحدة",
                                                tint = SanadCoralRed
                                            )
                                        }
                                    }
                                }
                            }

                            if (showAddUnitRow) {
                                Surface(
                                    color = SanadSurfaceElevated,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SanadEmeraldGreen, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = newUnitName,
                                                onValueChange = { newUnitName = it },
                                                label = { Text("اسم الوحدة (تنكة/كرتونة)") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = newUnitFactorText,
                                                onValueChange = { newUnitFactorText = it },
                                                label = { Text("معامل التحويل") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(0.8f)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = newUnitRetailText,
                                                onValueChange = { newUnitRetailText = it },
                                                label = { Text("سعر البيع للوحدة") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = newUnitBarcode,
                                                onValueChange = { newUnitBarcode = it },
                                                label = { Text("باركود الوحدة") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                val factor = newUnitFactorText.toDoubleOrNull() ?: 1.0
                                                val baseRetail = retailText.toDoubleOrNull() ?: 0.0
                                                val unitRetail = newUnitRetailText.toDoubleOrNull() ?: (baseRetail * factor)
                                                if (newUnitName.isNotBlank() && factor > 0) {
                                                    secondaryUnits = secondaryUnits + ProductUnitConversion(
                                                        unitName = newUnitName.trim(),
                                                        conversionFactor = factor,
                                                        barcode = newUnitBarcode.trim(),
                                                        retailPrice = unitRetail,
                                                        wholesalePrice = unitRetail * 0.94
                                                    )
                                                    newUnitName = ""
                                                    newUnitRetailText = ""
                                                    newUnitBarcode = ""
                                                    showAddUnitRow = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("تأكيد إضافة الوحدة")
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = SanadBorder)

                            // Batch & Expiry Tracking Toggle (تتبع الدفعات / الصلاحية)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SanadNavyDark)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تتبع الدفعات / الصلاحية",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadTextPrimary
                                    )
                                    Text(
                                        text = "تنبيهات تلقائية للدفعات القريبة من الانتهاء أو منتهية الصلاحية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SanadTextSecondary
                                    )
                                }
                                Switch(
                                    checked = trackBatchesAndExpiry,
                                    onCheckedChange = { trackBatchesAndExpiry = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = SanadEmeraldGreen)
                                )
                            }

                            if (trackBatchesAndExpiry) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "حالة صلاحية الدفعة الحالية:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SanadTextSecondary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = if (!markAsExpiredDemo) SanadEmeraldContainer else SanadNavyDark,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.clickable { markAsExpiredDemo = false }
                                        ) {
                                            Text(
                                                text = "صالحة (6 أشهر)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (!markAsExpiredDemo) SanadEmeraldGreen else SanadTextSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                        Surface(
                                            color = if (markAsExpiredDemo) SanadRedContainer else SanadNavyDark,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.clickable { markAsExpiredDemo = true }
                                        ) {
                                            Text(
                                                text = "منتهي الصلاحية (للتنبيه)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (markAsExpiredDemo) SanadCoralRed else SanadTextSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Save Button Bar
                Surface(
                    color = SanadSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("إلغاء", color = SanadTextSecondary)
                        }

                        Button(
                            onClick = {
                                val expiryTs = if (!trackBatchesAndExpiry) {
                                    null
                                } else if (markAsExpiredDemo) {
                                    System.currentTimeMillis() - (3 * 86_400_000L)
                                } else {
                                    System.currentTimeMillis() + (180 * 86_400_000L)
                                }
                                onSaveProduct(
                                    name,
                                    category,
                                    barcode,
                                    transactionCurrency,
                                    costText.toDoubleOrNull() ?: 0.0,
                                    marginText.toDoubleOrNull() ?: 0.0,
                                    wholesaleText.toDoubleOrNull() ?: 0.0,
                                    retailText.toDoubleOrNull() ?: 0.0,
                                    availableQtyText.toDoubleOrNull() ?: 0.0,
                                    safetyThresholdText.toDoubleOrNull() ?: 10.0,
                                    baseUnitName,
                                    secondaryUnits,
                                    soldByWeightPlu,
                                    taxType,
                                    customTaxText.toDoubleOrNull() ?: 0.0,
                                    isCompositeBundle,
                                    bundleComponents,
                                    trackBatchesAndExpiry,
                                    expiryTs
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp)
                                .testTag("save_product_submit_button")
                        ) {
                            Text(
                                text = "حفظ بيانات الصنف",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FastWriteOffDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirmWriteOff: (Double, String) -> Unit
) {
    var qtyText by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("انتهاء صلاحية / تلف مخزني") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SanadSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = SanadCoralRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إتلاف سريع: ${product.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SanadTextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "الكمية المتاحة حالياً: ${formatNumberCompact(product.availableQuantity)} ${product.baseUnitName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SanadTextSecondary
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("الكمية المراد إتلافها (${product.baseUnitName})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الإتلاف (إلزامي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val lossVal = (qtyText.toDoubleOrNull() ?: 0.0) * product.weightedAverageCost
                Text(
                    text = "قيمة الخسارة المحاسبية المقدرة: ${formatMoney(lossVal, product.transactionCurrency)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = SanadCoralRed
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmWriteOff(qtyText.toDoubleOrNull() ?: 1.0, reason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SanadCoralRed)
            ) {
                Text("تأكيد الإتلاف وخصم المخزون")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = SanadTextSecondary)
            }
        }
    )
}

@Composable
private fun PhysicalInventoryCountDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onCommitCount: (String, Map<Long, Double>, String) -> Unit
) {
    var sessionTitle by remember { mutableStateOf("جلسة جرد فعلي شامل") }
    var notes by remember { mutableStateOf("مطابقة أرصدة المستودع") }
    val countedMap = remember(products) {
        mutableStateMapOf<Long, String>().apply {
            products.forEach { put(it.id, formatNumberCompact(it.availableQuantity)) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SanadSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SanadBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "بدء جلسة جرد فعلي (Loyverse Inventory Count)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SanadEmeraldGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sessionTitle,
                    onValueChange = { sessionTitle = it },
                    label = { Text("عنوان جلسة الجرد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { prod ->
                        val countedStr = countedMap[prod.id] ?: ""
                        val countedVal = countedStr.toDoubleOrNull() ?: prod.availableQuantity
                        val diff = countedVal - prod.availableQuantity

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
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadTextPrimary
                                    )
                                    Text(
                                        text = "الدفتر: ${formatNumberCompact(prod.availableQuantity)} ${prod.baseUnitName} | الفرق: ${formatNumberCompact(diff)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            diff > 0 -> SanadEmeraldGreen
                                            diff < 0 -> SanadCoralRed
                                            else -> SanadTextSecondary
                                        }
                                    )
                                }
                                OutlinedTextField(
                                    value = countedStr,
                                    onValueChange = { countedMap[prod.id] = it },
                                    label = { Text("الفعلي") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(100.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = SanadTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsed = countedMap.mapValues { (id, str) ->
                                str.toDoubleOrNull() ?: (products.find { it.id == id }?.availableQuantity ?: 0.0)
                            }
                            onCommitCount(sessionTitle, parsed, notes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen)
                    ) {
                        Text("اعتماد الجرد وتسوية الفروقات")
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemMovementHistoryDialog(
    movements: List<StockMovementEntity>,
    onDismiss: () -> Unit
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
                        text = "سجل حركة المواد (${movements.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SanadAmberWarning
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (movements.isEmpty()) {
                    Text(
                        text = "لا توجد حركات مسجلة بعد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SanadTextSecondary,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(movements, key = { it.id }) { mov ->
                            val isPositive = mov.quantityDelta >= 0
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
                                            text = "${mov.productName} (${mov.movementType.arabicLabel})",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadTextPrimary
                                        )
                                        Text(
                                            text = "${mov.reasonOrNotes} • ${formatArabicDate(mov.timestamp)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SanadTextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        val sign = if (isPositive) "+" else ""
                                        Text(
                                            text = "$sign${formatNumberCompact(mov.quantityDelta)} ${mov.unitName}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isPositive) SanadEmeraldGreen else SanadCoralRed
                                        )
                                        Text(
                                            text = "الرصيد: ${formatNumberCompact(mov.balanceAfter)}",
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
        }
    }
}
