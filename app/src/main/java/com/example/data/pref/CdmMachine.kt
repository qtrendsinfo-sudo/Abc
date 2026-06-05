package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "cdm_machines")
data class CdmMachine(
    @PrimaryKey val id: Int,
    val merchantName: String,
    val branchName: String,
    val terminalId: String,
    val mapsUrl: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = false,
    val notes: String = "",
    val status: String = "ACTIVE", // ACTIVE, DOWN, CROWDED
    val lastReportType: String = "Operational",
    val lastReportTime: Long = System.currentTimeMillis()
) : Serializable {

    val shortBranchName: String
        get() = branchName.replace(" Branch", "").replace(" Banch", "")

    val isOnline: Boolean
        get() = status != "DOWN"
}

object CdmDataProvider {
    // Companion list of the 131 machines provided by the user
    val rawMachines = listOf(
        RawCdm(1, "KeyBS Merchant", "Bin Omran Branch-KeyBS Merchant", "10000135", "https://maps.app.goo.gl/r7JNNza8Vt18HLtJ9"),
        RawCdm(2, "Fresh Way Supermarket", "Al Rayyan Branch-Fresh Way Supermarket", "10000140", "https://maps.app.goo.gl/kA3MRiBFLtp4pq3T6"),
        RawCdm(3, "Zadak Hypermarket", "Sanniya Branch-Zadak Hypermarket", "10000141", "https://goo.gl/maps/bn8WJx1AxRbPD8Ar6"),
        RawCdm(4, "LU Grocery", "Sanniya Branch-LU Grocery", "10000142", "https://goo.gl/maps/8fut43DTvXjzqBaE9"),
        RawCdm(5, "Khayrat Al Doha Hypermarket", "Mamoura Banch-Khayrat Al Doha", "10000144", "https://goo.gl/maps/9scqJvk5i3BG6CuY6"),
        RawCdm(6, "Point Eight Supermarket", "Al Wukair Branch-Point Eight Supermarket", "10000149", "https://maps.app.goo.gl/4CorJYkmoEjpqR9c9"),
        RawCdm(7, "Prime Touch Delivery", "Sanniya Branch-Prime Touch Delivery", "10000153", "https://waze.com/ul/hthkx5p855"),
        RawCdm(8, "Cassava Mini Mart", "Al Saad Branch-Cassava Mini Mart", "10000155", "https://goo.gl/maps/wFZ4UWUJMMyKpNZv5"),
        RawCdm(9, "Al Abid Supermarket", "Al Khor Branch-Al Abid Supermarket", "10000156", "https://maps.app.goo.gl/LuxwVrj1dpLwANZL8"),
        RawCdm(10, "Dafna Prime Mart", "Dafna Branch- Dafna Prime Mart", "10000159", "https://maps.app.goo.gl/r9vtQPJSNLFNX96p8"),
        RawCdm(11, "Madeena Hypermarket", "Wakra Branch- Madeena Hypermarket", "10000160", "https://goo.gl/maps/v5aaGiHGVVuCjuL38"),
        RawCdm(12, "Al Shariyas Food Center", "Al Saad Branch-Al Shariyas Food Center", "10000164", "https://goo.gl/maps/B9ysVozHbCorZuhBA"),
        RawCdm(13, "Wadi Al Dahab", "Freej Abdul Azeez Branch-Wadi Al Dahab", "10000165", "https://goo.gl/maps/FzvMpcUvYTXArkQF7"),
        RawCdm(14, "Muraikh Food Complex", "Muaither Branch-Muraikh Food Complex", "10000166", "https://goo.gl/maps/QxQBknV6eHHHi8K1A"),
        RawCdm(15, "Mobile Point", "Wakra Branch-Mobile Point", "10000167", "https://goo.gl/maps/tX3tzXWTEoZk6afj7"),
        RawCdm(16, "Al Hamed Grocery", "Najma Branch-Al Hamed Grocery", "10000168", "https://goo.gl/maps/cTPwzQJCkwp54Qpq5"),
        RawCdm(17, "Metro Mart", "Souq Al Jaber Branch-Metro Mart", "10000169", "https://maps.app.goo.gl/Mt5AYkuGCsXcQigV8"),
        RawCdm(18, "Wusail Shopping Centre", "Um Salal Ali Branch-Wusail Shopping Centre", "10000171", "https://maps.app.goo.gl/AF31MsdtqxjwUJDm6"),
        RawCdm(19, "Sanniya Food City Market", "Sanniya Branch-Sanniya Food City Market", "10000172", "https://maps.app.goo.gl/mFy3X27RC1AkFQou5"),
        RawCdm(20, "Al Hadoud Supermarket", "Al Khor Branch-Al Hadoud Supermarket", "10000175", "https://goo.gl/maps/YkQroFZcndARTu7f9"),
        RawCdm(21, "Unknown CDM (Terminal 178)", "Doha Branch-Unknown Merchant", "10000178", ""),
        RawCdm(22, "Al Danube Food Stuff", "Sanniya Branch- Al Danube Food Stuff", "10000180", "https://maps.app.goo.gl/mMGvBRX9CDP9UomZ7"),
        RawCdm(23, "Rawiya Supermarket", "Al Kharaitiyat Branch Rawiya Supermarket", "10000181", "https://goo.gl/maps/7gduuobToyPgCgQZ6"),
        RawCdm(24, "Info Madeena Mobile", "Madeena Khalifa Branch-Info Madeena Mobile", "10000182", "https://goo.gl/maps/foXf53BdmhWYiH3i7"),
        RawCdm(25, "Go Do Delivery-1", "Sanniya Branch-Go Do Delivery-1", "10000183", "https://maps.app.goo.gl/dNTckpE2uWWW9bXr7"),
        RawCdm(26, "Al Ikhlas Supermarket", "Al Rayyan Branch-Al Ikhlas Supermarket", "10000185", "https://maps.app.goo.gl/W3LFxmbUSDfDzoQu8"),
        RawCdm(27, "Mishal Supermarket", "Al Saad Branch-Mishal Supermarket", "10000187", "https://maps.app.goo.gl/fxJdJ1BDHqXDNf8t5"),
        RawCdm(28, "Mr.Delivery", "Sanniya Branch-Mr.Delivery", "10000193", "https://goo.gl/maps/PVzhkiSaubiMNdC78"),
        RawCdm(29, "Falcon Big Mart", "Bin Omran Branch-Falcon Big Mart", "10000194", "https://maps.app.goo.gl/JTuiEmcdZK2ybdcu7"),
        RawCdm(30, "Blue Diamond Supermarket", "Muglina Branch-Blue Diamond Supermarket", "10000195", "https://maps.app.goo.gl/oDy9iPSK8yiMLWdi8"),
        RawCdm(31, "Jaal Phone", "Al Khor Branch-Jaal Phone", "10000197", "https://goo.gl/maps/J3QQYuVU3aKMGXwYA"),
        RawCdm(32, "Cosmo Supermarket", "Ain Khalid Branch-Cosmo Supermarket", "10000198", "https://goo.gl/maps/KMi1o6PZDudHgjvPA"),
        RawCdm(33, "Fresh Choice Supermarket", "Al Aziziya Branch-Fresh Choice Supermarket", "10000199", "https://maps.app.goo.gl/Y9bXSRRRQRQNPvh2A"),
        RawCdm(34, "Arbash Mobile", "Wakra Branch-Arbash Mobile", "10000200", "https://goo.gl/maps/tFncaesk1RnQ3zgX7"),
        RawCdm(35, "Green Fresh Supermarket", "Sanniya Branch-Green Fresh Supermarket", "10000201", "https://maps.app.goo.gl/zwVKVTAPf5XsYANT7"),
        RawCdm(36, "Marzam Supermarket", "Sanniya Branch-Marzam Supermarket", "10000202", "https://maps.app.goo.gl/QhZ6fTvYJAHWceibA"),
        RawCdm(37, "Grandex Supermarket", "Al Azizia Branch-Grandex Supermarket", "10000204", "https://goo.gl/maps/NhcbxMKV2danYDQe9"),
        RawCdm(38, "Int.Tel.Centre", "Wakra Branch-Int.Tel.Centre", "10000207", "https://goo.gl/maps/N9u51HqmyjH2"),
        RawCdm(39, "Malabar Supermarket", "Sanniya Branch-Malabar Supermarket", "10000208", "https://goo.gl/maps/ZK3gF15D6yDFncab6"),
        RawCdm(40, "Bathool Supermarket", "Lusail Branch-Bathool Supermarket", "10000211", "https://maps.app.goo.gl/PubPTcHtkb8EBeoN7"),
        RawCdm(41, "Swift Delivery-1", "Al Wukair Branch-Ezdan 29", "10000212", "https://goo.gl/maps/xBcoQB8XW9nnuK548"),
        RawCdm(42, "Al Fajer Shopping Center", "Najma Branch-Al Fajer Shopping Center", "10000213", "https://goo.gl/maps/bFq46uZt9L98Ge539"),
        RawCdm(43, "Swift Delivery-4", "Al Wukair Branch- swift Ezdan 29", "10000214", "https://goo.gl/maps/xBcoQB8XW9nnuK548"),
        RawCdm(44, "Seven Mart Hypermarket", "Fereej Abdul Azeez Branch-Seven Mart Hypermarket", "10000215", "https://maps.app.goo.gl/3Q3LLwMtyz6iatFj7"),
        RawCdm(45, "Danube Supermarket", "Sanniya Branch-Danube Supermarket", "10000217", "https://goo.gl/maps/q88JrGRgNJBP6Dfx7"),
        RawCdm(46, "Al Hebah Subermarket", "Sanniya Branch-St16-Al Hebah Subermarket", "10000218", "https://maps.app.goo.gl/TupouoX4VSZVQziR9"),
        RawCdm(47, "Swift Delivery-2", "Al Wukair Branch- Ezdan 32", "10000219", "https://goo.gl/maps/xBcoQB8XW9nnuK548"),
        RawCdm(48, "Sanniya Food City Market-3", "Sanniya Branch-Sanniya Food City Market-3", "10000220", "https://waze.com/ul/hthkx4ytyb"),
        RawCdm(49, "Masters Supermarket", "Simaisma Branch-Masters Supermarket", "10000221", "https://maps.app.goo.gl/AtRFG6cN8p9szFpA6"),
        RawCdm(50, "Deshi Taza Hypermarket", "Al Aziziya Branch-Deshi Taza Hypermarket", "10000222", "https://maps.app.goo.gl/sp3MGcDxGpZhH8ss5"),
        RawCdm(51, "Al Shariyas Food Center Azizyza", "Al Aziziya Branch-Al Shariyas Food Center Azizyza", "10000223", "https://maps.app.goo.gl/gegeNBnxumrUfkoE9"),
        RawCdm(52, "Clear Zone Supermarket", "Al Kheesa Branch- Clear Zone Supermarket", "10000224", "https://goo.gl/maps/CFtegk5cJVDGvFes9"),
        RawCdm(53, "Shop Well Supermarket", "Sanniya Branch-Shop Well Supermarket", "10000225", "https://maps.app.goo.gl/kHpWgLM7jWenpDde6"),
        RawCdm(54, "Delivex Delivery", "Saniiya Branch-Delivex Delivery", "10000226", "https://waze.com/ul/hthkx61hw3"),
        RawCdm(55, "Garden Fresh Supermarket", "Muntazah Branch- Garden Fresh Supermarket", "10000227", "https://goo.gl/maps/MGwEpzhbq7aN8YAC9"),
        RawCdm(56, "Rosa Supermarket", "Al Saad Branch-Rosa Supermarket", "10000228", "https://maps.app.goo.gl/CzrCSzo5ap1eVBRj7"),
        RawCdm(57, "Salik Delivery", "Saniiya Branch-Salik Delivery", "10000229", "https://maps.app.goo.gl/AgAceFcjtYD8gFQ76"),
        RawCdm(58, "Swift Delivery-3", "Al Wukair Branch-Swift Ezdan 38", "10000230", "https://goo.gl/maps/g2LAWC6uXXSTYUwA7"),
        RawCdm(59, "Fast Grocery 2", "Mansoura Branch-Fast Grocery 2", "10000231", "https://maps.app.goo.gl/Wgae38zJ77ojRJ1D8"),
        RawCdm(60, "Maherjeh Grocery", "Thumama Branch- Maherjeh Grocery", "10000232", "https://maps.app.goo.gl/6fPDpK4GJRNZ17Z36"),
        RawCdm(61, "Bongo Bazar Supermarket", "Khartiyat Branch-Bongo Bazar Supermarket", "10000233", "https://maps.app.goo.gl/v8AN8NuUMSmTB4iD7"),
        RawCdm(62, "New Kenz Supermarket", "Al Aziziya Branch-New Kenz Supermarket", "10000234", "https://maps.app.goo.gl/rUAcMytKeKX5hhin6"),
        RawCdm(63, "Al Mashoor Supermarket", "Al Wukair Branch-Al Mashoor Supermarket", "10000235", "https://maps.app.goo.gl/pDGAXAh7upRHHYf1A"),
        RawCdm(64, "Tawa Trading & Services", "Old Airport Road Branch-Tawa Trading & Services", "10000236", "https://goo.gl/maps/iMAfKnHDdFH49gaD7"),
        RawCdm(65, "Food Corner Supermarket", "Abu Hamour Branch-Food Corner Supermarket", "10000237", "https://goo.gl/maps/Hs7Um5263eiLd8tB9"),
        RawCdm(66, "WIFI Grocery", "Sanniya Branch-WIFI Grocery", "10000238", "https://maps.app.goo.gl/CfKUQEpznHpcne3p8"),
        RawCdm(67, "Unknown CDM (Terminal 239)", "Doha Branch-Unknown Merchant", "10000239", ""),
        RawCdm(68, "Friendly Mart", "Najma Branch-Friendly Mart", "10000240", "https://goo.gl/maps/eUioBS87vagfREFz9"),
        RawCdm(69, "Al Kuwari Food Stuff", "Madina Khalifa Branch-Al Kuwari Food Stuff", "10000241", "https://goo.gl/maps/Hr7eySeqtrMiC6w96"),
        RawCdm(70, "Al Nosour Delivery", "Sanniya Branch-Al Nosour Delivery", "10000242", "https://maps.app.goo.gl/6WTsrMBhF4vCUXNj"),
        RawCdm(71, "Carry Food Minimart", "Bin Mahmood Branch-Carry Food Minimart", "10000243", "https://maps.app.goo.gl/H3erDHza7FfsxL7w9"),
        RawCdm(72, "Cmax Mobiles & Watches", "Salwa Road Branch- Cmax Mobiles & Watches", "10000244", "https://maps.app.goo.gl/2R86BrmV5qZEhdM37"),
        RawCdm(73, "Monir Supermarket", "Sanniya Branch-Monir Supermarket", "10000245", "https://maps.app.goo.gl/RzkgTDHtVgHnghpK8"),
        RawCdm(74, "Al Markhiya Mobiles", "Al Markhiya Branch-Al Markhiya Mobiles", "10000246", "https://goo.gl/maps/dfT4VdhdqffNwGBSA"),
        RawCdm(75, "Sanniya Food City Market-2", "Sanniya Branch-Sanniya Food City Market-2", "10000247", "https://maps.app.goo.gl/No4fejHDL1uHXNdW9"),
        RawCdm(76, "Siniyari Grocery", "Sanniya Branch-Siniyari Grocery", "10000248", "https://maps.app.goo.gl/G9xLura2Hb66zhLd6"),
        RawCdm(77, "Shahbiyah Supermarket", "Al Waab Branch- Shahbiyah Supermarket", "10000249", "https://goo.gl/maps/NWurvSQbTXkABumb9"),
        RawCdm(78, "Urban Food Market-2", "New Salata Branch-Urban Food Market-2", "10000250", "https://maps.app.goo.gl/VVwHE7B8ivKViSGg6"),
        RawCdm(79, "Food City Market", "Al Saad Branch-Food City Market", "10000251", "https://maps.app.goo.gl/no4JPtGu9HDgzRTRA"),
        RawCdm(80, "Selection Mart Supermarket", "Hilal Branch-Selection Mart Supermarket", "10000252", "https://goo.gl/maps/uGq4rB1Ni4Mfkuq86"),
        RawCdm(81, "Al Koot Supermarket", "Al Kheesa Branch-Al Koot Supermarket", "10000253", "https://goo.gl/maps/whqnbeKRxeBVVp4u9"),
        RawCdm(82, "Shimla Grocery", "Sanniya Branch-Shimla Grocery", "10000254", "https://goo.gl/maps/JJD6wZgKkyYh96P2A"),
        RawCdm(83, "Samyog Trading & Services", "Souq Al Jaber Branch-Samyog Tading & Services", "10000255", "https://maps.app.goo.gl/9RmPXoU6jLDp1z736"),
        RawCdm(84, "Door Step Supermarket", "Fereej Abdul Azeez Branch-Door Step Supermarket", "10000256", "https://maps.app.goo.gl/DE4QJRZDTY9nD68F6"),
        RawCdm(85, "Al Kausar Supermarket", "Al Gharaffa Branch-Al Kausar Supermarket", "10000257", "https://maps.app.goo.gl/igXSnnE71gPJqw6K9"),
        RawCdm(86, "Afia Mart", "Al Rayyan Branch- Afia Mart", "10000258", "https://maps.app.goo.gl/Wu6hxybSzwhTpH4f9"),
        RawCdm(87, "Beep Delivery", "Sanniya Branch-Beep Delivery", "10000259", "https://maps.app.goo.gl/xZ7hTFCV7VjqbXo27"),
        RawCdm(88, "Smart Shopping center", "Muntazah Branch-Smart Shopping Center", "10000260", "https://goo.gl/maps/uT3zNS2nfimqmDGS6"),
        RawCdm(89, "Perfect Mobiles", "Bin Omran Branch-Perfect Mobile", "10000261", "https://maps.app.goo.gl/6tw83CtNUnV3K6BcA"),
        RawCdm(90, "SRS Restaurant", "Al Rayyan Branch-SRS Restaurant", "10000262", "https://goo.gl/maps/psA7stN5AamQQhXf7"),
        RawCdm(91, "Go Do Delivery-3", "Sanniya Branch-Go Do Delivery-3", "10000263", "https://goo.gl/maps/ogBnqiangNzoMSo18"),
        RawCdm(92, "Noor Al Wakra Food Center", "Wakra Branch-Noor Al Wakra Food Center", "10000264", "https://goo.gl/maps/ZAPNT2is4JTGmkbBA"),
        RawCdm(93, "Al Malki Trade Center Supermarket", "Al Khor Branch-Al Malki Trade Center Supermarket", "10000265", "https://maps.app.goo.gl/ZZoSHMSCYdtCii5q6"),
        RawCdm(94, "Fast Grocery", "Mansoura Branch-Fast Grocery", "10000266", "https://goo.gl/maps/La5aDJm1pBP8UUmF8"),
        RawCdm(95, "Noorul Arab cafeteria", "Sanniya Branch-Noorul Arab cafeteria", "10000267", "https://goo.gl/maps/GTJE4TTmpzYWqFQX7"),
        RawCdm(96, "Shams Al Madeena Supermarket", "Sanniya Branch-Shams Al Madeena Supermarket", "10000268", "https://maps.app.goo.gl/HP9LS78pLBGV5xiy8"),
        RawCdm(97, "Iqra Supermarket", "Bin Omran Branch-Iqra Supermarket", "10000269", "https://goo.gl/maps/v2baFkkEKpLkXtMQ7"),
        RawCdm(98, "Al Habari Food Stuff", "Al Khor Branch-Al Habari Food Stuff", "10000270", "https://goo.gl/maps/NhtzxMax1z5msh2QA"),
        RawCdm(99, "Garden Fresh Supermarket-2", "Muglina Branch-Garden Fresh Supermarket-2", "10000271", "https://goo.gl/maps/t7ZcyxDAScun4DGs9"),
        RawCdm(100, "Onesa Supermarket", "Sanniya Branch-Onesa Supemarket", "10000272", "https://goo.gl/maps/cyPo1EDoSDNV6NhTA"),
        RawCdm(101, "Al Hammadi Supermarket", "Sanniya Branch-Al Hammadi Supermarket", "10000273", "https://maps.app.goo.gl/w3swmXvPuFZ9B3VdA"),
        RawCdm(102, "New Madeena Hypermarket", "Doha Jadeed Branch-New Madeena Hypermarket", "10000274", "https://goo.gl/maps/vWzhzuSAQNRGypGq6"),
        RawCdm(103, "Go Do Delivery-4", "Sanniya Branch-Go Do Delivery-4", "10000275", "https://maps.app.goo.gl/dNTckpE2uWWW9bXr7"),
        RawCdm(104, "Unknown CDM (Terminal 276)", "Doha Branch-Unknown Merchant", "10000276", ""),
        RawCdm(105, "Jaseela Supermarket", "Umm Garn Branch-Jaseela Supermarket", "10000277", "https://goo.gl/maps/r2JSTASj193A4jjJ7"),
        RawCdm(106, "Dolphin Grocery", "Sanniya Branch-Dolphin Grocery", "10000278", "https://goo.gl/maps/rcxvqvBmuhPSzV5v9"),
        RawCdm(107, "Galaxy Food Centre", "Muntazah Branch-Galaxy Food Centre", "10000279", "https://goo.gl/maps/zFhz9N64URHswN9S6"),
        RawCdm(108, "LULU AL BIDA GROCERY", "Al Kheesa Branch-LULU AL BIDA GROCERY", "10000280", "https://goo.gl/maps/VifXsWPNb8dZ5vma9"),
        RawCdm(109, "Road Track Trading", "Muglina Branch-Road Track Trading", "10000281", "https://goo.gl/maps/kk86iDWPNZUR4EE38"),
        RawCdm(110, "Road Track Trading-2", "Fereej Abdul Azeez Branch-Road Track Trading-2", "10000282", "https://maps.app.goo.gl/E38FDmGb8duCD1h16"),
        RawCdm(111, "Able Mart Hypermarket", "sanniya Branch-Able Mart Hypermarket", "10000283", "https://maps.app.goo.gl/Gzpz4k2xtewzBCaRA"),
        RawCdm(112, "Danube Food Center", "Sanniya Branch-Danube Food Centre", "10000284", "https://goo.gl/maps/uvBZTFQ2Y4pDCDi5A"),
        RawCdm(113, "Al Fajer Shopping Center-2", "Sanniya Branch- Al Fajer Shopping Center-2", "10000285", "https://goo.gl/maps/bn8WJx1AxRbPD8Ar6"),
        RawCdm(114, "Star Max Grocery", "Sanniya Branch- Star Max Grocery", "10000286", "https://goo.gl/maps/JnhNYX1G73qfkdsDA"),
        RawCdm(115, "Paradise Grocery", "Sanniya Branch-Paradise Grocery", "10000287", "https://goo.gl/maps/wMYrHHEGEm4Hq3jJ8"),
        RawCdm(116, "Oruma Grocery", "Sanniya Branch-Oruma Grocery", "10000288", "https://goo.gl/maps/BsDKsMVZtwT7ANar7"),
        RawCdm(117, "Better Buys Trading", "Muntazah Branch-Better Buys Trading", "10000289", "https://goo.gl/maps/tCJrJ8tvCnRuPSVK7"),
        RawCdm(118, "PRIME MART-2", "Salwa Road-Prime Mart-2", "10000290", "https://goo.gl/maps/guM1QFjiqDJigujb8"),
        RawCdm(119, "Insaaf Supermarket", "Al Aziziya Branch-Insaaf Supermarket", "10000291", "https://maps.app.goo.gl/1bYnoCp28PrCmzxQA"),
        RawCdm(120, "Go Do Delivery-2", "Sanniya Branch-Go Do Delivery-2", "10000292", "https://maps.app.goo.gl/8SETSMsGNfxhxyoTA"),
        RawCdm(121, "Dolphin Grocery-2", "Sanniya-37 Branch-Dolphin Grocery-2", "10000293", "https://goo.gl/maps/bgNuBq9AwGJ8AuFN9"),
        RawCdm(122, "Al Kaabi Grocery", "Madina Khalifa Branch-Al Kaabi Grocery", "10000294", "https://goo.gl/maps/qru7QGZq41SsYjG99"),
        RawCdm(123, "Skyline Supermarket", "Lukta Branch-Skyline Supermarket", "10000295", "https://goo.gl/maps/3z5veTkKu9541kF4A"),
        RawCdm(124, "Food Market", "Sanniya Branch-Food Market", "10000296", "https://goo.gl/maps/bWswKTAvwDrHm7FeA"),
        RawCdm(125, "Grand Bazar Supermarket", "Doha Jadeed Branch-Grand Bazar Supermarket", "10000297", "https://goo.gl/maps/sERGqyrzLCBoz6zK7"),
        RawCdm(126, "Prime Mart", "Midmac Signal Branch-Prime Mart", "10000298", "https://goo.gl/maps/qDn8FgctmWu3NSNe6"),
        RawCdm(127, "Kfone Trading", "Muntaza Branch-Kfone Trading", "10000303", "https://goo.gl/maps/KPjta6YbiVWUkzMSA"),
        RawCdm(128, "Abdulla Al Jaber Grocery", "Sanniya Branch-Abdulla Al Jaber Grocery", "10000309", "https://goo.gl/maps/NzNAuxhsHLZ9BD4GA"),
        RawCdm(129, "Global Smart Trading", "Muaither Branch-Global Smart Trading", "10000417", "https://maps.app.goo.gl/qkRVPC9eCsbXFv9d7"),
        RawCdm(130, "HFM Supermarket", "Ain Khalid Branch-HFM Supermarket", "10000418", "https://maps.app.goo.gl/Ey7j7yxWDf19UYbBA"),
        RawCdm(131, "Nana Food Mart", "UmSalal Ali Branch- Nana Food Mart", "10000419", "https://maps.app.goo.gl/JyajYxxscgdsiHRj9")
    )

