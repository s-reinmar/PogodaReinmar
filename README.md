# PogodaReinmar

Hybrydowa aplikacja pogodowa dla systemu Android napisana w **Kotlin** i **Jetpack Compose** (Material Design 3). Dla lokalizacji w Polsce łączy oficjalne pomiary naziemne oraz ostrzeżenia meteorologiczne **IMGW-PIB** z globalnymi prognozami i wyszukiwarką miast **Open-Meteo**, a dla lokalizacji zagranicznych automatycznie korzysta z danych **Open-Meteo**.

---

## Najważniejsze funkcje

### 1. Hybrydowy silnik danych pogodowych (IMGW-PIB + Open-Meteo)
- **Polska:** równoległe pobieranie danych (`supervisorScope`) ze stacji synoptycznych IMGW-PIB (`api/data/synop`), ostrzeżeń meteorologicznych IMGW (`api/data/warningsmeteo`) oraz prognoz Open-Meteo (`v1/forecast`).
- **Katalog 62 stacji synoptycznych IMGW-PIB:** dopasowywanie najbliższej aktywnej stacji pomiarowej za pomocą odległości ortodromicznej (wzór Haversine) wraz z mapowaniem kodów województw **TERYT**.
- **Świat:** automatyczne kierowanie zapytań dla miast spoza Polski do Open-Meteo z uwzględnieniem lokalnej strefy czasowej i czasu lokalnego miejscowości.
- **Odporność na awarie (graceful fallback):** w przypadku chwilowej niedostępności jednego z serwisów aplikacja prezentuje dane z drugiego źródła i wyświetla odpowiedni status w interfejsie.

### 2. Panel pogodowy (Bento Grid) i prognozy
- **Sekcja Hero:** aktualna temperatura (priorytet pomiaru IMGW w Polsce), temperatura odczuwalna, dzienny zakres maks./min., opis zjawiska, czas lokalny oraz informacja o najbliższej stacji IMGW (nazwa, dystans w km, godzina pomiaru).
- **Prognoza godzinowa (24h):** przewijana oś czasu z temperaturą, ikoną pogody oraz prawdopodobieństwem opadów.
- **Prognoza 7-dniowa:** dzienne minima/maksima z wizualnym paskiem zakresu temperatur i prawdopodobieństwem opadów.
- **Adaptacyjna siatka Bento Grid (6 kafelków):** układ 2-kolumnowy na smartfonach i 3-kolumnowy na ekranach `>= 600 dp` (tablety/składane urządzenia):
  - **Wiatr** — prędkość w `km/h` i `m/s` oraz kierunkowy kompas,
  - **Ciśnienie** — wartość w `hPa` ze wskaźnikiem zakresu,
  - **Wilgotność** — procentowy poziom i ocena komfortu,
  - **Opad dobowy** — suma w `mm` wraz ze wskazaniem źródła pomiaru,
  - **Indeks UV** — wartość maksymalna i kolorowa skala ryzyka,
  - **Wschód i zachód słońca** — łuk solarny rysowany w Compose `Canvas` z bieżącą pozycją słońca.

### 3. Ostrzeżenia meteorologiczne IMGW-PIB i powiadomienia
- **Banery ostrzeżeń na ekranie:** prezentacja aktywnych alertów IMGW (stopnie 1–3, okres obowiązywania, prawdopodobieństwo, biuro prognoz, rozwijany opis) dopasowanych po kodzie województwa TERYT.
- **Monitorowanie ulubionych miast (PL):** zbiorczy baner i wskaźniki aktywnych ostrzeżeń dla przypiętych polskich miejscowości.
- **Powiadomienia systemowe Android:** dedykowany kanał powiadomień (`imgw_weather_alerts`) z obsługą uprawnienia runtime `POST_NOTIFICATIONS` (Android 13+), filtrem minimalnego stopnia zagrożenia (`1+`, `2+`, `Tylko 3`), ręcznym sprawdzaniem feedu IMGW oraz deduplikacją wysłanych powiadomień.

