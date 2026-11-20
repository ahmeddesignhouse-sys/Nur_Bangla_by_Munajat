package com.example.util

import java.util.Calendar
import java.util.Locale

data class DailyHadithItem(
    val id: String,
    val arabic: String,
    val bangla: String,
    val english: String,
    val sourceBn: String,
    val sourceEn: String,
    val narratorBn: String,
    val narratorEn: String
) {
    fun meaning(isEnglish: Boolean): String = if (isEnglish) english else bangla
    fun source(isEnglish: Boolean): String = if (isEnglish) sourceEn else sourceBn
    fun narrator(isEnglish: Boolean): String = if (isEnglish) narratorEn else narratorBn
}

data class DailyAyahItem(
    val id: String,
    val arabic: String,
    val bangla: String,
    val english: String,
    val sourceBn: String,
    val sourceEn: String
) {
    fun meaning(isEnglish: Boolean): String = if (isEnglish) english else bangla
    fun source(isEnglish: Boolean): String = if (isEnglish) sourceEn else sourceBn
}

data class DailyInspirationEntry(
    val dateKey: String,
    val daysAgo: Int,
    val gregorianDateBn: String,
    val gregorianDateEn: String,
    val hijriDateBn: String,
    val hijriDateEn: String,
    val hadith: DailyHadithItem,
    val ayah: DailyAyahItem
) {
    fun gregorianDate(isEnglish: Boolean): String = if (isEnglish) gregorianDateEn else gregorianDateBn
    fun hijriDate(isEnglish: Boolean): String = if (isEnglish) hijriDateEn else hijriDateBn

    fun formatShareText(isEnglish: Boolean): String = buildString {
        if (isEnglish) {
            appendLine("Nur Bangla — Daily Hadith & Ayah ($gregorianDateEn | $hijriDateEn)")
            appendLine()
            appendLine("Hadith of the Day (${hadith.sourceEn}):")
            appendLine(hadith.arabic)
            appendLine("English: ${hadith.english}")
            appendLine("Bangla: ${hadith.bangla}")
            appendLine()
            appendLine("Daily Ayah (${ayah.sourceEn}):")
            appendLine(ayah.arabic)
            appendLine("English: ${ayah.english}")
            appendLine("Bangla: ${ayah.bangla}")
        } else {
            appendLine("নূর বাংলা — দৈনিক হাদিস ও আয়াত ($gregorianDateBn | $hijriDateBn)")
            appendLine()
            appendLine("আজকের হাদিস (${hadith.sourceBn}):")
            appendLine(hadith.arabic)
            appendLine("বাংলা: ${hadith.bangla}")
            appendLine("English: ${hadith.english}")
            appendLine()
            appendLine("আজকের আয়াত (${ayah.sourceBn}):")
            appendLine(ayah.arabic)
            appendLine("বাংলা: ${ayah.bangla}")
            appendLine("English: ${ayah.english}")
        }
    }.trim()
}

object DailyInspirationCatalog {

