package com.example.util

import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class WeightUnit(val labelBn: String, val labelEn: String) {
    GRAM("গ্রাম", "Grams"),
    TOLA("তোলা / ভরি", "Tola");

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

enum class NisabBasis(val labelBn: String, val labelEn: String) {
    SILVER("রূপা নিসাব (৬১২.৩৬ গ্রাম)", "Silver Nisab (612.36g)"),
    GOLD("স্বর্ণ নিসাব (৮৭.৪৮ গ্রাম)", "Gold Nisab (87.48g)");

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn

    companion object {
        fun safeValueOf(raw: String): NisabBasis =
            entries.find { it.name == raw } ?: SILVER
    }
}

data class ZakatInput(
    val cashInHandBdt: Double = 0.0,
    val bankBalanceBdt: Double = 0.0,
    val goldWeight: Double = 0.0,
    val goldUnit: WeightUnit = WeightUnit.GRAM,
    val silverWeight: Double = 0.0,
    val silverUnit: WeightUnit = WeightUnit.GRAM,
    val businessGoodsBdt: Double = 0.0,
    val debtsBdt: Double = 0.0,
    val goldPricePerGramBdt: Double = ZakatCalculator.DEFAULT_GOLD_PRICE_PER_GRAM_BDT,
    val silverPricePerGramBdt: Double = ZakatCalculator.DEFAULT_SILVER_PRICE_PER_GRAM_BDT,
    val nisabBasis: NisabBasis = NisabBasis.SILVER
)

data class ZakatCalculationResult(
    val goldValueBdt: Double,
    val silverValueBdt: Double,
    val grossWealthBdt: Double,
    val debtsBdt: Double,
    val netZakatableWealthBdt: Double,
    val goldNisabBdt: Double,
    val silverNisabBdt: Double,
    val activeNisabBdt: Double,
    val isZakatDue: Boolean,
    val zakatDueBdt: Double
)

data class ZakatHistoryRecord(
    val id: String,
    val timestampMillis: Long,
    val netWealthBdt: Double,
    val nisabThresholdBdt: Double,
    val zakatDueBdt: Double,
    val nisabBasis: NisabBasis
)

object ZakatCalculator {
    const val GRAMS_PER_TOLA = 11.664
    const val GOLD_NISAB_GRAMS = 87.48   // 7.5 tola
    const val SILVER_NISAB_GRAMS = 612.36 // 52.5 tola
    const val ZAKAT_RATE = 0.025

    // Editable default market reference rates in BDT per gram (Bangladesh)
    const val DEFAULT_GOLD_PRICE_PER_GRAM_BDT = 13500.0
    const val DEFAULT_SILVER_PRICE_PER_GRAM_BDT = 210.0

    fun toGrams(weight: Double, unit: WeightUnit): Double {
        val safe = weight.coerceAtLeast(0.0)
        return when (unit) {
            WeightUnit.GRAM -> safe
            WeightUnit.TOLA -> safe * GRAMS_PER_TOLA
        }
    }

    fun calculate(input: ZakatInput): ZakatCalculationResult {
        val cash = input.cashInHandBdt.coerceAtLeast(0.0)
        val bank = input.bankBalanceBdt.coerceAtLeast(0.0)
        val business = input.businessGoodsBdt.coerceAtLeast(0.0)
        val debts = input.debtsBdt.coerceAtLeast(0.0)
        val goldPrice = input.goldPricePerGramBdt.coerceAtLeast(0.0)
        val silverPrice = input.silverPricePerGramBdt.coerceAtLeast(0.0)

        val goldGrams = toGrams(input.goldWeight, input.goldUnit)
        val silverGrams = toGrams(input.silverWeight, input.silverUnit)

        val goldValue = goldGrams * goldPrice
        val silverValue = silverGrams * silverPrice
        val grossWealth = cash + bank + goldValue + silverValue + business
        val netWealth = (grossWealth - debts).coerceAtLeast(0.0)

        val goldNisab = GOLD_NISAB_GRAMS * goldPrice
        val silverNisab = SILVER_NISAB_GRAMS * silverPrice
        val activeNisab = when (input.nisabBasis) {
            NisabBasis.GOLD -> goldNisab
            NisabBasis.SILVER -> silverNisab
        }

        val due = activeNisab > 0.0 && netWealth >= activeNisab
        val zakatAmount = if (due) netWealth * ZAKAT_RATE else 0.0

        return ZakatCalculationResult(
            goldValueBdt = goldValue,
            silverValueBdt = silverValue,
            grossWealthBdt = grossWealth,
            debtsBdt = debts,
            netZakatableWealthBdt = netWealth,
            goldNisabBdt = goldNisab,
            silverNisabBdt = silverNisab,
            activeNisabBdt = activeNisab,
            isZakatDue = due,
            zakatDueBdt = zakatAmount
        )
    }

    fun formatBdt(amount: Double, isEnglish: Boolean): String {
        val formatter = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
        val formatted = formatter.format(amount.coerceAtLeast(0.0))
        return if (isEnglish) {
            "BDT $formatted"
        } else {
            "৳ ${BanglaUtils.toBanglaDigits(formatted)}"
        }
    }

    fun parseHistoryJson(json: String): List<ZakatHistoryRecord> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = ArrayList<ZakatHistoryRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ZakatHistoryRecord(
                        id = obj.getString("id"),
                        timestampMillis = obj.getLong("timestampMillis"),
                        netWealthBdt = obj.getDouble("netWealthBdt"),
                        nisabThresholdBdt = obj.getDouble("nisabThresholdBdt"),
                        zakatDueBdt = obj.getDouble("zakatDueBdt"),
                        nisabBasis = NisabBasis.safeValueOf(obj.optString("nisabBasis", NisabBasis.SILVER.name))
                    )
                )
            }
            list.sortedByDescending { it.timestampMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeHistoryJson(records: List<ZakatHistoryRecord>): String {
        val arr = JSONArray()
        records.take(50).forEach { rec ->
            val obj = JSONObject()
            obj.put("id", rec.id)
            obj.put("timestampMillis", rec.timestampMillis)
            obj.put("netWealthBdt", rec.netWealthBdt)
            obj.put("nisabThresholdBdt", rec.nisabThresholdBdt)
            obj.put("zakatDueBdt", rec.zakatDueBdt)
            obj.put("nisabBasis", rec.nisabBasis.name)
            arr.put(obj)
        }
        return arr.toString()
    }
}