### 4. Ulubione miasta, GPS i wyszukiwarka globalna
- **Zintegrowany panel Hub (`Ulubione` / `Alerty IMGW`):** zarządzanie zapisanymi miastami i konfiguracją ostrzeżeń w jednym miejscu oraz pasek szybkiego przełączania ulubionych lokalizacji.
- **Wyszukiwarka miejscowości na świecie:** Open-Meteo Geocoding API z mechanizmem debounce (`300 ms`) i wynikami w aktualnie wybranym języku.
- **Lokalizacja GPS:** Google Play Services `FusedLocationProviderClient` z fallbackiem do systemowego `LocationManager`, odwrotnym geokodowaniem (`Geocoder`) i domyślnym fallbackiem na Warszawę przy braku uprawnień lub wyłączonym GPS.

### 5. Wielojęzyczność i warstwa wizualna
- **Języki aplikacji:** Polski (`PL`), Angielski (`EN`), Niemiecki (`DE`) — zmiana w locie bez restartu aplikacji, obejmująca interfejs, opisy pogody, daty i nazwy lokalizacji.
- **Dynamiczne tło atmosferyczne:** płynnie animowany gradient nieba reagujący na porę dnia/nocy oraz warunki pogodowe (z polem gwiazd w pogodne noce), karty w stylu glassmorphism oraz ekran ładowania typu shimmer.
- **Typografia i ikony:** lokalne fonty `Outfit`, `Plus Jakarta Sans` i `JetBrains Mono` oraz dedykowany zestaw grafik `.webp` dla zjawisk pogodowych, kierunków wiatru i metryk.

---

## Stos technologiczny

| Obszar | Technologia / Biblioteka |
|---|---|
| Język i platforma | Kotlin `2.4.21`, Android (`minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`), Java 11 |
| Interfejs użytkownika | Jetpack Compose (BOM `2024.09.00`), Material Design 3, Material Icons Extended, Edge-to-Edge |
| Architektura i stan | MVVM + Clean Architecture, `ViewModel`, `StateFlow`, `collectAsStateWithLifecycle` |
| Asynchroniczność | Kotlin Coroutines & Flow (`1.10.2`) |
| Sieć i serializacja | Retrofit `2.12.0`, OkHttp `4.10.0`, `kotlinx.serialization` (`1.8.1`) |
| Trwały zapis lokalny | Jetpack DataStore Preferences (`1.1.7`) |
| Lokalizacja | Google Play Services Location (`21.3.0`), Android `Geocoder` |
| Testy | JUnit 4, `kotlinx-coroutines-test`, Robolectric `4.16.1`, Compose UI Test |

---

## Architektura i struktura projektu

Aplikacja korzysta z warstwowej architektury **Clean Architecture / MVVM** z jednokierunkowym przepływem stanu (`WeatherUiState`) oraz prostym wstrzykiwaniem zależności przez konstruktory (`WeatherViewModel.Factory` w `MainActivity`).

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/
│   ├── MainActivity.kt                          # Punkt wejścia, edge-to-edge, inicjalizacja zależności
│   ├── data/
│   │   ├── location/AndroidLocationProvider.kt  # GPS (FusedLocation + LocationManager) i Geocoder
│   │   ├── notifications/ImgwAlertNotificationHelper.kt # Powiadomienia systemowe alertów IMGW
│   │   ├── preferences/
│   │   │   ├── AlertPreferences.kt              # DataStore: ustawienia i historia powiadomień IMGW
│   │   │   ├── FavoriteCitiesPreferences.kt     # DataStore: zapisane ulubione miasta (JSON)
│   │   │   └── LanguagePreferences.kt           # DataStore: wybrany język (PL/EN/DE)
│   │   ├── remote/
│   │   │   ├── NetworkClient.kt                 # Konfiguracja Retrofit, OkHttp i Json
│   │   │   ├── api/WeatherApiServices.kt        # Kontrakty API: IMGW-PIB i Open-Meteo
│   │   │   └── dto/                             # Modele DTO (ImgwDtos.kt, OpenMeteoDtos.kt)
│   │   └── repository/HybridWeatherRepositoryImpl.kt # Hybrydowe łączenie IMGW + Open-Meteo
│   ├── domain/
│   │   ├── catalog/ImgwStationCatalog.kt        # Katalog 62 stacji IMGW, Haversine, mapowanie TERYT
│   │   ├── model/                               # Modele domenowe (WeatherModels.kt, AppLanguage.kt)
│   │   ├── repository/WeatherRepository.kt      # Interfejsy repozytorium i lokalizacji
│   │   └── util/                                # PolishDateTimeUtils.kt, WeatherCodeMapper.kt
│   ├── presentation/
│   │   ├── WeatherUiState.kt                    # Niezmienny stan UI (sealed WeatherContentState)
│   │   └── WeatherViewModel.kt                  # Logika prezentacji, debounce wyszukiwania, alerty
│   └── ui/
│       ├── theme/                               # Color.kt (palety nieba), Theme.kt, Type.kt
│       └── weather/                             # Komponenty Compose: ekran główny, Hero, Bento Grid,
│                                                # prognozy 24h/7d, Hub, banery alertów, shimmer, lokalizacja
└── res/
    ├── drawable-nodpi/                          # Grafiki pogodowe, metryk i kierunków wiatru (.webp)
    ├── font/                                    # outfit.ttf, plus_jakarta_sans.ttf, jetbrains_mono.ttf
    ├── mipmap-*/                                # Adaptacyjna ikona aplikacji
    └── values/ | values-en/ | values-de/        # Zasoby językowe (PL / EN / DE)
