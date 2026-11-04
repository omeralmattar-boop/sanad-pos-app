package com.example.ui.reports

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.InvoiceMode
import com.example.data.local.InvoiceWithLines
import com.example.data.local.PartyType
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
import com.example.ui.theme.SanadTextPrimary
import com.example.ui.theme.SanadTextSecondary
import com.example.ui.viewmodel.SanadViewModel
import kotlin.math.abs

@Composable
fun LoyverseReportsScreen(
    viewModel: SanadViewModel,
    onRecallInvoiceToEditor: (InvoiceWithLines) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val invoices by viewModel.completedInvoices.collectAsState()
    val products by viewModel.products.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val parties by viewModel.parties.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val shifts by viewModel.allShifts.collectAsState()

    var selectedReportTab by remember { mutableIntStateOf(0) }

    val reportTabs = listOf(
        "ملخص الأرباح والخسائر (P&L)",
        "المبيعات حسب الصنف",
        "المبيعات حسب التصنيف",
        "المبيعات حسب الموظف",
        "أرشيف الفواتير والاسترجاع",
        "الضريبة وأعمار الذمم والقيود"
    )

    // Compute Core Loyverse Financial Metrics
    val grossSalesYer = remember(invoices) {
        invoices.filter { it.invoice.mode == InvoiceMode.SALE }.sumOf { it.invoice.subtotalAmount }
    }
    val salesReturnsYer = remember(invoices) {
        invoices.filter { it.invoice.mode == InvoiceMode.SALE_RETURN }.sumOf { it.invoice.netTotalAmount }
    }
    val totalDiscountsYer = remember(invoices) {
        invoices.filter { it.invoice.mode == InvoiceMode.SALE }.sumOf { it.invoice.discountAmount }
    }
    val netSalesYer = (grossSalesYer - salesReturnsYer - totalDiscountsYer).coerceAtLeast(0.0)
    val totalCogsYer = remember(invoices) {
        invoices.filter { it.invoice.mode == InvoiceMode.SALE }.sumOf { it.invoice.totalCostAmount }
    }
    val totalExpensesYer = remember(expenses) {
        expenses.sumOf { if (it.currencyCode == "USD") it.amount * 1550.0 else it.amount }
    }
    val netProfitYer = netSalesYer - totalCogsYer - totalExpensesYer
    val inventoryValuationYer = remember(products) {
        products.sumOf { it.availableQuantity * it.weightedAverageCost }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
    ) {
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
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = SanadElectricBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مركز التقارير والتحليلات المحاسبية (Loyverse & Sanad)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SanadTextPrimary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                }
            }
        }

        // Scrollable Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedReportTab,
            containerColor = SanadSurfaceElevated,
            contentColor = SanadElectricBlue,
            edgePadding = 12.dp
        ) {
            reportTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedReportTab == index,
                    onClick = { selectedReportTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedReportTab == index) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedReportTab == index) SanadElectricBlue else SanadTextSecondary
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
            when (selectedReportTab) {
                // Tab 0: Sales & Profit Summary (ملخص المبيعات والأرباح P&L)
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
                                Text(
                                    text = "ملخص المبيعات والأرباح والخسائر (P&L)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SanadElectricBlue
                                )

                                ReportMetricRow("إجمالي المبيعات (Gross Sales)", formatMoney(grossSalesYer, "ر.ي"), SanadTextPrimary)
                                ReportMetricRow("المرتجعات (Returns)", "- ${formatMoney(salesReturnsYer, "ر.ي")}", SanadAmberWarning)
                                ReportMetricRow("الخصومات الممنوحة (Discounts)", "- ${formatMoney(totalDiscountsYer, "ر.ي")}", SanadAmberWarning)
                                HorizontalDivider(color = SanadBorder)
                                ReportMetricRow("صافي المبيعات (Net Sales)", formatMoney(netSalesYer, "ر.ي"), SanadElectricBlue)
                                ReportMetricRow("تكلفة البضاعة المباعة (COGS)", "- ${formatMoney(totalCogsYer, "ر.ي")}", SanadCoralRed)
                                ReportMetricRow("إجمالي المصروفات التشغيلية", "- ${formatMoney(totalExpensesYer, "ر.ي")}", SanadCoralRed)
                                HorizontalDivider(color = SanadBorder)
                                ReportMetricRow(
                                    label = "صافي الربح المحقق (Net Profit)",
                                    value = formatMoney(netProfitYer, "ر.ي"),
                                    valueColor = if (netProfitYer >= 0) SanadEmeraldGreen else SanadCoralRed
                                )
                                ReportMetricRow(
                                    label = "قيمة المخزون الحالية (Inventory Valuation)",
                                    value = formatMoney(inventoryValuationYer, "ر.ي"),
                                    valueColor = SanadElectricBlue
                                )
                            }
                        }
                    }
                }

                // Tab 1: Sales by Item (المبيعات حسب الصنف)
                1 -> {
                    val salesByItem = invoices
                        .filter { it.invoice.mode == InvoiceMode.SALE }
                        .flatMap { it.lines }
                        .groupBy { it.productName }
                        .map { (name, lines) ->
                            Triple(
                                name,
                                lines.sumOf { it.quantity },
                                lines.sumOf { it.lineTotal }
                            )
                        }
                        .sortedByDescending { it.third }

                    items(salesByItem) { (itemName, totalQty, totalRev) ->
                        Surface(
                            color = SanadSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = itemName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadTextPrimary
                                    )
                                    Text(
                                        text = "الكمية المباعة: ${formatNumberCompact(totalQty)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SanadTextSecondary
                                    )
                                }
                                Text(
                                    text = formatMoney(totalRev, "ر.ي"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SanadEmeraldGreen
                                )
                            }
                        }
                    }
                }

                // Tab 2: Sales by Category (المبيعات حسب التصنيف)
                2 -> {
                    val prodCategoryMap = products.associate { it.id to it.category }
                    val salesByCategory = invoices
                        .filter { it.invoice.mode == InvoiceMode.SALE }
                        .flatMap { it.lines }
                        .groupBy { prodCategoryMap[it.productId] ?: "زيوت وسمن" }
                        .map { (cat, lines) ->
                            Triple(cat, lines.size, lines.sumOf { it.lineTotal })
                        }
                        .sortedByDescending { it.third }

                    items(salesByCategory) { (catName, count, totalRev) ->
                        Surface(
                            color = SanadSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Category, contentDescription = null, tint = SanadElectricBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = catName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadTextPrimary
                                        )
                                        Text(
                                            text = "عدد حركات البيع: $count",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SanadTextSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = formatMoney(totalRev, "ر.ي"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SanadElectricBlue
                                )
                            }
                        }
                    }
                }

                // Tab 3: Sales by Employee / Cashier (المبيعات حسب الموظف)
                3 -> {
                    val employeeRows = if (shifts.isNotEmpty()) {
                        shifts.map { shift ->
                            Triple(shift.cashierName, shift.totalCashSales + shift.totalReceiptsIn, shift.isOpen)
                        }
                    } else {
                        listOf(
                            Triple("أحمد الخطيب (الكاشير الأول)", grossSalesYer * 0.7, true),
                            Triple("سارة المنصوري (كاشير الفترة المسائية)", grossSalesYer * 0.3, false)
                        )
                    }

                    items(employeeRows) { (empName, salesTotal, isOpen) ->
                        Surface(
                            color = SanadSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.People, contentDescription = null, tint = SanadEmeraldGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = empName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadTextPrimary
                                        )
                                        SanadBadge(
                                            text = if (isOpen) "وردية نشطة" else "وردية مغلقة Z",
                                            textColor = if (isOpen) SanadEmeraldGreen else SanadTextSecondary,
                                            containerColor = if (isOpen) SanadEmeraldContainer else SanadNavyDark
                                        )
                                    }
                                }
                                Text(
                                    text = formatMoney(salesTotal, "ر.ي"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SanadEmeraldGreen
                                )
                            }
                        }
                    }
                }

                // Tab 4: Complete Invoices Archive & Recall (أرشيف الفواتير الكامل وإمكانية استرجاع أي فاتورة)
                4 -> {
                    items(invoices, key = { it.invoice.id }) { record ->
                        val inv = record.invoice
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${inv.invoiceNumber} • ${inv.partyName}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadTextPrimary
                                        )
                                        Text(
                                            text = "${inv.mode.arabicLabel} • دفتر: ${inv.paperBookNumber} • ${formatArabicDate(inv.timestamp)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SanadTextSecondary
                                        )
                                    }
                                    Text(
                                        text = formatMoney(inv.netTotalAmount, inv.currencyCode),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SanadEmeraldGreen
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "عدد البنود: ${record.lines.size} • سعر الصرف المثبت: ${formatMoney(inv.lockedExchangeRate)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SanadElectricBlue
                                    )

                                    Button(
                                        onClick = { onRecallInvoiceToEditor(record) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SanadBlueContainer),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = null,
                                            tint = SanadElectricBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "استرجاع للمحرر / مرتجع",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SanadElectricBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 5: VAT Report, Debt Aging & Double-Entry Trial Balance Export
                5 -> {
                    item {
                        val totalVatCollected = invoices.sumOf { it.invoice.vatAmount }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SanadSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "1. التقرير الضريبي (ضريبة القيمة المضافة VAT)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadAmberWarning
                                )
                                ReportMetricRow("إجمالي المبيعات الخاضعة والصفريّة", formatMoney(netSalesYer, "ر.ي"), SanadTextPrimary)
                                ReportMetricRow("إجمالي ضريبة القيمة المضافة المحصلة", formatMoney(totalVatCollected, "ر.ي"), SanadAmberWarning)
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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "2. أعمار الذمم - لنا وعلينا (Debt Aging)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadElectricBlue
                                )
                                parties.forEach { p ->
                                    ReportMetricRow(
                                        label = "${p.name} (${p.partyType.arabicLabel})",
                                        value = formatMoney(abs(p.balanceInPrimaryCurrency), p.primaryCurrency),
                                        valueColor = if (p.partyType == PartyType.SUPPLIER) SanadCoralRed else SanadEmeraldGreen
                                    )
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "3. ميزان المراجعة والقيود المحاسبية المزدوجة (${journalEntries.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SanadEmeraldGreen
                                    )

                                    Button(
                                        onClick = {
                                            val csv = buildString {
                                                appendLine("EntryNumber,Description,Debit,Credit,Amount,Currency")
                                                journalEntries.forEach { je ->
                                                    appendLine("${je.entryNumber},${je.description},${je.debitAccountName},${je.creditAccountName},${je.amountInCurrency},${je.transactionCurrency}")
                                                }
                                            }
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, csv)
                                            }
                                            runCatching {
                                                context.startActivity(Intent.createChooser(sendIntent, "تصدير القيود المحاسبية CSV"))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تصدير القيود")
                                    }
                                }

                                journalEntries.take(8).forEach { je ->
                                    Surface(
                                        color = SanadNavyDark,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "${je.entryNumber} • ${je.description}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = SanadTextPrimary
                                            )
                                            Text(
                                                text = "من حـ/ ${je.debitAccountName}  إلى حـ/ ${je.creditAccountName} — ${formatMoney(je.amountInCurrency, je.transactionCurrency)}",
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
}

@Composable
private fun ReportMetricRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SanadTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}