    fun getInitialMachines(): List<CdmMachine> {
        return rawMachines.map { raw ->
            val (lat, lng) = getCoordinatesForBranch(raw.id, raw.branchName)
            // Generate some random initial status reports to make it extremely dynamic and colorful!
            val initialStatus = when {
                raw.id % 23 == 0 -> "DOWN"
                raw.id % 17 == 0 -> "CROWDED"
                else -> "ACTIVE"
            }
            val initialReport = when (initialStatus) {
                "DOWN" -> "Offline / Cash Box Full"
                "CROWDED" -> "Busy / 10+ Min Backlog"
                else -> "Fully Operational"
            }
            CdmMachine(
                id = raw.id,
                merchantName = raw.merchantName,
                branchName = raw.branchName,
                terminalId = raw.terminalId,
                mapsUrl = if (raw.mapsUrl.isEmpty()) "https://maps.google.com/?q=${lat},${lng}" else raw.mapsUrl,
                latitude = lat,
                longitude = lng,
                isFavorite = raw.id in listOf(1, 3, 6, 12, 54), // Pre-set a few cute favorites
                notes = "",
                status = initialStatus,
                lastReportType = initialReport,
                lastReportTime = System.currentTimeMillis() - (raw.id * 300000L) % 7200000L // realistic relative times
            )
        }
    }

