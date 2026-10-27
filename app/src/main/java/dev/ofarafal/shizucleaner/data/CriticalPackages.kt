package dev.ofarafal.shizucleaner.data

/**
 * Pakiety, których usunięcie grozi bootloopem lub utratą dzwonienia/SMS/sieci.
 * Zostają w kodzie (a nie w JSON), żeby żadna podmieniona baza nie mogła ich odblokować.
 * Bez zależności od Androida: używa tego też serwis działający jako shell oraz testy.
 */
object CriticalPackages {

    private val exact = setOf(
        "android",
        "moe.shizuku.privileged.api",
        "com.android.systemui",
        "com.android.phone",
        "com.android.settings",
        "com.android.server.telecom",
        "com.android.shell",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.inputdevices",
        "com.android.keychain",
        "com.android.certinstaller",
        "com.android.location.fused",
        "com.android.externalstorage",
        "com.android.mtp",
        "com.android.vpndialogs",
        "com.android.carrierconfig",
        "com.android.mms.service",
        "com.android.se",
        "com.android.ons",
        "com.android.emergency",
        "com.android.proxyhandler",
        "com.android.statementservice",
        "com.android.captiveportallogin",
        "com.android.companiondevicemanager",
        "com.android.credentialmanager",
        "com.android.managedprovisioning",
        "com.android.cellbroadcastreceiver",
        "com.android.networkstack",
        "com.android.networkstack.inprocess",
        "com.google.android.networkstack",
        "com.google.android.networkstack.tethering",
        "com.google.android.cellbroadcastservice",
        "com.google.android.captiveportallogin",
        "com.google.android.ext.services",
        "com.google.android.ext.shared",
        "com.android.vending",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.webview",
        "com.android.webview",
        "com.android.providers.media",
        "com.android.providers.media.module",
        "com.android.providers.contacts",
        "com.android.providers.calendar",
        "com.android.providers.telephony",
        "com.android.providers.settings",
        "com.android.providers.downloads",
        "com.android.providers.downloads.ui",
        "com.android.providers.userdictionary",
        "com.android.providers.blockednumber",
        "com.android.providers.applications",
        "com.samsung.android.incallui",
        "com.samsung.android.dialer",
        "com.samsung.android.messaging",
        "com.samsung.android.app.contacts",
        "com.samsung.android.app.telephonyui",
        "com.sec.android.app.launcher",
        "com.sec.imsservice",
        "com.sec.epdg",
        "com.sec.phone",
    )

    private val prefixes = listOf(
        "com.samsung.android.knox.",
        "com.sec.android.provider.",
        "com.samsung.android.providers.",
    )

    fun isCritical(pkg: String): Boolean = pkg in exact || prefixes.any { pkg.startsWith(it) }
}