```

---

## Zewnętrzne źródła danych (API)

Aplikacja korzysta wyłącznie z otwartych, publicznych interfejsów API — **nie wymaga konfiguracji żadnych kluczy API** do uruchomienia:

- **IMGW-PIB (dane synoptyczne):** `https://danepubliczne.imgw.pl/api/data/synop`
- **IMGW-PIB (ostrzeżenia meteorologiczne):** `https://danepubliczne.imgw.pl/api/data/warningsmeteo`
- **Open-Meteo (prognoza i warunki bieżące):** `https://api.open-meteo.com/v1/forecast`
- **Open-Meteo (geokodowanie / wyszukiwanie miast):** `https://geocoding-api.open-meteo.com/v1/search`

---

## Uprawnienia Android

Deklarowane w `AndroidManifest.xml`:
- `INTERNET`, `ACCESS_NETWORK_STATE` — pobieranie danych pogodowych i ostrzeżeń.
- `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` — opcjonalne ustalanie bieżącej lokalizacji GPS (uprawnienie runtime z bezpiecznym fallbackiem do Warszawy).
- `POST_NOTIFICATIONS` — opcjonalne powiadomienia systemowe o aktywnych ostrzeżeniach IMGW-PIB na Android 13+ (API 33+).

---

## Budowanie i uruchamianie

### Wymagania
- Android Studio (wersja obsługująca AGP `9.1.1` / Android SDK 36) lub środowisko z zainstalowanym Android SDK i JDK 11+.
- Urządzenie lub emulator z Androidem **7.0 (API 24)** lub nowszym.

### Komendy Gradle

Budowanie wersji debugowej APK:
```bash
./gradlew :app:assembleDebug
```

Uruchomienie lokalnych testów jednostkowych i Robolectric (JVM, bez potrzeby emulatora):
```bash
./gradlew :app:testDebugUnitTest
```

*(W środowiskach bez wrappera użyj bezpośrednio polecenia `gradle :app:assembleDebug` oraz `gradle :app:testDebugUnitTest`.)*

---

## Testy

W katalogu `app/src/test/java/com/example/` znajdują się testy jednostkowe i testy Robolectric obejmujące kluczową logikę aplikacji:
- `ImgwStationCatalogTest.kt` — wyszukiwanie najbliższej stacji (Haversine), normalizacja nazw i mapowanie kodów TERYT.
- `GlobalWeatherRoutingTest.kt` — rozróżnianie lokalizacji w Polsce (IMGW + Open-Meteo) i za granicą (wyłącznie Open-Meteo).
- `ImgwAlertsTest.kt` — parsowanie i filtrowanie ostrzeżeń IMGW, progi zagrożenia oraz logika powiadomień.
- `FavoriteCitiesPreferencesTest.kt` — trwały zapis, dodawanie i usuwanie ulubionych miast w DataStore.
- `ExampleRobolectricTest.kt` / `ExampleUnitTest.kt` — testy środowiskowe i bazowe.

---

## Licencja

Projekt jest udostępniany na licencji **MIT** — szczegóły w pliku [LICENSE](LICENSE).

