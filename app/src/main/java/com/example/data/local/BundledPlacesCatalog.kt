package com.example.data.local

import com.example.data.repository.MosqueRepository
import com.example.data.repository.NearbyHalalRestaurant
import com.example.data.repository.NearbyMosque
import com.example.util.PrayerTimeCalculator

/**
 * Curated offline / bundled fallback catalog of well-known mosques and Halal restaurants
 * across Narayanganj and Dhaka, Bangladesh.
 *
 * Used automatically whenever all online Overpass mirrors and Nominatim fail so the
 * Mosque Finder and Halal Finder screens are NEVER empty.
 */
object BundledPlacesCatalog {

    private data class SeedMosque(
        val id: String,
        val nameBn: String,
        val nameEn: String,
        val address: String,
        val latitude: Double,
        val longitude: Double
    )

    private data class SeedHalalRestaurant(
        val id: String,
        val nameBn: String,
        val nameEn: String,
        val address: String,
        val cuisine: String,
        val isExplicitHalal: Boolean,
        val is100PercentHalal: Boolean,
        val latitude: Double,
        val longitude: Double
    )

    private val seedMosques = listOf(
        // Narayanganj well-known mosques
        SeedMosque(
            id = "bundled_m_ngj_chashara",
            nameBn = "চাষাড়া কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Chashara Central Jame Mosque",
            address = "Chashara, Narayanganj",
            latitude = 23.6238,
            longitude = 90.5000
        ),
        SeedMosque(
            id = "bundled_m_ngj_boro",
            nameBn = "নারায়ণগঞ্জ কেন্দ্রীয় বড় জামে মসজিদ",
            nameEn = "Narayanganj Central Boro Jame Mosque",
            address = "BB Road, Narayanganj",
            latitude = 23.6194,
            longitude = 90.5036
        ),
        SeedMosque(
            id = "bundled_m_ngj_khanpur",
            nameBn = "খানপুর বড় জামে মসজিদ",
            nameEn = "Khanpur Boro Jame Mosque",
            address = "Khanpur, Narayanganj",
            latitude = 23.6272,
            longitude = 90.5062
        ),
        SeedMosque(
            id = "bundled_m_ngj_deobhog",
            nameBn = "দেওভোগ কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Deobhog Central Jame Mosque",
            address = "Deobhog, Narayanganj",
            latitude = 23.6152,
            longitude = 90.4938
        ),
        SeedMosque(
            id = "bundled_m_ngj_bandar",
            nameBn = "বন্দর কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Bandar Central Jame Mosque",
            address = "Bandar, Narayanganj",
            latitude = 23.6142,
            longitude = 90.5145
        ),
        SeedMosque(
            id = "bundled_m_ngj_fatullah",
            nameBn = "ফতুল্লা বাজার কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Fatullah Bazar Central Jame Mosque",
            address = "Fatullah, Narayanganj",
            latitude = 23.6355,
            longitude = 90.4832
        ),
        SeedMosque(
            id = "bundled_m_ngj_jalkuri",
            nameBn = "জালকুড়ি কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Jalkuri Central Jame Mosque",
            address = "Jalkuri, Narayanganj",
            latitude = 23.6580,
            longitude = 90.4960
        ),
        SeedMosque(
            id = "bundled_m_ngj_siddhirganj",
            nameBn = "সিদ্ধিরগঞ্জ কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Siddhirganj Central Jame Mosque",
            address = "Siddhirganj, Narayanganj",
            latitude = 23.6810,
            longitude = 90.5210
        ),
        SeedMosque(
            id = "bundled_m_ngj_signboard",
            nameBn = "সাইনবোর্ড কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Signboard Central Jame Mosque",
            address = "Signboard, Narayanganj",
            latitude = 23.6890,
            longitude = 90.4956
        ),
        SeedMosque(
            id = "bundled_m_ngj_goaldi",
            nameBn = "গোয়ালদী ঐতিহাসিক শাহী মসজিদ",
            nameEn = "Goaldi Historic Shahi Mosque",
            address = "Sonargaon, Narayanganj",
            latitude = 23.6494,
            longitude = 90.6085
        ),
        // Dhaka well-known mosques
        SeedMosque(
            id = "bundled_m_dhk_baitul_mukarram",
            nameBn = "বায়তুল মোকাররম জাতীয় মসজিদ",
            nameEn = "Baitul Mukarram National Mosque",
            address = "Topkhana Road, Paltan, Dhaka",
            latitude = 23.7294,
            longitude = 90.4125
        ),
        SeedMosque(
            id = "bundled_m_dhk_tara",
            nameBn = "তারা মসজিদ (স্টার মসজিদ)",
            nameEn = "Star Mosque (Tara Masjid)",
            address = "Armanitola, Old Dhaka",
            latitude = 23.7155,
            longitude = 90.4018
        ),
        SeedMosque(
            id = "bundled_m_dhk_kakrail",
            nameBn = "কাকরাইল মার্কাজ মসজিদ",
            nameEn = "Kakrail Markaz Mosque",
            address = "Kakrail, Ramna, Dhaka",
            latitude = 23.7405,
            longitude = 90.4070
        ),
        SeedMosque(
            id = "bundled_m_dhk_highcourt",
            nameBn = "হাইকোর্ট মাজার জামে মসজিদ",
            nameEn = "High Court Mazar Jame Mosque",
            address = "Shahbagh, Dhaka",
            latitude = 23.7302,
            longitude = 90.4021
        ),
        SeedMosque(
            id = "bundled_m_dhk_taqwa",
            nameBn = "ধানমন্ডি তাকওয়া মসজিদ",
            nameEn = "Dhanmondi Taqwa Mosque",
            address = "Road 12A, Dhanmondi, Dhaka",
            latitude = 23.7510,
            longitude = 90.3712
        ),
        SeedMosque(
            id = "bundled_m_dhk_sobhanbagh",
            nameBn = "সুবহানবাগ জামে মসজিদ",
            nameEn = "Sobhanbagh Jame Mosque",
            address = "Mirpur Road, Dhanmondi, Dhaka",
            latitude = 23.7549,
            longitude = 90.3765
        ),
        SeedMosque(
            id = "bundled_m_dhk_satgambuj",
            nameBn = "সাত গম্বুজ মসজিদ",
            nameEn = "Sat Gambuj Mosque",
            address = "Mohammadpur, Dhaka",
            latitude = 23.7580,
            longitude = 90.3587
        ),
        SeedMosque(
            id = "bundled_m_dhk_gulshan_azad",
            nameBn = "গুলশান কেন্দ্রীয় (আজাদ) মসজিদ",
            nameEn = "Gulshan Central (Azad) Mosque",
            address = "Gulshan-2, Dhaka",
            latitude = 23.7937,
            longitude = 90.4143
        ),
        SeedMosque(
            id = "bundled_m_dhk_mirpur10",
            nameBn = "মিরপুর-১০ কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Mirpur-10 Central Jame Mosque",
            address = "Mirpur-10, Dhaka",
            latitude = 23.8069,
            longitude = 90.3687
        ),
        SeedMosque(
            id = "bundled_m_dhk_uttara7",
            nameBn = "উত্তরা সেক্টর ৭ কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Uttara Sector 7 Central Jame Mosque",
            address = "Sector 7, Uttara, Dhaka",
            latitude = 23.8690,
            longitude = 90.3982
        )
    )

