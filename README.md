# ShizuCleaner

Debloat systemu Android przez Shizuku (uprawnienia shell, uid 2000). Bez analityki. Jedyne użycie internetu to sprawdzenie i pobranie aktualizacji z GitHub Releases (można wyłączyć sprawdzanie przy starcie).

## Aktualizacje

Każdy push do `main` buduje podpisany APK (GitHub Actions) i publikuje go jako wydanie `v<numer builda>`. Aplikacja (przycisk „Aktualizacje”) porównuje numer ostatniego wydania ze swoim `versionCode`, pobiera `ShizuCleaner.apk`, sprawdza sumę SHA-256 i instaluje go nad obecną wersją. Dane i uprawnienia zostają.

Warunki:
- Repozytorium musi być **publiczne** (aplikacja nie ma tokena, żeby czytać prywatne wydania).
- Sekrety Actions: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Ten sam klucz musi podpisywać każdą wersję, inaczej Android odrzuci aktualizację. Klucz nie jest w repozytorium; zrób jego kopię.
- Pierwszą wersję zainstaluj z wydania na GitHubie, nie z Android Studio. Build lokalny jest podpisany kluczem debug i nie zaktualizuje się z GitHuba (trzeba wtedy odinstalować i zainstalować od nowa).
- Przy pierwszej aktualizacji Android poprosi o zgodę „instaluj aplikacje z tego źródła”.

## Stan: v0.1 — szkielet, niezbudowany ani nieprzetestowany na urządzeniu

Zrobione:
- Ekran **Debloat** (pierwszy po uruchomieniu): systemowe aplikacje z oceną ryzyka (Bezpieczne / Ostrożnie / Nieznane), filtry, szukajka, "Zaznacz bezpieczne".
- Dwa tryby: **Usuń dla użytkownika 0** (`pm uninstall -k --user 0`) i **Wyłącz** (`pm disable-user --user 0`).
- Ekran **Przywróć**: stan czytany prosto z systemu (aplikacje usunięte/wyłączone), przywracanie przez `pm install-existing` + `pm enable`. Nie ma własnego logu, który mógłby się rozjechać.
- Zabezpieczenia: lista pakietów krytycznych (nie pokazują się wcale), dynamiczna ochrona launchera, klawiatury, dialera i SMS, allowlista poleceń po stronie serwisu shell.

Do zrobienia:
- Skaner resztek po odinstalowanych aplikacjach + nadpisywanie przed kasowaniem.
- `pm clear` / `pm trim-caches`.
- Baza pakietów jako aktualizowalny plik JSON zamiast Kotlina.
- Tryb wielu użytkowników / profil służbowy (teraz zawsze `--user 0`).

## Build

Android Studio (Ladybug+) → Open → katalog repo. JDK 17, compileSdk 35. Wersje AGP/Kotlin/Compose w `build.gradle.kts` bumpnij do aktualnych. Z wiersza poleceń: `./gradlew assembleDebug`.

## Użycie

1. Zainstaluj Shizuku i uruchom (Wireless debugging lub `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`).
2. Otwórz ShizuCleaner, nadaj uprawnienie.
3. Zacznij od trybu **Wyłącz** dla wszystkiego, co oznaczone "Ostrożnie".

## Awaryjne przywracanie (adb)

Gdyby telefon nie wstał albo Shizuku przestało działać:

```
adb shell pm install-existing --user 0 <pakiet>
adb shell pm enable --user 0 <pakiet>
```

## Ograniczenia

- Przywracanie działa tylko dla aplikacji, których APK zostaje na partycji systemowej (czyli preinstalowanych). Zwykłej aplikacji użytkownika po `pm uninstall` nie przywrócisz.
- Reset fabryczny przywraca wszystkie usunięte aplikacje systemowe.
- Lista pakietów to punkt startowy — zweryfikuj na swoim modelu i wersji One UI, zanim zaufasz oznaczeniom.
- Nadpisywanie plików na flash nie gwarantuje fizycznego skasowania (wear leveling). Nie obiecuj "nieodzyskiwalności" w opisie.

## Dystrybucja

F-Droid / sideload. `QUERY_ALL_PACKAGES` nie przejdzie w Google Play dla tego typu aplikacji.
