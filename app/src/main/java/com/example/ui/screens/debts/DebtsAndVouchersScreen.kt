package com.example.ui.screens.debts

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.FinancialAccountEntity
import com.example.data.local.LedgerEntryEntity
import com.example.data.local.PartyEntity
import com.example.data.local.PartyType
import com.example.data.local.VoucherDirection
import com.example.ui.components.DirectNumpadDialog
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
import com.example.ui.theme.SanadSlateContainer
import com.example.ui.theme.SanadSlatePurple
import com.example.ui.theme.SanadSurface
import com.example.ui.theme.SanadSurfaceElevated
import com.example.ui.theme.SanadTextMuted
import com.example.ui.theme.SanadTextPrimary
import com.example.ui.theme.SanadTextSecondary
import com.example.ui.viewmodel.SanadViewModel
import kotlin.math.abs

@Composable
fun DebtsAndVouchersScreen(
    viewModel: SanadViewModel,
    externalOpenVoucherModal: Boolean = false,
    onDismissExternalVoucherModal: () -> Unit = {}
) {
    val context = LocalContext.current
    val parties by viewModel.parties.collectAsState()
    val currencies by viewModel.currencies.collectAsState()
    val accounts by viewModel.financialAccounts.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var overdueOnly by remember { mutableStateOf(false) }
    var selectedTypeFilter by remember { mutableStateOf<PartyType?>(null) }
    var showVoucherModal by remember { mutableStateOf(false) }
    var preselectedPartyForVoucher by remember { mutableStateOf<PartyEntity?>(null) }
    var statementParty by remember { mutableStateOf<PartyEntity?>(null) }

    val yerRate = currencies.find { it.code == "YER" }?.exchangeRateToBase ?: 1550.0
    val now = System.currentTimeMillis()

    // Calculate Summary Totals converted to YER for the 3 Header Cards:
    // 1. Coral Red [علينا (Payables)]
    // 2. Emerald Green [لنا (Receivables)]
    // 3. Slate [عمليات خاصة (Private)]
    val totalPayablesYer = remember(parties, yerRate) {
        parties.filter { it.partyType == PartyType.SUPPLIER || it.balanceInPrimaryCurrency < 0 }
            .sumOf { p ->
                val raw = abs(p.balanceInPrimaryCurrency)
                if (p.primaryCurrency == "USD") raw * yerRate else raw
            }
    }

    val totalReceivablesYer = remember(parties, yerRate) {
        parties.filter { it.partyType == PartyType.CUSTOMER && it.balanceInPrimaryCurrency > 0 }
            .sumOf { p ->
                if (p.primaryCurrency == "USD") p.balanceInPrimaryCurrency * yerRate else p.balanceInPrimaryCurrency
            }
    }

    val totalPrivateYer = remember(parties, yerRate) {
        parties.filter { it.partyType == PartyType.PRIVATE }
            .sumOf { p ->
                val raw = abs(p.balanceInPrimaryCurrency)
                if (p.primaryCurrency == "USD") raw * yerRate else raw
            }
    }

    val filteredParties = remember(parties, searchQuery, overdueOnly, selectedTypeFilter, now) {
        parties.filter { party ->
            val matchesSearch = searchQuery.isBlank() ||
                party.name.contains(searchQuery, ignoreCase = true) ||
                party.phone.contains(searchQuery, ignoreCase = true)
            val matchesOverdue = !overdueOnly || (party.dueDateTimestamp != null && party.dueDateTimestamp < now)
            val matchesType = selectedTypeFilter == null || party.partyType == selectedTypeFilter
            matchesSearch && matchesOverdue && matchesType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
    ) {
        // 1. Top Control Bar: Title + [خيار المتأخرات (Overdue Filter)] + [+] Voucher Button
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
                        text = "إدارة الديون والذمم والسندات",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SanadTextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // [خيار المتأخرات (Overdue Filter)]
                        Surface(
                            color = if (overdueOnly) SanadRedContainer else SanadSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (overdueOnly) SanadCoralRed else SanadBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { overdueOnly = !overdueOnly }
                                .testTag("overdue_filter_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = if (overdueOnly) SanadCoralRed else SanadAmberWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "خيار المتأخرات",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (overdueOnly) SanadCoralRed else SanadTextPrimary
                                )
                            }
                        }

                        // [+] Register Voucher Button
                        Button(
                            onClick = {
                                preselectedPartyForVoucher = null
                                showVoucherModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("open_voucher_modal_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "دفعة / قبضة",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // 2. 3 Summary Header Cards: Coral Red [علينا], Emerald Green [لنا], Slate [عمليات خاصة]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Coral Red: علينا (Payables)
                    SummaryDebtHeaderCard(
                        title = "علينا (للموردين)",
                        amountFormatted = formatMoney(totalPayablesYer, "ر.ي"),
                        accentColor = SanadCoralRed,
                        containerColor = SanadRedContainer,
                        isSelected = selectedTypeFilter == PartyType.SUPPLIER,
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == PartyType.SUPPLIER) null else PartyType.SUPPLIER
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Emerald Green: لنا (Receivables)
                    SummaryDebtHeaderCard(
                        title = "لنا (عند العملاء)",
                        amountFormatted = formatMoney(totalReceivablesYer, "ر.ي"),
                        accentColor = SanadEmeraldGreen,
                        containerColor = SanadEmeraldContainer,
                        isSelected = selectedTypeFilter == PartyType.CUSTOMER,
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == PartyType.CUSTOMER) null else PartyType.CUSTOMER
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Slate: عمليات خاصة (Private)
                    SummaryDebtHeaderCard(
                        title = "عمليات خاصة",
                        amountFormatted = formatMoney(totalPrivateYer, "ر.ي"),
                        accentColor = SanadSlatePurple,
                        containerColor = SanadSlateContainer,
                        isSelected = selectedTypeFilter == PartyType.PRIVATE,
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == PartyType.PRIVATE) null else PartyType.PRIVATE
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live Exchange Rate Bar
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
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "سعر الصرف المباشر للذمم: 1 $ = ${formatMoney(yerRate, "ر.ي")}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanadEmeraldGreen
                        )
                        Text(
                            text = "إجمالي الحسابات: ${filteredParties.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SanadTextSecondary
                        )
                    }
                }

                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث باسم العميل أو المورد أو رقم الهاتف...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SanadTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SanadElectricBlue)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SanadElectricBlue,
                        unfocusedBorderColor = SanadBorder,
                        focusedContainerColor = SanadNavyDark,
                        unfocusedContainerColor = SanadNavyDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HorizontalDivider(color = SanadBorder)

        // 3. Party Cards List with Account Statement (كشف حساب) & WhatsApp Share
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredParties, key = { it.id }) { party ->
                val isReceivable = party.partyType == PartyType.CUSTOMER ||
                    (party.partyType == PartyType.PRIVATE && party.balanceInPrimaryCurrency >= 0)
                val isOverdue = party.dueDateTimestamp != null && party.dueDateTimestamp < now
                val sym = if (party.primaryCurrency == "USD") "$" else "ر.ي"

                val badgeColor = when (party.partyType) {
                    PartyType.CUSTOMER -> SanadEmeraldGreen
                    PartyType.SUPPLIER -> SanadCoralRed
                    PartyType.PRIVATE -> SanadSlatePurple
                }
                val badgeBg = when (party.partyType) {
                    PartyType.CUSTOMER -> SanadEmeraldContainer
                    PartyType.SUPPLIER -> SanadRedContainer
                    PartyType.PRIVATE -> SanadSlateContainer
                }

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
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = party.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SanadTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SanadBadge(
                                        text = party.partyType.arabicLabel,
                                        textColor = badgeColor,
                                        containerColor = badgeBg
                                    )
                                    if (isOverdue) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SanadBadge(
                                            text = "متأخر السداد",
                                            textColor = SanadCoralRed,
                                            containerColor = SanadRedContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "هاتف: ${party.phone} • ${party.address.ifBlank { "حساب معتمد" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SanadTextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isReceivable) "لنا" else "علينا",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor
                                )
                                Text(
                                    text = formatMoney(abs(party.balanceInPrimaryCurrency), sym),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = badgeColor
                                )
                            }
                        }

                        HorizontalDivider(color = SanadBorder.copy(alpha = 0.6f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // [تسجيل دفعة / قبضة]
                            Surface(
                                color = SanadBlueContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, SanadElectricBlue, RoundedCornerShape(8.dp))
                                    .clickable {
                                        preselectedPartyForVoucher = party
                                        showVoucherModal = true
                                    }
                            ) {
                                Text(
                                    text = "تسجيل دفعة / قبضة",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SanadElectricBlue,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // [كشف حساب]
                            Surface(
                                color = SanadNavyDark,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .border(1.dp, SanadBorder, RoundedCornerShape(8.dp))
                                    .clickable { statementParty = party }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = SanadTextPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "كشف حساب",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SanadTextPrimary
                                    )
                                }
                            }

                            // [مشاركة واتساب]
                            Surface(
                                color = SanadEmeraldContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .border(1.dp, SanadEmeraldGreen, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val msg = "تحية طيبة من نظام سند للمحاسبة إلى (${party.name})، رصيد حسابكم الحالي هو ${
                                            formatMoney(abs(party.balanceInPrimaryCurrency), sym)
                                        }."
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, msg)
                                        }
                                        runCatching {
                                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب عبر واتساب"))
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = SanadEmeraldGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "واتساب",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SanadEmeraldGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Unified Voucher Modal ("تسجيل دفعة / قبضة")
    if (showVoucherModal || externalOpenVoucherModal) {
        UnifiedVoucherModal(
            initialParty = preselectedPartyForVoucher,
            parties = parties,
            accounts = accounts,
            defaultYerRate = yerRate,
            onDismiss = {
                showVoucherModal = false
                preselectedPartyForVoucher = null
                onDismissExternalVoucherModal()
            },
            onSaveVoucher = { pType, pId, pName, direction, curr, lockedRate, amount, accId, accName, notes ->
                viewModel.registerVoucher(
                    partyType = pType,
                    partyId = pId,
                    partyName = pName,
                    direction = direction,
                    currencyCode = curr,
                    lockedRate = lockedRate,
                    amount = amount,
                    accountId = accId,
                    accountName = accName,
                    notes = notes
                )
                showVoucherModal = false
                preselectedPartyForVoucher = null
                onDismissExternalVoucherModal()
            },
            onQuickCreateParty = { name, phone, type, curr ->
                viewModel.quickCreateParty(name, phone, type, curr)
            }
        )
    }

    // Account Statement Modal (كشف حساب تفصيلي)
    statementParty?.let { target ->
        PartyAccountStatementDialog(
            party = target,
            vouchers = vouchers.filter { it.partyId == target.id },
            onDismiss = { statementParty = null }
        )
    }
}