    val HADITHS: List<DailyHadithItem> = listOf(
        DailyHadithItem(
            id = "h01",
            arabic = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
            bangla = "নিশ্চয়ই সকল আমল নিয়তের ওপর নির্ভরশীল। আর প্রত্যেক ব্যক্তি তাই পাবে যা সে নিয়ত করে।",
            english = "Actions are judged by intentions, and every person will get what they intended.",
            sourceBn = "সহীহ বুখারী ১ • সহীহ মুসলিম ১৯০৭",
            sourceEn = "Sahih al-Bukhari 1 • Sahih Muslim 1907",
            narratorBn = "হযরত উমর ইবনুল খাত্তাব (রা.)",
            narratorEn = "Narrated by Umar ibn al-Khattab (RA)"
        ),
        DailyHadithItem(
            id = "h02",
            arabic = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
            bangla = "তোমাদের মধ্যে সর্বোত্তম সেই ব্যক্তি, যে কুরআন শিক্ষা করে এবং অন্যকে শিক্ষা দেয়।",
            english = "The best among you are those who learn the Quran and teach it.",
            sourceBn = "সহীহ বুখারী ৫০২৭",
            sourceEn = "Sahih al-Bukhari 5027",
            narratorBn = "হযরত উসমান ইবনে আফফান (রা.)",
            narratorEn = "Narrated by Uthman ibn Affan (RA)"
        ),
        DailyHadithItem(
            id = "h03",
            arabic = "الطُّهُورُ شَطْرُ الإِيمَانِ، وَالْحَمْدُ لِلَّهِ تَمْلأُ الْمِيزَانَ",
            bangla = "পবিত্রতা ঈমানের অর্ধেক, আর আলহামদুলিল্লাহ (সমস্ত প্রশংসা আল্লাহর) পাল্লাকে পূর্ণ করে দেয়।",
            english = "Cleanliness is half of faith, and Alhamdulillah (all praise is for Allah) fills the scales.",
            sourceBn = "সহীহ মুসলিম ২২৩",
            sourceEn = "Sahih Muslim 223",
            narratorBn = "হযরত আবু মালিক আল-আশআরী (রা.)",
            narratorEn = "Narrated by Abu Malik al-Ash'ari (RA)"
        ),
        DailyHadithItem(
            id = "h04",
            arabic = "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
            bangla = "তোমাদের কেউ প্রকৃত মুমিন হতে পারবে না, যতক্ষণ না সে নিজের জন্য যা পছন্দ করে তার ভাইয়ের জন্যও তাই পছন্দ করে।",
            english = "None of you truly believes until he loves for his brother what he loves for himself.",
            sourceBn = "সহীহ বুখারী ১৩ • সহীহ মুসলিম ৪৫",
            sourceEn = "Sahih al-Bukhari 13 • Sahih Muslim 45",
            narratorBn = "হযরত আনাস ইবনে মালিক (রা.)",
            narratorEn = "Narrated by Anas ibn Malik (RA)"
        ),
        DailyHadithItem(
            id = "h05",
            arabic = "تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ",
            bangla = "তোমার ভাইয়ের দিকে তাকিয়ে মুচকি হাসা তোমার জন্য একটি সদকা (দান)।",
            english = "Your smiling in the face of your brother is an act of charity for you.",
            sourceBn = "জামে আত-তিরমিযী ১৯৫৬",
            sourceEn = "Jami at-Tirmidhi 1956",
            narratorBn = "হযরত আবু যর (রা.)",
            narratorEn = "Narrated by Abu Dharr (RA)"
        ),
        DailyHadithItem(
            id = "h06",
            arabic = "أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ",
            bangla = "আল্লাহর নিকট সর্বাধিক প্রিয় আমল হলো যা নিয়মিত করা হয়, যদিও তা পরিমাণে অল্প হয়।",
            english = "The most beloved deeds to Allah are those done consistently, even if they are small.",
            sourceBn = "সহীহ বুখারী ৬৪৬৪ • সহীহ মুসলিম ৭৮৩",
            sourceEn = "Sahih al-Bukhari 6464 • Sahih Muslim 783",
            narratorBn = "হযরত আয়েশা (রা.)",
            narratorEn = "Narrated by Aisha (RA)"
        ),
        DailyHadithItem(
            id = "h07",
            arabic = "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ",
            bangla = "যে ব্যক্তি জ্ঞান অন্বেষণের পথে চলে, আল্লাহ তার জন্য জান্নাতের পথ সহজ করে দেন।",
            english = "Whoever takes a path in search of knowledge, Allah will make easy for him a path to Paradise.",
            sourceBn = "সহীহ মুসলিম ২৬৯৯",
            sourceEn = "Sahih Muslim 2699",
            narratorBn = "হযরত আবু হুরায়রা (রা.)",
            narratorEn = "Narrated by Abu Hurairah (RA)"
        ),
        DailyHadithItem(
            id = "h08",
            arabic = "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
            bangla = "যে ব্যক্তি আল্লাহ ও শেষ দিবসের প্রতি ঈমান রাখে, সে যেন উত্তম কথা বলে অথবা চুপ থাকে।",
            english = "Whoever believes in Allah and the Last Day should speak good or remain silent.",
            sourceBn = "সহীহ বুখারী ৬০১৮ • সহীহ মুসলিম ৪৭",
            sourceEn = "Sahih al-Bukhari 6018 • Sahih Muslim 47",
            narratorBn = "হযরত আবু হুরায়রা (রা.)",
            narratorEn = "Narrated by Abu Hurairah (RA)"
        ),
        DailyHadithItem(
            id = "h09",
            arabic = "يَسِّرُوا وَلَا تُعَسِّرُوا، وَبَشِّرُوا وَلَا تُنَفِّرُوا",
            bangla = "তোমরা সহজ করো, কঠিন করো না; মানুষকে সুসংবাদ দাও, তাদের দূরে সরিয়ে দিও না।",
            english = "Make things easy and do not make them difficult; give glad tidings and do not drive people away.",
            sourceBn = "সহীহ বুখারী ৬৯ • সহীহ মুসলিম ১৭৩৪",
            sourceEn = "Sahih al-Bukhari 69 • Sahih Muslim 1734",
            narratorBn = "হযরত আনাস ইবনে মালিক (রা.)",
            narratorEn = "Narrated by Anas ibn Malik (RA)"
        ),
        DailyHadithItem(
            id = "h10",
            arabic = "الرَّاحِمُونَ يَرْحَمُهُمُ الرَّحْمَنُ، ارْحَمُوا مَنْ فِي الأَرْضِ يَرْحَمْكُمْ مَنْ فِي السَّمَاءِ",
            bangla = "দয়াশীলদের ওপর পরম করুণাময় আল্লাহ দয়া করেন। তোমরা পৃথিবীবাসীর প্রতি দয়া করো, আসমানের মালিক তোমাদের প্রতি দয়া করবেন।",
            english = "The Most Merciful shows mercy to the merciful. Show mercy to those on earth, and the One in the heavens will show mercy to you.",
            sourceBn = "সুনান আবু দাউদ ৪৯৪১ • তিরমিযী ১৯২৪",
            sourceEn = "Sunan Abi Dawud 4941 • Tirmidhi 1924",
            narratorBn = "হযরত আব্দুল্লাহ ইবনে আমর (রা.)",
            narratorEn = "Narrated by Abdullah ibn Amr (RA)"
        ),
        DailyHadithItem(
            id = "h11",
            arabic = "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ، وَمَا زَادَ اللَّهُ عَبْدًا بِعَفْوٍ إِلَّا عِزًّا",
            bangla = "সদকা (দান) করলে কখনো সম্পদ কমে না, আর ক্ষমা করলে আল্লাহ বান্দার সম্মান কেবল বৃদ্ধিই করেন।",
            english = "Charity never decreases wealth, and Allah only increases the honor of a servant who forgives.",
            sourceBn = "সহীহ মুসলিম ২৫৮৮",
            sourceEn = "Sahih Muslim 2588",
            narratorBn = "হযরত আবু হুরায়রা (রা.)",
            narratorEn = "Narrated by Abu Hurairah (RA)"
        ),
        DailyHadithItem(
            id = "h12",
            arabic = "الدُّعَاءُ هُوَ الْعِبَادَةُ",
            bangla = "দোয়া-ই হলো ইবাদতের মূল।",
            english = "Supplication (Dua) is the essence of worship.",
            sourceBn = "সুনান আবু দাউদ ১৪৭৯ • তিরমিযী ২৯৬৯",
            sourceEn = "Sunan Abi Dawud 1479 • Tirmidhi 2969",
            narratorBn = "হযরত নুমান ইবনে বাশির (রা.)",
            narratorEn = "Narrated by Nu'man ibn Bashir (RA)"
        ),
        DailyHadithItem(
            id = "h13",
            arabic = "لَيْسَ الشَّدِيدُ بِالصُّرَعَةِ، إِنَّمَا الشَّدِيدُ الَّذِي يَمْلِكُ نَفْسَهُ عِنْدَ الْغَضَبِ",
            bangla = "প্রকৃত শক্তিশালী সে নয় যে কুস্তিতে অন্যকে হারিয়ে দেয়; বরং প্রকৃত শক্তিশালী সেই যে রাগের সময় নিজেকে সংযত রাখে।",
            english = "The strong person is not the one who overcomes others in wrestling, but the one who controls himself in anger.",
            sourceBn = "সহীহ বুখারী ৬১১৪ • সহীহ মুসলিম ২৬০৯",
            sourceEn = "Sahih al-Bukhari 6114 • Sahih Muslim 2609",
            narratorBn = "হযরত আবু হুরায়রা (রা.)",
            narratorEn = "Narrated by Abu Hurairah (RA)"
        ),
        DailyHadithItem(
            id = "h14",
            arabic = "مَنْ صَلَّى الْبَرْدَيْنِ دَخَلَ الْجَنَّةَ",
            bangla = "যে ব্যক্তি দুই শীতল ওয়াক্তের নামাজ (ফজর ও আসর) নিয়মিত আদায় করবে, সে জান্নাতে প্রবেশ করবে।",
            english = "Whoever prays the two cool prayers (Fajr and Asr) regularly will enter Paradise.",
            sourceBn = "সহীহ বুখারী ৫৭৪ • সহীহ মুসলিম ৬৩৫",
            sourceEn = "Sahih al-Bukhari 574 • Sahih Muslim 635",
            narratorBn = "হযরত আবু মুসা আল-আশআরী (রা.)",
            narratorEn = "Narrated by Abu Musa al-Ash'ari (RA)"
        ),
        DailyHadithItem(
            id = "h15",
            arabic = "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            bangla = "দুটি বাক্য জিহ্বায় উচ্চারণে খুবই সহজ, মীযানের পাল্লায় অত্যন্ত ভারী এবং পরম করুণাময় আল্লাহর নিকট অতি প্রিয়: সুবহানাল্লাহি ওয়া বিহামদিহি, সুবহানাল্লাহিল আযীম।",
            english = "Two words are light on the tongue, heavy on the scale, and beloved to the Most Merciful: SubhanAllahi wa bihamdihi, SubhanAllahil-Azim.",
            sourceBn = "সহীহ বুখারী ৬৪০৬ • সহীহ মুসলিম ২৬৯৪",
            sourceEn = "Sahih al-Bukhari 6406 • Sahih Muslim 2694",
            narratorBn = "হযরত আবু হুরায়রা (রা.)",
            narratorEn = "Narrated by Abu Hurairah (RA)"
        ),
        DailyHadithItem(
            id = "h16",
            arabic = "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ",
            bangla = "তুমি যেখানেই থাকো আল্লাহকে ভয় করো, কোনো মন্দ কাজের পরপরই একটি ভালো কাজ করো যা মন্দকে মুছে দেবে এবং মানুষের সাথে উত্তম আচরণ করো।",
            english = "Fear Allah wherever you are, follow a bad deed with a good deed to erase it, and treat people with noble character.",
            sourceBn = "জামে আত-তিরমিযী ১৯৮৭",
            sourceEn = "Jami at-Tirmidhi 1987",
            narratorBn = "হযরত আবু যর (রা.)",
            narratorEn = "Narrated by Abu Dharr (RA)"
        ),
        DailyHadithItem(
            id = "h17",
            arabic = "مَنْ لَا يَشْكُرُ النَّاسَ لَا يَشْكُرُ اللَّهَ",
            bangla = "যে ব্যক্তি মানুষের প্রতি কৃতজ্ঞতা প্রকাশ করে না, সে আল্লাহর প্রতিও কৃতজ্ঞতা প্রকাশ করে না।",
            english = "Whoever is not grateful to people is not grateful to Allah.",
            sourceBn = "সুনান আবু দাউদ ৪৮১১ • তিরমিযী ১৯৫৪",
            sourceEn = "Sunan Abi Dawud 4811 • Tirmidhi 1954",
            narratorBn = "হযরত আবু হুরায়রা (রা.)",
            narratorEn = "Narrated by Abu Hurairah (RA)"
        ),
        DailyHadithItem(
            id = "h18",
            arabic = "الدَّالُّ عَلَى الْخَيْرِ كَفَاعِلِهِ",
            bangla = "যে ব্যক্তি কোনো ভালো কাজের পথ দেখায়, সে ওই ভালো কাজ সম্পাদনকারীর সমান সওয়াব পায়।",
            english = "The one who guides someone to a good deed will have a reward equal to the one who does it.",
            sourceBn = "সহীহ মুসলিম ১৮৯৩ • তিরমিযী ২৬৭০",
            sourceEn = "Sahih Muslim 1893 • Tirmidhi 2670",
            narratorBn = "হযরত আবু মাসউদ আল-আনসারী (রা.)",
            narratorEn = "Narrated by Abu Mas'ud al-Ansari (RA)"
        ),
        DailyHadithItem(
            id = "h19",
            arabic = "وَاللَّهُ فِي عَوْنِ الْعَبْدِ مَا كَانَ الْعَبْدُ فِي عَوْنِ أَخِيهِ",
            bangla = "আল্লাহ ততক্ষণ বান্দার সাহায্যে থাকেন, যতক্ষণ বান্দা তার ভাইয়ের সাহায্যে নিয়োজিত থাকে।",
            english = "Allah remains in the aid of a servant as long as the servant remains in the aid of his brother.",
            sourceBn = "সহীহ মুসলিম ২৬৯৯",
            sourceEn = "Sahih Muslim 2699",
            narratorBn = "হযরত আবু হুরায়রা (রা.)",
            narratorEn = "Narrated by Abu Hurairah (RA)"
        ),
        DailyHadithItem(
            id = "h20",
            arabic = "إِنَّ اللَّهَ لَا يَنْظُرُ إِلَى صُوَرِكُمْ وَأَمْوَالِكُمْ، وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ وَأَعْمَالِكُمْ",
            bangla = "নিশ্চয়ই আল্লাহ তোমাদের বাহ্যিক অবয়ব ও সম্পদের দিকে তাকান না; বরং তিনি তোমাদের অন্তর ও আমলের দিকে তাকান।",
            english = "Verily, Allah does not look at your appearance or wealth, but rather He looks at your hearts and deeds.",
            sourceBn = "সহীহ মুসলিম ২৫৬৪",
            sourceEn = "Sahih Muslim 2564",
            narratorBn = "হযরত আবু হুরায়রা (রা.)",
            narratorEn = "Narrated by Abu Hurairah (RA)"
        ),
        DailyHadithItem(
            id = "h21",
            arabic = "لَا تَحْقِرَنَّ مِنَ الْمَعْرُوفِ شَيْئًا، وَلَوْ أَنْ تَلْقَى أَخَاكَ بِوَجْهٍ طَلْقٍ",
            bangla = "কোনো ভালো কাজকে কখনো তুচ্ছ মনে করো না, যদিও তা তোমার ভাইয়ের সাথে হাসিমুখে সাক্ষাৎ করা হয়।",
            english = "Do not belittle any good deed, even meeting your brother with a cheerful face.",
            sourceBn = "সহীহ মুসলিম ২৬২৬",
            sourceEn = "Sahih Muslim 2626",
            narratorBn = "হযরত আবু যর (রা.)",
            narratorEn = "Narrated by Abu Dharr (RA)"
        )
    )

