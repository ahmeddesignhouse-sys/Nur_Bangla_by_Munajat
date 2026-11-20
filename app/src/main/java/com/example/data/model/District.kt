package com.example.data.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class BdDistrict(
    val id: String,
    val nameBn: String,
    val divisionBn: String,
    val latitude: Double,
    val longitude: Double
)

object BangladeshiDistricts {
    val ALL_64: List<BdDistrict> = listOf(
        // ঢাকা বিভাগ (13)
        BdDistrict("dhaka", "ঢাকা", "ঢাকা", 23.8103, 90.4125),
        BdDistrict("faridpur", "ফরিদপুর", "ঢাকা", 23.6071, 89.8429),
        BdDistrict("gazipur", "গাজীপুর", "ঢাকা", 23.9999, 90.4203),
        BdDistrict("gopalganj", "গোপালগঞ্জ", "ঢাকা", 23.0051, 89.8266),
        BdDistrict("kishoreganj", "কিশোরগঞ্জ", "ঢাকা", 24.4449, 90.7766),
        BdDistrict("madaripur", "মাদারীপুর", "ঢাকা", 23.1641, 90.1897),
        BdDistrict("manikganj", "মানিকগঞ্জ", "ঢাকা", 23.8617, 90.0003),
        BdDistrict("munshiganj", "মুন্সিগঞ্জ", "ঢাকা", 23.5422, 90.5305),
        BdDistrict("narayanganj", "নারায়ণগঞ্জ", "ঢাকা", 23.6238, 90.5000),
        BdDistrict("narsingdi", "নরসিংদী", "ঢাকা", 23.9322, 90.7154),
        BdDistrict("rajbari", "রাজবাড়ী", "ঢাকা", 23.7574, 89.6445),
        BdDistrict("shariatpur", "শরীয়তপুর", "ঢাকা", 23.2423, 90.4348),
        BdDistrict("tangail", "টাঙ্গাইল", "ঢাকা", 24.2513, 89.9167),

        // চট্টগ্রাম বিভাগ (11)
        BdDistrict("chattogram", "চট্টগ্রাম", "চট্টগ্রাম", 22.3569, 91.7832),
        BdDistrict("bandarban", "বান্দরবান", "চট্টগ্রাম", 22.1953, 92.2184),
        BdDistrict("brahmanbaria", "ব্রাহ্মণবাড়িয়া", "চট্টগ্রাম", 23.9571, 91.1119),
        BdDistrict("chandpur", "চাঁদপুর", "চট্টগ্রাম", 23.2333, 90.6713),
        BdDistrict("comilla", "কুমিল্লা", "চট্টগ্রাম", 23.4607, 91.1809),
        BdDistrict("coxsbazar", "কক্সবাজার", "চট্টগ্রাম", 21.4272, 92.0058),
        BdDistrict("feni", "ফেনী", "চট্টগ্রাম", 23.0159, 91.3976),
        BdDistrict("khagrachhari", "খাগড়াছড়ি", "চট্টগ্রাম", 23.1193, 91.9847),
        BdDistrict("lakshmipur", "লক্ষ্মীপুর", "চট্টগ্রাম", 22.9447, 90.8282),
        BdDistrict("noakhali", "নোয়াখালী", "চট্টগ্রাম", 22.8696, 91.0994),
        BdDistrict("rangamati", "রাঙ্গামাটি", "চট্টগ্রাম", 22.7324, 92.2985),

        // রাজশাহী বিভাগ (8)
        BdDistrict("rajshahi", "রাজশাহী", "রাজশাহী", 24.3745, 88.6042),
        BdDistrict("bogura", "বগুড়া", "রাজশাহী", 24.8465, 89.3776),
        BdDistrict("chapainawabganj", "চাঁপাইনবাবগঞ্জ", "রাজশাহী", 24.5965, 88.2775),
        BdDistrict("joypurhat", "জয়পুরহাট", "রাজশাহী", 25.0968, 89.0227),
        BdDistrict("naogaon", "নওগাঁ", "রাজশাহী", 24.7936, 88.9318),
        BdDistrict("natore", "নাটোর", "রাজশাহী", 24.4206, 89.0003),
        BdDistrict("pabna", "পাবনা", "রাজশাহী", 24.0064, 89.2372),
        BdDistrict("sirajganj", "সিরাজগঞ্জ", "রাজশাহী", 24.4534, 89.7007),

        // খুলনা বিভাগ (10)
        BdDistrict("khulna", "খুলনা", "খুলনা", 22.8456, 89.5403),
        BdDistrict("bagerhat", "বাগেরহাট", "খুলনা", 22.6516, 89.7859),
        BdDistrict("chuadanga", "চুয়াডাঙ্গা", "খুলনা", 23.6402, 88.8418),
        BdDistrict("jashore", "যশোর", "খুলনা", 23.1664, 89.2081),
        BdDistrict("jhenaidah", "ঝিনাইদহ", "খুলনা", 23.5448, 89.1539),
        BdDistrict("kushtia", "কুষ্টিয়া", "খুলনা", 23.9013, 89.1205),
        BdDistrict("magura", "মাগুরা", "খুলনা", 23.4873, 89.4199),
        BdDistrict("meherpur", "মেহেরপুর", "খুলনা", 23.7622, 88.6318),
        BdDistrict("narail", "নড়াইল", "খুলনা", 23.1725, 89.5127),
        BdDistrict("satkhira", "সাতক্ষীরা", "খুলনা", 22.7185, 89.0705),

        // বরিশাল বিভাগ (6)
        BdDistrict("barishal", "বরিশাল", "বরিশাল", 22.7010, 90.3535),
        BdDistrict("barguna", "বরগুনা", "বরিশাল", 22.1592, 90.1256),
        BdDistrict("bhola", "ভোলা", "বরিশাল", 22.6859, 90.6482),
        BdDistrict("jhalokathi", "ঝালকাঠি", "বরিশাল", 22.6406, 90.1987),
        BdDistrict("patuakhali", "পটুয়াখালী", "বরিশাল", 22.3596, 90.3299),
        BdDistrict("pirojpur", "পিরোজপুর", "বরিশাল", 22.5841, 89.9720),

        // সিলেট বিভাগ (4)
        BdDistrict("sylhet", "সিলেট", "সিলেট", 24.8949, 91.8687),
        BdDistrict("habiganj", "হবিগঞ্জ", "সিলেট", 24.3749, 91.4155),
        BdDistrict("moulvibazar", "মৌলভীবাজার", "সিলেট", 24.4829, 91.7774),
        BdDistrict("sunamganj", "সুনামগঞ্জ", "সিলেট", 25.0658, 91.3950),

        // রংপুর বিভাগ (8)
        BdDistrict("rangpur", "রংপুর", "রংপুর", 25.7439, 89.2752),
        BdDistrict("dinajpur", "দিনাজপুর", "রংপুর", 25.6217, 88.6354),
        BdDistrict("gaibandha", "গাইবান্ধা", "রংপুর", 25.3288, 89.5281),
        BdDistrict("kurigram", "কুড়িগ্রাম", "রংপুর", 25.8054, 89.6362),
        BdDistrict("lalmonirhat", "লালমনিরহাট", "রংপুর", 25.9923, 89.2847),
        BdDistrict("nilphamari", "নীলফামারী", "রংপুর", 25.9318, 88.8560),
        BdDistrict("panchagarh", "পঞ্চগড়", "রংপুর", 26.3411, 88.5542),
        BdDistrict("thakurgaon", "ঠাকুরগাঁও", "রংপুর", 26.0337, 88.4617),

        // ময়মনসিংহ বিভাগ (4)
        BdDistrict("mymensingh", "ময়মনসিংহ", "ময়মনসিংহ", 24.7471, 90.4203),
        BdDistrict("jamalpur", "জামালপুর", "ময়মনসিংহ", 24.9375, 89.9378),
        BdDistrict("netrokona", "নেত্রকোণা", "ময়মনসিংহ", 24.8710, 90.7279),
        BdDistrict("sherpur", "শেরপুর", "ময়মনসিংহ", 25.0205, 90.0153)
    )

    val DEFAULT: BdDistrict = ALL_64.first() // ঢাকা

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
