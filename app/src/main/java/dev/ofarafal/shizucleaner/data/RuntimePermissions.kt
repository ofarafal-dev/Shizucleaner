package dev.ofarafal.shizucleaner.data

data class PermCategory(val id: String, val label: String, val perms: List<String>)

/**
 * Uprawnienia runtime, które aplikacja może cofać. Lista jest zamknięta celowo:
 * CommandPolicy przepuści `pm revoke` tylko dla tych nazw. Czysty Kotlin (używa go też serwis shell).
 */
object RuntimePermissions {

    val categories: List<PermCategory> = listOf(
        PermCategory("camera", "Kamera", listOf("android.permission.CAMERA")),
        PermCategory("mic", "Mikrofon", listOf("android.permission.RECORD_AUDIO")),
        PermCategory(
            "location", "Lokalizacja",
            listOf(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_BACKGROUND_LOCATION",
            ),
        ),
        PermCategory(
            "contacts", "Kontakty",
            listOf("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS", "android.permission.GET_ACCOUNTS"),
        ),
        PermCategory(
            "sms", "SMS",
            listOf(
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS",
                "android.permission.RECEIVE_SMS",
                "android.permission.RECEIVE_MMS",
                "android.permission.RECEIVE_WAP_PUSH",
            ),
        ),
        PermCategory(
            "phone", "Telefon",
            listOf(
                "android.permission.READ_PHONE_STATE",
                "android.permission.READ_PHONE_NUMBERS",
                "android.permission.READ_CALL_LOG",
                "android.permission.WRITE_CALL_LOG",
                "android.permission.CALL_PHONE",
                "android.permission.ANSWER_PHONE_CALLS",
            ),
        ),
        PermCategory(
            "storage", "Pliki i media",
            listOf(
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_MEDIA_AUDIO",
            ),
        ),
        PermCategory(
            "other", "Kalendarz i czujniki",
            listOf(
                "android.permission.READ_CALENDAR",
                "android.permission.WRITE_CALENDAR",
                "android.permission.BODY_SENSORS",
                "android.permission.ACTIVITY_RECOGNITION",
            ),
        ),
    )

    val all: Set<String> = categories.flatMap { it.perms }.toSet()
}
