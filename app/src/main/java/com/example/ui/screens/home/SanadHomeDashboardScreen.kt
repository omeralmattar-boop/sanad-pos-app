package com.example.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.InvoiceMode
import com.example.ui.components.SanadBadge
import com.example.ui.components.formatMoney
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
import com.example.ui.theme.SanadSlatePurple
import com.example.ui.theme.SanadSurface
import com.example.ui.theme.SanadTextPrimary
import com.example.ui.theme.SanadTextSecondary
import com.example.ui.viewmodel.SanadViewModel

enum class QuickDashboardDestination {
    NEW_SALES_INVOICE,
    NEW_PURCHASE_INVOICE,
    NEW_VOUCHER,
    NEW_EXPENSE,
    INVENTORY_LIST,
    B2B_IMPORT_WIZARD,
    AI_OCR_SCANNER,
    DEBTS_SCREEN,
    SHIFTS_Z_REPORT,
    TREASURY_ACCOUNTS,
    LOYVERSE_REPORTS,
    EMPLOYEES_ACCESS,
    AI_VOICE_CHAT
}

private data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val accent: Color,
    val destination: QuickDashboardDestination
)

@Composable
fun SanadHomeDashboardScreen(
    viewModel: SanadViewModel,
    showHomeOcrShortcut: Boolean = true,
    onQuickNavigate: (QuickDashboardDestination) -> Unit
) {
    val invoices by viewModel.completedInvoices.collectAsState()
    val products by viewModel.products.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val currencies by viewModel.currencies.collectAsState()
    val heldInvoices by viewModel.heldInvoices.collectAsState()

    val yerRate = currencies.find { it.code == "YER" }?.exchangeRateToBase ?: 1550.0
    val now = System.currentTimeMillis()

    val totalSalesYer = remember(invoices) {
        invoices.filter { it.invoice.mode == InvoiceMode.SALE }.sumOf { it.invoice.netTotalAmount }
    }
    val totalCogsYer = remember(invoices) {
        invoices.filter { it.invoice.mode == InvoiceMode.SALE }.sumOf { it.invoice.totalCostAmount }
    }
    val totalExpensesYer = remember(expenses, yerRate) {
        expenses.sumOf { if (it.currencyCode == "USD") it.amount * yerRate else it.amount }
    }
    val netProfitYer = totalSalesYer - totalCogsYer - totalExpensesYer

    val expiredCount = remember(products, now) {
        products.count { it.nearestExpiryTimestamp != null && it.nearestExpiryTimestamp < now }
    }
    val lowStockCount = remember(products) {
        products.count { it.availableQuantity <= it.safetyThreshold }
    }
    val inventoryCostValuation = remember(products) {
        products.sumOf { it.availableQuantity * it.weightedAverageCost }
    }

    val quickActions = remember(showHomeOcrShortcut) {
        buildList {
            add(QuickActionItem("فاتورة بيع", Icons.Default.PointOfSale, SanadElectricBlue, QuickDashboardDestination.NEW_SALES_INVOICE))
            add(QuickActionItem("فاتورة شراء", Icons.Default.AddShoppingCart, SanadEmeraldGreen, QuickDashboardDestination.NEW_PURCHASE_INVOICE))
            add(QuickActionItem("دفعة / قبضة", Icons.Default.AccountBalance, SanadAmberWarning, QuickDashboardDestination.NEW_VOUCHER))
            add(QuickActionItem("مصروف جديد", Icons.Default.Payments, SanadCoralRed, QuickDashboardDestination.NEW_EXPENSE))
            add(QuickActionItem("المخزون والجرد", Icons.Default.Inventory2, SanadElectricBlue, QuickDashboardDestination.INVENTORY_LIST))
            add(QuickActionItem("استيراد مورد B2B", Icons.Default.CloudUpload, SanadEmeraldGreen, QuickDashboardDestination.B2B_IMPORT_WIZARD))
            if (showHomeOcrShortcut) {
                add(QuickActionItem("تصوير فاتورة", Icons.Default.AutoAwesome, SanadAmberWarning, QuickDashboardDestination.AI_OCR_SCANNER))
            }
            add(QuickActionItem("الديون وكشف حساب", Icons.Default.ReceiptLong, SanadCoralRed, QuickDashboardDestination.DEBTS_SCREEN))
            add(QuickActionItem("الورديات (Z-Report)", Icons.Default.Schedule, SanadElectricBlue, QuickDashboardDestination.SHIFTS_Z_REPORT))
            add(QuickActionItem("الصناديق والخزينة", Icons.Default.AccountBalanceWallet, SanadEmeraldGreen, QuickDashboardDestination.TREASURY_ACCOUNTS))
            add(QuickActionItem("التقارير والمحاسبة", Icons.Default.Assessment, SanadAmberWarning, QuickDashboardDestination.LOYVERSE_REPORTS))
            add(QuickActionItem("الموظفون و PIN", Icons.Default.AdminPanelSettings, SanadSlatePurple, QuickDashboardDestination.EMPLOYEES_ACCESS))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Prominent Unified AI Voice Command Button ([الأمر الصوتي الشامل بالذكاء الاصطناعي 🎙️])
        item {
            Button(
                onClick = { onQuickNavigate(QuickDashboardDestination.AI_VOICE_CHAT) },
                colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_ai_voice_command_button")
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "الأمر الصوتي الشامل بالذكاء الاصطناعي 🎙️",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // 2. 12 Quick Actions Grid (3 columns x 4 rows)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SanadSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SanadBorder, RoundedCornerShape(16.dp))
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
                            text = "الوصول السريع لكافة الأنظمة (12 وحدة)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                        SanadBadge(
                            text = "1 $ = ${formatMoney(yerRate, "ر.ي")}",
                            textColor = SanadEmeraldGreen,
                            containerColor = SanadEmeraldContainer
                        )
                    }

                    quickActions.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { action ->
                                Surface(
                                    color = SanadNavyDark,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, action.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable { onQuickNavigate(action.destination) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = action.icon,
                                            contentDescription = null,
                                            tint = action.accent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = action.title,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SanadTextPrimary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Last 7 Days Sales Chart Card (مبيعات آخر 7 أيام)
        item {
            val recentTotals = remember(invoices) {
                val list = invoices.filter { it.invoice.mode == InvoiceMode.SALE }
                    .take(7)
                    .map { it.invoice.netTotalAmount }
                    .reversed()
                if (list.isEmpty()) listOf(28000.0, 42000.0, 35000.0, 58000.0, 49000.0, 64000.0, 72000.0) else list
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SanadSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SanadBorder, RoundedCornerShape(16.dp))
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
                            text = "مبيعات آخر 7 أيام",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanadElectricBlue
                        )
                        Text(
                            text = formatMoney(totalSalesYer, "ر.ي"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanadEmeraldGreen
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(top = 8.dp)
                    ) {
                        val maxVal = (recentTotals.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                        val count = recentTotals.size.coerceAtLeast(1)
                        val slotWidth = size.width / count
                        val barWidth = slotWidth * 0.55f

                        recentTotals.forEachIndexed { idx, value ->
                            val ratio = (value / maxVal).toFloat().coerceIn(0.12f, 1f)
                            val barHeight = size.height * ratio
                            val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                            drawRoundRect(
                                color = if (idx == count - 1) Color(0xFF10B981) else Color(0xFF589BF7),
                                topLeft = Offset(left, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }
                }
            }
        }

        // 4. Profit & Loss Card (بطاقة الأرباح والخسائر) + Inventory Analysis Card (تحليل المخزون)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // الأرباح والخسائر
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanadSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        .clickable { onQuickNavigate(QuickDashboardDestination.LOYVERSE_REPORTS) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "الأرباح والخسائر",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SanadEmeraldGreen
                        )
                        Text(
                            text = "صافي الربح: ${formatMoney(netProfitYer, "ر.ي")}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                        Text(
                            text = "المصاريف: ${formatMoney(totalExpensesYer, "ر.ي")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SanadCoralRed
                        )
                        Text(
                            text = "فواتير معلقة: ${heldInvoices.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SanadAmberWarning
                        )
                    }
                }

                // تحليل المخزون
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanadSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SanadBorder, RoundedCornerShape(14.dp))
                        .clickable { onQuickNavigate(QuickDashboardDestination.INVENTORY_LIST) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "تحليل المخزون",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SanadElectricBlue
                        )
                        Text(
                            text = "القيمة: ${formatMoney(inventoryCostValuation, "ر.ي")}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                        Text(
                            text = "منتهي الصلاحية: $expiredCount صنف",
                            style = MaterialTheme.typography.labelSmall,
                            color = SanadCoralRed
                        )
                        Text(
                            text = "تحت حد الأمان: $lowStockCount صنف",
                            style = MaterialTheme.typography.labelSmall,
                            color = SanadAmberWarning
                        )
                    }
                }
            }
        }
    }
}
