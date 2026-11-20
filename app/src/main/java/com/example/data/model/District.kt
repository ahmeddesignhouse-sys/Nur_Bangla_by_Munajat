package com.example.data.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class BdDistrict(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val divisionBn: String,
    val divisionEn: String,
    val latitude: Double,
    val longitude: Double
) {
    fun displayName(isEnglish: Boolean): String = if (isEnglish) nameEn else nameBn
    fun displayDivision(isEnglish: Boolean): String = if (isEnglish) divisionEn else divisionBn
}

object BangladeshiDistricts {
    val ALL_64: List<BdDistrict> = listOf(
        // Dhaka Division (13)
        BdDistrict("dhaka", "ঢাকা", "Dhaka", "ঢাকা", "Dhaka", 23.8103, 90.4125),
        BdDistrict("faridpur", "ফরিদপুর", "Faridpur", "ঢাকা", "Dhaka", 23.6071, 89.8429),
        BdDistrict("gazipur", "গাজীপুর", "Gazipur", "ঢাকা", "Dhaka", 23.9999, 90.4203),
        BdDistrict("gopalganj", "গোপালগঞ্জ", "Gopalganj", "ঢাকা", "Dhaka", 23.0051, 89.8266),
        BdDistrict("kishoreganj", "কিশোরগঞ্জ", "Kishoreganj", "ঢাকা", "Dhaka", 24.4449, 90.7766),
        BdDistrict("madaripur", "মাদারীপুর", "Madaripur", "ঢাকা", "Dhaka", 23.1641, 90.1897),
        BdDistrict("manikganj", "মানিকগঞ্জ", "Manikganj", "ঢাকা", "Dhaka", 23.8617, 90.0003),
        BdDistrict("munshiganj", "মুন্সিগঞ্জ", "Munshiganj", "ঢাকা", "Dhaka", 23.5422, 90.5305),
        BdDistrict("narayanganj", "নারায়ণগঞ্জ", "Narayanganj", "ঢাকা", "Dhaka", 23.6238, 90.5000),
        BdDistrict("narsingdi", "নরসিংদী", "Narsingdi", "ঢাকা", "Dhaka", 23.9322, 90.7154),
        BdDistrict("rajbari", "রাজবাড়ী", "Rajbari", "ঢাকা", "Dhaka", 23.7574, 89.6445),
        BdDistrict("shariatpur", "শরীয়তপুর", "Shariatpur", "ঢাকা", "Dhaka", 23.2423, 90.4348),
        BdDistrict("tangail", "টাঙ্গাইল", "Tangail", "ঢাকা", "Dhaka", 24.2513, 89.9167),

        // Chattogram Division (11)
        BdDistrict("chattogram", "চট্টগ্রাম", "Chattogram", "চট্টগ্রাম", "Chattogram", 22.3569, 91.7832),
        BdDistrict("bandarban", "বান্দরবান", "Bandarban", "চট্টগ্রাম", "Chattogram", 22.1953, 92.2184),
        BdDistrict("brahmanbaria", "ব্রাহ্মণবাড়িয়া", "Brahmanbaria", "চট্টগ্রাম", "Chattogram", 23.9571, 91.1119),
        BdDistrict("chandpur", "চাঁদপুর", "Chandpur", "চট্টগ্রাম", "Chattogram", 23.2333, 90.6713),
        BdDistrict("comilla", "কুমিল্লা", "Cumilla", "চট্টগ্রাম", "Chattogram", 23.4607, 91.1809),
        BdDistrict("coxsbazar", "কক্সবাজার", "Cox's Bazar", "চট্টগ্রাম", "Chattogram", 21.4272, 92.0058),
        BdDistrict("feni", "ফেনী", "Feni", "চট্টগ্রাম", "Chattogram", 23.0159, 91.3976),
        BdDistrict("khagrachhari", "খাগড়াছড়ি", "Khagrachhari", "চট্টগ্রাম", "Chattogram", 23.1193, 91.9847),
        BdDistrict("lakshmipur", "লক্ষ্মীপুর", "Lakshmipur", "চট্টগ্রাম", "Chattogram", 22.9447, 90.8282),
        BdDistrict("noakhali", "নোয়াখালী", "Noakhali", "চট্টগ্রাম", "Chattogram", 22.8696, 91.0994),
        BdDistrict("rangamati", "রাঙ্গামাটি", "Rangamati", "চট্টগ্রাম", "Chattogram", 22.7324, 92.2985),

        // Rajshahi Division (8)
        BdDistrict("rajshahi", "রাজশাহী", "Rajshahi", "রাজশাহী", "Rajshahi", 24.3745, 88.6042),
        BdDistrict("bogura", "বগুড়া", "Bogura", "রাজশাহী", "Rajshahi", 24.8465, 89.3776),
        BdDistrict("chapainawabganj", "চাঁপাইনবাবগঞ্জ", "Chapainawabganj", "রাজশাহী", "Rajshahi", 24.5965, 88.2775),
        BdDistrict("joypurhat", "জয়পুরহাট", "Joypurhat", "রাজশাহী", "Rajshahi", 25.0968, 89.0227),
        BdDistrict("naogaon", "নওগাঁ", "Naogaon", "রাজশাহী", "Rajshahi", 24.7936, 88.9318),
        BdDistrict("natore", "নাটোর", "Natore", "রাজশাহী", "Rajshahi", 24.4206, 89.0003),
        BdDistrict("pabna", "পাবনা", "Pabna", "রাজশাহী", "Rajshahi", 24.0064, 89.2372),
        BdDistrict("sirajganj", "সিরাজগঞ্জ", "Sirajganj", "রাজশাহী", "Rajshahi", 24.4534, 89.7007),

        // Khulna Division (10)
        BdDistrict("khulna", "খুলনা", "Khulna", "খুলনা", "Khulna", 22.8456, 89.5403),
        BdDistrict("bagerhat", "বাগেরহাট", "Bagerhat", "খুলনা", "Khulna", 22.6516, 89.7859),
        BdDistrict("chuadanga", "চুয়াডাঙ্গা", "Chuadanga", "খুলনা", "Khulna", 23.6402, 88.8418),
        BdDistrict("jashore", "যশোর", "Jashore", "খুলনা", "Khulna", 23.1664, 89.2081),
        BdDistrict("jhenaidah", "ঝিনাইদহ", "Jhenaidah", "খুলনা", "Khulna", 23.5448, 89.1539),
        BdDistrict("kushtia", "কুষ্টিয়া", "Kushtia", "খুলনা", "Khulna", 23.9013, 89.1205),
        BdDistrict("magura", "মাগুরা", "Magura", "খুলনা", "Khulna", 23.4873, 89.4199),
        BdDistrict("meherpur", "মেহেরপুর", "Meherpur", "খুলনা", "Khulna", 23.7622, 88.6318),
        BdDistrict("narail", "নড়াইল", "Narail", "খুলনা", "Khulna", 23.1725, 89.5127),
        BdDistrict("satkhira", "সাতক্ষীরা", "Satkhira", "খুলনা", "Khulna", 22.7185, 89.0705),

        // Barishal Division (6)
        BdDistrict("barishal", "বরিশাল", "Barishal", "বরিশাল", "Barishal", 22.7010, 90.3535),
        BdDistrict("barguna", "বরগুনা", "Barguna", "বরিশাল", "Barishal", 22.1592, 90.1256),
        BdDistrict("bhola", "ভোলা", "Bhola", "বরিশাল", "Barishal", 22.6859, 90.6482),
        BdDistrict("jhalokathi", "ঝালকাঠি", "Jhalokathi", "বরিশাল", "Barishal", 22.6406, 90.1987),
        BdDistrict("patuakhali", "পটুয়াখালী", "Patuakhali", "বরিশাল", "Barishal", 22.3596, 90.3299),
        BdDistrict("pirojpur", "পিরোজপুর", "Pirojpur", "বরিশাল", "Barishal", 22.5841, 89.9720),

        // Sylhet Division (4)
        BdDistrict("sylhet", "সিলেট", "Sylhet", "সিলেট", "Sylhet", 24.8949, 91.8687),
        BdDistrict("habiganj", "হবিগঞ্জ", "Habiganj", "সিলেট", "Sylhet", 24.3749, 91.4155),
        BdDistrict("moulvibazar", "মৌলভীবাজার", "Moulvibazar", "সিলেট", "Sylhet", 24.4829, 91.7774),
        BdDistrict("sunamganj", "সুনামগঞ্জ", "Sunamganj", "সিলেট", "Sylhet", 25.0658, 91.3950),

        // Rangpur Division (8)
        BdDistrict("rangpur", "রংপুর", "Rangpur", "রংপুর", "Rangpur", 25.7439, 89.2752),
        BdDistrict("dinajpur", "দিনাজপুর", "Dinajpur", "রংপুর", "Rangpur", 25.6217, 88.6354),
        BdDistrict("gaibandha", "গাইবান্ধা", "Gaibandha", "রংপুর", "Rangpur", 25.3288, 89.5281),
        BdDistrict("kurigram", "কুড়িগ্রাম", "Kurigram", "রংপুর", "Rangpur", 25.8054, 89.6362),
        BdDistrict("lalmonirhat", "লালমনিরহাট", "Lalmonirhat", "রংপুর", "Rangpur", 25.9923, 89.2847),
        BdDistrict("nilphamari", "নীলফামারী", "Nilphamari", "রংপুর", "Rangpur", 25.9318, 88.8560),
        BdDistrict("panchagarh", "পঞ্চগড়", "Panchagarh", "রংপুর", "Rangpur", 26.3411, 88.5542),
        BdDistrict("thakurgaon", "ঠাকুরগাঁও", "Thakurgaon", "রংপুর", "Rangpur", 26.0337, 88.4617),

        // Mymensingh Division (4)
        BdDistrict("mymensingh", "ময়মনসিংহ", "Mymensingh", "ময়মনসিংহ", "Mymensingh", 24.7471, 90.4203),
        BdDistrict("jamalpur", "জামালপুর", "Jamalpur", "ময়মনসিংহ", "Mymensingh", 24.9375, 89.9378),
        BdDistrict("netrokona", "নেত্রকোণা", "Netrokona", "ময়মনসিংহ", "Mymensingh", 24.8710, 90.7279),
        BdDistrict("sherpur", "শেরপুর", "Sherpur", "ময়মনসিংহ", "Mymensingh", 25.0205, 90.0153)
    )

    val DEFAULT: BdDistrict = ALL_64.first() // Dhaka

    fun findById(id: String): BdDistrict = ALL_64.find { it.id == id } ?: DEFAULT

    fun findNearest(latitude: Double, longitude: Double): BdDistrict {
        return ALL_64.minByOrNull { dist ->
            haversineKm(latitude, longitude, dist.latitude, dist.longitude)
        } ?: DEFAULT
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
