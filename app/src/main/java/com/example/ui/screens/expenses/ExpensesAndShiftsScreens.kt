package com.example.ui.screens.expenses

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.FinancialAccountEntity
import com.example.data.local.PartyEntity
import com.example.data.local.PartyType
import com.example.data.local.ShiftOpeningSource
import com.example.ui.components.SanadBadge
import com.example.ui.components.formatArabicDate
import com.example.ui.components.formatMoney
import com.example.ui.components.formatNumberCompact
import com.example.ui.components.formatShortDate
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

// ============================================================================
// 1. MULTI-CURRENCY EXPENSES SCREEN & NEW EXPENSE MODAL (المصاريف ومصروف جديد)
// ============================================================================
data class ExpenseCategoryOption(
    val label: String,
    val icon: ImageVector
)

private val expenseCategories = listOf(
    ExpenseCategoryOption("رواتب", Icons.Default.Groups),
    ExpenseCategoryOption("إيجار", Icons.Default.HomeWork),
    ExpenseCategoryOption("فواتير", Icons.Default.Receipt),
    ExpenseCategoryOption("تسويق", Icons.Default.Campaign),
    ExpenseCategoryOption("مواصلات", Icons.Default.DirectionsBus),
    ExpenseCategoryOption("صيانة", Icons.Default.Build),
    ExpenseCategoryOption("+ أخرى", Icons.Default.MoreHoriz)
)

