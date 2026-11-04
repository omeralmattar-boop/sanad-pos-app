package com.example.ui.employees

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.SanadBadge
import com.example.ui.theme.SanadBlueContainer
import com.example.ui.theme.SanadBorder
import com.example.ui.theme.SanadElectricBlue
import com.example.ui.theme.SanadEmeraldContainer
import com.example.ui.theme.SanadEmeraldGreen
import com.example.ui.theme.SanadNavyDark
import com.example.ui.theme.SanadSurface
import com.example.ui.theme.SanadSurfaceElevated
import com.example.ui.theme.SanadTextPrimary
import com.example.ui.theme.SanadTextSecondary

data class EmployeeRoleAccess(
    val id: Long,
    val name: String,
    val roleTitle: String,
    val pinCode: String,
    val allowGrantDiscount: Boolean,
    val allowDeleteInvoiceItems: Boolean,
    val allowViewCostAndProfits: Boolean,
    val allowProcessReturns: Boolean,
    val allowPurchaseCreate: Boolean = true,        // purchase.create
    val allowInventoryEditProduct: Boolean = true,  // inventory.edit_product
    val allowTreasuryManage: Boolean = true         // treasury.manage
) {
    fun hasPermission(permissionCode: String): Boolean {
        return when (permissionCode) {
            "purchase.create" -> allowPurchaseCreate
            "inventory.edit_product" -> allowInventoryEditProduct
            "treasury.manage" -> allowTreasuryManage
            else -> true
        }
    }
}

