package dev.ofarafal.shizucleaner.data

enum class Risk(val label: String) {
    SAFE("Bezpieczne"),
    CAUTION("Ostrożnie"),
    UNKNOWN("Nieznane"),
    CRITICAL("Krytyczne"),
}

data class KnownPackage(val pkg: String, val label: String, val risk: Risk, val note: String)

/**
 * Lista startowa. Przed wydaniem zweryfikuj na prawdziwym urządzeniu (S21 / One UI).
 * Pakiety spoza bazy trafiają jako UNKNOWN i są domyślnie ukryte.
 */
object PackageDb {

    private fun s(pkg: String, label: String, note: String) =
        KnownPackage(pkg, label, Risk.SAFE, note)

    private fun c(pkg: String, label: String, note: String) =
        KnownPackage(pkg, label, Risk.CAUTION, note)

    private val entries = listOf(
        // Meta / Microsoft / inne preinstalowane
        s("com.facebook.appmanager", "Facebook App Manager", "Aktualizator aplikacji Meta"),
        s("com.facebook.services", "Facebook Services", "Usługi w tle Meta"),
        s("com.facebook.system", "Facebook System", "Instalator aplikacji Meta"),
        s("com.facebook.katana", "Facebook", "Aplikacja Facebook"),
        s("com.microsoft.skydrive", "OneDrive", "Chmura Microsoft"),
        s("com.microsoft.office.officehubrow", "Microsoft 365", "Pakiet Office"),
        s("com.microsoft.office.outlook", "Outlook", "Poczta Microsoft"),
        s("com.microsoft.appmanager", "Link to Windows", "Companion do Windows"),
        s("com.linkedin.android", "LinkedIn", "Aplikacja LinkedIn"),
        s("com.netflix.mediaclient", "Netflix", "Aplikacja Netflix"),
        s("com.netflix.partner.activation", "Netflix Activation", "Aktywacja partnerska"),
        s("com.spotify.music", "Spotify", "Aplikacja Spotify"),
        s("com.amazon.mShop.android.shopping", "Amazon", "Aplikacja zakupowa"),

        // Google
        s("com.google.android.apps.tachyon", "Google Meet", "Rozmowy wideo"),
        s("com.google.android.apps.youtube.music", "YouTube Music", "Odtwarzacz muzyki"),
        s("com.google.android.youtube", "YouTube", "Aplikacja YouTube"),
        s("com.google.android.videos", "Google TV", "Filmy i seriale"),
        s("com.google.android.apps.magazines", "Google News", "Wiadomości"),
        s("com.google.android.apps.docs", "Dysk Google", "Chmura Google"),
        s("com.google.android.apps.docs.editors.docs", "Dokumenty Google", "Edytor"),
        s("com.google.android.apps.docs.editors.sheets", "Arkusze Google", "Edytor"),
        s("com.google.android.apps.docs.editors.slides", "Prezentacje Google", "Edytor"),
        s("com.google.android.gm", "Gmail", "Poczta Google"),
        s("com.google.android.keep", "Google Keep", "Notatki"),
        s("com.google.android.apps.podcasts", "Podcasty Google", "Odtwarzacz podcastów"),
        s("com.google.android.apps.nbu.files", "Files by Google", "Menedżer plików"),
        s("com.google.android.projection.gearhead", "Android Auto", "Tryb samochodowy"),
        s("com.google.android.feedback", "Market Feedback Agent", "Raporty zwrotne"),
        s("com.google.android.printservice.recommendation", "Print Recommendation", "Podpowiedzi drukowania"),
        c("com.google.android.apps.photos", "Zdjęcia Google", "Jeśli to Twoja galeria, zostaw"),
        c("com.google.android.apps.maps", "Mapy Google", "Używane przez inne aplikacje"),
        c("com.google.android.apps.messaging", "Wiadomości Google", "Może być domyślnym SMS"),
        c("com.google.android.apps.googleassistant", "Asystent Google", "Wyłącza Asystenta"),
        c("com.google.android.googlequicksearchbox", "Google (wyszukiwarka)", "Widżet wyszukiwania, Asystent"),
        c("com.google.android.apps.wellbeing", "Digital Wellbeing", "Czas przed ekranem"),
        c("com.google.android.apps.turbo", "Device Health Services", "Zdrowie baterii"),
        c("com.google.android.apps.walletnfcrel", "Portfel Google", "Płatności NFC"),
        c("com.google.android.syncadapters.calendar", "Sync kalendarza", "Synchronizacja kalendarza Google"),
        c("com.google.android.tts", "Google TTS", "Synteza mowy, dostępność"),
        c("com.google.android.marvin.talkback", "TalkBack", "Czytnik ekranu"),
        c("com.google.ar.core", "Usługi Google dla AR", "Wymagane przez aplikacje AR"),
        c("com.google.android.onetimeinitializer", "One Time Init", "Pomocnik konfiguracji"),
        c("com.google.android.partnersetup", "Partner Setup", "Konfiguracja partnerska"),
        c("com.google.android.setupwizard", "Kreator konfiguracji", "Po konfiguracji zbędny, ale ryzykowny"),

        // Samsung
        s("com.samsung.android.bixby.agent", "Bixby Voice", "Asystent głosowy"),
        s("com.samsung.android.bixby.wakeup", "Bixby Wake-up", "Wybudzanie głosem"),
        c("com.samsung.android.bixby.service", "Bixby Service", "Usługa tła Bixby"),
        c("com.samsung.android.app.routines", "Tryby i rutyny", "Automatyzacje Samsunga"),
        c("com.samsung.android.visionintelligence", "Bixby Vision", "Powiązane z aparatem"),
        s("com.samsung.android.app.spage", "Samsung Free", "Panel po lewej od ekranu głównego"),
        s("com.samsung.android.game.gamehome", "Game Launcher", "Launcher gier"),
        s("com.samsung.android.game.gametools", "Game Tools", "Nakładka w grach"),
        c("com.samsung.android.game.gos", "Game Optimizing Service", "Dławienie wydajności w grach"),
        s("com.samsung.android.app.tips", "Wskazówki", "Samouczki Samsunga"),
        s("com.samsung.android.arzone", "AR Zone", "Zestaw funkcji AR"),
        s("com.samsung.android.aremoji", "AR Emoji", "Awatary"),
        s("com.samsung.android.ardrawing", "AR Doodle", "Rysowanie w AR"),
        s("com.samsung.android.stickercenter", "Sticker Center", "Naklejki"),
        s("com.samsung.android.livestickers", "Live Stickers", "Naklejki w klawiaturze"),
        s("com.samsung.android.kidsinstaller", "Kids Installer", "Instalator trybu dziecięcego"),
        s("com.samsung.android.email.provider", "Samsung Email", "Klient poczty"),
        s("com.samsung.android.voc", "Samsung Members", "Wsparcie i diagnostyka"),
        s("com.samsung.sree", "Samsung Global Goals", "Kampanie charytatywne"),
        s("com.samsung.android.rubin.app", "Customization Service", "Personalizacja i zbieranie danych"),
        s("com.sec.android.diagmonagent", "Diagnostic Monitoring", "Telemetria"),
        s("com.sec.android.easyMover", "Smart Switch", "Przenoszenie danych"),
        c("com.samsung.android.app.sharelive", "Quick Share", "Udostępnianie plików"),
        c("com.samsung.android.app.watchmanager", "Galaxy Wearable", "Zarządzanie zegarkiem"),
        c("com.samsung.android.themestore", "Galaxy Themes", "Motywy"),
        c("com.samsung.android.mdx", "Link to Windows (usługa)", "Integracja z Windows"),
        c("com.sec.android.app.samsungapps", "Galaxy Store", "Sklep Samsunga"),
        c("com.samsung.android.mobileservice", "Samsung Experience Service", "Konto Samsung, synchronizacja"),
        c("com.samsung.android.samsungpass", "Samsung Pass", "Menedżer haseł i biometria"),
        c("com.samsung.android.spay", "Samsung Pay", "Płatności"),
        c("com.samsung.android.smartcallprovider", "Smart Call", "Identyfikacja rozmówców"),
        c("com.sec.android.app.sbrowser", "Samsung Internet", "Przeglądarka"),
        c("com.samsung.android.app.notes", "Samsung Notes", "Notatki"),
        c("com.samsung.android.app.reminder", "Przypomnienia", "Przypomnienia Samsunga"),
        c("com.samsung.android.calendar", "Kalendarz Samsung", "Kalendarz"),

        // Xiaomi / MIUI
        s("com.miui.analytics", "MIUI Analytics", "Telemetria"),
        s("com.miui.msa.global", "MSA", "Reklamy systemowe"),
        s("com.miui.systemAdSolution", "System Ads", "Reklamy systemowe"),
        s("com.miui.bugreport", "Bug Report", "Raporty błędów"),
        s("com.miui.yellowpage", "Yellow Pages", "Książka firm"),
        s("com.miui.player", "Mi Music", "Odtwarzacz muzyki"),
        s("com.mi.globalbrowser", "Mi Browser", "Przeglądarka"),
        s("com.xiaomi.glgm", "Mi Games", "Promocje gier"),
        s("com.miui.hybrid", "Quick Apps", "Aplikacje błyskawiczne"),
        c("com.xiaomi.mipicks", "GetApps", "Sklep Xiaomi"),
        c("com.miui.cloudservice", "Mi Cloud", "Chmura Xiaomi"),
        c("com.xiaomi.joyose", "Joyose", "Zarządzanie wydajnością"),
        c("com.miui.daemon", "MIUI Daemon", "Usługa tła"),

        // AOSP
        s("com.android.egg", "Easter Egg", "Wielkanocne jajko Androida"),
        s("com.android.dreams.basic", "Basic Daydreams", "Wygaszacz"),
        s("com.android.dreams.phototable", "Photo Table", "Wygaszacz"),
        s("com.android.bookmarkprovider", "Bookmark Provider", "Zakładki przeglądarki"),
        s("com.android.providers.partnerbookmarks", "Partner Bookmarks", "Zakładki partnerskie"),
        s("com.android.bips", "Default Print Service", "Usługa drukowania"),
        c("com.android.printspooler", "Print Spooler", "Kolejka wydruku"),
        c("com.android.traceur", "System Tracing", "Narzędzie dla developerów"),
        c("com.android.wallpaperbackup", "Wallpaper Backup", "Kopia tapety"),
        c("com.android.stk", "SIM Toolkit", "Menu operatora SIM"),
    )