@Composable
fun MultiCurrencyExpensesScreen(
    viewModel: SanadViewModel,
    externalOpenNewExpenseModal: Boolean = false,
    onDismissExternalExpenseModal: () -> Unit = {}
) {
    val expenses by viewModel.expenses.collectAsState()
    val accounts by viewModel.financialAccounts.collectAsState()
    val parties by viewModel.parties.collectAsState()
    val currencies by viewModel.currencies.collectAsState()

    var selectedCurrencyFilter by remember { mutableStateOf("كل العملات") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var showNewExpenseModal by remember { mutableStateOf(false) }

    val yerRate = currencies.find { it.code == "YER" }?.exchangeRateToBase ?: 1550.0

    val filteredExpenses = remember(expenses, selectedCurrencyFilter, selectedCategoryFilter) {
        expenses.filter { exp ->
            val matchCurr = selectedCurrencyFilter == "كل العملات" || exp.currencyCode == selectedCurrencyFilter
            val matchCat = selectedCategoryFilter == "الكل" || exp.category == selectedCategoryFilter
            matchCurr && matchCat
        }
    }

    val totalsByCurrency = remember(filteredExpenses) {
        filteredExpenses.groupBy { it.currencyCode }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
    ) {
        // Top Header + Currency Filter Chips (كل العملات | USD | TRY | YER) + "إجمالي التصفية" Cards
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المصاريف متعددة العملات",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SanadTextPrimary
                    )

                    Button(
                        onClick = { showNewExpenseModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SanadCoralRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_new_expense_modal_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "مصروف جديد",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Currency Filter Chips: (كل العملات | USD | TRY | YER)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("كل العملات", "USD", "TRY", "YER").forEach { curr ->
                        val selected = selectedCurrencyFilter == curr
                        Surface(
                            color = if (selected) SanadElectricBlue else SanadSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (selected) SanadElectricBlue else SanadBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCurrencyFilter = curr }
                                .testTag("expense_currency_filter_$curr")
                        ) {
                            Text(
                                text = curr,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) Color.White else SanadTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // "إجمالي التصفية" Cards per Currency
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val displayMap = if (totalsByCurrency.isEmpty()) {
                        mapOf("YER" to 0.0)
                    } else {
                        totalsByCurrency
                    }
                    displayMap.forEach { (curr, sum) ->
                        Surface(
                            color = SanadRedContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(1.dp, SanadCoralRed.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text(
                                    text = "إجمالي التصفية ($curr)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SanadCoralRed
                                )
                                Text(
                                    text = formatMoney(sum, curr),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SanadTextPrimary
                                )
                            }
                        }
                    }
                }

                // Category Filter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val catFilters = listOf("الكل") + expenseCategories.map { it.label.removePrefix("+ ").trim() }
                    catFilters.forEach { cat ->
                        val selected = selectedCategoryFilter == cat
                        Surface(
                            color = if (selected) SanadAmberContainer else SanadSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (selected) SanadAmberWarning else SanadBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCategoryFilter = cat }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) SanadAmberWarning else SanadTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = SanadBorder)

        // Expenses List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredExpenses, key = { it.id }) { exp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanadSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SanadBadge(
                                    text = exp.category,
                                    textColor = SanadCoralRed,
                                    containerColor = SanadRedContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = exp.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الحساب الدافع: ${exp.financialAccountName} • ${formatArabicDate(exp.timestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SanadTextSecondary
                            )
                            if (!exp.supplierPartyName.isNullOrBlank()) {
                                Text(
                                    text = "مرتبط بالمورد: ${exp.supplierPartyName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SanadElectricBlue
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatMoney(exp.amount, exp.currencyCode),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SanadCoralRed
                            )
                            Text(
                                text = "يعادل ${formatMoney(exp.baseUsdEquivalent, "$")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SanadTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewExpenseModal || externalOpenNewExpenseModal) {
        NewExpenseModalDialog(
            accounts = accounts,
            suppliers = parties.filter { it.partyType == PartyType.SUPPLIER },
            defaultYerRate = yerRate,
            onDismiss = {
                showNewExpenseModal = false
                onDismissExternalExpenseModal()
            },
            onCreateCashboxForCurrency = { curr ->
                viewModel.createAndSelectCashBoxForCurrency(curr)
            },
            onSaveExpense = { title, cat, curr, rate, amount, accId, accName, supId, supName, notes ->
                viewModel.registerExpense(
                    title = title,
                    category = cat,
                    currencyCode = curr,
                    lockedRate = rate,
                    amount = amount,
                    accountId = accId,
                    accountName = accName,
                    supplierPartyId = supId,
                    supplierPartyName = supName,
                    notes = notes
                )
                showNewExpenseModal = false
                onDismissExternalExpenseModal()
            }
        )
    }
}

@Composable
private fun NewExpenseModalDialog(
    accounts: List<FinancialAccountEntity>,
    suppliers: List<PartyEntity>,
    defaultYerRate: Double,
    onDismiss: () -> Unit,
    onCreateCashboxForCurrency: (String) -> Unit,
    onSaveExpense: (
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
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("فواتير") }
    var currencyCode by remember { mutableStateOf("YER") }
    var lockedRate by remember { mutableStateOf(defaultYerRate) }
    var amountText by remember { mutableStateOf("45000") }

    val matchingAccounts = remember(accounts, currencyCode) {
        accounts.filter { it.currencyCode == currencyCode }
    }
    var selectedAccount by remember(matchingAccounts) {
        mutableStateOf(matchingAccounts.firstOrNull())
    }
    var selectedSupplier by remember { mutableStateOf<PartyEntity?>(null) }
    var notes by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = SanadNavyDark,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = SanadCoralRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مصروف جديد",
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icon-Coded Category Chips: (رواتب | إيجار | فواتير | تسويق | مواصلات | صيانة | + أخرى)
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
                                text = "تصنيف المصروف:",
                                style = MaterialTheme.typography.labelLarge,
                                color = SanadTextSecondary
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                expenseCategories.forEach { catOpt ->
                                    val cleanLabel = catOpt.label.removePrefix("+ ").trim()
                                    val isSelected = selectedCategory == cleanLabel
                                    Surface(
                                        color = if (isSelected) SanadRedContainer else SanadNavyDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .border(
                                                1.5.dp,
                                                if (isSelected) SanadCoralRed else SanadBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedCategory = cleanLabel }
                                            .testTag("expense_cat_${cleanLabel}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = catOpt.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) SanadCoralRed else SanadTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = catOpt.label,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) SanadCoralRed else SanadTextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("بيان / عنوان المصروف *") },
                                placeholder = { Text("مثال: سداد فاتورة كهرباء الصالة / رواتب الموظفين") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expense_title_input")
                            )
                        }
                    }

                    // Currency, Rate Lock & Amount
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("YER", "USD", "TRY", "SAR").forEach { curr ->
                                    val selected = currencyCode == curr
                                    Surface(
                                        color = if (selected) SanadElectricBlue else SanadNavyDark,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                currencyCode = curr
                                                lockedRate = when (curr) {
                                                    "USD" -> 1.0
                                                    "TRY" -> 34.50
                                                    "SAR" -> 3.75
                                                    else -> defaultYerRate
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = curr,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (selected) Color.White else SanadTextSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                label = { Text("مبلغ المصروف ($currencyCode)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expense_amount_input")
                            )

                            // Payment Account with Inline [+ إضافة صندوق] Fallback
                            if (matchingAccounts.isEmpty()) {
                                Button(
                                    onClick = { onCreateCashboxForCurrency(currencyCode) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadAmberWarning),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("+ إضافة صندوق بعملة $currencyCode", color = SanadNavyDark)
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    matchingAccounts.forEach { acc ->
                                        val isSel = selectedAccount?.id == acc.id
                                        Surface(
                                            color = if (isSel) SanadEmeraldContainer else SanadNavyDark,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .border(
                                                    1.dp,
                                                    if (isSel) SanadEmeraldGreen else SanadBorder,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { selectedAccount = acc }
                                        ) {
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSel) SanadEmeraldGreen else SanadTextPrimary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Optional Supplier Link
                            Text(
                                text = "ربط اختياري بمورد:",
                                style = MaterialTheme.typography.labelMedium,
                                color = SanadTextSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = if (selectedSupplier == null) SanadBlueContainer else SanadNavyDark,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { selectedSupplier = null }
                                ) {
                                    Text(
                                        text = "بدون مورد",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selectedSupplier == null) SanadElectricBlue else SanadTextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                suppliers.forEach { sup ->
                                    val isSel = selectedSupplier?.id == sup.id
                                    Surface(
                                        color = if (isSel) SanadBlueContainer else SanadNavyDark,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { selectedSupplier = sup }
                                    ) {
                                        Text(
                                            text = sup.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSel) SanadElectricBlue else SanadTextSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات إضافية") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Bottom Save Bar
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
                                val acc = selectedAccount ?: accounts.firstOrNull()
                                onSaveExpense(
                                    title.ifBlank { "مصروف $selectedCategory" },
                                    selectedCategory,
                                    currencyCode,
                                    lockedRate,
                                    amountText.toDoubleOrNull() ?: 0.0,
                                    acc?.id ?: 1L,
                                    acc?.name ?: "الصندوق النقدي الرئيسي",
                                    selectedSupplier?.id,
                                    selectedSupplier?.name,
                                    notes
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SanadCoralRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp)
                                .testTag("save_expense_submit_button")
                        ) {
                            Text(
                                text = "حفظ المصروف",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. ANALYTICAL SHIFT MANAGEMENT SCREEN (شاشة إدارة الورديات متعددة الجيوب)
// ============================================================================
@Composable
fun AnalyticalShiftsAndTreasuryScreen(
    viewModel: SanadViewModel,
    initialSubTab: Int = 0, // 0 = إدارة الورديات, 1 = الحسابات المالية والخزينة
    onClose: () -> Unit = {}
) {
    val activeShift by viewModel.activeShift.collectAsState()
    val accounts by viewModel.financialAccounts.collectAsState()

    var currentSubTab by remember { mutableStateOf(initialSubTab) }
    var cashierName by remember(activeShift) {
        mutableStateOf(activeShift?.cashierName ?: "أحمد الخطيب")
    }
    var openingSource by remember(activeShift) {
        mutableStateOf(activeShift?.openingSource ?: ShiftOpeningSource.FROM_CASHBOX)
    }
    val primaryCashAccount = remember(accounts) {
        accounts.firstOrNull { !it.isVault } ?: accounts.firstOrNull()
    }
    var openingBalanceText by remember(activeShift) {
        mutableStateOf(formatNumberCompact(activeShift?.openingCustodyBalance ?: 250000.0))
    }
    val lockedSecondaryIds = remember(activeShift) {
        mutableStateListOf<Long>().apply {
            addAll(activeShift?.lockedSecondaryAccountIds ?: listOf(2L, 3L))
        }
    }
    var actualClosingCountedText by remember { mutableStateOf("") }
    var zReportNotes by remember { mutableStateOf("مطابقة الوردية وإغلاق Z-Report") }

    // Treasury Dialogs
    var vaultTransferAccount by remember { mutableStateOf<FinancialAccountEntity?>(null) }
    var reconcileAccount by remember { mutableStateOf<FinancialAccountEntity?>(null) }
    var showAddAccountDialog by remember { mutableStateOf(false) }

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
                    Text(
                        text = if (currentSubTab == 0) "إدارة الورديات متعددة الجيوب (Z-Report)" else "الحسابات المالية والخزينة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SanadTextPrimary
                    )
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to "إدارة الورديات (Z-Report)", 1 to "الحسابات المالية والخزينة").forEach { (idx, label) ->
                        val selected = currentSubTab == idx
                        Surface(
                            color = if (selected) SanadElectricBlue else SanadSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (selected) SanadElectricBlue else SanadBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { currentSubTab = idx }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) Color.White else SanadTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = SanadBorder)

        if (currentSubTab == 0) {
            // Sub-tab 0: Analytical Shift Management (إدارة الورديات متعددة الجيوب)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Analytical Informational Banner
                item {
                    Surface(
                        color = SanadBlueContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SanadElectricBlue, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SanadElectricBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "نظام الورديات التحليلي متعدد الجيوب: تسجيل رصيد العهدة الافتتاحي لا يغير أرصدة الصناديق الرئيسية حتى يتم جرد وإغلاق تقرير الوردية (Z-Report).",
                                style = MaterialTheme.typography.bodySmall,
                                color = SanadTextPrimary
                            )
                        }
                    }
                }

                // Shift Setup Card
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
                            OutlinedTextField(
                                value = cashierName,
                                onValueChange = { cashierName = it },
                                label = { Text("اسم الكاشير المناوب") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shift_cashier_name_input")
                            )

                            Text(
                                text = "مصدر الرصيد الافتتاحي:",
                                style = MaterialTheme.typography.labelLarge,
                                color = SanadTextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ShiftOpeningSource.entries.forEach { src ->
                                    val selected = openingSource == src
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
                                            .clickable { openingSource = src }
                                    ) {
                                        Text(
                                            text = src.arabicLabel,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (selected) Color.White else SanadTextSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            // Opening Balance with [تعبئة من الصندوق] Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = openingBalanceText,
                                    onValueChange = { openingBalanceText = it },
                                    label = { Text("الرصيد الافتتاحي للعهدة (ر.ي)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = {
                                        val cashBalance = primaryCashAccount?.currentBalance ?: 4850000.0
                                        openingBalanceText = formatNumberCompact(cashBalance)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("fill_from_cashbox_button")
                                ) {
                                    Text(
                                        text = "تعبئة من الصندوق",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            HorizontalDivider(color = SanadBorder)

                            // Secondary Financial Accounts Lock-in (حسابات مالية أخرى مقفلة ضمن الوردية)
                            Text(
                                text = "حسابات مالية أخرى مقفلة ضمن الوردية (للمطابقة في Z-Report):",
                                style = MaterialTheme.typography.labelLarge,
                                color = SanadTextPrimary
                            )

                            accounts.filter { it.id != primaryCashAccount?.id }.forEach { acc ->
                                val isLocked = lockedSecondaryIds.contains(acc.id)
                                Surface(
                                    color = SanadNavyDark,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isLocked) lockedSecondaryIds.remove(acc.id) else lockedSecondaryIds.add(acc.id)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isLocked,
                                                onCheckedChange = {
                                                    if (it) lockedSecondaryIds.add(acc.id) else lockedSecondaryIds.remove(acc.id)
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = SanadElectricBlue)
                                            )
                                            Text(
                                                text = "${acc.name} (${formatMoney(acc.currentBalance, acc.currencyCode)})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SanadTextPrimary
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isLocked) SanadEmeraldGreen else SanadTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.openNewShift(
                                        cashierName = cashierName,
                                        openingSource = openingSource,
                                        primaryAccountId = primaryCashAccount?.id ?: 1L,
                                        primaryAccountName = primaryCashAccount?.name ?: "الصندوق النقدي الرئيسي",
                                        currencyCode = primaryCashAccount?.currencyCode ?: "YER",
                                        openingCustody = openingBalanceText.toDoubleOrNull() ?: 0.0,
                                        secondaryAccountIds = lockedSecondaryIds.toList()
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("تحديث / فتح عهدة الوردية النشطة")
                            }
                        }
                    }
                }

                // Active Shift Z-Report Reconciliation Card
                activeShift?.let { shift ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, SanadEmeraldGreen, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ملخص الوردية النشطة وتقرير الإغلاق (Z-Report)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SanadEmeraldGreen
                                )
                                Text(
                                    text = "الكاشير: ${shift.cashierName} • الرصيد الافتتاحي: ${formatMoney(shift.openingCustodyBalance, shift.currencyCode)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextSecondary
                                )
                                Text(
                                    text = "مبيعات نقدية: +${formatMoney(shift.totalCashSales)} | مقبوضات: +${formatMoney(shift.totalReceiptsIn)} | مدفوعات ومصاريف: -${formatMoney(shift.totalPaymentsOut + shift.totalExpensesOut)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextPrimary
                                )
                                Text(
                                    text = "الرصيد الدفتري المتوقع عند الإغلاق: ${formatMoney(shift.expectedClosingBalance, shift.currencyCode)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadElectricBlue
                                )

                                OutlinedTextField(
                                    value = actualClosingCountedText,
                                    onValueChange = { actualClosingCountedText = it },
                                    label = { Text("الرصيد الفعلي المعدود في الدرج (${shift.currencyCode})") },
                                    placeholder = { Text(formatNumberCompact(shift.expectedClosingBalance)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        val actual = actualClosingCountedText.toDoubleOrNull() ?: shift.expectedClosingBalance
                                        viewModel.closeActiveShiftZReport(actual, zReportNotes)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SanadCoralRed),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("إغلاق الوردية وإصدار تقرير Z-Report")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Sub-tab 1: Financial Accounts & Treasury (الصناديق والمحافظ والبنوك مع ترحيل للخزينة وتسوية الحساب)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بطاقات الصناديق والمحافظ والبنوك (${accounts.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                        Button(
                            onClick = { showAddAccountDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ إضافة حساب")
                        }
                    }
                }

                items(accounts, key = { it.id }) { acc ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SanadTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SanadBadge(
                                            text = acc.accountType.arabicLabel,
                                            textColor = SanadElectricBlue,
                                            containerColor = SanadBlueContainer
                                        )
                                    }
                                    Text(
                                        text = acc.notes.ifBlank { "عملة الحساب: ${acc.currencyCode}" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SanadTextSecondary
                                    )
                                }

                                Text(
                                    text = formatMoney(acc.currentBalance, acc.currencyCode),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = SanadEmeraldGreen
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Micro-action 1: [ترحيل للخزينة (Vault Transfer)]
                                Surface(
                                    color = SanadEmeraldContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, SanadEmeraldGreen, RoundedCornerShape(8.dp))
                                        .clickable { vaultTransferAccount = acc }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = null,
                                            tint = SanadEmeraldGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ترحيل للخزينة",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SanadEmeraldGreen
                                        )
                                    }
                                }

                                // Micro-action 2: [تسوية الحساب (Balance Reconciliation)]
                                Surface(
                                    color = SanadAmberContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, SanadAmberWarning, RoundedCornerShape(8.dp))
                                        .clickable { reconcileAccount = acc }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = SanadAmberWarning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "تسوية الحساب",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
    }

    // Vault Transfer Dialog
    vaultTransferAccount?.let { sourceAcc ->
        var amountStr by remember { mutableStateOf("100000") }
        val vaultAcc = accounts.firstOrNull { it.isVault } ?: accounts.lastOrNull()
        AlertDialog(
            onDismissRequest = { vaultTransferAccount = null },
            containerColor = SanadSurface,
            title = { Text("ترحيل للخزينة من: ${sourceAcc.name}", color = SanadTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "الخزينة المستلمة: ${vaultAcc?.name ?: "الخزينة المركزية"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SanadEmeraldGreen
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("المبلغ المرحّل (${sourceAcc.currencyCode})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.transferToVault(
                            sourceAccountId = sourceAcc.id,
                            targetVaultId = vaultAcc?.id ?: 6L,
                            amount = amountStr.toDoubleOrNull() ?: 0.0,
                            notes = "ترحيل سيولة للخزينة المركزية"
                        )
                        vaultTransferAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen)
                ) {
                    Text("تأكيد الترحيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { vaultTransferAccount = null }) {
                    Text("إلغاء", color = SanadTextSecondary)
                }
            }
        )
    }

    // Balance Reconciliation Dialog
    reconcileAccount?.let { acc ->
        var actualStr by remember { mutableStateOf(formatNumberCompact(acc.currentBalance)) }
        var reason by remember { mutableStateOf("مطابقة الجرد النقدي الفعلي") }
        AlertDialog(
            onDismissRequest = { reconcileAccount = null },
            containerColor = SanadSurface,
            title = { Text("تسوية رصيد: ${acc.name}", color = SanadTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "الرصيد الدفتري الحالي: ${formatMoney(acc.currentBalance, acc.currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SanadTextSecondary
                    )
                    OutlinedTextField(
                        value = actualStr,
                        onValueChange = { actualStr = it },
                        label = { Text("الرصيد الفعلي الصحيح (${acc.currencyCode})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("سبب التسوية") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reconcileAccountBalance(
                            accountId = acc.id,
                            actualBalance = actualStr.toDoubleOrNull() ?: acc.currentBalance,
                            reason = reason
                        )
                        reconcileAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SanadAmberWarning)
                ) {
                    Text("اعتماد التسوية", color = SanadNavyDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { reconcileAccount = null }) {
                    Text("إلغاء", color = SanadTextSecondary)
                }
            }
        )
    }

    if (showAddAccountDialog) {
        var accName by remember { mutableStateOf("") }
        var accCurr by remember { mutableStateOf("YER") }
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            containerColor = SanadSurface,
            title = { Text("إضافة صندوق / حساب مالي جديد", color = SanadTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text("اسم الحساب المالي") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accCurr,
                        onValueChange = { accCurr = it.uppercase() },
                        label = { Text("العملة (YER / USD / SAR / TRY)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createAndSelectCashBoxForCurrency(accCurr, accName)
                        showAddAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen)
                ) {
                    Text("حفظ الحساب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("إلغاء", color = SanadTextSecondary)
                }
            }
        )
    }
}
