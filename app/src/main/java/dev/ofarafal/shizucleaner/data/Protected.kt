package dev.ofarafal.shizucleaner.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager

/** Aplikacje, których usunięcie odcięłoby użytkownika od telefonu: ta aplikacja, Shizuku, launchery, klawiatura, dialer, SMS. */
fun protectedPackages(ctx: Context): Set<String> {
    val pm = ctx.packageManager
    val set = mutableSetOf(ctx.packageName, "moe.shizuku.privileged.api")

    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    runCatching {
        pm.queryIntentActivities(home, 0).forEach { set += it.activityInfo.packageName }
    }
    runCatching {
        pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.let { set += it }
    }
    runCatching {
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')?.takeIf { it.isNotEmpty() }?.let { set += it }
    }
    runCatching {
        ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { set += it }
    }
    runCatching {
        Telephony.Sms.getDefaultSmsPackage(ctx)?.let { set += it }
    }
    return set
}
