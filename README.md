# Enklawa

Osobny „świat” w aplikacji na Androida: własny pulpit i przeglądarka, a po wyjściu wracasz do zwykłego telefonu. Tryb amnezji: wszystko, co świat zapisuje, jest szyfrowane losowym kluczem trzymanym tylko w RAM; przy wyjściu klucz znika, pliki są kasowane, a proces kończony.

## Stan (prototyp 0.1)

- Powłoka pełnoekranowa, `FLAG_SECURE`, wyłączony podgląd w ostatnich aplikacjach.
- Tryb amnezji: `SessionKey` (AES-256 w RAM) + `SessionVault` (AES-GCM, losowe nazwy plików) + `WorldSession` (kasowanie, sprzątanie po przerwanym procesie).
- Przeglądarka: WebView z `androidx.webkit.Profile` (osobny profil usuwany przy wyjściu), `IME_FLAG_NO_PERSONALIZED_LEARNING`, tylko http(s).
- Aktualizacje z GitHub Releases (SHA-256 + PackageInstaller).

Kolejne wersje: tryb z pamięcią (Argon2 + Keystore, wiele magazynów), Tor, notatki, pliki, komunikator webowy, PWA.

## Czego ta ochrona nie daje

Android hosta jest niezaufany. Logi systemowe, schowek i klawiatura mogą coś zapamiętać. To prywatność na co dzień, nie ochrona przed zaawansowanym przeciwnikiem ani malware na telefonie. Dane przeglądarki trzyma systemowy WebView poza kluczem sesji (po wyjściu usuwany jest cały profil, ale to nie jest crypto-erase). Nadpisywanie na flashu nie daje gwarancji, dlatego aplikacja opiera się na zniszczeniu klucza.

## Build i wydania

Każdy push do `main` buduje podpisany APK w GitHub Actions i publikuje wydanie `v<numer builda>` (= `versionCode`). Build failuje przy 0 testów.

Sekrety Actions (Settings → Secrets and variables → Actions): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Ten sam klucz musi podpisywać każdą wersję. Pierwszą instalację zrób z wydania na GitHubie.
