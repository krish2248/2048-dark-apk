package com.personal.game2048.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
import androidx.core.content.FileProvider
import com.personal.game2048.data.Stats
import com.personal.game2048.pdf.StatsPdfGenerator
import java.util.Locale

fun shareText(context: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(i, null))
}

fun shareStatsPdf(context: Context, stats: Stats) {
    val file = StatsPdfGenerator().generate(context, stats)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "My 2048 stats")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(i, "Share stats"))
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
    }
}

/** Best guess only, used to preselect a flag. Needs no permission; the player can change it. */
fun guessCountry(context: Context): String {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val candidates = listOf(tm?.simCountryIso, tm?.networkCountryIso, Locale.getDefault().country)
    return candidates.firstOrNull { it?.length == 2 && it.all(Char::isLetter) }?.uppercase(Locale.ROOT) ?: "US"
}
