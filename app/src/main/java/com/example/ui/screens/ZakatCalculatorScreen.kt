package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.NoorUiState
import com.example.util.BanglaUtils
import com.example.util.LocalAppLanguage
import com.example.util.NisabBasis
import com.example.util.WeightUnit
import com.example.util.ZakatCalculator
import com.example.util.ZakatHistoryRecord
import com.example.util.ZakatInput
import com.example.util.isEnglishLang
import com.example.util.tr
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

@Composable
fun ZakatCalculatorScreen(
    uiState: NoorUiState,
    onSaveMetalPrices: (Double, Double) -> Unit,
    onSaveHistory: (ZakatHistoryRecord) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onBack: () -> Unit
) {
    val isEn = isEnglishLang(LocalAppLanguage.current)

    var cashText by remember { mutableStateOf("") }
    var bankText by remember { mutableStateOf("") }
    var goldWeightText by remember { mutableStateOf("") }
    var goldUnit by remember { mutableStateOf(WeightUnit.GRAM) }
    var silverWeightText by remember { mutableStateOf("") }
    var silverUnit by remember { mutableStateOf(WeightUnit.GRAM) }
    var businessText by remember { mutableStateOf("") }
    var debtsText by remember { mutableStateOf("") }
    var nisabBasis by remember { mutableStateOf(NisabBasis.SILVER) }

    var goldPriceText by remember {
        mutableStateOf(formatPlainNumber(uiState.settings.goldPricePerGramBdt))
    }
    var silverPriceText by remember {
        mutableStateOf(formatPlainNumber(uiState.settings.silverPricePerGramBdt))
    }

    LaunchedEffect(uiState.settings.goldPricePerGramBdt, uiState.settings.silverPricePerGramBdt) {
        goldPriceText = formatPlainNumber(uiState.settings.goldPricePerGramBdt)
        silverPriceText = formatPlainNumber(uiState.settings.silverPricePerGramBdt)
    }

    val goldPriceVal = parseNumericInput(goldPriceText) ?: uiState.settings.goldPricePerGramBdt
    val silverPriceVal = parseNumericInput(silverPriceText) ?: uiState.settings.silverPricePerGramBdt

    val zakatInput = remember(
        cashText, bankText, goldWeightText, goldUnit,
        silverWeightText, silverUnit, businessText, debtsText,
        goldPriceVal, silverPriceVal, nisabBasis
    ) {
        ZakatInput(
            cashInHandBdt = parseNumericInput(cashText) ?: 0.0,
            bankBalanceBdt = parseNumericInput(bankText) ?: 0.0,
            goldWeight = parseNumericInput(goldWeightText) ?: 0.0,
            goldUnit = goldUnit,
            silverWeight = parseNumericInput(silverWeightText) ?: 0.0,
            silverUnit = silverUnit,
            businessGoodsBdt = parseNumericInput(businessText) ?: 0.0,
            debtsBdt = parseNumericInput(debtsText) ?: 0.0,
            goldPricePerGramBdt = goldPriceVal,
            silverPricePerGramBdt = silverPriceVal,
            nisabBasis = nisabBasis
        )
    }

    val result = remember(zakatInput) { ZakatCalculator.calculate(zakatInput) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("zakat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr(isEn, "পিছনে যান", "Back")
                    )
                }
                Column {
                    Text(
                        text = tr(isEn, "যাকাত ক্যালকুলেটর", "Zakat Calculator"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(isEn, "টাকা (BDT) ও নিসাব হিসাব", "BDT Wealth & Nisab Calculation"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(
                onClick = {
                    cashText = ""
                    bankText = ""
                    goldWeightText = ""
                    silverWeightText = ""
                    businessText = ""
                    debtsText = ""
                },
                modifier = Modifier.testTag("zakat_clear_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr(isEn, "রিসেট", "Reset"))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Summary Output Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("zakat_result_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isZakatDue) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        }
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = tr(isEn, "যাকাত হিসাবের ফলাফল", "Zakat Summary"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr(isEn, "মোট যাকাতযোগ্য সম্পদ:", "Total Zakatable Wealth:"),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = ZakatCalculator.formatBdt(result.netZakatableWealthBdt, isEn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr(isEn, "নিসাব সীমা:", "Nisab Threshold:"),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = ZakatCalculator.formatBdt(result.activeNisabBdt, isEn),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = tr(isEn, "প্রদেয় যাকাত (২.৫%):", "Zakat Due (2.5%):"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (result.isZakatDue) {
                                    ZakatCalculator.formatBdt(result.zakatDueBdt, isEn)
                                } else {
                                    tr(isEn, "যাকাত ফরজ নয় (নিসাবের নিচে)", "No Zakat due (Below Nisab)")
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (result.isZakatDue) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("zakat_due_output")
                            )
                        }

                        Button(
                            onClick = {
                                onSaveHistory(
                                    ZakatHistoryRecord(
                                        id = UUID.randomUUID().toString(),
                                        timestampMillis = System.currentTimeMillis(),
                                        netWealthBdt = result.netZakatableWealthBdt,
                                        nisabThresholdBdt = result.activeNisabBdt,
                                        zakatDueBdt = result.zakatDueBdt,
                                        nisabBasis = nisabBasis
                                    )
                                )
                            },
                            enabled = result.netZakatableWealthBdt > 0.0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("zakat_save_history_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(isEn, "হিসাব সংরক্ষণ করুন", "Save Calculation to History"))
                        }
                    }
                }
            }

            // Editable Nisab & Gold/Silver Price per Gram Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = tr(isEn, "নিসাব ও বাজারদর (প্রতি গ্রাম টাকা)", "Nisab & Gold/Silver Price (BDT/gram)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NisabBasis.entries.forEach { basis ->
                                FilterChip(
                                    selected = nisabBasis == basis,
                                    onClick = { nisabBasis = basis },
                                    label = { Text(basis.label(isEn), style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("nisab_basis_${basis.name}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = goldPriceText,
                                onValueChange = { goldPriceText = sanitizeDecimalInput(it) },
                                label = { Text(tr(isEn, "স্বর্ণ / গ্রাম (৳)", "Gold / g (BDT)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("zakat_gold_price_input")
                            )
                            OutlinedTextField(
                                value = silverPriceText,
                                onValueChange = { silverPriceText = sanitizeDecimalInput(it) },
                                label = { Text(tr(isEn, "রূপা / গ্রাম (৳)", "Silver / g (BDT)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("zakat_silver_price_input")
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${tr(isEn, "রূপা নিসাব (৬১২.৩৬ গ্রাম)", "Silver Nisab (612.36g)")}: ${ZakatCalculator.formatBdt(result.silverNisabBdt, isEn)}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${tr(isEn, "স্বর্ণ নিসাব (৮৭.৪৮ গ্রাম)", "Gold Nisab (87.48g)")}: ${ZakatCalculator.formatBdt(result.goldNisabBdt, isEn)}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onSaveMetalPrices(goldPriceVal, silverPriceVal)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("zakat_save_rates_button")
                            ) {
                                Text(tr(isEn, "বাজারদর সেভ করুন", "Save Rates"))
                            }
                            TextButton(
                                onClick = {
                                    goldPriceText = formatPlainNumber(ZakatCalculator.DEFAULT_GOLD_PRICE_PER_GRAM_BDT)
                                    silverPriceText = formatPlainNumber(ZakatCalculator.DEFAULT_SILVER_PRICE_PER_GRAM_BDT)
                                    onSaveMetalPrices(
                                        ZakatCalculator.DEFAULT_GOLD_PRICE_PER_GRAM_BDT,
                                        ZakatCalculator.DEFAULT_SILVER_PRICE_PER_GRAM_BDT
                                    )
                                }
                            ) {
                                Text(tr(isEn, "ডিফল্ট দর", "Defaults"))
                            }
                        }
                    }
                }
            }

            // Assets & Liabilities Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = tr(isEn, "আপনার সম্পদ ও দায় (টাকায়)", "Your Assets & Liabilities (BDT)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        NumericBdtField(
                            value = cashText,
                            onValueChange = { cashText = sanitizeDecimalInput(it) },
                            label = tr(isEn, "হাতে থাকা নগদ টাকা (৳)", "Cash in Hand (BDT)"),
                            tag = "zakat_cash_input"
                        )

                        NumericBdtField(
                            value = bankText,
                            onValueChange = { bankText = sanitizeDecimalInput(it) },
                            label = tr(isEn, "ব্যাংক ব্যালেন্স ও সঞ্চয় (৳)", "Bank Balance & Savings (BDT)"),
                            tag = "zakat_bank_input"
                        )

                        // Gold input + Gram/Tola selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr(isEn, "স্বর্ণের পরিমাণ:", "Gold Weight:"),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    WeightUnit.entries.forEach { unit ->
                                        FilterChip(
                                            selected = goldUnit == unit,
                                            onClick = { goldUnit = unit },
                                            label = { Text(unit.label(isEn), style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                            NumericBdtField(
                                value = goldWeightText,
                                onValueChange = { goldWeightText = sanitizeDecimalInput(it) },
                                label = "${tr(isEn, "স্বর্ণ", "Gold")} (${goldUnit.label(isEn)})",
                                supporting = if (result.goldValueBdt > 0.0) {
                                    "${tr(isEn, "মূল্য", "Value")}: ${ZakatCalculator.formatBdt(result.goldValueBdt, isEn)}"
                                } else null,
                                tag = "zakat_gold_weight_input"
                            )
                        }

                        // Silver input + Gram/Tola selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr(isEn, "রূপার পরিমাণ:", "Silver Weight:"),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    WeightUnit.entries.forEach { unit ->
                                        FilterChip(
                                            selected = silverUnit == unit,
                                            onClick = { silverUnit = unit },
                                            label = { Text(unit.label(isEn), style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                            NumericBdtField(
                                value = silverWeightText,
                                onValueChange = { silverWeightText = sanitizeDecimalInput(it) },
                                label = "${tr(isEn, "রূপা", "Silver")} (${silverUnit.label(isEn)})",
                                supporting = if (result.silverValueBdt > 0.0) {
                                    "${tr(isEn, "মূল্য", "Value")}: ${ZakatCalculator.formatBdt(result.silverValueBdt, isEn)}"
                                } else null,
                                tag = "zakat_silver_weight_input"
                            )
                        }

                        NumericBdtField(
                            value = businessText,
                            onValueChange = { businessText = sanitizeDecimalInput(it) },
                            label = tr(isEn, "ব্যবসায়িক পণ্য ও বিনিয়োগ (৳)", "Business Goods & Inventory (BDT)"),
                            tag = "zakat_business_input"
                        )

                        NumericBdtField(
                            value = debtsText,
                            onValueChange = { debtsText = sanitizeDecimalInput(it) },
                            label = tr(isEn, "ঋণ / দেনা — বাদ যাবে (৳)", "Debts / Loans to Subtract (BDT)"),
                            tag = "zakat_debts_input"
                        )
                    }
                }
            }

            // Saved Calculation History Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = tr(isEn, "সংরক্ষিত যাকাত ইতিহাস", "Saved Zakat History"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.zakatHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = tr(
                                isEn,
                                "এখনো কোনো যাকাত হিসাব সংরক্ষণ করা হয়নি। উপরে সম্পদের তথ্য দিয়ে 'হিসাব সংরক্ষণ করুন' বাটনে ট্যাপ করুন।",
                                "No saved Zakat calculations yet. Enter your wealth above and tap 'Save Calculation to History'."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.zakatHistory, key = { it.id }) { rec ->
                    val recCal = remember(rec.timestampMillis, uiState.activeTimeZoneId) {
                        Calendar.getInstance(TimeZone.getTimeZone(uiState.activeTimeZoneId)).apply {
                            timeInMillis = rec.timestampMillis
                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zakat_history_${rec.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = BanglaUtils.getGregorianDate(recCal, isEn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (rec.zakatDueBdt > 0.0) {
                                        "${tr(isEn, "যাকাত", "Zakat")}: ${ZakatCalculator.formatBdt(rec.zakatDueBdt, isEn)}"
                                    } else {
                                        tr(isEn, "যাকাত ফরজ নয়", "No Zakat due")
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${tr(isEn, "মোট সম্পদ", "Net Wealth")}: ${ZakatCalculator.formatBdt(rec.netWealthBdt, isEn)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteHistory(rec.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("zakat_delete_${rec.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = tr(isEn, "মুছুন", "Delete"),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun NumericBdtField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    supporting: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    )
}

private fun sanitizeDecimalInput(raw: String): String {
    // Allow both Bangla and ASCII digits plus single decimal point
    val ascii = buildString {
        for (ch in raw) {
            when (ch) {
                in '0'..'9', '.' -> append(ch)
                in '০'..'৯' -> append(('0'.code + (ch.code - '০'.code)).toChar())
            }
        }
    }
    val parts = ascii.split('.')
    return if (parts.size <= 1) ascii.take(12)
    else "${parts[0].take(10)}.${parts.drop(1).joinToString("").take(2)}"
}

private fun parseNumericInput(raw: String): Double? = sanitizeDecimalInput(raw).toDoubleOrNull()

private fun formatPlainNumber(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(java.util.Locale.US, "%.2f", value)
}
