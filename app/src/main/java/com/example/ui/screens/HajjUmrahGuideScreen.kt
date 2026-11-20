package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.StatusOnTimeColor
import com.example.util.BanglaUtils
import com.example.util.HajjGuideStep
import com.example.util.HajjPracticalSection
import com.example.util.HajjStepCategory
import com.example.util.HajjUmrahCatalog
import com.example.util.LocalAppLanguage
import com.example.util.isEnglishLang
import com.example.util.tr

@Composable
fun HajjUmrahGuideScreen(
    completedStepIds: Set<String>,
    onToggleStepCompleted: (String) -> Unit,
    onResetChecklist: () -> Unit,
    onStatusMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val isEn = isEnglishLang(LocalAppLanguage.current)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Steps, 1 = Practical Info
    var selectedCategory by remember { mutableStateOf(HajjStepCategory.ALL) }
    var expandedStepId by remember { mutableStateOf<String?>(HajjUmrahCatalog.STEPS.first().id) }
    var showResetDialog by remember { mutableStateOf(false) }

    val allSteps = HajjUmrahCatalog.STEPS
    val totalSteps = allSteps.size
    val completedCount = allSteps.count { it.id in completedStepIds }
    val progressFraction = if (totalSteps > 0) completedCount.toFloat() / totalSteps.toFloat() else 0f

    val filteredSteps = remember(selectedCategory) {
        when (selectedCategory) {
            HajjStepCategory.ALL -> allSteps
            HajjStepCategory.UMRAH -> allSteps.filter { it.isUmrah }
            HajjStepCategory.HAJJ -> allSteps.filter { it.isHajj }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("hajj_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr(isEn, "পিছনে যান", "Back")
                    )
                }
                Column {
                    Text(
                        text = tr(isEn, "হজ্জ ও উমরাহ গাইড", "Hajj & Umrah Guide"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            isEn,
                            "ধাপে ধাপে অফলাইন গাইড, দোয়া ও চেকলিস্ট",
                            "Offline step-by-step guide, Duas & checklist"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (completedCount > 0) {
                IconButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("hajj_reset_checklist_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = tr(isEn, "চেকলিস্ট রিসেট", "Reset Checklist")
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checklist Progress Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        isEn,
                                        "${BanglaUtils.toBanglaNumber(totalSteps)}টির মধ্যে ${BanglaUtils.toBanglaNumber(completedCount)}টি ধাপ সম্পন্ন",
                                        "$completedCount of $totalSteps steps done"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.testTag("hajj_progress_text")
                                )
                                Text(
                                    text = tr(
                                        isEn,
                                        "অফলাইনে সংরক্ষিত • যেকোনো সময় টিক দিয়ে ট্র্যাক করুন",
                                        "Saved offline • Tick steps as you complete them"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (completedCount == totalSteps) StatusOnTimeColor else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${BanglaUtils.formatNumber((progressFraction * 100).toInt(), isEn)}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (completedCount == totalSteps) StatusOnTimeColor else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Primary Mode Toggle: Steps & Checklist vs Practical Info
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null) },
                        modifier = Modifier.testTag("hajj_tab_steps")
                    ) {
                        Text(tr(isEn, "ধাপ ও চেকলিস্ট", "Steps & Checklist"))
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier.testTag("hajj_tab_practical")
                    ) {
                        Text(tr(isEn, "জরুরি তথ্য", "Practical Info"))
                    }
                }
            }

            if (selectedTab == 0) {
                // Category Filter Chips (All / Umrah / Hajj)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HajjStepCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.title(isEn)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hajj_filter_${cat.name.lowercase()}")
                            )
                        }
                    }
                }

                items(filteredSteps, key = { it.id }) { step ->
                    val isDone = step.id in completedStepIds
                    val isExpanded = expandedStepId == step.id
                    HajjStepCard(
                        step = step,
                        isDone = isDone,
                        isExpanded = isExpanded,
                        isEnglish = isEn,
                        onToggleExpand = {
                            expandedStepId = if (isExpanded) null else step.id
                        },
                        onToggleCompleted = { onToggleStepCompleted(step.id) },
                        onCopyDua = {
                            copyStepDua(context, step, isEn)
                            onStatusMessage(tr(isEn, "দোয়া কপি করা হয়েছে", "Dua copied to clipboard"))
                        },
                        onShareDua = {
                            shareStepDua(context, step, isEn)
                        }
                    )
                }
            } else {
                items(HajjUmrahCatalog.PRACTICAL_SECTIONS, key = { it.id }) { section ->
                    HajjPracticalSectionCard(section = section, isEnglish = isEn)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(tr(isEn, "চেকলিস্ট রিসেট করবেন?", "Reset Checklist?")) },
            text = {
                Text(
                    tr(
                        isEn,
                        "আপনার সম্পন্ন করা সকল হজ্জ ও উমরাহ ধাপের টিক চিহ্ন মুছে যাবে।",
                        "All ticked Hajj & Umrah steps will be unchecked."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetChecklist()
                        showResetDialog = false
                    }
                ) {
                    Text(tr(isEn, "রিসেট করুন", "Reset"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(tr(isEn, "বাতিল", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun HajjStepCard(
    step: HajjGuideStep,
    isDone: Boolean,
    isExpanded: Boolean,
    isEnglish: Boolean,
    onToggleExpand: () -> Unit,
    onToggleCompleted: () -> Unit,
    onCopyDua: () -> Unit,
    onShareDua: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hajj_step_card_${step.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = isDone,
                    onCheckedChange = { onToggleCompleted() },
                    modifier = Modifier.testTag("hajj_step_check_${step.id}")
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDone) StatusOnTimeColor else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = BanglaUtils.formatNumber(step.stepNumber, isEnglish),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step.isUmrah) {
                            StepRitualBadge(
                                label = tr(isEnglish, "উমরাহ", "Umrah"),
                                isPrimary = true
                            )
                        }
                        if (step.isHajj) {
                            StepRitualBadge(
                                label = tr(isEnglish, "হজ্জ", "Hajj"),
                                isPrimary = false
                            )
                        }
                    }
                    Text(
                        text = step.title(isEnglish),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = step.subtitle(isEnglish),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = tr(isEnglish, "বিস্তারিত", "Toggle Details"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()

                    // Step Description
                    Text(
                        text = step.description(isEnglish),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Step Dua Card (Arabic + Bangla + English)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = step.duaTitle(isEnglish),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = step.duaArabic,
                                fontFamily = AmiriFontFamily,
                                fontSize = 23.sp,
                                lineHeight = 36.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))

                            // Primary language meaning
                            Text(
                                text = if (isEnglish) "English: ${step.duaEnglish}" else "বাংলা অর্থ: ${step.duaBangla}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // Secondary language meaning (so both Bangla & English are always accessible)
                            Text(
                                text = if (isEnglish) "বাংলা: ${step.duaBangla}" else "English: ${step.duaEnglish}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCopyDua,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr(isEnglish, "দোয়া কপি", "Copy Dua"))
                                }
                                OutlinedButton(
                                    onClick = onShareDua,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr(isEnglish, "শেয়ার", "Share"))
                                }
                            }
                        }
                    }

                    // Checklist Action Row
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleCompleted),
                        color = if (isDone) StatusOnTimeColor.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isDone) StatusOnTimeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = step.checklistPrompt(isEnglish),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isDone) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDone) StatusOnTimeColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRitualBadge(label: String, isPrimary: Boolean) {
    Surface(
        color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun HajjPracticalSectionCard(
    section: HajjPracticalSection,
    isEnglish: Boolean
) {
    val icon = when (section.id) {
        "packing" -> Icons.Default.Luggage
        "ihram_rules" -> Icons.Default.Checkroom
        else -> Icons.Default.WarningAmber
    }
    val tint = if (section.id == "common_mistakes") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hajj_practical_${section.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint)
                }
                Text(
                    text = section.title(isEnglish),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            section.items(isEnglish).forEachIndexed { index, itemText ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${BanglaUtils.formatNumber(index + 1, isEnglish)}.",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                    Text(
                        text = itemText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun buildShareableStepDua(step: HajjGuideStep, isEnglish: Boolean): String {
    return buildString {
        appendLine("${step.stepNumber}. ${step.title(isEnglish)}")
        appendLine(step.duaTitle(isEnglish))
        appendLine()
        appendLine(step.duaArabic)
        appendLine()
        appendLine("বাংলা: ${step.duaBangla}")
        appendLine("English: ${step.duaEnglish}")
        appendLine()
        append("— Nur Bangla / নূর বাংলা")
    }
}

private fun copyStepDua(context: Context, step: HajjGuideStep, isEnglish: Boolean) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(step.duaTitle(isEnglish), buildShareableStepDua(step, isEnglish)))
}

private fun shareStepDua(context: Context, step: HajjGuideStep, isEnglish: Boolean) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, step.title(isEnglish))
        putExtra(Intent.EXTRA_TEXT, buildShareableStepDua(step, isEnglish))
    }
    context.startActivity(
        Intent.createChooser(sendIntent, tr(isEnglish, "দোয়া শেয়ার করুন", "Share Dua"))
    )
}
