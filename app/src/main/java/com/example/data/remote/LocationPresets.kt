package com.example.data.remote

data class LocationPreset(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object IndonesianLocationPresets {
    val cities = listOf(
        LocationPreset("Bogor, Jawa Barat", -6.5971, 106.8060),
        LocationPreset("Jakarta Pusat", -6.1751, 106.8650),
        LocationPreset("Bandung, Jawa Barat", -6.9175, 107.6191),
        LocationPreset("Yogyakarta", -7.7956, 110.3695),
        LocationPreset("Surabaya, Jawa Timur", -7.2575, 112.7521),
        LocationPreset("Malang, Jawa Timur", -7.9666, 112.6326),
        LocationPreset("Denpasar, Bali", -8.6705, 115.2126),
        LocationPreset("Medan, Sumatera Utara", 3.5952, 98.6722),
        LocationPreset("Makassar, Sulawesi Selatan", -5.1477, 119.4327)
    )
}
