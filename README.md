# ShizuCleaner

Debloat systemu Android przez Shizuku (uprawnienia shell, uid 2000). Bez analityki. Jedyne użycie internetu to sprawdzenie i pobranie aktualizacji z GitHub Releases (można wyłączyć sprawdzanie przy starcie).

## Aktualizacje

Każdy push do `main` buduje podpisany APK (GitHub Actions) i publikuje go jako wydanie `v<numer builda>`. Aplikacja (przycisk „Aktualizacje”) porównuje numer ostatniego wydania ze swoim `versionCode`, pobiera `ShizuCleaner.apk`, sprawdza sumę SHA-256 i instaluje go nad obecną wersją. Dane i uprawnienia zostają.

Warunki:
- Repozytorium musi być **publiczne** (aplikacja nie ma tokena, żeby czytać prywatne wydania).
- Sekrety Actions: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Ten sam klucz musi podpisywać każdą wersję, inaczej Android odrzuci aktualizację. Klucz nie jest w repozytorium; zrób jego kopię.
- Pierwszą wersję zainstaluj z wydania na GitHubie, nie z Android Studio. Build lokalny jest podpisany kluczem debug i nie zaktualizuje się z GitHuba (trzeba wtedy odinstalować i zainstalować od nowa).
- Przy pierwszej aktualizacji Android poprosi o zgodę „instaluj aplikacje z tego źródła”.

## Zakładki

- **Debloat** (pierwsza): aplikacje systemowe z oceną ryzyka (Bezpieczne / Ostrożnie / Nieznane), filtry, szukajka. Tryby: *Usuń dla użytkownika 0* (`pm uninstall -k --user 0`) i *Wyłącz*. Podzakładka *Przywróć* czyta stan prosto z systemu.
- **Aplikacje**: Twoje aplikacje z rozmiarami (aplikacja / dane / cache). *Odinstaluj i wyczyść resztki* usuwa aplikację razem z danymi, a potem kasuje jej foldery w `Android/data`, `obb`, `media` i znane foldery w pamięci (z nadpisaniem). Do tego *Wyczyść dane* (`pm clear`) i *Wyczyść cache wszystkich* (`pm trim-caches`).
- **Resztki**: skan folderów po aplikacjach, których już nie ma na telefonie. Aplikacje usunięte debloatem tylko dla użytkownika są pomijane, bo ich dane zostają celowo.
- **Pliki**: duże pliki, duplikaty (po zawartości, SHA-256), stare APK, puste foldery.
- **Ustawienia**: liczba przebiegów nadpisywania (0 / 1 / 3), aktualizacja bazy pakietów, aktualizacje aplikacji.

## Bezpieczeństwo

- Serwis Shizuku wykonuje wyłącznie polecenia z allowlisty (`CommandPolicy`), a operacje na plikach tylko w `/storage/emulated/0` (`FsOps`): odrzuca root pamięci, wyjścia poza nią (`..`, dowiązania symboliczne), główne foldery systemowe (`Android`, `DCIM`, `Download`…) i dane własnej aplikacji.
- Pakiety krytyczne (dzwonienie, SMS, sieć, Play Services, Knox) są zablokowane w kodzie (`CriticalPackages`) i podmieniona baza JSON ich nie odblokuje. Dodatkowo chronione są launcher, klawiatura, dialer, domyślne SMS, Shizuku i ta aplikacja.
- Kasowanie jest nieodwracalne i wymaga potwierdzenia z opisem trybu.
- Testy jednostkowe (ochrona ścieżek, kasowanie, allowlista, parsowanie) uruchamiają się w CI przy każdym buildzie.

## Baza pakietów

`app/src/main/assets/packages.json`: oceny ryzyka (`SAFE` / `CAUTION`) i `leftoverFolders`. Aplikacja pobiera aktualną wersję z repozytorium (Ustawienia → Aktualizuj bazę), więc poprawki nie wymagają nowego wydania. Lista startowa jest niepełna — zweryfikuj na swoim modelu i wersji One UI.

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

- Skany duplikatów i dużych plików czytają całą pamięć; na dużym telefonie to minuty.
- Rozmiary aplikacji pochodzą z `dumpsys diskstats`; jeśli producent zmienił format, lista pokaże się bez rozmiarów.
- Przywracanie działa tylko dla aplikacji, których APK zostaje na partycji systemowej (czyli preinstalowanych). Zwykłej aplikacji użytkownika po `pm uninstall` nie przywrócisz.
- Reset fabryczny przywraca wszystkie usunięte aplikacje systemowe.
- Lista pakietów to punkt startowy — zweryfikuj na swoim modelu i wersji One UI, zanim zaufasz oznaczeniom.
- Nadpisywanie plików na flash nie gwarantuje fizycznego skasowania (wear leveling). Nie obiecuj "nieodzyskiwalności" w opisie.

## Dystrybucja

F-Droid / sideload. `QUERY_ALL_PACKAGES` nie przejdzie w Google Play dla tego typu aplikacji.
