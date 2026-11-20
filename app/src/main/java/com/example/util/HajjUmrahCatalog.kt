package com.example.util

enum class HajjStepCategory(val titleBn: String, val titleEn: String) {
    ALL("সকল ধাপ", "All Steps"),
    UMRAH("উমরাহ", "Umrah"),
    HAJJ("হজ্জ", "Hajj");

    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn
}

data class HajjGuideStep(
    val id: String,
    val stepNumber: Int,
    val isUmrah: Boolean,
    val isHajj: Boolean,
    val titleBn: String,
    val titleEn: String,
    val subtitleBn: String,
    val subtitleEn: String,
    val descriptionBn: String,
    val descriptionEn: String,
    val duaTitleBn: String,
    val duaTitleEn: String,
    val duaArabic: String,
    val duaBangla: String,
    val duaEnglish: String,
    val checklistPromptBn: String,
    val checklistPromptEn: String
) {
    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn
    fun subtitle(isEnglish: Boolean): String = if (isEnglish) subtitleEn else subtitleBn
    fun description(isEnglish: Boolean): String = if (isEnglish) descriptionEn else descriptionBn
    fun duaTitle(isEnglish: Boolean): String = if (isEnglish) duaTitleEn else duaTitleBn
    fun checklistPrompt(isEnglish: Boolean): String = if (isEnglish) checklistPromptEn else checklistPromptBn
}

data class HajjPracticalSection(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val itemsBn: List<String>,
    val itemsEn: List<String>
) {
    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn
    fun items(isEnglish: Boolean): List<String> = if (isEnglish) itemsEn else itemsBn
}

object HajjUmrahCatalog {