@Composable
fun EmployeesAccessScreen(
    onClose: () -> Unit
) {
    val employees = remember {
        mutableStateListOf(
            EmployeeRoleAccess(
                id = 1L,
                name = "المشرف العام (المدير)",
                roleTitle = "مدير النظام",
                pinCode = "1234",
                allowGrantDiscount = true,
                allowDeleteInvoiceItems = true,
                allowViewCostAndProfits = true,
                allowProcessReturns = true,
                allowPurchaseCreate = true,
                allowInventoryEditProduct = true,
                allowTreasuryManage = true
            ),
            EmployeeRoleAccess(
                id = 2L,
                name = "أحمد الخطيب (الكاشير الأول)",
                roleTitle = "كاشير مبيعات ومشتريات",
                pinCode = "2580",
                allowGrantDiscount = true,
                allowDeleteInvoiceItems = false,
                allowViewCostAndProfits = false,
                allowProcessReturns = true,
                allowPurchaseCreate = true,
                allowInventoryEditProduct = true,
                allowTreasuryManage = false
            ),
            EmployeeRoleAccess(
                id = 3L,
                name = "سارة المنصوري (كاشير مسائي)",
                roleTitle = "كاشير نقطة بيع",
                pinCode = "4455",
                allowGrantDiscount = false,
                allowDeleteInvoiceItems = false,
                allowViewCostAndProfits = false,
                allowProcessReturns = false,
                allowPurchaseCreate = false,
                allowInventoryEditProduct = false,
                allowTreasuryManage = false
            )
        )
    }

    var showAddEmployeeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
    ) {
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
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = SanadElectricBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إدارة الموظفين والصلاحيات (PIN & RBAC)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SanadTextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { showAddEmployeeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SanadElectricBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_employee_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة كاشير")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                    }
                }
            }
        }

        HorizontalDivider(color = SanadBorder)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(employees, key = { it.id }) { emp ->
                val index = employees.indexOfFirst { it.id == emp.id }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SanadElectricBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = emp.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SanadTextPrimary
                                    )
                                    Text(
                                        text = emp.roleTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SanadTextSecondary
                                    )
                                }
                            }

                            SanadBadge(
                                text = "PIN: ${emp.pinCode}",
                                textColor = SanadEmeraldGreen,
                                containerColor = SanadEmeraldContainer
                            )
                        }

                        HorizontalDivider(color = SanadBorder.copy(alpha = 0.6f))

                        // Core System Permission Codes required for OCR / Purchase / Inventory / Treasury
                        PermissionToggleRow(
                            label = "إنشاء واعتماد فواتير المشتريات واستيراد OCR",
                            codeBadge = "purchase.create",
                            checked = emp.allowPurchaseCreate,
                            onCheckedChange = { checked ->
                                if (index >= 0) employees[index] = emp.copy(allowPurchaseCreate = checked)
                            }
                        )
                        PermissionToggleRow(
                            label = "إنشاء وتعديل بطاقات الأصناف والأسعار في المخزون",
                            codeBadge = "inventory.edit_product",
                            checked = emp.allowInventoryEditProduct,
                            onCheckedChange = { checked ->
                                if (index >= 0) employees[index] = emp.copy(allowInventoryEditProduct = checked)
                            }
                        )
                        PermissionToggleRow(
                            label = "إدارة الصناديق النقدية والخزينة والسداد النقدي",
                            codeBadge = "treasury.manage",
                            checked = emp.allowTreasuryManage,
                            onCheckedChange = { checked ->
                                if (index >= 0) employees[index] = emp.copy(allowTreasuryManage = checked)
                            }
                        )

                        HorizontalDivider(color = SanadBorder.copy(alpha = 0.4f))

                        PermissionToggleRow(
                            label = "السماح بمنح خصم على الفواتير",
                            checked = emp.allowGrantDiscount,
                            onCheckedChange = { checked ->
                                if (index >= 0) employees[index] = emp.copy(allowGrantDiscount = checked)
                            }
                        )
                        PermissionToggleRow(
                            label = "السماح بحذف بنود الفاتورة أثناء البيع",
                            checked = emp.allowDeleteInvoiceItems,
                            onCheckedChange = { checked ->
                                if (index >= 0) employees[index] = emp.copy(allowDeleteInvoiceItems = checked)
                            }
                        )
                        PermissionToggleRow(
                            label = "السماح برؤية تكلفة الشراء والأرباح",
                            checked = emp.allowViewCostAndProfits,
                            onCheckedChange = { checked ->
                                if (index >= 0) employees[index] = emp.copy(allowViewCostAndProfits = checked)
                            }
                        )
                        PermissionToggleRow(
                            label = "السماح بعمل مرتجع مبيعات أو مشتريات",
                            checked = emp.allowProcessReturns,
                            onCheckedChange = { checked ->
                                if (index >= 0) employees[index] = emp.copy(allowProcessReturns = checked)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddEmployeeDialog) {
        var name by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("كاشير مبيعات") }
        var pin by remember { mutableStateOf("1122") }

        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            containerColor = SanadSurface,
            title = { Text("إضافة موظف / كاشير جديد", color = SanadTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الموظف / الكاشير") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("المسمى الوظيفي") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.take(6) },
                        label = { Text("رمز الدخول السريع (PIN)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            employees.add(
                                EmployeeRoleAccess(
                                    id = System.currentTimeMillis(),
                                    name = name.trim(),
                                    roleTitle = role.trim(),
                                    pinCode = pin.ifBlank { "0000" },
                                    allowGrantDiscount = true,
                                    allowDeleteInvoiceItems = false,
                                    allowViewCostAndProfits = false,
                                    allowProcessReturns = true,
                                    allowPurchaseCreate = true,
                                    allowInventoryEditProduct = true,
                                    allowTreasuryManage = false
                                )
                            )
                            showAddEmployeeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SanadEmeraldGreen)
                ) {
                    Text("حفظ الموظف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmployeeDialog = false }) {
                    Text("إلغاء", color = SanadTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PermissionToggleRow(
    label: String,
    codeBadge: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = SanadTextPrimary
            )
            if (codeBadge != null) {
                Spacer(modifier = Modifier.height(2.dp))
                SanadBadge(
                    text = codeBadge,
                    textColor = SanadElectricBlue,
                    containerColor = SanadBlueContainer
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = SanadEmeraldGreen)
        )
    }
}
