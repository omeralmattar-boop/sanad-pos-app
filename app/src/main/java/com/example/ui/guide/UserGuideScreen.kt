package com.example.ui.guide

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

data class OcrPathTableRow(
    val pathTitle: String,
    val whereLabel: String,
    val whatHappens: String
)

data class GuideAccordionSection(
    val key: String,
    val title: String,
    val subtitleBadge: String,
    val bullets: List<String>,
    val pathsTable: List<OcrPathTableRow> = emptyList()
)

@Composable
fun UserGuideScreen(
    initialTopicKey: String? = null,
    onClose: () -> Unit
) {
    val sections = remember {
        listOf(
            GuideAccordionSection(
                key = "where_to_open_invoices",
                title = "1. أين أفتح الفواتير وما المسارات المرتبطة؟",
                subtitleBadge = "الفواتير والمسارات",
                bullets = listOf(
                    "من شريط التنقل السفلي اختر تبويب [الفواتير] أو من الشاشة الرئيسية اضغط على بطاقات الوصول السريع.",
                    "يضم محرر الفواتير الموحد 6 أوضاع متكاملة في شريط علوي واحد: (1. فاتورة بيع جديدة | 2. فاتورة شراء جديدة | 3. عرض سعر Quotation | 4. فاتورة مرتجع بيع | 5. فاتورة مرتجع شراء | 6. تسجيل تلف).",
                    "وضع [عرض سعر Quotation] يحفظ العرض دون التأثير على المخزون أو الصندوق النقدي، ويمكنك تحويله إلى فاتورة بيع فعلية بضغطة زر واحدة.",
                    "عند تدوير الجهاز أفقياً (Landscape)، يتحول المحرر تلقائياً إلى شاشة كاشير Aronium المقسمة (شبكة الأصناف 60% يساراً وجدول الفاتورة 40% يميناً)."
                )
            ),
            GuideAccordionSection(
                key = "cash_or_credit",
                title = "2. نقدي أم آجل — ما الفرق؟",
                subtitleBadge = "نقدي أم آجل",
                bullets = listOf(
                    "نقداً (Cash): يُودع المبلغ في الصندوق النقدي المختار (في البيع) أو يُخصم منه (في الشراء). إذا لم يوجد صندوق مطابق لعملة الفاتورة، يظهر زر فوري [+ إضافة صندوق بـ العملة] دون مغادرة الشاشة.",
                    "دين / آجل (Credit): يُسجل المبلغ في ذمة العميل (لنا) أو ذمة المورد (علينا) مع تثبيت سعر الصرف التاريخي (Rate Lock) على الفاتورة.",
                    "دفع متعدد (Split Payment): يتيح في محرر الفواتير قبض جزء نقداً وتسجيل المتبقي كدين آجل على حساب العميل في نفس العملية.",
                    "قفل الوردية المغلقة: الفواتير التي تنتمي إلى وردية تم إغلاقها بتقرير Z-Report تُقفل تلقائياً ضد التعديل أو الحذف لحماية المطابقة المحاسبية."
                )
            ),
            GuideAccordionSection(
                key = "sale_invoice_steps",
                title = "3. فاتورة بيع — خطوات من السلة حتى الحفظ؟",
                subtitleBadge = "فاتورة بيع",
                bullets = listOf(
                    "اختر وضع [فاتورة بيع جديدة] وحدد فئة التسعير [مفرق / جملة] والعميل وعملة المعاملة.",
                    "أضف الأصناف عبر الكاميرا أو قارئ الباركود أو البحث الفوري، وانقر مباشرة على مربع الكمية أو السعر لفتح لوحة الأرقام السريعة.",
                    "الحساب العكسي للسطر: انقر على [مجموع السطر] وأدخل الإجمالي ليقوم النظام بحساب سعر الوحدة تلقائياً (unitPrice = lineTotal / quantity).",
                    "الخصم التلقائي من الإجمالي: انقر على [الإجمالي النهائي] في شريط الدفع السفلي وخفّض المبلغ ليحتسب الفرق تلقائياً كخصم فاتورة.",
                    "قاعدة FEFO: للأصناف التي تتبع الدفعات والصلاحية، يصرف النظام تلقائياً من الدفعة الأقرب انتهاءً أولاً.",
                    "يدعم المحرر اختصارات لوحة المفاتيح السريعة (F2–F8 و F10 للدفع والحفظ السريع)، وبعد الحفظ يمكنك النقر على [تصدير B2B (.sanad)] لمشاركة الفاتورة مع المشتري."
                )
            ),
            GuideAccordionSection(
                key = "purchase_invoice_steps",
                title = "4. فاتورة الشراء — ماذا تفعل؟ وشراء يدوي خطوة بخطوة.",
                subtitleBadge = "فاتورة الشراء",
                bullets = listOf(
                    "تُغذي فاتورة الشراء كميات المخزون وتُحدث متوسط التكلفة المرجح (Weighted Average Cost - WAC) آلياً لكل صنف.",
                    "للشراء اليدوي: افتح [فاتورة شراء جديدة]، اختر المورد، حدد عملة الشراء وسعر الصرف، وأضف الأصناف والكميات وتكلفة الوحدة.",
                    "يمكنك تفعيل الضريبة وإدخال تعديلات مالية (شحن إضافي أو خصم مكتسب) ثم اختيار طريقة الدفع وحفظ الفاتورة."
                )
            ),
            GuideAccordionSection(
                key = "ocrPurchaseFlow",
                title = "5. تصوير فاتورة مورد (ocrPurchase) ومراحل المعالج (ocrPurchaseFlow).",
                subtitleBadge = "ocrPurchaseFlow",
                bullets = listOf(
                    "المرحلة 1 (الالتقاط): التقاط صورة بالكاميرا، اختيار من المعرض، أو اختيار ملف PDF (<= 8MB) مع مؤشر القراءة وتلميح المدة والخصوصية.",
                    "المرحلة 2 (المراجعة والمطابقة): مراجعة بيانات المورد والرقم الضريبي ورقم الفاتورة وعملة الورقة ومطابقة كل سطر مستخرج.",
                    "المرحلة 3 (أسعار جديدة): تحديد سعر بيع المفرق (إلزامي) وسعر الجملة للأصناف الجديدة التي تُنشأ لأول مرة.",
                    "المرحلة 4 (رادار الأسعار): رصد الأصناف التي تغيرت تكلفتها مقارنة بالكتالوج واقتراح تحديث سعر بيع المفرق.",
                    "المرحلة 5 (الدفع والمنصة): اختيار مصدر دفع واحد (نقد | حساب بنكي | آجل)، ضبط الضريبة وتضمينها في التكلفة، الشحن والخصم، ومراجعة ملخص الأسطر المرفوضة.",
                    "المرحلة 6 (تم): عرض رقم فاتورة الشراء النهائي (PUR-...) ووسمها بالوردية المفتوحة الحالية."
                )
            ),
            GuideAccordionSection(
                key = "ocrPurchaseDraft",
                title = "6. استئناف تصوير فاتورة غير مكتمل (ocrPurchaseDraft).",
                subtitleBadge = "ocrPurchaseDraft",
                bullets = listOf(
                    "يحفظ النظام مسودة محلية واحدة صالحة لمدة 24 ساعة (بدون بايتات الصور حفاظاً على الذاكرة والخصوصية) فور انتهاء قراءة الفاتورة.",
                    "عند فتح شاشة تصوير فاتورة مورد مع وجود مسودة نشطة، تظهر نافذة حوارية تخيرك بين: [استئناف | بدء من جديد | إلغاء].",
                    "تُمسح المسودة تلقائياً بمجرد إتمام الحفظ النهائي في المرحلة السادسة."
                )
            ),
            GuideAccordionSection(
                key = "ocrPurchaseMapping",
                title = "7. ربط أسطر تصوير الفاتورة (ocrPurchaseMapping).",
                subtitleBadge = "ocrPurchaseMapping",
                bullets = listOf(
                    "لكل سطر مستخرج تتوفر أزرار وحقول متكاملة: [مطابقة / إعادة مطابقة]، [إنشاء جديد]، و[تجاهل / إلغاء التجاهل].",
                    "الحقول القابلة للتعديل في كل سطر: الاسم، الوحدة/المعامل، الكمية، التكلفة، مجموع السطر، [تأكيد مجاني / تكلفة صفر]، باركود اختياري، سعر الجملة، ضريبة سطر، ولوط/تاريخ صلاحية.",
                    "المصحح الحسابي للسطر: إذا اختلف (الكمية × التكلفة) عن مجموع السطر المقروء، يبرز زرّا [تصحيح من الوحدة] و[تصحيح من المجموع] لمعالجة الفرق بضغطة واحدة."
                )
            ),
            GuideAccordionSection(
                key = "ocrPurchaseCurrencyLots",
                title = "8. عملة الفاتورة المصورة، الشحن، اللوطات، والإبقاء على عملة الورقة (ocrPurchaseCurrencyLots).",
                subtitleBadge = "ocrPurchaseCurrencyLots",
                bullets = listOf(
                    "يتيح المعالج تحديد عملة الورقة وسعر الصرف مقابل العملة الأساسية، مع خيار [الإبقاء على عملة الورقة] عند الحفظ.",
                    "عند تفعيل [تضمين الضريبة في التكلفة] وإدخال الشحن الإضافي وخصم المورد، يوزع النظام التكاليف نسبياً لحساب التكلفة المحملة (Landed Cost WAC).",
                    "عند وجود رقم تشغيلة أو تاريخ صلاحية على السطر يمكنك الضغط على [تفعيل تتبع الدفعات FEFO] ليصرف النظام الأقرب انتهاءً أولاً."
                )
            ),
            GuideAccordionSection(
                key = "ocrPurchaseErrors",
                title = "9. أخطاء شائعة عند تصوير فاتورة المورد (ocrPurchaseErrors).",
                subtitleBadge = "ocrPurchaseErrors",
                bullets = listOf(
                    "منع تكرار الفاتورة: يرفض النظام حفظ فاتورة تحمل نفس المورد ونفس رقم الفاتورة المسجل مسبقاً.",
                    "نقص سعر المفرق للأصناف الجديدة: لا يسمح المعالج بتجاوز المرحلة 3 أو الحفظ النهائي قبل تحديد سعر بيع مفرق أكبر من صفر لكل صنف جديد.",
                    "الأسطر المجانية غير المؤكدة: إذا كانت تكلفة السطر صفراً، يلزم الضغط على [تأكيد مجاني / تكلفة صفر] لاعتماده.",
                    "عدم كفاية رصيد الخزينة أو البنك: عند الدفع نقداً أو من حساب بنكي، يتحقق النظام من صلاحية `treasury.manage` ومن كفاية الرصيد الفعلي."
                )
            ),
            GuideAccordionSection(
                key = "ocrPurchasePayPlatform",
                title = "10. الدفع والمنصة في تصوير فاتورة المورد (ocrPurchasePayPlatform).",
                subtitleBadge = "ocrPurchasePayPlatform",
                bullets = listOf(
                    "حصر الدفع في معالج التصوير بمصدر واحد فقط: [نقد | حساب بنكي | آجل] (بدون دفع مختلط داخل المعالج؛ ويمكن تعديل الفاتورة بعد الحفظ لتقسيم الدفع).",
                    "التحقق من صلاحية `treasury.manage` وكفاية رصيد الخزينة/البنك عند الدفع النقدي أو البنكي.",
                    "توسم فاتورة الشراء الناتجة بالوردية المفتوحة الحالية تلقائياً لتظهر في مطابقة الوردية وتقرير Z-Report."
                )
            ),
            GuideAccordionSection(
                key = "ocrPurchaseButtons",
                title = "11. أزرار تصوير فاتورة المورد (ocrPurchaseButtons).",
                subtitleBadge = "ocrPurchaseButtons",
                bullets = listOf(
                    "المرحلة 1 (الالتقاط): [التقاط صورة]، [اختيار من المعرض]، [اختيار ملف PDF <= 8MB]، مؤشر القراءة، وتلميح المدة/الخصوصية.",
                    "نافذة الاستئناف: [استئناف | بدء من جديد | إلغاء].",
                    "رأس المراجعة: [رجوع]، [مساعدة (?)]، اختيار/إضافة مورد، رقم ضريبي، رقم فاتورة (تلقائي OCR-* عند الفراغ)، تاريخ، وعملة الورقة.",
                    "لكل سطر: [مطابقة / إعادة مطابقة]، [إنشاء جديد]، [تجاهل / إلغاء التجاهل]، الاسم، الوحدة/المعامل، الكمية، التكلفة، مجموع السطر، [تأكيد مجاني / تكلفة صفر]، باركود اختياري، سعر الجملة، ضريبة سطر، لوط/تاريخ صلاحية مع [تفعيل تتبع الدفعات FEFO]، وأزرار [تصحيح من الوحدة] و[تصحيح من المجموع].",
                    "المراحل 3–6: [متابعة] في الأسعار الجديدة ورادار الأسعار، وفي الدفع: تفعيل الضريبة، [تضمين الضريبة في التكلفة]، التعديلات المالية، [الإبقاء على عملة الورقة]، الملاحظات، ملخص الأسطر المرفوضة، [تأكيد وحفظ]، وفي النهاية رقم الفاتورة وزر [تم]."
                )
            ),
            GuideAccordionSection(
                key = "ocrPurchasePaths",
                title = "12. مسارات تصوير فاتورة المورد (ocrPurchasePaths - مع الجدول الكامل).",
                subtitleBadge = "ocrPurchasePaths",
                bullets = listOf(
                    "يُفتح معالج تصوير فاتورة المورد عبر المسارات الأربعة المعتمدة فقط، ولا يوجد زر OCR داخل تبويب الفواتير أو نموذج الشراء اليدوي:",
                    "1) الإعدادات -> بطاقة استيراد وتصدير -> تصوير فاتورة مورد.",
                    "2) اختصار الشاشة الرئيسية «تصوير فاتورة» (مع إمكانية تخصيص ظهوره من الإعدادات).",
                    "3) مشاركة أو فتح ملف PDF أو صورة من النظام خارجياً إلى التطبيق.",
                    "4) مسار منفصل لاستيراد ملف فاتورة الكترونية (.sanad) لدمج فواتير الموردين المهيكلة B2B."
                ),
                pathsTable = listOf(
                    OcrPathTableRow(
                        pathTitle = "1. بطاقة استيراد وتصدير",
                        whereLabel = "الإعدادات -> المتجر والعملات",
                        whatHappens = "يفتح معالج تصوير فاتورة مورد (AI OCR) مباشرة أو معالج B2B (.sanad)."
                    ),
                    OcrPathTableRow(
                        pathTitle = "2. اختصار الشاشة الرئيسية",
                        whereLabel = "الرئيسية -> شبكة الوصول السريع «تصوير فاتورة»",
                        whatHappens = "يفتح المعالج بضغطة واحدة (مع إمكانية إظهاره أو إخفائه من الإعدادات)."
                    ),
                    OcrPathTableRow(
                        pathTitle = "3. مشاركة / فتح PDF خارجي",
                        whereLabel = "مشاركة أو فتح ملف PDF/صورة من النظام",
                        whatHappens = "يستقبل التطبيق الملف ويفتح معالج التصوير تلقائياً في المرحلة الأولى/الثانية."
                    ),
                    OcrPathTableRow(
                        pathTitle = "4. استيراد فاتورة الكترونية (.sanad)",
                        whereLabel = "معالج استيراد فاتورة مورد B2B منفصل",
                        whatHappens = "يقرأ ملف .sanad أو JSON المهيكل في 6 خطوات مستقلة عن OCR البصري."
                    )
                )
            ),
            GuideAccordionSection(
                key = "ocrPurchaseShortcuts",
                title = "13. اختصارات تصوير فاتورة المورد (ocrPurchaseShortcuts).",
                subtitleBadge = "ocrPurchaseShortcuts",
                bullets = listOf(
                    "يمكنك التحكم في ظهور اختصار «تصوير فاتورة» على الشاشة الرئيسية من خلال: [الإعدادات -> بطاقة استيراد وتصدير -> إظهار اختصار «تصوير فاتورة» في الشاشة الرئيسية].",
                    "يمكنك أيضاً سؤال المساعد الذكي (سند AI) عن \"ocr\" أو \"تصوير فاتورة\" أو \"ocrPurchaseButtons\" أو \"ocrPurchasePaths\" للحصول على إرشاد فوري."
                )
            )
        )
    }

    val expandedMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            sections.forEachIndexed { index, sec ->
                put(sec.key, if (initialTopicKey != null) sec.key == initialTopicKey else index < 4)
            }
            if (initialTopicKey != null) {
                put("ocrPurchasePaths", true)
            }
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(initialTopicKey) {
        if (initialTopicKey != null) {
            val targetIdx = sections.indexOfFirst { it.key == initialTopicKey }
            if (targetIdx >= 0) {
                expandedMap[initialTopicKey] = true
                listState.animateScrollToItem(targetIdx)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanadNavyDark)
    ) {
        // Top Header
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = SanadElectricBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "دليل الاستخدام التفاعلي والقواعد المحاسبية (13 بطاقة)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanadTextPrimary
                        )
                        Text(
                            text = "الفواتير، الدفع النقدي والآجل، ومعالج تصوير فواتير الموردين (ocrPurchaseFlow & Paths)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SanadTextSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_user_guide_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SanadTextSecondary)
                }
            }
        }

        HorizontalDivider(color = SanadBorder)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(sections, key = { _, it -> it.key }) { _, sec ->
                val isExpanded = expandedMap[sec.key] == true
                Card(
                    colors = CardDefaults.cardColors(containerColor = SanadSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isExpanded) SanadElectricBlue else SanadBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { expandedMap[sec.key] = !isExpanded }
                        .testTag("guide_accordion_${sec.key}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                SanadBadge(
                                    text = sec.subtitleBadge,
                                    textColor = SanadElectricBlue,
                                    containerColor = SanadBlueContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = sec.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SanadTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            // Electric Sky Blue (+) / (-) Icon Box matching the Dark Navy Accordion design
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(SanadBlueContainer, CircleShape)
                                    .border(1.dp, SanadElectricBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.Remove else Icons.Default.Add,
                                    contentDescription = if (isExpanded) "طي" else "توسيع",
                                    tint = SanadElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = SanadBorder.copy(alpha = 0.6f))
                                sec.bullets.forEach { bullet ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "• ",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = SanadElectricBlue
                                        )
                                        Text(
                                            text = bullet,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SanadTextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Complete Paths Table (المسار | أين؟ | ماذا يحدث؟)
                                if (sec.pathsTable.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = SanadNavyDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, SanadElectricBlue.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                            .testTag("ocr_paths_table")
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SanadEmeraldContainer, RoundedCornerShape(6.dp))
                                                    .padding(vertical = 6.dp, horizontal = 8.dp)
                                            ) {
                                                Text(
                                                    text = "المسار",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadEmeraldGreen,
                                                    modifier = Modifier.weight(0.28f)
                                                )
                                                Text(
                                                    text = "أين؟",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadElectricBlue,
                                                    modifier = Modifier.weight(0.32f)
                                                )
                                                Text(
                                                    text = "ماذا يحدث؟",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SanadTextPrimary,
                                                    modifier = Modifier.weight(0.40f)
                                                )
                                            }

                                            sec.pathsTable.forEach { row ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = row.pathTitle,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = SanadEmeraldGreen,
                                                        modifier = Modifier.weight(0.28f)
                                                    )
                                                    Text(
                                                        text = row.whereLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SanadElectricBlue,
                                                        modifier = Modifier.weight(0.32f)
                                                    )
                                                    Text(
                                                        text = row.whatHappens,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SanadTextPrimary,
                                                        modifier = Modifier.weight(0.40f)
                                                    )
                                                }
                                                HorizontalDivider(color = SanadBorder.copy(alpha = 0.4f))
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
}