    val STEPS: List<HajjGuideStep> = listOf(
        HajjGuideStep(
            id = "step_01_ihram",
            stepNumber = 1,
            isUmrah = true,
            isHajj = true,
            titleBn = "ইহরাম ও নিয়ত (মীকাত)",
            titleEn = "Ihram & Intention (Miqat)",
            subtitleBn = "মীকাত অতিক্রমের পূর্বে ইহরাম বাঁধা",
            subtitleEn = "Enter Ihram before crossing Miqat",
            descriptionBn = "মীকাতে পৌঁছার আগেই গোসল করে পরিচ্ছন্ন হোন। পুরুষরা সেলাইবিহীন দুটি সাদা চাদর পরিধান করবেন এবং নারীরা স্বাভাবিক শালীন পোশাক পরবেন (মুখ ও হাত খোলা থাকবে)। দুই রাকাত নফল নামাজ পড়ে হজ্জ বা উমরাহর নিয়ত করুন।",
            descriptionEn = "Perform Ghusl and maintain cleanliness before reaching Miqat. Men wear two unstitched white sheets; women wear modest everyday clothing keeping face and hands uncovered. Pray two rak'ahs if possible and make the intention for Umrah or Hajj.",
            duaTitleBn = "ইহরামের নিয়ত ও তালবিয়াহ",
            duaTitleEn = "Intention & Talbiyah",
            duaArabic = "لَبَّيْكَ اللَّهُمَّ عُمْرَةً / حَجًّا — لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ",
            duaBangla = "হে আল্লাহ! আমি উমরাহ / হজ্জের জন্য উপস্থিত হয়েছি। আমি হাজির হে আল্লাহ আমি হাজির! তোমার কোনো শরীক নেই। নিশ্চয়ই সমস্ত প্রশংসা, নিয়ামত ও রাজত্ব একমাত্র তোমারই।",
            duaEnglish = "Here I am, O Allah, for Umrah / Hajj. Here I am at Your service, You have no partner. Truly all praise, grace, and sovereignty belong to You alone.",
            checklistPromptBn = "গোসল, ইহরামের পোশাক ও নিয়ত সম্পন্ন হয়েছে",
            checklistPromptEn = "Ghusl, Ihram garments, and intention completed"
        ),
        HajjGuideStep(
            id = "step_02_masjid_haram",
            stepNumber = 2,
            isUmrah = true,
            isHajj = true,
            titleBn = "তালবিয়াহ ও মসজিদুল হারামে প্রবেশ",
            titleEn = "Talbiyah & Entering Masjid al-Haram",
            subtitleBn = "ডান পা দিয়ে প্রবেশ ও কাবা দর্শনের দোয়া",
            subtitleEn = "Enter with right foot & Dua upon seeing the Kaaba",
            descriptionBn = "পথে তালবিয়াহ পড়তে থাকুন (পুরুষরা উচ্চস্বরে, নারীরা নিম্নস্বরে)। ওযুসহ ডান পা দিয়ে মসজিদুল হারামে প্রবেশ করুন। প্রথমবার কাবা শরীফ দেখার সাথে সাথে আন্তরিকভাবে দোয়া করুন, কারণ এ সময় দোয়া কবুল হয়।",
            descriptionEn = "Continue reciting Talbiyah along the way (audibly for men, softly for women). Enter Masjid al-Haram in Wudu with your right foot. Upon first seeing the Holy Kaaba, raise your hands and make heartfelt Dua.",
            duaTitleBn = "মসজিদে প্রবেশের দোয়া",
            duaTitleEn = "Dua for Entering the Mosque",
            duaArabic = "بِسْمِ اللَّهِ وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            duaBangla = "আল্লাহর নামে এবং রাসূলুল্লাহ (সা.)-এর প্রতি দরুদ ও সালাম। হে আল্লাহ! আমার জন্য আপনার রহমতের দরজাসমূহ খুলে দিন।",
            duaEnglish = "In the name of Allah, and peace and blessings upon the Messenger of Allah. O Allah, open for me the doors of Your mercy.",
            checklistPromptBn = "ওযুসহ মসজিদুল হারামে প্রবেশ ও কাবা দর্শনে দোয়া করা হয়েছে",
            checklistPromptEn = "Entered Masjid al-Haram in Wudu and made Dua upon seeing the Kaaba"
        ),
        HajjGuideStep(
            id = "step_03_tawaf",
            stepNumber = 3,
            isUmrah = true,
            isHajj = true,
            titleBn = "তাওয়াফ (কাবা শরীফ ৭ চক্কর)",
            titleEn = "Tawaf (7 Circuits around Kaaba)",
            subtitleBn = "হাজরে আসওয়াদ থেকে শুরু করে ৭ চক্কর",
            subtitleEn = "7 counter-clockwise circuits from Hajr al-Aswad",
            descriptionBn = "পুরুষরা ইযতিবা (ডান কাঁধ খোলা রাখা) করবেন। হাজরে আসওয়াদের বরাবর দাঁড়িয়ে ইশারা করে 'বিসমিল্লাহি আল্লাহু আকবার' বলে তাওয়াফ শুরু করুন। কাবা বাম দিকে রেখে ৭ চক্কর সম্পন্ন করুন। পুরুষরা প্রথম ৩ চক্করে রমল (দ্রুত পদক্ষেপে হাঁটা) করবেন। রুকনে ইয়ামানি থেকে হাজরে আসওয়াদ পর্যন্ত 'রাব্বানা আতিনা...' পড়ুন।",
            descriptionEn = "Men perform Idtiba (uncover the right shoulder). Align with Hajr al-Aswad, raise your right hand saying 'Bismillahi Allahu Akbar', and complete 7 counter-clockwise circuits keeping the Kaaba on your left. Men perform Raml (brisk walk) in the first 3 circuits. Recite Rabbana Atina between Rukn al-Yamani and Hajr al-Aswad.",
            duaTitleBn = "রুকনে ইয়ামানি ও হাজরে আসওয়াদের মাঝে দোয়া",
            duaTitleEn = "Dua between Rukn al-Yamani & Hajr al-Aswad",
            duaArabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            duaBangla = "হে আমাদের প্রতিপালক! আমাদের দুনিয়ায় কল্যাণ দিন এবং আখিরাতেও কল্যাণ দিন, আর আমাদের জাহান্নামের আজাব থেকে রক্ষা করুন।",
            duaEnglish = "Our Lord, grant us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.",
            checklistPromptBn = "হাজরে আসওয়াদ থেকে ৭ চক্কর তাওয়াফ সম্পন্ন হয়েছে",
            checklistPromptEn = "Completed all 7 circuits of Tawaf starting from Hajr al-Aswad"
        ),
        HajjGuideStep(
            id = "step_04_maqam_zamzam",
            stepNumber = 4,
            isUmrah = true,
            isHajj = true,
            titleBn = "মাকামে ইবরাহীমে ২ রাকাত ও যমযম পান",
            titleEn = "Two Rak'ahs at Maqam Ibrahim & Zamzam",
            subtitleBn = "তাওয়াফ শেষে নামাজ ও দোয়াসহ যমযম পান",
            subtitleEn = "Pray 2 rak'ahs & drink Zamzam water",
            descriptionBn = "তাওয়াফ শেষে পুরুষরা ডান কাঁধ ঢেকে নিন। মাকামে ইবরাহীমের পেছনে (অথবা ভিড় থাকলে মসজিদের যেকোনো স্থানে) ২ রাকাত নামাজ আদায় করুন (প্রথম রাকাতে সূরা কাফিরুন ও দ্বিতীয় রাকাতে সূরা ইখলাস পড়া সুন্নত)। এরপর কিবলামুখী হয়ে দোয়া পড়ে পরিতৃপ্ত হয়ে যমযম পানি পান করুন।",
            descriptionEn = "After Tawaf, men cover both shoulders. Pray 2 rak'ahs behind Maqam Ibrahim (or anywhere in the mosque if crowded), reciting Surah Al-Kafirun in the 1st and Surah Al-Ikhlas in the 2nd. Then drink Zamzam water facing the Qibla while making Dua.",
            duaTitleBn = "যমযম পানি পানের দোয়া",
            duaTitleEn = "Dua when Drinking Zamzam Water",
            duaArabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا وَاسِعًا، وَشِفَاءً مِنْ كُلِّ دَاءٍ",
            duaBangla = "হে আল্লাহ! আমি আপনার নিকট উপকারী জ্ঞান, প্রশস্ত রিযিক এবং সকল রোগ থেকে আরোগ্য প্রার্থনা করছি।",
            duaEnglish = "O Allah, I ask You for beneficial knowledge, abundant provision, and healing from every illness.",
            checklistPromptBn = "২ রাকাত ওয়াজিবুত তাওয়াফ ও যমযম পান সম্পন্ন হয়েছে",
            checklistPromptEn = "Prayed 2 rak'ahs after Tawaf and drank Zamzam water"
        ),
        HajjGuideStep(
            id = "step_05_sai",
            stepNumber = 5,
            isUmrah = true,
            isHajj = true,
            titleBn = "সায়ী (সাফা ও মারওয়া পাহাড়ের মাঝে ৭ চক্কর)",
            titleEn = "Sa'i (7 Laps between Safa & Marwah)",
            subtitleBn = "সাফা থেকে শুরু, মারওয়ায় ৭ম চক্কর শেষ",
            subtitleEn = "Start at Safa, conclude 7th lap at Marwah",
            descriptionBn = "সাফা পাহাড়ে উঠে কিবলামুখী হয়ে দোয়া পড়ে মারওয়ার দিকে যাত্রা শুরু করুন। সাফা থেকে মারওয়া ১ চক্কর এবং মারওয়া থেকে সাফা ২য় চক্কর — এভাবে মোট ৭ চক্কর মারওয়ায় শেষ করুন। দুটি সবুজ বাতির মাঝখানে পুরুষরা হালকা দৌড়াবেন (রমল); নারীরা স্বাভাবিকভাবে হাঁটবেন।",
            descriptionEn = "Ascend Safa, face the Kaaba, make Dua, and walk toward Marwah (Lap 1). Return from Marwah to Safa (Lap 2) until completing 7 laps ending at Marwah. Men jog lightly between the two green markers; women walk at a normal pace.",
            duaTitleBn = "সাফা ও মারওয়া পাহাড়ে পড়ার দোয়া",
            duaTitleEn = "Dua at Safa & Marwah",
            duaArabic = "إِنَّ الصَّفَا وَالْمَرْوَةَ مِن شَعَائِرِ اللَّهِ — لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            duaBangla = "নিশ্চয়ই সাফা ও মারওয়া আল্লাহর নিদর্শনসমূহের অন্তর্ভুক্ত। এক আল্লাহ ছাড়া কোনো উপাস্য নেই, তাঁর কোনো শরীক নেই। রাজত্ব ও প্রশংসা তাঁরই, এবং তিনি সবকিছুর ওপর সর্বশক্তিমান।",
            duaEnglish = "Indeed, Safa and Marwah are among the symbols of Allah. None has the right to be worshipped except Allah alone without partner; to Him belongs dominion and praise, and He is over all things capable.",
            checklistPromptBn = "সাফা থেকে মারওয়া পর্যন্ত ৭ চক্কর সায়ী সম্পন্ন হয়েছে",
            checklistPromptEn = "Completed 7 laps of Sa'i between Safa and Marwah"
        ),
        HajjGuideStep(
            id = "step_06_halq_umrah",
            stepNumber = 6,
            isUmrah = true,
            isHajj = false,
            titleBn = "হলক বা কসর (উমরাহ সমাপ্তি)",
            titleEn = "Halq or Taqsir (Completing Umrah)",
            subtitleBn = "মাথা মুণ্ডন বা চুল ছাঁটা ও ইহরাম মুক্ত হওয়া",
            subtitleEn = "Shave/trim hair and exit Umrah Ihram",
            descriptionBn = "সায়ী শেষে পুরুষরা সম্পূর্ণ মাথা মুণ্ডন (হলক — উত্তম) অথবা মাথার সব দিক থেকে সমানভাবে চুল ছোট (কসর) করবেন। নারীরা চুলের অগ্রভাগ থেকে এক আঙুলের ডগা পরিমাণ (প্রায় ১ ইঞ্চি) কেটে নেবেন। এর মাধ্যমে উমরাহ সম্পন্ন হলো এবং ইহরামের নিষেধাজ্ঞা শেষ হলো।",
            descriptionEn = "After Sa'i, men either shave their entire head (Halq, preferred) or trim hair evenly all over (Taqsir). Women trim about a fingertip's length (approx. 1 inch) from the ends of their hair. Your Umrah is now complete and Ihram restrictions are lifted.",
            duaTitleBn = "কৃতজ্ঞতা ও কবুলিয়াতের দোয়া",
            duaTitleEn = "Dua of Gratitude & Acceptance",
            duaArabic = "رَبَّنَا تَقَبَّلْ مِنَّا ۖ إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ",
            duaBangla = "হে আমাদের প্রতিপালক! আমাদের পক্ষ থেকে কবুল করুন। নিশ্চয়ই আপনি সর্বশ্রোতা, সর্বজ্ঞ।",
            duaEnglish = "Our Lord, accept this from us. Indeed, You are the All-Hearing, the All-Knowing.",
            checklistPromptBn = "হলক / কসর সম্পন্ন করে উমরাহ সমাপ্ত করা হয়েছে",
            checklistPromptEn = "Completed Halq/Taqsir and exited Umrah Ihram"
        ),
        HajjGuideStep(
            id = "step_07_mina",
            stepNumber = 7,
            isUmrah = false,
            isHajj = true,
            titleBn = "মিনা — ইয়াওমুত তারবিয়াহ (৮ জিলহজ)",
            titleEn = "Mina — Day of Tarwiyah (8th Dhul Hijjah)",
            subtitleBn = "হজ্জের ইহরাম বেঁধে মিনায় অবস্থান",
            subtitleEn = "Enter Hajj Ihram & stay overnight in Mina",
            descriptionBn = "৮ জিলহজ সকালে মক্কার অবস্থানস্থল থেকে হজ্জের ইহরাম বেঁধে মিনার উদ্দেশ্যে রওনা হোন। মিনায় যোহর, আসর, মাগরিব, এশা ও ৯ জিলহজের ফজর নামাজ নিজ নিজ ওয়াক্তে কসর (৪ রাকাত ফরযকে ২ রাকাত) করে আদায় করুন এবং বেশি বেশি তালবিয়াহ ও জিকির করুন।",
            descriptionEn = "On the morning of 8th Dhul Hijjah, enter Ihram for Hajj from your lodging in Makkah and proceed to Mina. Pray Dhuhr, Asr, Maghrib, Isha, and 9th Dhul Hijjah Fajr in Mina (shortening 4-rak'ah prayers to 2 without combining), staying busy in Talbiyah and Dhikr.",
            duaTitleBn = "মিনায় অবস্থান ও জিকিরের দোয়া",
            duaTitleEn = "Supplication in Mina",
            duaArabic = "اللَّهُمَّ هَذِهِ مِنًى فَامْنُنْ عَلَيَّ بِمَا مَنَنْتَ بِهِ عَلَى أَوْلِيَائِكَ وَأَهْلِ طَاعَتِكَ",
            duaBangla = "হে আল্লাহ! এটি মিনা। আপনি আপনার প্রিয় বান্দা ও অনুগতদের ওপর যে অনুগ্রহ করেছেন, আমার ওপরও সেই অনুগ্রহ দান করুন।",
            duaEnglish = "O Allah, this is Mina; bestow upon me the favors You have bestowed upon Your close friends and obedient servants.",
            checklistPromptBn = "৮ জিলহজ মিনায় ৫ ওয়াক্ত নামাজ ও রাত্রিযাপন সম্পন্ন হয়েছে",
            checklistPromptEn = "Stayed in Mina and prayed the 5 prayers on 8th Dhul Hijjah"
        ),
        HajjGuideStep(
            id = "step_08_arafat",
            stepNumber = 8,
            isUmrah = false,
            isHajj = true,
            titleBn = "আরাফাতের ময়দান — উকুফে আরাফাহ (৯ জিলহজ)",
            titleEn = "Arafat — Day of Arafat (9th Dhul Hijjah)",
            subtitleBn = "হজ্জের সর্বশ্রেষ্ঠ রুকন (ফরয)",
            subtitleEn = "The supreme pillar of Hajj (Wuquf)",
            descriptionBn = "৯ জিলহজ সূর্যোদয়ের পর মিনা থেকে আরাফাতে পৌঁছান। যোহর ও আসর যোহরের ওয়াক্তে একসাথে (জম'এ তাকদীম ও কসর) আদায় করুন। দুপুর থেকে সূর্যাস্ত পর্যন্ত কিবলামুখী হয়ে কান্নাকাটি, তওবা, জিকির ও দোয়ায় মশগুল থাকুন। সূর্যাস্তের আগে আরাফাত ত্যাগ করবেন না।",
            descriptionEn = "After sunrise on 9th Dhul Hijjah, travel from Mina to Arafat. Combine and shorten Dhuhr and Asr at Dhuhr time. Spend the afternoon until sunset facing the Qibla in sincere repentance, Dhikr, and Dua. Do not leave Arafat before sunset.",
            duaTitleBn = "আরাফাতের সর্বোত্তম দোয়া",
            duaTitleEn = "Best Dua of the Day of Arafat",
            duaArabic = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            duaBangla = "এক আল্লাহ ছাড়া কোনো উপাস্য নেই, তাঁর কোনো শরীক নেই। সমস্ত রাজত্ব ও প্রশংসা তাঁরই, এবং তিনি সবকিছুর ওপর সর্বশক্তিমান।",
            duaEnglish = "None has the right to be worshipped except Allah alone without partner. To Him belongs all sovereignty and praise, and He has power over all things.",
            checklistPromptBn = "৯ জিলহজ সূর্যাস্ত পর্যন্ত আরাফাতে অবস্থান সম্পন্ন হয়েছে",
            checklistPromptEn = "Observed Wuquf at Arafat until sunset on 9th Dhul Hijjah"
        ),
        HajjGuideStep(
            id = "step_09_muzdalifah",
            stepNumber = 9,
            isUmrah = false,
            isHajj = true,
            titleBn = "মুযদালিফায় রাত্রিযাপন ও কঙ্কর সংগ্রহ",
            titleEn = "Muzdalifah Overnight & Pebble Collection",
            subtitleBn = "মাগরিব ও এশা একসাথে আদায় ও খোলা আকাশের নিচে রাতযাপন",
            subtitleEn = "Combine Maghrib & Isha, rest under open sky",
            descriptionBn = "৯ জিলহজ সূর্যাস্তের পর মাগরিব না পড়েই শান্তভাবে মুযদালিফার দিকে রওনা হোন। মুযদালিফায় পৌঁছে এশার ওয়াক্তে মাগরিব (৩ রাকাত) ও এশা (২ রাকাত কসর) একসাথে আদায় করুন। খোলা আকাশের নিচে বিশ্রাম নিন, ফজরের পর মাশআরুল হারামে দোয়া করুন এবং জামারায় নিক্ষেপের জন্য ছোট চনাবুটের মতো ৪৯ বা ৭০টি কঙ্কর সংগ্রহ করুন।",
            descriptionEn = "After sunset on 9th Dhul Hijjah, depart calmly for Muzdalifah without praying Maghrib first. Upon arrival at Isha time, combine Maghrib (3 rak'ahs) and shortened Isha (2 rak'ahs). Rest overnight, pray Fajr early, make Dua until bright dawn, and collect 49 or 70 chickpea-sized pebbles.",
            duaTitleBn = "মাশআরুল হারামে (মুযদালিফা) দোয়া",
            duaTitleEn = "Dua at Al-Mash'ar al-Haram (Muzdalifah)",
            duaArabic = "اللَّهُمَّ كَمَا أَوْقَفْتَنَا فِيهِ وَأَرَيْتَنَاهُ فَوَفِّقْنَا لِذِكْرِكَ كَمَا هَدَيْتَنَا، وَاغْفِرْ لَنَا وَارْحَمْنَا",
            duaBangla = "হে আল্লাহ! আপনি যেমন আমাদের এখানে অবস্থান করার তাওফীক দিয়েছেন, তেমনি আপনার জিকির করার তাওফীক দিন এবং আমাদের ক্ষমা ও রহম করুন।",
            duaEnglish = "O Allah, just as You enabled us to stand here, guide us to remember You as You guided us, and forgive us and have mercy upon us.",
            checklistPromptBn = "মুযদালিফায় রাত্রিযাপন, ফজর ও কঙ্কর সংগ্রহ সম্পন্ন হয়েছে",
            checklistPromptEn = "Spent the night in Muzdalifah, prayed Fajr, and collected pebbles"
        ),
        HajjGuideStep(
            id = "step_10_jamarat",
            stepNumber = 10,
            isUmrah = false,
            isHajj = true,
            titleBn = "জামারায় কঙ্কর নিক্ষেপ, কুরবানি ও হলক (১০ জিলহজ)",
            titleEn = "Stoning Jamarat, Qurbani & Halq (10th Dhul Hijjah)",
            subtitleBn = "বড় জামারায় ৭টি কঙ্কর নিক্ষেপ, কুরবানি ও মাথা মুণ্ডন",
            subtitleEn = "Stone Jamarat al-Aqabah (7 pebbles), Qurbani & Haircut",
            descriptionBn = "১০ জিলহজ সূর্যোদয়ের আগে মিনায় ফিরে বড় জামারায় (জামারায়ে আকাবা) প্রতিটি কঙ্কর নিক্ষেপের সময় 'বিসমিল্লাহি আল্লাহু আকবার' বলে ৭টি কঙ্কর নিক্ষেপ করুন (এখান থেকে তালবিয়াহ বন্ধ হবে)। এরপর হজ্জের কুরবানি (দমে শুকর) দিন এবং মাথা মুণ্ডন (হলক) বা চুল ছাঁটুন। এবার সাধারণ পোশাক পরা বৈধ হবে।",
            descriptionEn = "Return to Mina before sunrise on 10th Dhul Hijjah. Throw 7 pebbles one by one at Jamarat al-Aqabah saying 'Bismillahi Allahu Akbar' with each throw (stop Talbiyah now). Next, offer the Hajj sacrifice (Hady/Qurbani), then shave (Halq) or trim (Taqsir) hair. You may now wear regular clothing (initial Tahallul).",
            duaTitleBn = "কঙ্কর নিক্ষেপের তাকবীর",
            duaTitleEn = "Takbir while Stoning Jamarat",
            duaArabic = "بِسْمِ اللَّهِ، اللَّهُ أَكْبَرُ، رَغْمًا لِلشَّيْطَانِ وَرِضًا لِلرَّحْمَنِ",
            duaBangla = "আল্লাহর নামে, আল্লাহ সর্বশ্রেষ্ঠ; শয়তানের লাঞ্ছনা ও পরম করুণাময়ের সন্তুষ্টির জন্য।",
            duaEnglish = "In the name of Allah, Allah is the Greatest — in defiance of Shaytan and seeking the pleasure of the Most Merciful.",
            checklistPromptBn = "বড় জামারায় ৭টি কঙ্কর, কুরবানি ও হলক/কসর সম্পন্ন হয়েছে",
            checklistPromptEn = "Stoned Jamarat al-Aqabah, completed Qurbani, and shaved/trimmed hair"
        ),
        HajjGuideStep(
            id = "step_11_tawaf_ifadah",
            stepNumber = 11,
            isUmrah = false,
            isHajj = true,
            titleBn = "তাওয়াফে ইফাদা (তাওয়াফে যিয়ারত) ও সায়ী",
            titleEn = "Tawaf al-Ifadah (Tawaf az-Ziyarah) & Sa'i",
            subtitleBn = "হজ্জের ফরয তাওয়াফ (১০–১২ জিলহজ)",
            subtitleEn = "Obligatory Hajj Tawaf (10th–12th Dhul Hijjah)",
            descriptionBn = "১০ জিলহজ (অথবা ১২ জিলহজ সূর্যাস্তের পূর্বে) মক্কায় গিয়ে কাবা শরীফের ৭ চক্কর ফরয তাওয়াফ (তাওয়াফে ইফাদা) আদায় করুন, মাকামে ইবরাহীমে ২ রাকাত নামাজ পড়ুন এবং হজ্জের ৭ চক্কর সায়ী (সাফা-মারওয়া) সম্পন্ন করুন। এর মাধ্যমে হজ্জের সকল নিষেধাজ্ঞা পূর্ণরূপে মুক্ত হয়ে যায়।",
            descriptionEn = "Proceed to Makkah on 10th Dhul Hijjah (or anytime before sunset on 12th Dhul Hijjah) to perform the obligatory 7-circuit Tawaf al-Ifadah, pray 2 rak'ahs behind Maqam Ibrahim, and perform the 7-lap Hajj Sa'i between Safa and Marwah. Full Tahallul is now achieved.",
            duaTitleBn = "তাওয়াফে ইফাদার দোয়া",
            duaTitleEn = "Dua during Tawaf al-Ifadah",
            duaArabic = "اللَّهُمَّ اجْعَلْهُ حَجًّا مَبْرُورًا، وَسَعْيًا مَشْكُورًا، وَذَنْبًا مَغْفُورًا",
            duaBangla = "হে আল্লাহ! একে কবুল হজ্জ (হজ্জে মাবরূর), প্রশংসিত প্রচেষ্টা এবং গুনাহ মাফের মাধ্যম বানিয়ে দিন।",
            duaEnglish = "O Allah, make it an accepted Hajj (Hajj Mabrur), a rewarded striving, and forgiven sins.",
            checklistPromptBn = "ফরয তাওয়াফে ইফাদা ও হজ্জের সায়ী সম্পন্ন হয়েছে",
            checklistPromptEn = "Completed obligatory Tawaf al-Ifadah and Hajj Sa'i"
        ),
        HajjGuideStep(
            id = "step_12_tashreeq_wida",
            stepNumber = 12,
            isUmrah = false,
            isHajj = true,
            titleBn = "আইয়ামে তাশরীক (১১–১৩ জিলহজ) ও বিদায়ী তাওয়াফ",
            titleEn = "Days of Tashreeq (11–13th) & Farewell Tawaf",
            subtitleBn = "তিন জামারায় কঙ্কর নিক্ষেপ ও মক্কা ত্যাগের পূর্বে তাওয়াফে বিদা",
            subtitleEn = "Stone all 3 Jamarat & perform Tawaf al-Wida before departure",
            descriptionBn = "১১ ও ১২ জিলহজ (এবং চাইলে ১৩ জিলহজ) দুপুর (যাওয়াল)-এর পর ছোট, মধ্যম ও বড় — তিনটি জামারায় ৭টি করে মোট ২১টি কঙ্কর নিক্ষেপ করুন (ছোট ও মধ্যম জামারার পর কিবলামুখী হয়ে দীর্ঘ দোয়া করুন)। মক্কা থেকে বিদায় নেওয়ার আগে ৭ চক্কর বিদায়ী তাওয়াফ (তাওয়াফে বিদা) আদায় করুন (ঋতুমতী নারীদের জন্য মাফ)।",
            descriptionEn = "On the 11th and 12th (and optionally 13th) of Dhul Hijjah after Dhuhr (Zawal), stone the Small, Medium, and Large Jamarat with 7 pebbles each (making long Dua facing Qibla after the first two). Before departing Makkah, perform the 7-circuit Farewell Tawaf (Tawaf al-Wida; waived for menstruating women).",
            duaTitleBn = "বিদায়ী তাওয়াফের দোয়া",
            duaTitleEn = "Dua for Farewell Tawaf",
            duaArabic = "اللَّهُمَّ لَا تَجْعَلْهُ آخِرَ الْعَهْدِ بِبَيْتِكَ الْحَرَامِ، وَارْزُقْنِي الْعَوْدَ إِلَيْهِ مَرَّاتٍ بَعْدَ مَرَّاتٍ",
            duaBangla = "হে আল্লাহ! আপনার সম্মানিত ঘরের সাথে এটিকে আমার শেষ সাক্ষাৎ করবেন না, এবং আমাকে বারবার এখানে ফিরে আসার তাওফীক দান করুন।",
            duaEnglish = "O Allah, do not make this my last visit to Your Sacred House, and grant me the blessing to return to it again and again.",
            checklistPromptBn = "আইয়ামে তাশরীকের রমি ও বিদায়ী তাওয়াফ সম্পন্ন হয়েছে",
            checklistPromptEn = "Completed stoning on Tashreeq days and Farewell Tawaf"
        )
    )