    private val seedHalalRestaurants = listOf(
        // Narayanganj well-known Halal restaurants
        SeedHalalRestaurant(
            id = "bundled_h_ngj_al_madina",
            nameBn = "হোটেল আল-মদিনা হালাল রেস্টুরেন্ট",
            nameEn = "Hotel Al-Madina Halal Restaurant",
            address = "Chashara, Narayanganj",
            cuisine = "Bangla • Biryani • Halal",
            isExplicitHalal = true,
            is100PercentHalal = true,
            latitude = 23.6226,
            longitude = 90.5015
        ),
        SeedHalalRestaurant(
            id = "bundled_h_ngj_kacchi_bhai",
            nameBn = "কাচ্চি ভাই — চাষাড়া",
            nameEn = "Kacchi Bhai — Chashara",
            address = "Chashara, Narayanganj",
            cuisine = "Kacchi Biryani • Mughlai",
            isExplicitHalal = true,
            is100PercentHalal = false,
            latitude = 23.6242,
            longitude = 90.4997
        ),
        SeedHalalRestaurant(
            id = "bundled_h_ngj_sultans_dine",
            nameBn = "সুলতান'স ডাইন — নারায়ণগঞ্জ",
            nameEn = "Sultan's Dine — Narayanganj",
            address = "BB Road, Chashara, Narayanganj",
            cuisine = "Kacchi • Kebabs • Halal",
            isExplicitHalal = true,
            is100PercentHalal = false,
            latitude = 23.6231,
            longitude = 90.5008
        ),
        SeedHalalRestaurant(
            id = "bundled_h_ngj_bismillah",
            nameBn = "বিসমিল্লাহ হালাল বিরিয়ানি ও কাবাব",
            nameEn = "Bismillah Halal Biryani & Kebab",
            address = "Kalir Bazar, Narayanganj",
            cuisine = "Biryani • Kebab • Tehari",
            isExplicitHalal = true,
            is100PercentHalal = true,
            latitude = 23.6198,
            longitude = 90.5042
        ),
        SeedHalalRestaurant(
            id = "bundled_h_ngj_star_kabab",
            nameBn = "স্টার কাবাব ও হালাল ডাইনিং",
            nameEn = "Star Kabab & Halal Dining",
            address = "Khanpur, Narayanganj",
            cuisine = "Kebab • Grill • Bangla",
            isExplicitHalal = true,
            is100PercentHalal = false,
            latitude = 23.6265,
            longitude = 90.5048
        ),
        SeedHalalRestaurant(
            id = "bundled_h_ngj_kasturi",
            nameBn = "কস্তুরী হালাল রেস্টুরেন্ট",
            nameEn = "Kasturi Halal Restaurant",
            address = "Fatullah, Narayanganj",
            cuisine = "Bangla • Bhorta • Curry",
            isExplicitHalal = true,
            is100PercentHalal = false,
            latitude = 23.6362,
            longitude = 90.4846
        ),
        SeedHalalRestaurant(
            id = "bundled_h_ngj_chillox",
            nameBn = "চিলক্স — চাষাড়া",
            nameEn = "Chillox — Chashara",
            address = "Chashara, Narayanganj",
            cuisine = "Burgers • Fast Food",
            isExplicitHalal = false,
            is100PercentHalal = false,
            latitude = 23.6246,
            longitude = 90.4991
        ),
        SeedHalalRestaurant(
            id = "bundled_h_ngj_boishakhi",
            nameBn = "বৈশাখী বাংলা হালাল হোটেল",
            nameEn = "Boishakhi Bangla Halal Hotel",
            address = "Signboard, Narayanganj",
            cuisine = "Bangla • Rice & Fish",
            isExplicitHalal = true,
            is100PercentHalal = false,
            latitude = 23.6884,
            longitude = 90.4962
        ),
        // Dhaka well-known Halal restaurants
        SeedHalalRestaurant(
            id = "bundled_h_dhk_haji_biryani",
            nameBn = "হাজী বিরিয়ানি (পুরান ঢাকা)",
            nameEn = "Haji Biryani (Old Dhaka)",
            address = "Nazira Bazar, Old Dhaka",
            cuisine = "Traditional Kacchi • Halal",
            isExplicitHalal = true,
            is100PercentHalal = true,
            latitude = 23.7204,
            longitude = 90.4059
        ),
        SeedHalalRestaurant(
            id = "bundled_h_dhk_al_razzaque",
            nameBn = "হোটেল আল-রাজ্জাক (হালাল)",
            nameEn = "Hotel Al-Razzaque (Halal)",
            address = "North South Road, Old Dhaka",
            cuisine = "Bangla • Mughlai • Halal",
            isExplicitHalal = true,
            is100PercentHalal = true,
            latitude = 23.7210,
            longitude = 90.4085
        ),
        SeedHalalRestaurant(
            id = "bundled_h_dhk_nanna",
            nameBn = "নান্না মিয়া বিরিয়ানি",
            nameEn = "Nanna Miah Biryani",
            address = "Becharam Dewry, Old Dhaka",
            cuisine = "Morog Polao • Biryani",
            isExplicitHalal = true,
            is100PercentHalal = true,
            latitude = 23.7165,
            longitude = 90.4028
        ),
        SeedHalalRestaurant(
            id = "bundled_h_dhk_star_kabab",
            nameBn = "স্টার কাবাব অ্যান্ড রেস্টুরেন্ট",
            nameEn = "Star Kabab & Restaurant",
            address = "Satmasjid Road, Dhanmondi, Dhaka",
            cuisine = "Kebab • Kacchi • Bangla",
            isExplicitHalal = true,
            is100PercentHalal = true,
            latitude = 23.7408,
            longitude = 90.3755
        ),
        SeedHalalRestaurant(
            id = "bundled_h_dhk_sultans_dine",
            nameBn = "সুলতান'স ডাইন — ধানমন্ডি",
            nameEn = "Sultan's Dine — Dhanmondi",
            address = "Satmasjid Road, Dhanmondi, Dhaka",
            cuisine = "Kacchi Biryani • Kebab",
            isExplicitHalal = true,
            is100PercentHalal = false,
            latitude = 23.7465,
            longitude = 90.3712
        ),
        SeedHalalRestaurant(
            id = "bundled_h_dhk_kacchi_bhai_bailey",
            nameBn = "কাচ্চি ভাই — বেইলি রোড",
            nameEn = "Kacchi Bhai — Bailey Road",
            address = "Bailey Road, Dhaka",
            cuisine = "Kacchi Biryani • Halal",
            isExplicitHalal = true,
            is100PercentHalal = false,
            latitude = 23.7418,
            longitude = 90.4102
        ),
        SeedHalalRestaurant(
            id = "bundled_h_dhk_fakruddin",
            nameBn = "ফখরুদ্দিন বিরিয়ানি",
            nameEn = "Fakruddin Biryani",
            address = "Bailey Road, Dhaka",
            cuisine = "Traditional Kacchi • Halal",
            isExplicitHalal = true,
            is100PercentHalal = true,
            latitude = 23.7422,
            longitude = 90.4114
        ),
        SeedHalalRestaurant(
            id = "bundled_h_dhk_handi",
            nameBn = "হান্ডি হালাল রেস্টুরেন্ট",
            nameEn = "Handi Halal Restaurant",
            address = "Gulshan-2, Dhaka",
            cuisine = "Mughlai • Tandoori • Halal",
            isExplicitHalal = true,
            is100PercentHalal = false,
            latitude = 23.7944,
            longitude = 90.4136
        ),
        SeedHalalRestaurant(
            id = "bundled_h_dhk_mezban",
            nameBn = "মেজবান বাড়ি হালাল ডাইনিং",
            nameEn = "Mezban Bari Halal Dining",
            address = "Sector 7, Uttara, Dhaka",
            cuisine = "Mezbani Beef • Bangla",
            isExplicitHalal = true,
            is100PercentHalal = false,
            latitude = 23.8682,
            longitude = 90.3990
        )
    )

