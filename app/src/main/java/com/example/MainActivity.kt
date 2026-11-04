package com.example

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.InvoiceMode
import com.example.data.preferences.SanadAppSettings
import com.example.data.preferences.SanadPreferencesRepository
import com.example.ui.ai.GeminiVoiceChatScreen
import com.example.ui.employees.EmployeesAccessScreen
import com.example.ui.guide.UserGuideScreen
import com.example.ui.reports.LoyverseReportsScreen
import com.example.ui.screens.b2b.AiSupplierInvoiceScannerScreen
import com.example.ui.screens.b2b.B2BSupplierImportWizardScreen
import com.example.ui.screens.debts.DebtsAndVouchersScreen
import com.example.ui.screens.expenses.AnalyticalShiftsAndTreasuryScreen
import com.example.ui.screens.expenses.MultiCurrencyExpensesScreen
import com.example.ui.screens.home.SanadHomeDashboardScreen
import com.example.ui.screens.inventory.InventoryManagementScreen
import com.example.ui.screens.invoices.UnifiedInvoiceEditorScreen
import com.example.ui.settings.ExtendedSettingsScreen
import com.example.ui.theme.SanadAmberWarning
import com.example.ui.theme.SanadBorder
import com.example.ui.theme.SanadCoralRed
import com.example.ui.theme.SanadElectricBlue
import com.example.ui.theme.SanadEmeraldGreen
import com.example.ui.theme.SanadNavyDark
import com.example.ui.theme.SanadSurface
import com.example.ui.theme.SanadSurfaceElevated
import com.example.ui.theme.SanadTextPrimary
import com.example.ui.theme.SanadTextSecondary
import com.example.ui.theme.SanadTheme
import com.example.ui.viewmodel.SanadViewModel
import kotlinx.coroutines.launch

enum class OverlaySubScreen {
    NONE,
    GEMINI_VOICE_CHAT,
    LOYVERSE_REPORTS,
    EMPLOYEES_PERMISSIONS,
    B2B_IMPORT_WIZARD,
    AI_OCR_SCANNER,
    SHIFTS_MANAGEMENT,
    TREASURY_ACCOUNTS,
    USER_GUIDE
}

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // CRITICAL: MUST use commit() instead of apply() so the secret is written
                // synchronously before DebugAppCheckProviderFactory reads it.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        val sharedExternalLabel = when (intent?.action) {
            Intent.ACTION_SEND, Intent.ACTION_VIEW -> {
                intent?.data?.lastPathSegment ?: intent?.type ?: "application/pdf"
            }
            else -> null
        }
        setContent {
            SanadTheme(forceRtl = true, immersiveFullScreen = true) {
                SanadMainShell(initialSharedFileLabel = sharedExternalLabel)
            }
        }
    }
}