    val PRACTICAL_SECTIONS: List<HajjPracticalSection> = listOf(
        HajjPracticalSection(
            id = "packing",
            titleBn = "প্রয়োজনীয় জিনিসপত্র (কী কী সাথে নেবেন)",
            titleEn = "What to Pack (Essential Checklist)",
            itemsBn = listOf(
                "পুরুষদের জন্য ২ সেট সুতির ইহরামের চাদর, ইহরামের বেল্ট ও পায়ের পাতার উপরের অংশ খোলা থাকে এমন আরামদায়ক স্যান্ডেল।",
                "নারীদের জন্য আরামদায়ক ঢিলেঢালা আবায়া, স্কার্ফ, মোজা ও দীর্ঘ হাঁটার উপযোগী জুতা।",
                "সুগন্ধিমুক্ত সাবান, শ্যাম্পু, পেট্রোলিয়াম জেলি ও সানস্ক্রিন (ইহরাম অবস্থায় ব্যবহারের জন্য)।",
                "পাসপোর্ট, ভিসা, হজ্জ কার্ড, টিকার সনদ ও হোটেল ঠিকানার ফটোকপি + গলায় ঝোলানো ছোট পাউচ।",
                "ব্যক্তিগত নিয়মিত ওষুধ, ওরস্যালাইন, প্যারাসিটামল, ব্যান্ড-এইড, ছোট ছাতা ও রিফিলযোগ্য পানির বোতল।",
                "হালকা জায়নামাজ, কঙ্কর রাখার ছোট ব্যাগ, পাওয়ার ব্যাংক, ইউনিভার্সাল অ্যাডাপ্টার ও ছোট কাঁচি (চুল কাটার জন্য)।"
            ),
            itemsEn = listOf(
                "For men: 2 sets of breathable cotton Ihram sheets, Ihram waist belt, and comfortable sandals exposing the ankles and top of the feet.",
                "For women: Lightweight breathable abayas, hijabs, socks, and supportive walking shoes.",
                "Unscented soap, unscented moisturizer/petroleum jelly, and unscented wet wipes for Ihram.",
                "Passport, visa, Nusuk/Hajj ID card, vaccination certificate, hotel card, and a secure neck/waist pouch.",
                "Personal prescription medicines, oral rehydration salts (ORS), pain relievers, blister plasters, compact umbrella, and refillable water bottle.",
                "Lightweight travel prayer mat, drawstring pebble bag, power bank, universal travel adapter, and small nail clipper/scissors (used after exiting Ihram)."
            )
        ),
        HajjPracticalSection(
            id = "ihram_rules",
            titleBn = "ইহরামের বিধিবিধান (পুরুষ ও নারী)",
            titleEn = "Ihram Rules (Men & Women)",
            itemsBn = listOf(
                "পুরুষদের নিয়ম: সেলাইযুক্ত পোশাক (শার্ট, প্যান্ট, পাঞ্জাবি, অন্তর্বাস) পরা যাবে না এবং মাথা বা মুখ ঢাকা যাবে না।",
                "নারীদের নিয়ম: স্বাভাবিক শালীন সেলাইযুক্ত পোশাক পরবেন, তবে মুখমণ্ডলে কাপড় লাগানো (নিকাব) ও হাতমোজা পরা নিষিদ্ধ।",
                "উভয়ের জন্য নিষিদ্ধ: সুগন্ধি বা পারফিউম ব্যবহার করা, নখ বা শরীরের কোনো চুল কাটা/উপড়ানো।",
                "উভয়ের জন্য নিষিদ্ধ: ঝগড়া-বিবাদ, অশ্লীল কথা, গুনাহের কাজ, শিকার করা বা হারামের গাছপালা/ঘাস ছেঁড়া।",
                "উভয়ের জন্য বৈধ: গোসল করা (সুগন্ধিহীন), ছাতা ব্যবহার করা, ঘড়ি/চশমা/বেল্ট পরা ও ইহরামের চাদর পরিবর্তন করা।"
            ),
            itemsEn = listOf(
                "For Men: No stitched/fitted garments (shirts, trousers, underwear), no head coverings (caps/hats), and no shoes covering the ankles.",
                "For Women: Wear modest everyday stitched clothing; do not wear a face veil (niqab touching the face) or gloves.",
                "Prohibited for Both: Applying perfume or scented products, trimming nails, or cutting/plucking hair from the body.",
                "Prohibited for Both: Arguing, foul speech, marital intimacy, hunting animals, or cutting plants/trees within the Haram boundaries.",
                "Permitted for Both: Bathing with unscented soap, carrying an umbrella, wearing glasses/watches/money belts, and changing dirty Ihram sheets."
            )
        ),
        HajjPracticalSection(
            id = "common_mistakes",
            titleBn = "সাধারণ ভুলগুলো (যা এড়িয়ে চলা জরুরি)",
            titleEn = "Common Mistakes to Avoid",
            itemsBn = listOf(
                "মীকাত অতিক্রম করার পর ইহরাম বাঁধা — এতে দম (জরিমানা) ওয়াজিব হতে পারে; বিমানে থাকতেই মীকাতের আগে ইহরাম বেঁধে নিন।",
                "তাওয়াফের সময় হাতিম (হিজরে ইসমাইল)-এর ভেতর দিয়ে হাঁটা — হাতিম কাবার অংশ, তাই এর বাইরে দিয়ে চক্কর দিতে হবে।",
                "হাজরে আসওয়াদ চুম্বন করতে গিয়ে অন্য হাজিদের ধাক্কা দেওয়া — দূর থেকে হাত ইশারা করে 'বিসমিল্লাহি আল্লাহু আকবার' বলাই যথেষ্ট।",
                "আরাফাতের সীমানার বাইরে অবস্থান করা বা সূর্যাস্তের আগেই আরাফাত ত্যাগ করা — সাইনবোর্ড দেখে আরাফাতের সীমানার ভেতরেই থাকুন।",
                "জামারায় বড় পাথর বা জুতা নিক্ষেপ করা — শুধুমাত্র ছোট চনাবুট আকৃতির কঙ্কর শান্তভাবে নিক্ষেপ করতে হবে।",
                "তাওয়াফ ও সায়ীতে মনগড়া নির্দিষ্ট দোয়া পড়তে গিয়ে অন্যদের বিরক্ত করা — কুরআন-হাদিসের দোয়া ও নিজের ভাষায় একান্ত মনে দোয়া করুন।"
            ),
            itemsEn = listOf(
                "Crossing Miqat without Ihram — always enter Ihram before crossing Miqat (even aboard a flight) to avoid incurring a penalty (Dam).",
                "Walking inside the Hatim (Hijr Ismail) during Tawaf — it is part of the Kaaba, so circuits must go outside its curved wall.",
                "Pushing or harming fellow pilgrims to kiss the Black Stone — gesturing from afar while saying 'Bismillahi Allahu Akbar' is complete Sunnah.",
                "Standing outside Arafat's boundaries or leaving before sunset — verify boundary signs and stay within Arafat until after sunset.",
                "Throwing large stones or shoes at the Jamarat — use only small chickpea-sized pebbles with calm focus and Takbir.",
                "Reciting group chants loudly from booklets and disturbing others — recite authentic Quranic/Prophetic Duas and pray sincerely in your own language."
            )
        )
    )
}