    fun mosquesSortedByDistance(userLat: Double, userLon: Double): List<NearbyMosque> {
        return seedMosques.map { seed ->
            NearbyMosque(
                id = seed.id,
                nameBn = seed.nameBn,
                nameEn = seed.nameEn,
                address = seed.address,
                latitude = seed.latitude,
                longitude = seed.longitude,
                distanceKm = MosqueRepository.haversineKm(userLat, userLon, seed.latitude, seed.longitude),
                bearingFromUserDeg = MosqueRepository.bearingDegrees(userLat, userLon, seed.latitude, seed.longitude),
                qiblaBearingDeg = PrayerTimeCalculator.calculateQiblaBearing(seed.latitude, seed.longitude)
            )
        }.sortedBy { it.distanceKm }
    }

    fun halalRestaurantsSorted(userLat: Double, userLon: Double): List<NearbyHalalRestaurant> {
        return seedHalalRestaurants.map { seed ->
            val (badgeBn, badgeEn) = when {
                seed.is100PercentHalal -> "১০০% হালাল" to "100% Halal"
                seed.isExplicitHalal -> "হালাল খাবার" to "Halal Verified"
                else -> "হালাল-বান্ধব" to "Halal-Friendly"
            }
            NearbyHalalRestaurant(
                id = seed.id,
                nameBn = seed.nameBn,
                nameEn = seed.nameEn,
                address = seed.address,
                cuisine = seed.cuisine,
                halalBadgeBn = badgeBn,
                halalBadgeEn = badgeEn,
                rating = null,
                openingHours = null,
                latitude = seed.latitude,
                longitude = seed.longitude,
                distanceKm = MosqueRepository.haversineKm(userLat, userLon, seed.latitude, seed.longitude),
                bearingFromUserDeg = MosqueRepository.bearingDegrees(userLat, userLon, seed.latitude, seed.longitude),
                isExplicitHalal = seed.isExplicitHalal
            )
        }.sortedWith(
            compareByDescending<NearbyHalalRestaurant> { it.isExplicitHalal }
                .thenBy { it.distanceKm }
        )
    }
}