    val AYAHS: List<DailyAyahItem> = listOf(
        DailyAyahItem(
            id = "a01",
            arabic = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            bangla = "নিশ্চয়ই কষ্টের সাথেই স্বস্তি রয়েছে। নিশ্চয়ই কষ্টের সাথেই স্বস্তি রয়েছে।",
            english = "For indeed, with hardship comes ease. Indeed, with hardship comes ease.",
            sourceBn = "কুরআন — সূরা আল-ইনশিরাহ ৯৪:৫-৬",
            sourceEn = "Quran — Surah Ash-Sharh 94:5-6"
        ),
        DailyAyahItem(
            id = "a02",
            arabic = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
            bangla = "সুতরাং তোমরা আমাকে স্মরণ করো, আমিও তোমাদের স্মরণ করবো। আমার কৃতজ্ঞতা প্রকাশ করো এবং অকৃতজ্ঞ হয়ো না।",
            english = "So remember Me; I will remember you. And be grateful to Me and do not deny Me.",
            sourceBn = "কুরআন — সূরা আল-বাকারা ২:১৫২",
            sourceEn = "Quran — Surah Al-Baqarah 2:152"
        ),
        DailyAyahItem(
            id = "a03",
            arabic = "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
            bangla = "আল্লাহ কোনো ব্যক্তিকে তার সাধ্যের অতিরিক্ত বোঝা চাপিয়ে দেন না।",
            english = "Allah does not burden a soul beyond that it can bear.",
            sourceBn = "কুরআন — সূরা আল-বাকারা ২:২৮৬",
            sourceEn = "Quran — Surah Al-Baqarah 2:286"
        ),
        DailyAyahItem(
            id = "a04",
            arabic = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            bangla = "জেনে রেখো, আল্লাহর স্মরণেই অন্তরসমূহ প্রশান্তি লাভ করে।",
            english = "Unquestionably, by the remembrance of Allah do hearts find tranquility.",
            sourceBn = "কুরআন — সূরা আর-রাদ ১৩:২৮",
            sourceEn = "Quran — Surah Ar-Ra'd 13:28"
        ),
        DailyAyahItem(
            id = "a05",
            arabic = "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ",
            bangla = "আমার বান্দারা যখন আমার সম্পর্কে তোমাকে জিজ্ঞেস করে, তখন বলো— আমি তো নিকটেই আছি। যে আহ্বানকারী আমাকে ডাকে, আমি তার ডাকে সাড়া দিই।",
            english = "And when My servants ask you concerning Me — indeed I am near. I respond to the invocation of the supplicant when he calls upon Me.",
            sourceBn = "কুরআন — সূরা আল-বাকারা ২:১৮৬",
            sourceEn = "Quran — Surah Al-Baqarah 2:186"
        ),
        DailyAyahItem(
            id = "a06",
            arabic = "وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
            bangla = "যে ব্যক্তি আল্লাহর ওপর ভরসা করে, তাঁর জন্য তিনিই যথেষ্ট।",
            english = "And whoever relies upon Allah — then He is sufficient for him.",
            sourceBn = "কুরআন — সূরা আত-তালাক ৬৫:৩",
            sourceEn = "Quran — Surah At-Talaq 65:3"
        ),
        DailyAyahItem(
            id = "a07",
            arabic = "لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ",
            bangla = "তোমরা যদি কৃতজ্ঞতা প্রকাশ করো, তবে আমি অবশ্যই তোমাদের নিয়ামত আরও বাড়িয়ে দেবো।",
            english = "If you are grateful, I will surely increase you in favor.",
            sourceBn = "কুরআন — সূরা ইবরাহীম ১৪:৭",
            sourceEn = "Quran — Surah Ibrahim 14:7"
        ),
        DailyAyahItem(
            id = "a08",
            arabic = "إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",
            bangla = "নিশ্চয়ই আল্লাহ ধৈর্যশীলদের সাথে আছেন।",
            english = "Indeed, Allah is with the patient.",
            sourceBn = "কুরআন — সূরা আল-বাকারা ২:১৫৩",
            sourceEn = "Quran — Surah Al-Baqarah 2:153"
        ),
        DailyAyahItem(
            id = "a09",
            arabic = "وَقُل رَّبِّ زِدْنِي عِلْمًا",
            bangla = "আর বলো: হে আমার প্রতিপালক! আমার জ্ঞান বৃদ্ধি করে দিন।",
            english = "And say: My Lord, increase me in knowledge.",
            sourceBn = "কুরআন — সূরা ত্ব-হা ২০:১১৪",
            sourceEn = "Quran — Surah Ta-Ha 20:114"
        ),
        DailyAyahItem(
            id = "a10",
            arabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            bangla = "হে আমাদের প্রতিপালক! আমাদের দুনিয়াতে কল্যাণ দান করুন এবং আখিরাতেও কল্যাণ দান করুন, আর আমাদের জাহান্নামের আজাব থেকে রক্ষা করুন।",
            english = "Our Lord, grant us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.",
            sourceBn = "কুরআন — সূরা আল-বাকারা ২:২০১",
            sourceEn = "Quran — Surah Al-Baqarah 2:201"
        ),
        DailyAyahItem(
            id = "a11",
            arabic = "وَمَا تَوْفِيقِي إِلَّا بِاللَّهِ ۚ عَلَيْهِ تَوَكَّلْتُ وَإِلَيْهِ أُنِيبُ",
            bangla = "আমার তাওফীক তো কেবল আল্লাহর পক্ষ থেকেই। আমি তাঁর ওপরই ভরসা করেছি এবং তাঁর দিকেই ফিরে যাই।",
            english = "And my success is not but through Allah. Upon Him I have relied, and to Him I return.",
            sourceBn = "কুরআন — সূরা হুদ ১১:৮৮",
            sourceEn = "Quran — Surah Hud 11:88"
        ),
        DailyAyahItem(
            id = "a12",
            arabic = "قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ ۚ إِنَّ اللَّهَ يَغْفِرُ الذُّنُوبَ جَمِيعًا",
            bangla = "বলো, হে আমার বান্দারা যারা নিজেদের প্রতি সীমালঙ্ঘন করেছো, আল্লাহর রহমত থেকে নিরাশ হয়ো না। নিশ্চয়ই আল্লাহ সকল গুনাহ ক্ষমা করে দেন।",
            english = "Say, O My servants who have transgressed against themselves, do not despair of the mercy of Allah. Indeed, Allah forgives all sins.",
            sourceBn = "কুরআন — সূরা আয-যুমার ৩৯:৫৩",
            sourceEn = "Quran — Surah Az-Zumar 39:53"
        ),
        DailyAyahItem(
            id = "a13",
            arabic = "إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنكَرِ",
            bangla = "নিশ্চয়ই নামাজ মানুষকে অশ্লীল ও মন্দ কাজ থেকে বিরত রাখে।",
            english = "Indeed, prayer prohibits immorality and wrongdoing.",
            sourceBn = "কুরআন — সূরা আল-আনকাবুত ২৯:৪৫",
            sourceEn = "Quran — Surah Al-Ankabut 29:45"
        ),
        DailyAyahItem(
            id = "a14",
            arabic = "وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ",
            bangla = "তোমরা যেখানেই থাকো না কেন, তিনি তোমাদের সাথেই আছেন।",
            english = "And He is with you wherever you are.",
            sourceBn = "কুরআন — সূরা আল-হাদীদ ৫৭:৪",
            sourceEn = "Quran — Surah Al-Hadid 57:4"
        ),
        DailyAyahItem(
            id = "a15",
            arabic = "وَرَحْمَتِي وَسِعَتْ كُلَّ شَيْءٍ",
            bangla = "আর আমার রহমত প্রতিটি বস্তুকেই পরিবেষ্টন করে আছে।",
            english = "And My mercy encompasses all things.",
            sourceBn = "কুরআন — সূরা আল-আরাফ ৭:১৫৬",
            sourceEn = "Quran — Surah Al-A'raf 7:156"
        ),
        DailyAyahItem(
            id = "a16",
            arabic = "وَتَعَاوَنُوا عَلَى الْبِرِّ وَالتَّقْوَىٰ وَلَا تَعَاوَنُوا عَلَى الْإِثْمِ وَالْعُدْوَانِ",
            bangla = "তোমরা সৎকর্ম ও তাকওয়ার কাজে একে অপরকে সহযোগিতা করো, পাপ ও সীমালঙ্ঘনের কাজে সহযোগিতা করো না।",
            english = "And cooperate in righteousness and piety, but do not cooperate in sin and aggression.",
            sourceBn = "কুরআন — সূরা আল-মায়িদাহ ৫:২",
            sourceEn = "Quran — Surah Al-Ma'idah 5:2"
        ),
        DailyAyahItem(
            id = "a17",
            arabic = "فَمَن يَعْمَلْ مِثْقَالَ ذَرَّةٍ خَيْرًا يَرَهُ",
            bangla = "অতঃপর কেউ অণু পরিমাণ সৎকাজ করলে তা সে দেখতে পাবে।",
            english = "So whoever does an atom's weight of good will see it.",
            sourceBn = "কুরআন — সূরা আয-যিলযাল ৯৯:৭",
            sourceEn = "Quran — Surah Az-Zalzalah 99:7"
        ),
        DailyAyahItem(
            id = "a18",
            arabic = "إِنَّ اللَّهَ يُحِبُّ الْمُحْسِنِينَ",
            bangla = "নিশ্চয়ই আল্লাহ সৎকর্মশীলদের ভালোবাসেন।",
            english = "Indeed, Allah loves the doers of good.",
            sourceBn = "কুরআন — সূরা আল-বাকারা ২:১৯৫",
            sourceEn = "Quran — Surah Al-Baqarah 2:195"
        ),
        DailyAyahItem(
            id = "a19",
            arabic = "وَقُولُوا لِلنَّاسِ حُسْنًا",
            bangla = "আর তোমরা মানুষের সাথে উত্তম ও সুন্দর ভাষায় কথা বলো।",
            english = "And speak to people with good and kind words.",
            sourceBn = "কুরআন — সূরা আল-বাকারা ২:৮৩",
            sourceEn = "Quran — Surah Al-Baqarah 2:83"
        ),
        DailyAyahItem(
            id = "a20",
            arabic = "وَمَا أَرْسَلْنَاكَ إِلَّا رَحْمَةً لِّلْعَالَمِينَ",
            bangla = "আর আমি তো আপনাকে বিশ্বজগতের প্রতি কেবল রহমতস্বরূপই প্রেরণ করেছি।",
            english = "And We have not sent you, [O Muhammad], except as a mercy to the worlds.",
            sourceBn = "কুরআন — সূরা আল-আম্বিয়া ২১:১০৭",
            sourceEn = "Quran — Surah Al-Anbiya 21:107"
        ),
        DailyAyahItem(
            id = "a21",
            arabic = "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً",
            bangla = "হে আমাদের প্রতিপালক! আপনি আমাদের হেদায়াত দান করার পর আমাদের অন্তরকে বক্র করবেন না এবং আপনার পক্ষ থেকে আমাদের রহমত দান করুন।",
            english = "Our Lord, let not our hearts deviate after You have guided us and grant us from Yourself mercy.",
            sourceBn = "কুরআন — সূরা আলে ইমরান ৩:৮",
            sourceEn = "Quran — Surah Ali 'Imran 3:8"
        )
    )