    private val byPkg: Map<String, KnownPackage> = entries.associateBy { it.pkg }

    // Pakiety, których usunięcie grozi bootloopem lub utratą dzwonienia/SMS/sieci.
    private val criticalExact = setOf(
        "android", "moe.shizuku.privileged.api",
        "com.android.systemui", "com.android.phone", "com.android.settings",
        "com.android.server.telecom", "com.android.shell",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller",
        "com.android.packageinstaller", "com.google.android.packageinstaller",
        "com.android.bluetooth", "com.android.nfc", "com.android.inputdevices",
        "com.android.keychain", "com.android.certinstaller", "com.android.location.fused",
        "com.android.externalstorage", "com.android.mtp", "com.android.vpndialogs",
        "com.android.carrierconfig", "com.android.mms.service", "com.android.se",
        "com.android.ons", "com.android.emergency", "com.android.proxyhandler",
        "com.android.statementservice", "com.android.captiveportallogin",
        "com.android.companiondevicemanager", "com.android.credentialmanager",
        "com.android.managedprovisioning", "com.android.cellbroadcastreceiver",
        "com.android.networkstack", "com.android.networkstack.inprocess",
        "com.google.android.networkstack", "com.google.android.networkstack.tethering",
        "com.google.android.cellbroadcastservice", "com.google.android.captiveportallogin",
        "com.google.android.ext.services", "com.google.android.ext.shared",
        "com.android.vending", "com.google.android.gms", "com.google.android.gsf",
        "com.google.android.webview", "com.android.webview",
        "com.android.providers.media", "com.android.providers.media.module",
        "com.android.providers.contacts", "com.android.providers.calendar",
        "com.android.providers.telephony", "com.android.providers.settings",
        "com.android.providers.downloads", "com.android.providers.downloads.ui",
        "com.android.providers.userdictionary", "com.android.providers.blockednumber",
        "com.android.providers.applications",
        "com.samsung.android.incallui", "com.samsung.android.dialer",
        "com.samsung.android.messaging", "com.samsung.android.app.contacts",
        "com.samsung.android.app.telephonyui", "com.sec.android.app.launcher",
        "com.sec.imsservice", "com.sec.epdg", "com.sec.phone",
    )

    private val criticalPrefixes = listOf(
        "com.samsung.android.knox.",
        "com.sec.android.provider.",
        "com.samsung.android.providers.",
    )

    fun lookup(pkg: String): KnownPackage? = byPkg[pkg]

    fun isCritical(pkg: String): Boolean =
        pkg in criticalExact || criticalPrefixes.any { pkg.startsWith(it) }
}
