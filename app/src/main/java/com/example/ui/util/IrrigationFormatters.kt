package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object IrrigationFormatters {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
    private val dateTimeFormat = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
    private val dayLabelFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun formatCountdown(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        val m = safe / 60
        val s = safe % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    fun formatDurationReadable(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return when {
            mins > 0 && secs > 0 -> "${mins}m ${secs}d"
            mins > 0 -> "$mins menit"
            else -> "$secs detik"
        }
    }

    fun formatScheduleTime(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    fun formatRelativeTime(timestampMillis: Long?): String {
        if (timestampMillis == null) return "Belum pernah disiram"
        val diff = System.currentTimeMillis() - timestampMillis
        val minutes = diff / 60_000L
        val hours = diff / 3600_000L
        return when {
            minutes < 1 -> "Baru saja"
            minutes < 60 -> "$minutes menit lalu"
            hours < 24 -> "$hours jam lalu"
            else -> dateTimeFormat.format(Date(timestampMillis))
        }
    }

    fun formatTimestamp(timestampMillis: Long): String {
        return dateTimeFormat.format(Date(timestampMillis))
    }

    fun formatSyncTime(timestampMillis: Long?): String {
        if (timestampMillis == null) return "Belum sinkron"
        return "Diperbarui ${timeFormat.format(Date(timestampMillis))}"
    }

    fun formatDaysMask(daysMask: Int): String {
        if (daysMask == 0b1111111) return "Setiap Hari"
        if (daysMask == 0b0011111) return "Hari Kerja"
        if (daysMask == 0b1100000) return "Akhir Pekan"
        val labels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val selected = labels.filterIndexed { index, _ -> (daysMask and (1 shl index)) != 0 }
        return if (selected.isEmpty()) "Tidak aktif" else selected.joinToString(", ")
    }

    fun formatApiDayShort(dateStr: String): String {
        return try {
            val parsed = apiDateFormat.parse(dateStr) ?: return dateStr
            dayLabelFormat.format(parsed)
        } catch (_: Exception) {
            dateStr.takeLast(5)
        }
    }

    fun weatherCodeToLabel(code: Int): String {
        return when (code) {
            0 -> "Cerah"
            1, 2 -> "Cerah Berawan"
            3 -> "Mendung"
            45, 48 -> "Berkabut"
            51, 53, 55 -> "Gerimis"
            61, 63, 65 -> "Hujan"
            80, 81, 82 -> "Hujan Lebat"
            95, 96, 99 -> "Badai Petir"
            else -> "Berawan"
        }
    }

    fun getStartOfDayMillis(daysAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return cal.timeInMillis
    }
}