@Composable
private fun SummaryDebtHeaderCard(
    title: String,
    amountFormatted: String,
    accentColor: Color,
    containerColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = accentColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1
            )
            Text(
                text = amountFormatted,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = SanadTextPrimary,
                maxLines = 1
            )
        }
    }
}

/**
 * Unified Voucher Modal ("تسجيل دفعة / قبضة") matching the exact specification:
 * - Account Type selector: [خاص | عميل | مورد]
 * - Party search / auto-create field
 * - Direction selector: Red-bordered [دفعة - يخرج من الصندوق (Payment Out)] vs Green-bordered [قبضة - يدخل إلى الصندوق (Receipt In)]
 * - Currency & Rate Lock notice: "يثبت سعر الصرف على سند الدفعة/القبضة ولا يتغير بعد التسجيل"
 * - Amount with built-in calculator (DirectNumpadDialog)
 * - Cash Box selector, Date, Notes, and [حفظ السند]
 */
@Composable
fun UnifiedVoucherModal(
    initialParty: PartyEntity?,
    parties: List<PartyEntity>,
    accounts: List<FinancialAccountEntity>,
    defaultYerRate: Double,
    onDismiss: () -> Unit,
    onSaveVoucher: (
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
    ) -> Unit,
    onQuickCreateParty: (String, String, PartyType, String) -> Unit
) {
    var selectedPartyType by remember {
        mutableStateOf(initialParty?.partyType ?: PartyType.CUSTOMER)
    }
    val matchingParties = remember(parties, selectedPartyType) {
        parties.filter { it.partyType == selectedPartyType }
    }
    var selectedParty by remember(selectedPartyType, initialParty) {
        mutableStateOf(
            if (initialParty != null && initialParty.partyType == selectedPartyType) {
                initialParty
            } else {
                matchingParties.firstOrNull()
            }
        )
    }
    var direction by remember {
        mutableStateOf(
            if (selectedPartyType == PartyType.SUPPLIER) VoucherDirection.PAYMENT_OUT else VoucherDirection.RECEIPT_IN
        )
    }
    var currencyCode by remember { mutableStateOf(selectedParty?.primaryCurrency ?: "YER") }
    var lockedRate by remember {
        mutableStateOf(if (currencyCode == "USD") 1.0 else defaultYerRate)
    }
    var amountText by remember { mutableStateOf("155000") }
    var showBuiltInCalculator by remember { mutableStateOf(false) }

    val matchingAccounts = remember(accounts, currencyCode) {
        accounts.filter { it.currencyCode == currencyCode }.ifEmpty { accounts }
    }
    var selectedAccount by remember(matchingAccounts) {
        mutableStateOf(matchingAccounts.firstOrNull())
    }
    var notes by remember { mutableStateOf("") }
    var customPartyNameInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = SanadNavyDark,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
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
                        Text(
                            text = "تسجيل دفعة / قبضة",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
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
                    // 1. Account Type Selector: [خاص | عميل | مورد]
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
                                text = "نوع الحساب:",
                                style = MaterialTheme.typography.labelLarge,
                                color = SanadTextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(PartyType.PRIVATE, PartyType.CUSTOMER, PartyType.SUPPLIER).forEach { pType ->
                                    val selected = selectedPartyType == pType
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
                                            .clickable {
                                                selectedPartyType = pType
                                                direction = if (pType == PartyType.SUPPLIER) {
                                                    VoucherDirection.PAYMENT_OUT
                                                } else {
                                                    VoucherDirection.RECEIPT_IN
                                                }
                                            }
                                            .testTag("voucher_party_type_${pType.name}")
                                    ) {
                                        Text(
                                            text = pType.arabicLabel,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (selected) Color.White else SanadTextSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            // Party Chips + Auto-Create Field
                            Text(
                                text = "اختر صاحب الحساب أو اكتب اسماً جديداً للإنشاء التلقائي:",
                                style = MaterialTheme.typography.labelMedium,
                                color = SanadTextSecondary
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                matchingParties.forEach { party ->
                                    val isSel = selectedParty?.id == party.id && customPartyNameInput.isBlank()
                                    Surface(
                                        color = if (isSel) SanadBlueContainer else SanadNavyDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                if (isSel) SanadElectricBlue else SanadBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                selectedParty = party
                                                customPartyNameInput = ""
                                            }
                                    ) {
                                        Text(
                                            text = party.name,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSel) SanadElectricBlue else SanadTextPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customPartyNameInput,
                                onValueChange = { customPartyNameInput = it },
                                label = { Text("أو اكتب اسم حساب جديد للإنشاء الفوري...") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = SanadElectricBlue)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 2. Direction Selector: Red-bordered [دفعة - يخرج من الصندوق] vs Green-bordered [قبضة - يدخل إلى الصندوق]
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
                                text = "اتجاه الحركة المالية:",
                                style = MaterialTheme.typography.labelLarge,
                                color = SanadTextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Red-bordered [دفعة - يخرج من الصندوق]
                                val isOut = direction == VoucherDirection.PAYMENT_OUT
                                Surface(
                                    color = if (isOut) SanadRedContainer else SanadNavyDark,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = if (isOut) 2.dp else 1.dp,
                                            color = if (isOut) SanadCoralRed else SanadBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { direction = VoucherDirection.PAYMENT_OUT }
                                        .testTag("voucher_direction_payment_out")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = SanadCoralRed
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "دفعة (سند صرف)",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadCoralRed
                                        )
                                        Text(
                                            text = "يخرج من الصندوق",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SanadTextSecondary
                                        )
                                    }
                                }

                                // Green-bordered [قبضة - يدخل إلى الصندوق]
                                val isIn = direction == VoucherDirection.RECEIPT_IN
                                Surface(
                                    color = if (isIn) SanadEmeraldContainer else SanadNavyDark,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = if (isIn) 2.dp else 1.dp,
                                            color = if (isIn) SanadEmeraldGreen else SanadBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { direction = VoucherDirection.RECEIPT_IN }
                                        .testTag("voucher_direction_receipt_in")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = SanadEmeraldGreen
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "قبضة (سند قبض)",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadEmeraldGreen
                                        )
                                        Text(
                                            text = "يدخل إلى الصندوق",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SanadTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Currency & Rate Lock Notice + Amount with Built-in Calculator
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
                                listOf("YER", "USD", "SAR", "TRY").forEach { curr ->
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
                                                    "SAR" -> 3.75
                                                    "TRY" -> 34.50
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

                            // Rate Lock Notice Banner
                            Surface(
                                color = SanadBlueContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = SanadElectricBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "يثبت سعر الصرف على سند الدفعة/القبضة ولا يتغير بعد التسجيل (1 $ = ${formatMoney(lockedRate)})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SanadTextPrimary
                                    )
                                }
                            }

                            // Amount Field with Built-in Calculator Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = it },
                                    label = { Text("المبلغ ($currencyCode)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("voucher_amount_input")
                                )

                                Surface(
                                    color = SanadEmeraldContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .size(54.dp)
                                        .border(1.dp, SanadEmeraldGreen, RoundedCornerShape(12.dp))
                                        .clickable { showBuiltInCalculator = true }
                                        .testTag("voucher_calculator_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = "الآلة الحاسبة المدمجة",
                                            tint = SanadEmeraldGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Cash Box Selector, Date, and Notes
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
                                text = "الصندوق النقدي / الحساب المالي:",
                                style = MaterialTheme.typography.labelLarge,
                                color = SanadTextSecondary
                            )

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
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = if (isSel) SanadEmeraldGreen else SanadTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSel) SanadEmeraldGreen else SanadTextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = formatShortDate(System.currentTimeMillis()),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("تاريخ السند") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("البيان / الملاحظات...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Bottom [حفظ السند] Button
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
                                val finalPartyName = if (customPartyNameInput.isNotBlank()) {
                                    onQuickCreateParty(customPartyNameInput, "", selectedPartyType, currencyCode)
                                    customPartyNameInput.trim()
                                } else {
                                    selectedParty?.name ?: "أحمد الخطيب"
                                }
                                val finalPartyId = selectedParty?.id ?: 1L
                                val acc = selectedAccount
                                onSaveVoucher(
                                    selectedPartyType,
                                    finalPartyId,
                                    finalPartyName,
                                    direction,
                                    currencyCode,
                                    lockedRate,
                                    amountText.toDoubleOrNull() ?: 0.0,
                                    acc?.id ?: 1L,
                                    acc?.name ?: "الصندوق النقدي الرئيسي",
                                    notes.ifBlank { direction.arabicLabel }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (direction == VoucherDirection.RECEIPT_IN) SanadEmeraldGreen else SanadCoralRed
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp)
                                .testTag("save_voucher_submit_button")
                        ) {
                            Text(
                                text = "حفظ السند",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBuiltInCalculator) {
        DirectNumpadDialog(
            title = "حاسبة مبلغ السند",
            subtitle = direction.arabicLabel,
            initialValue = amountText.toDoubleOrNull() ?: 0.0,
            currencySymbol = currencyCode,
            onDismiss = { showBuiltInCalculator = false },
            onConfirm = { newVal ->
                amountText = formatNumberCompact(newVal)
                showBuiltInCalculator = false
            }
        )
    }
}

@Composable
private fun PartyAccountStatementDialog(
    party: PartyEntity,
    vouchers: List<LedgerEntryEntity>,
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
                    Column {
                        Text(
                            text = "كشف حساب تفصيلي: ${party.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                        Text(
                            text = "الرصيد الحالي: ${formatMoney(abs(party.balanceInPrimaryCurrency), party.primaryCurrency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SanadElectricBlue
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (vouchers.isEmpty()) {
                    Text(
                        text = "لا توجد سندات مسجلة لهذا الحساب بعد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SanadTextSecondary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vouchers, key = { it.id }) { vch ->
                            val isReceipt = vch.direction == VoucherDirection.RECEIPT_IN
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
                                            text = "${vch.voucherNumber} • ${vch.direction.arabicLabel}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadTextPrimary
                                        )
                                        Text(
                                            text = "${vch.notes} • ${formatArabicDate(vch.timestamp)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SanadTextSecondary
                                        )
                                    }
                                    Text(
                                        text = formatMoney(vch.amount, vch.currencyCode),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isReceipt) SanadEmeraldGreen else SanadCoralRed
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