@Composable
fun SanadMainShell(
    viewModel: SanadViewModel = viewModel(),
    initialSharedFileLabel: String? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val prefsRepo = remember { SanadPreferencesRepository(context) }
    val appSettings by prefsRepo.settingsFlow.collectAsState(initial = SanadAppSettings())

    // 6 Main Bottom Navigation / Rail Tabs:
    // 0: الرئيسية | 1: المخزون | 2: الفواتير | 3: الديون | 4: المصاريف | 5: الإعدادات
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeOverlay by remember {
        mutableStateOf(
            if (initialSharedFileLabel != null) OverlaySubScreen.AI_OCR_SCANNER else OverlaySubScreen.NONE
        )
    }
    var guideTopicKey by remember { mutableStateOf<String?>(null) }
    var openNewProductModalFromExternal by remember { mutableStateOf(false) }

    val toastMessage by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToastMessage()
        }
    }

    // Handle system back press when inside an overlay sub-screen or secondary tab
    BackHandler(enabled = activeOverlay != OverlaySubScreen.NONE || selectedTab != 0) {
        if (activeOverlay != OverlaySubScreen.NONE) {
            activeOverlay = OverlaySubScreen.NONE
        } else {
            selectedTab = 0
        }
    }

    val navTabs = listOf(
        Triple(0, "الرئيسية", Icons.Default.Home),
        Triple(1, "المخزون", Icons.Default.Inventory2),
        Triple(2, "الفواتير", Icons.Default.ReceiptLong),
        Triple(3, "الديون", Icons.Default.AccountBalance),
        Triple(4, "المصاريف", Icons.Default.Payments),
        Triple(5, "الإعدادات", Icons.Default.Settings)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SanadSurface,
                drawerContentColor = SanadTextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = appSettings.storeName,
                        style = MaterialTheme.typography.titleLarge,
                        color = SanadTextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "نظام سند المحاسبي المتكامل • Multi-Currency ERP & POS",
                        style = MaterialTheme.typography.bodySmall,
                        color = SanadElectricBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = SanadBorder)
                    Spacer(modifier = Modifier.height(4.dp))

                    navTabs.forEach { (idx, label, icon) ->
                        NavigationDrawerItem(
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                            selected = selectedTab == idx && activeOverlay == OverlaySubScreen.NONE,
                            onClick = {
                                activeOverlay = OverlaySubScreen.NONE
                                selectedTab = idx
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(imageVector = icon, contentDescription = label) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = SanadElectricBlue.copy(alpha = 0.2f),
                                selectedIconColor = SanadElectricBlue,
                                selectedTextColor = SanadElectricBlue,
                                unselectedIconColor = SanadTextSecondary,
                                unselectedTextColor = SanadTextPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = SanadBorder, modifier = Modifier.padding(vertical = 6.dp))
                    Text(
                        text = "الوحدات المتقدمة والذكاء الاصطناعي",
                        style = MaterialTheme.typography.labelMedium,
                        color = SanadTextSecondary
                    )

                    NavigationDrawerItem(
                        label = { Text("دليل الاستخدام التفاعلي (?)", fontWeight = FontWeight.Bold) },
                        selected = activeOverlay == OverlaySubScreen.USER_GUIDE,
                        onClick = {
                            activeOverlay = OverlaySubScreen.USER_GUIDE
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = SanadElectricBlue) }
                    )
                    NavigationDrawerItem(
                        label = { Text("المحاسب الصوتي الذكي Gemini AI", fontWeight = FontWeight.Bold) },
                        selected = activeOverlay == OverlaySubScreen.GEMINI_VOICE_CHAT,
                        onClick = {
                            activeOverlay = OverlaySubScreen.GEMINI_VOICE_CHAT
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = SanadElectricBlue) }
                    )
                    NavigationDrawerItem(
                        label = { Text("التقارير والمحاسبة الشاملة (Loyverse)", fontWeight = FontWeight.Bold) },
                        selected = activeOverlay == OverlaySubScreen.LOYVERSE_REPORTS,
                        onClick = {
                            activeOverlay = OverlaySubScreen.LOYVERSE_REPORTS
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null, tint = SanadEmeraldGreen) }
                    )
                    NavigationDrawerItem(
                        label = { Text("استيراد فاتورة مورد B2B (.sanad)", fontWeight = FontWeight.Bold) },
                        selected = activeOverlay == OverlaySubScreen.B2B_IMPORT_WIZARD,
                        onClick = {
                            activeOverlay = OverlaySubScreen.B2B_IMPORT_WIZARD
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = SanadElectricBlue) }
                    )
                    NavigationDrawerItem(
                        label = { Text("تصوير فاتورة مورد بالذكاء الاصطناعي", fontWeight = FontWeight.Bold) },
                        selected = activeOverlay == OverlaySubScreen.AI_OCR_SCANNER,
                        onClick = {
                            activeOverlay = OverlaySubScreen.AI_OCR_SCANNER
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SanadEmeraldGreen) }
                    )
                    NavigationDrawerItem(
                        label = { Text("إدارة الورديات وتقرير Z-Report", fontWeight = FontWeight.Bold) },
                        selected = activeOverlay == OverlaySubScreen.SHIFTS_MANAGEMENT,
                        onClick = {
                            activeOverlay = OverlaySubScreen.SHIFTS_MANAGEMENT
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = SanadAmberWarning) }
                    )
                    NavigationDrawerItem(
                        label = { Text("الحسابات المالية والخزينة", fontWeight = FontWeight.Bold) },
                        selected = activeOverlay == OverlaySubScreen.TREASURY_ACCOUNTS,
                        onClick = {
                            activeOverlay = OverlaySubScreen.TREASURY_ACCOUNTS
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SanadTextPrimary) }
                    )
                    NavigationDrawerItem(
                        label = { Text("الموظفون وصلاحيات الكاشير (PIN)", fontWeight = FontWeight.Bold) },
                        selected = activeOverlay == OverlaySubScreen.EMPLOYEES_PERMISSIONS,
                        onClick = {
                            activeOverlay = OverlaySubScreen.EMPLOYEES_PERMISSIONS
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Badge, contentDescription = null, tint = SanadCoralRed) }
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(SanadNavyDark)
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = SanadNavyDark,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                // Quick Module Bar + Menu Drawer Trigger + User Guide (?) + Smart AI Voice Shortcut
                Surface(
                    color = SanadSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("open_navigation_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "القائمة الجانبية",
                                tint = SanadTextPrimary
                            )
                        }

                        // Global (?) Interactive User Guide Button
                        OutlinedButton(
                            onClick = { activeOverlay = OverlaySubScreen.USER_GUIDE },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadElectricBlue),
                            modifier = Modifier.testTag("top_bar_user_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "دليل الاستخدام",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("دليل الاستخدام (?)", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { activeOverlay = OverlaySubScreen.GEMINI_VOICE_CHAT },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadElectricBlue),
                            modifier = Modifier.testTag("top_bar_voice_ai_button")
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الأمر الصوتي AI", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { activeOverlay = OverlaySubScreen.LOYVERSE_REPORTS },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadEmeraldGreen),
                            modifier = Modifier.testTag("top_bar_reports_button")
                        ) {
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("التقارير والمحاسبة", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { activeOverlay = OverlaySubScreen.B2B_IMPORT_WIZARD },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadElectricBlue)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استيراد مورد B2B", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { activeOverlay = OverlaySubScreen.AI_OCR_SCANNER },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadEmeraldGreen)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصوير فاتورة AI", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { activeOverlay = OverlaySubScreen.SHIFTS_MANAGEMENT },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadAmberWarning)
                        ) {
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الورديات Z", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { activeOverlay = OverlaySubScreen.TREASURY_ACCOUNTS },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadTextPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الخزينة", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { activeOverlay = OverlaySubScreen.EMPLOYEES_PERMISSIONS },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SanadCoralRed)
                        ) {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الموظفون", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            },
            bottomBar = {
                if (!isLandscape) {
                    NavigationBar(
                        containerColor = SanadSurfaceElevated
                    ) {
                        navTabs.forEach { (idx, label, icon) ->
                            NavigationBarItem(
                                selected = selectedTab == idx && activeOverlay == OverlaySubScreen.NONE,
                                onClick = {
                                    activeOverlay = OverlaySubScreen.NONE
                                    selectedTab = idx
                                },
                                icon = { Icon(imageVector = icon, contentDescription = label) },
                                label = { Text(text = label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SanadElectricBlue,
                                    selectedTextColor = SanadElectricBlue,
                                    unselectedIconColor = SanadTextSecondary,
                                    unselectedTextColor = SanadTextSecondary,
                                    indicatorColor = SanadElectricBlue.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier.testTag("bottom_nav_tab_$idx")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Landscape Mode Side NavigationRail
                if (isLandscape) {
                    NavigationRail(
                        containerColor = SanadSurfaceElevated,
                        contentColor = SanadTextPrimary
                    ) {
                        navTabs.forEach { (idx, label, icon) ->
                            NavigationRailItem(
                                selected = selectedTab == idx && activeOverlay == OverlaySubScreen.NONE,
                                onClick = {
                                    activeOverlay = OverlaySubScreen.NONE
                                    selectedTab = idx
                                },
                                icon = { Icon(imageVector = icon, contentDescription = label) },
                                label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SanadElectricBlue,
                                    selectedTextColor = SanadElectricBlue,
                                    unselectedIconColor = SanadTextSecondary,
                                    unselectedTextColor = SanadTextSecondary,
                                    indicatorColor = SanadElectricBlue.copy(alpha = 0.18f)
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (activeOverlay) {
                        OverlaySubScreen.USER_GUIDE -> UserGuideScreen(
                            initialTopicKey = guideTopicKey,
                            onClose = {
                                guideTopicKey = null
                                activeOverlay = OverlaySubScreen.NONE
                            }
                        )
                        OverlaySubScreen.GEMINI_VOICE_CHAT -> GeminiVoiceChatScreen(
                            viewModel = viewModel,
                            geminiApiKey = appSettings.geminiApiKey,
                            geminiModelName = appSettings.geminiModelName,
                            onModelChange = { newModel ->
                                scope.launch {
                                    prefsRepo.updateGeminiConfig(
                                        apiKey = appSettings.geminiApiKey,
                                        modelName = newModel
                                    )
                                }
                            },
                            onNavigateToInvoiceEditor = {
                                activeOverlay = OverlaySubScreen.NONE
                                selectedTab = 2
                            },
                            onClose = { activeOverlay = OverlaySubScreen.NONE }
                        )
                        OverlaySubScreen.LOYVERSE_REPORTS -> LoyverseReportsScreen(
                            viewModel = viewModel,
                            onRecallInvoiceToEditor = { record ->
                                viewModel.recallCompletedInvoiceToEditor(record)
                                activeOverlay = OverlaySubScreen.NONE
                                selectedTab = 2
                            },
                            onClose = { activeOverlay = OverlaySubScreen.NONE }
                        )
                        OverlaySubScreen.EMPLOYEES_PERMISSIONS -> EmployeesAccessScreen(
                            onClose = { activeOverlay = OverlaySubScreen.NONE }
                        )
                        OverlaySubScreen.B2B_IMPORT_WIZARD -> B2BSupplierImportWizardScreen(
                            viewModel = viewModel,
                            onClose = { activeOverlay = OverlaySubScreen.NONE }
                        )
                        OverlaySubScreen.AI_OCR_SCANNER -> AiSupplierInvoiceScannerScreen(
                            viewModel = viewModel,
                            geminiApiKey = appSettings.geminiApiKey,
                            geminiModelName = appSettings.geminiModelName,
                            initialSharedFileLabel = initialSharedFileLabel,
                            onOpenUserGuide = {
                                guideTopicKey = "ocrPurchaseFlow"
                                activeOverlay = OverlaySubScreen.USER_GUIDE
                            },
                            onClose = { activeOverlay = OverlaySubScreen.NONE }
                        )
                        OverlaySubScreen.SHIFTS_MANAGEMENT -> AnalyticalShiftsAndTreasuryScreen(
                            viewModel = viewModel,
                            initialSubTab = 0,
                            onClose = { activeOverlay = OverlaySubScreen.NONE }
                        )
                        OverlaySubScreen.TREASURY_ACCOUNTS -> AnalyticalShiftsAndTreasuryScreen(
                            viewModel = viewModel,
                            initialSubTab = 1,
                            onClose = { activeOverlay = OverlaySubScreen.NONE }
                        )
                        OverlaySubScreen.NONE -> {
                            when (selectedTab) {
                                0 -> SanadHomeDashboardScreen(
                                    viewModel = viewModel,
                                    showHomeOcrShortcut = appSettings.showHomeOcrShortcut,
                                    onQuickNavigate = { dest ->
                                        when (dest) {
                                            com.example.ui.screens.home.QuickDashboardDestination.NEW_SALES_INVOICE -> {
                                                viewModel.setInvoiceMode(InvoiceMode.SALE)
                                                selectedTab = 2
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.NEW_PURCHASE_INVOICE -> {
                                                viewModel.setInvoiceMode(InvoiceMode.PURCHASE)
                                                selectedTab = 2
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.NEW_VOUCHER -> {
                                                selectedTab = 3
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.NEW_EXPENSE -> {
                                                selectedTab = 4
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.INVENTORY_LIST -> {
                                                selectedTab = 1
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.B2B_IMPORT_WIZARD -> {
                                                activeOverlay = OverlaySubScreen.B2B_IMPORT_WIZARD
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.AI_OCR_SCANNER -> {
                                                activeOverlay = OverlaySubScreen.AI_OCR_SCANNER
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.DEBTS_SCREEN -> {
                                                selectedTab = 3
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.SHIFTS_Z_REPORT -> {
                                                activeOverlay = OverlaySubScreen.SHIFTS_MANAGEMENT
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.TREASURY_ACCOUNTS -> {
                                                activeOverlay = OverlaySubScreen.TREASURY_ACCOUNTS
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.LOYVERSE_REPORTS -> {
                                                activeOverlay = OverlaySubScreen.LOYVERSE_REPORTS
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.EMPLOYEES_ACCESS -> {
                                                activeOverlay = OverlaySubScreen.EMPLOYEES_PERMISSIONS
                                            }
                                            com.example.ui.screens.home.QuickDashboardDestination.AI_VOICE_CHAT -> {
                                                activeOverlay = OverlaySubScreen.GEMINI_VOICE_CHAT
                                            }
                                        }
                                    }
                                )
                                1 -> InventoryManagementScreen(
                                    viewModel = viewModel,
                                    externalOpenNewProductDialog = openNewProductModalFromExternal,
                                    onDismissExternalNewProductDialog = { openNewProductModalFromExternal = false }
                                )
                                2 -> UnifiedInvoiceEditorScreen(
                                    viewModel = viewModel,
                                    onOpenNewProductModal = {
                                        openNewProductModalFromExternal = true
                                        selectedTab = 1
                                    },
                                    onOpenAiAssistant = { activeOverlay = OverlaySubScreen.GEMINI_VOICE_CHAT },
                                    onOpenUserGuide = {
                                        guideTopicKey = "where_to_open_invoices"
                                        activeOverlay = OverlaySubScreen.USER_GUIDE
                                    },
                                    onCloseScreen = { selectedTab = 0 }
                                )
                                3 -> DebtsAndVouchersScreen(viewModel = viewModel)
                                4 -> MultiCurrencyExpensesScreen(viewModel = viewModel)
                                5 -> ExtendedSettingsScreen(
                                    viewModel = viewModel,
                                    prefsRepo = prefsRepo,
                                    appSettings = appSettings,
                                    onOpenOcrScanner = { activeOverlay = OverlaySubScreen.AI_OCR_SCANNER },
                                    onOpenB2BWizard = { activeOverlay = OverlaySubScreen.B2B_IMPORT_WIZARD }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