    fun daySerial(calendar: Calendar): Int {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        // Civil-date serial number (timezone-safe integer day index)
        val a = (14 - m) / 12
        val y2 = y + 4800 - a
        val m2 = m + 12 * a - 3
        return d + (153 * m2 + 2) / 5 + 365 * y2 + y2 / 4 - y2 / 100 + y2 / 400 - 32045
    }

    fun dateKeyFor(calendar: Calendar): String {
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun forDate(
        calendar: Calendar,
        hijriDayOffset: Int = 0,
        daysAgo: Int = 0
    ): DailyInspirationEntry {
        val serial = daySerial(calendar)
        val hadithIndex = Math.floorMod(serial, HADITHS.size)
        val ayahIndex = Math.floorMod(serial + 3, AYAHS.size)
        return DailyInspirationEntry(
            dateKey = dateKeyFor(calendar),
            daysAgo = daysAgo,
            gregorianDateBn = BanglaUtils.getGregorianDate(calendar, isEnglish = false),
            gregorianDateEn = BanglaUtils.getGregorianDate(calendar, isEnglish = true),
            hijriDateBn = BanglaUtils.getHijriDate(calendar, false, hijriDayOffset),
            hijriDateEn = BanglaUtils.getHijriDate(calendar, true, hijriDayOffset),
            hadith = HADITHS[hadithIndex],
            ayah = AYAHS[ayahIndex]
        )
    }

    fun todayAndPreviousSevenDays(
        todayCalendar: Calendar,
        hijriDayOffset: Int = 0
    ): List<DailyInspirationEntry> {
        return (0..7).map { offset ->
            val cal = (todayCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            forDate(cal, hijriDayOffset = hijriDayOffset, daysAgo = offset)
        }
    }
}