    private fun getCoordinatesForBranch(id: Int, branchName: String): Pair<Double, Double> {
        val lower = branchName.lowercase()
        val indexOffset = id * 0.0019

        val baseLat: Double
        val baseLng: Double

        when {
            lower.contains("sanniya") || lower.contains("saniiya") -> {
                // Doha Industrial Area (Sanniya)
                baseLat = 25.1950 + (id % 15) * 0.0062
                baseLng = 51.4360 - (id % 15) * 0.0048
            }
            lower.contains("rayyan") -> {
                baseLat = 25.2913 + (id % 8) * 0.0035
                baseLng = 51.4253 + (id % 8) * -0.0028
            }
            lower.contains("wukair") -> {
                baseLat = 25.1275 + (id % 5) * 0.0025
                baseLng = 51.5835 + (id % 5) * 0.0032
            }
            lower.contains("wakra") -> {
                baseLat = 25.1760 + (id % 6) * 0.0030
                baseLng = 51.6030 - (id % 6) * 0.0025
            }
            lower.contains("saad") || lower.contains("salwa") -> {
                baseLat = 25.2890 + (id % 5) * 0.0015
                baseLng = 51.4980 - (id % 5) * 0.0012
            }
            lower.contains("khor") -> {
                baseLat = 25.6880 + (id % 4) * 0.0045
                baseLng = 51.5050 + (id % 4) * -0.0030
            }
            lower.contains("dafna") || lower.contains("lusail") -> {
                baseLat = 25.3500 + (id % 6) * 0.0040
                baseLng = 51.5280 + (id % 6) * 0.0022
            }
            lower.contains("omran") || lower.contains("khalifa") -> {
                baseLat = 25.3130 + (id % 5) * 0.0020
                baseLng = 51.4950 + (id % 5) * 0.0015
            }
            lower.contains("najma") || lower.contains("mansoura") || lower.contains("muntazah") -> {
                baseLat = 25.2750 + (id % 8) * 0.0018
                baseLng = 51.5350 + (id % 8) * -0.0014
            }
            lower.contains("abdel azeez") || lower.contains("abdul azeez") || lower.contains("jaber") || lower.contains("mahmood") -> {
                baseLat = 25.2780 + (id % 6) * 0.0012
                baseLng = 51.5250 + (id % 6) * -0.0015
            }
            lower.contains("muaither") -> {
                baseLat = 25.2800 + (id % 5) * 0.0045
                baseLng = 51.4000 + (id % 5) * 0.0038
            }
            lower.contains("airport") || lower.contains("hilal") || lower.contains("thumama") -> {
                baseLat = 25.2450 + (id % 7) * 0.0028
                baseLng = 51.5520 - (id % 7) * 0.0022
            }
            lower.contains("kheesa") || lower.contains("kharaitiyat") || lower.contains("salata") -> {
                baseLat = 25.3850 + (id % 6) * 0.0035
                baseLng = 51.4650 - (id % 6) * 0.0030
            }
            lower.contains("khalid") || lower.contains("hamour") || lower.contains("azizia") || lower.contains("aziziya") -> {
                baseLat = 25.2300 + (id % 8) * 0.0030
                baseLng = 51.4500 + (id % 8) * 0.0028
            }
            else -> {
                // Doha Center distributed fallback
                baseLat = 25.2854 + Math.sin(id.toDouble()) * 0.065
                baseLng = 51.5310 + Math.cos(id.toDouble()) * 0.055
            }
        }

        return Pair(baseLat, baseLng)
    }
}

data class RawCdm(
    val id: Int,
    val merchantName: String,
    val branchName: String,
    val terminalId: String,
    val mapsUrl: String
)
