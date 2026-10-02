# SummitRead — Kotlin Multiplatform planı (Aşama A tamam, B ve C planı)

Son güncelleme: 2026-10-02. Dal: `kmp` (main'e dokunulmadı). Doğrulanmayanlar açıkça "Mac'te doğrulanmadı" diye işaretli.

## 1. Seçilen sürümler ve nedenleri

Kaynak: Maven Central / Google Maven `maven-metadata.xml`, Compose Multiplatform CHANGELOG, AGP sürüm notları (2026-10-02'de okundu).

| Bileşen | Önce | Şimdi | Not |
|---|---|---|---|
| Gradle | 9.1.0 | **9.8.0** | AGP 9.4 en az 9.6.0 ister; 9.8.0 güncel kararlı sürüm |
| AGP | 9.0.1 | **9.4.1** | KMP kitaplık eklentisi `com.android.kotlin.multiplatform.library`, JDK 17, Build Tools 36.0.0 |
| Kotlin | 2.1.20 | **2.4.20** | Compose MP 1.11 / Room 2.8 / KSP 2.3 ile derlendi ve çalıştı |
| KSP | 2.1.20-1.0.31 | **2.3.12** | KSP artık Kotlin'e bağlı numaralanmıyor; AGP 9 yerleşik Kotlin ve Kotlin 2.4 desteği içeriyor |
| Room | 2.7.1 | **2.8.5** | KMP (bundled SQLite sürücüsü); üretilen identity hash eskisiyle aynı (`7d5c28ee81283845c5dcb7202b3678ae`), `5.json` değişmedi |
| androidx.sqlite (bundled) | – | 2.6.2 | Room 2.8.5'in bağımlı olduğu sürüm |
| DataStore | 1.1.4 | **1.2.1** (`datastore-preferences-core`, KMP) | Aynı dosya biçimi (`.preferences_pb`) |
| kotlinx-datetime | – | **0.8.0** | `java.time` yerine; `Instant`/`Clock` artık `kotlin.time` |
| kotlinx-coroutines | 1.10.2 | 1.10.2 | değişmedi |
| Compose Multiplatform | – | **1.11.1** | bkz. aşağıdaki "neden 1.12.1 değil" |
| Compose BOM (Android UI) | 2026.03.01 | **2026.06.01** (Compose 1.11.4) | CMP 1.11.1 (Jetpack 1.11.2 tabanlı) ile aynı hat |
| Lifecycle | 2.10.0 | 2.10.0 | `lifecycle-viewmodel` 2.10 zaten KMP (iOS dahil) |

**Neden Compose Multiplatform 1.12.1 (en yeni kararlı) değil:** 1.12.x ve ona denk gelen androidx (Compose 1.12.1, Lifecycle 2.11) `compileSdk 37` ister. Bu makinede yalnız `android-36` ve `android-36.1` platformları kurulu; SDK platformu indirmek ve lisans onayı kullanıcı kararı olduğu için yapılmadı. 1.11.1 / BOM 2026.06.01 çifti `compileSdk 36` ile derleniyor. **Yükseltme yolu:** `android-37` platformu kurulduktan sonra `gradle/libs.versions.toml` içinde `composeMultiplatform = "1.12.1"`, `androidxComposeBom = "2026.09.00"`, `androidxLifecycle = "2.11.0"` ve iki modülde `compileSdk = 37`. GitHub Actions macOS runner'ında SDK platformu kurulabilir; iOS tarafı 1.12.x'in iOS düzeltmelerinden (Xcode 16 SDK uyumu, iOS 26 `LayoutRegion`) yararlanır, bu yüzden Aşama C'den önce 1.12.x'e geçmek önerilir.

Diğer yapılandırma notları:
- `gradle.properties`: `kotlin.native.ignoreDisabledTargets=true` (Windows'ta Apple hedefleri devre dışı uyarısı); `org.gradle.java.home` içindeki `:` kaçışlandı (`C\:/...`), çünkü AGP 9.4 lint'i `PropertyEscape` hatası veriyordu.
- `app` modülünde KSP, Room ve DataStore bağımlılıkları kalktı; Room/DataStore artık yalnız `shared` içinde.
- `compileSdk 36`, `minSdk 24`, `targetSdk 36`, `applicationId com.example.summitread`, **versionCode 2**, versionName 1.0.

## 2. Modül yapısı

```
settings.gradle.kts            :app, :shared
shared/                        Kotlin Multiplatform kitaplığı (android + iosArm64 + iosSimulatorArm64)
  build.gradle.kts             KMP + com.android.kotlin.multiplatform.library, Room eklentisi, KSP (kspAndroid/kspIosArm64/kspIosSimulatorArm64)
  schemas/                     Room şema dışa aktarımı (1..5.json) — app/schemas buradan taşındı
  src/commonMain               ortak kod (aşağıda)
  src/androidMain              Android actual'ları (Context tabanlı)
  src/iosMain                  iOS actual'ları (Mac'te doğrulanmadı)
  src/commonTest               kotlin.test testleri (JVM'de `:shared:testAndroidHostTest` ile koşar)
app/                           ince Android uygulaması: UI (Compose), tema, navigasyon, servis, WorkManager
```

iOS framework: `SummitReadShared` (statik). Swift tarafı için giriş noktası `IosPlatform.shared` (`shared/src/iosMain/.../IosAppContainer.kt`).

## 3. Neler `shared/commonMain`'e taşındı

Paket adları aynı kaldı (`com.example.summitread.…`), `app` içindeki importlar değişmedi.

- **Saf mantık:** `StreakCalculator`, `ReminderPlanner`, `PageMath`, `ElapsedTimeFormat`, `RelicEmojis`, `DayClock`, `ProfileLogic` (+`AvatarTier`), `HomeSort`, `HomeBody`, `Hsv`, `MountainLogic` (+`SkyModel`, `MilestoneStats`, …). `java.time` → kotlinx-datetime (`LocalDate`, `TimeZone`, `kotlin.time.Instant/Clock`); anlamlar aynı. `StreakCalculator.resetMoment` artık `ZonedDateTime` yerine `Instant` döndürür; `ReminderPlanner.plan(loggedDays, now: Instant, zone: TimeZone)`.
- **Veri:** `AppDatabase` (`@ConstructedBy` + `expect object AppDatabaseConstructor`), `BookDao`, 5 entity, `MIGRATION_4_5` (`SQLiteConnection` API'siyle, SQL aynı), `buildAppDatabase()` (BundledSQLiteDriver, `Dispatchers.IO`, `fallbackToDestructiveMigration(dropAllTables=false)`). Dosya adı `summitread_database`, sürüm 5.
- **DataStore:** `UserPreferences(DataStore<Preferences>)`; `createUserPreferencesDataStore { yol }`. Android yolu: `filesDir/datastore/user_preferences.preferences_pb` (öncekiyle aynı dosya).
- **Repository + ViewModel'ler:** `BookRepository(dao, ImageStorage)`, `HomeViewModel`, `MountainViewModel` (fabrikası `viewModelFactory { initializer }`'a çevrildi), `ProfileViewModel`.
- **Platform arayüzleri** (`platform` paketi): `ImageStorage`, `ReadingTimer` (+`TimerState/TimerStatus`), `ReminderScheduler` (+ ortak `StreakReminders.observe`, `StreakReminderTexts`), `TiltSensor` (+ ortak `TiltFilter`: sönümleme/kenetleme/NaN koruması), `Haptics`, `AppContainer`.

Bağımlılık notu: `Hsv` ve `MountainLogic` Compose `Color` kullandığı için `shared` yalnız `org.jetbrains.compose.runtime` ve `ui-graphics` ile Compose MP'ye bağlı (Compose derleyici eklentisi de uygulandı, Aşama B için hazır).

## 4. `app` içinde kalanlar

Tüm UI (`ui/*`, `theme/*`, `Navigation.kt`, `MainActivity`), `ReadingTimerService` (+ `AndroidReadingTimer`), `StreakNotificationWorker`, `WorkManagerReminderScheduler`, `NotificationChannels`. `MainActivity`, servis ve worker artık tek örnek veri katmanını `AndroidAppContainer.get(context)` ile alır (DataStore aynı dosyada iki örnek olamayacağı için tek örnek şart).

## 5. Platform soyutlamaları: durum tablosu

| Arayüz | Android | iOS |
|---|---|---|
| `ImageStorage` | `AndroidImageStorage` (shared/androidMain) — eski davranış aynı, doğrulandı (derleme) | `IosImageStorage` (Documents/images, `NSFileManager`) — Mac'te doğrulanmadı |
| `ReadingTimer` | `AndroidReadingTimer` → mevcut `ReadingTimerService` (FGS, bildirim, Pause/Resume/Stop) — emülatörde doğrulandı | `IosReadingTimer`: zaman damgası tabanlı uygulama içi sayaç, durdurunca bekleyen oturumu DataStore'a yazar, 2 saat sonra "sayaç hâlâ çalışıyor" yerel bildirimi — Mac'te doğrulanmadı. **Eksik:** uygulama OS tarafından öldürülürse sayaç kaybolur (Aşama C: durumu DataStore'a yaz) |
| `ReminderScheduler` | `WorkManagerReminderScheduler` (6 sa / 1 sa, aynı iş adları, worker çalışırken yeniden doğrular) | `IosReminderScheduler`: `UNUserNotificationCenter` zaman aralığı tetikleyicileri; metin planlama anında sabitlenir, her log sonrası yeniden planlanır — Mac'te doğrulanmadı. `requestPermission()` iOS'ta izin ister |
| `TiltSensor` | `AndroidTiltSensor` (rotation vector, ivmeölçer yedeği); `rememberTiltState` bunu kullanır | `IosTiltSensor` (CoreMotion `deviceMotion`); pitch işareti Android'e çevrilir (`-pitch`), roll aynı — işaretler cihazda doğrulanmadı |
| `Haptics` | `AppHaptics` (Compose `HapticFeedback`) arayüzü uygular | `IosHaptics` (UIKit feedback generator) — Mac'te doğrulanmadı |
| Saat / gün | `DayClock`, `kotlin.time.Clock` (ortak) | aynı (ortak) |
| Veri tabanı yolu | `getDatabasePath("summitread_database")` | `Documents/summitread_database` (`NSFileManager`) — Mac'te doğrulanmadı |
| Tercih yolu | `filesDir/datastore/user_preferences.preferences_pb` | `Documents/user_preferences.preferences_pb` — Mac'te doğrulanmadı |

## 6. Doğrulama sonuçları (Windows, Aşama A)

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :shared:testAndroidHostTest :app:lintDebug` → BUILD SUCCESSFUL, lint 0 hata / 27 uyarı (hepsi eski türden: GradleDependency, NewerVersionAvailable, ModifierParameter…).
- Test sayısı: `shared` 62 (commonTest) + `app` 5 (ThemeContrast 4, Hsv paleti 1) = **67** (önceki 64 + 3 yeni `PlatformLogicTest`).
- Room: `AppDatabase_Impl` içindeki sürüm 5 ve identity hash `7d5c28ee81283845c5dcb7202b3678ae`, eski `5.json` ile aynı; `shared/schemas/.../5.json` değişmedi.
- **Emülatör yükseltme testi (Pixel_9a, kullanıcının gerçek test verisi):** yedek alındı (`pre-kmp-backup.tar`), `adb install -r` (versionCode 1 → 2), çökme yok, 3 kitap / 14 oturum / 1 not / 2 relic / seri 2 / kullanıcı adı "efe" aynen duruyor, DB `user_version` 5 ve hash aynı, tercih dosyası bayt bayt aynı. Yazma yolları da denendi (sayfa kaydı 164→169 sayfa, 15. oturum; zamanlayıcı FGS başlat/durdur, bekleyen süre yazıldı). Sonra yedek geri yüklendi ve satır sayıları + tercih dosyası özeti ilk haline eşit doğrulandı. Uygulama silinmedi/verisi temizlenmedi.
- **iOS derleme durumu:** Windows'ta bile Kotlin/Native, Apple hedeflerinin klib derlemesini yapabiliyor: `:shared:compileKotlinIosSimulatorArm64`, `:shared:compileKotlinIosArm64` ve `:shared:compileTestKotlinIosSimulatorArm64` BAŞARILI (tüm `iosMain` kodu, UserNotifications/CoreMotion/UIKit bağlamaları dahil derleniyor). Derlenemeyen/çalıştırılamayanlar: framework **linklemesi** (Xcode araçları gerekir), iOS testlerinin çalıştırılması (`iosSimulatorArm64Test` Windows'ta atlanır) ve her türlü çalışma zamanı davranışı.

## 7. Aşama B planı — UI'ı `commonMain`'e taşı (Compose Multiplatform)

1. Önce `android-37` kurulup CMP 1.12.1 + BOM 2026.09.00 + Lifecycle 2.11'e geçiş (bkz. §1), Android build/test yeşil kalmalı.
2. `shared` içine `org.jetbrains.compose.foundation`, `material3` (CMP sürümü), `components.resources`, `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`/`runtime-compose`, `org.jetbrains.androidx.navigation:navigation-compose` (type-safe rotalar için 2.9.x), `haze` (KMP), `coil3` (KMP; yerel dosya yolları için ağ modülü gerekip gerekmediği Aşama B başında denenmeli) ekle.
3. Tema/renk/şekil/hareket (`theme/*`), `ui/components/*`, ekranlar sırayla taşınır; her adımda Android'de çalışır kalmalı. `android.*` bağımlılıklarını ayıkla: `LocalContext` (çekirdek: `ImageStorage`, `ReadingTimer`, `Haptics`, `TiltSensor` arayüzleri `CompositionLocal` ile verilir), `Calendar` (CalendarStrip, ProfileScreen yıl) → kotlinx-datetime, `java.time.LocalTime` (MountainCanvas), `Locale/String.format`, `android.graphics`, `PickVisualMedia` (iOS'ta `PHPicker`/`UIImagePickerController` için `expect/actual` görsel seçici).
4. **Kaynaklar:** `composeResources/font/` altına Inter (Regular/Medium/SemiBold/Bold), `values/strings.xml` (şimdiki `res/values/strings.xml` içeriği), çizimler (`ic_stat_summit` yalnız Android'de kalır; uygulama ikonu `app` içinde). `Res.font`/`Res.string` ile erişim; `R.string.*` kullanımları taşınır.
5. Navigasyon: `NavHost` + `MountainRoute(bookId)` (kotlinx-serialization eklentisi `shared`'a eklenir); `SharedTransitionLayout` CMP'de var; iOS'ta geri hareketi için navigation-compose'un iOS desteği kullanılır.
6. `MainActivity` yalnız `setContent { SummitReadApp(...) }` + Android'e özel kısımlar (izin, WorkManager gözlemi, kenar çubuğu stilleri) olarak kalır; ortak giriş `App()`.
7. Test: `ThemeContrastTest` commonTest'e; UI testleri Android'de kalır.

## 8. Aşama C planı — iOS uygulaması ve CI

1. `iosApp/` Xcode projesi (SwiftUI barındırıcı): `iOSApp.swift` içinde `UIViewControllerRepresentable` ile `ComposeUIViewController { App() }` (Aşama B'de `shared/iosMain` içinde `fun MainViewController()`); `SummitReadShared` framework'ünü çalıştırma betiğiyle bağla (`embedAndSignAppleFrameworkForXcode`). `Info.plist`: bildirim açıklaması gerekmez (izin çalışma anında), `UIBackgroundModes` gerekmez.
2. iOS 27 SDK: Xcode 27 betası/kararlısı ile derle; `IPHONEOS_DEPLOYMENT_TARGET` en az 16 (CMP 1.11/1.12 desteği), tavsiye 17. Compose 1.12.x'in iOS 26/27 düzeltmeleri için CMP güncellemesi (bkz. §1) Aşama C başlamadan yapılmalı.
3. `iosMain` kodu klib olarak derleniyor (bkz. §6) ama hiç çalıştırılmadı: Mac/CI'da `linkDebugFrameworkIosSimulatorArm64` ve simülatörde çalışma zamanı denemesi gerekir (Room/bundled SQLite iOS'ta dosya açma, bildirim izni, CoreMotion). iOS yerel bildirimleri için `UNUserNotificationCenterDelegate` (ön planda gösterim) Swift tarafında eklenmeli.
4. GitHub Actions (`.github/workflows/ios.yml`, `macos-latest` veya `macos-26`): JDK 17, Gradle önbelleği, `./gradlew :shared:linkReleaseFrameworkIosArm64 :shared:iosSimulatorArm64Test` (+ `:shared:allTests`), ardından `xcodebuild archive -scheme SummitRead -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO`, `Payload/SummitRead.app` zip'lenip **imzasız .ipa** artefaktı olarak yüklenir. Repo'yu GitHub'a yüklemek Efe'yle konuşulacak (STATUS.md).
5. Sideloadly / AltStore: .ipa'yı ücretsiz Apple ID ile imzalar (7 günlük sertifika, en çok 3 uygulama, haftalık yenileme). TestFlight yok. Bildirim ve CoreMotion yetkileri imzalamada sorun çıkarmaz; "Push" yetkisi kullanılmıyor (yalnız yerel bildirim).
6. iOS'a özel tamamlamalar: sayaç durumunu kalıcılaştır (§5), arka plana geçerken hatırlatıcıları yeniden planla (`scenePhase`), `Info.plist` `NSPhotoLibraryUsageDescription` (PHPicker için gerekmez, `UIImagePickerController` için gerekir), uygulama ikonu/başlatma ekranı.

## 9. Riskler

- **iOS kodu yalnız derlendi, çalıştırılmadı.** Aşama C'nin ilk işi Mac/CI'da framework linklemek ve simülatörde denemek; çalışma zamanı hataları (özellikle Room iOS yolu, bildirim, CoreMotion işaretleri) olası.
- **Compose 1.11 → 1.12 geçişi** compileSdk 37 gerektirir; geçişte Android UI'ında küçük görsel farklar olabilir (emülatörde karşılaştırmalı kontrol yapılmalı).
- **Bundled SQLite:** Android'de artık çerçeve SQLite yerine paketlenmiş SQLite kullanılıyor (APK ~ +1–2 MB/ABI). Mevcut veri sorunsuz açıldı (emülatörde). Gerçek kullanıcı verisiyle tek yönlü: eski sürüme (versionCode 1) geri dönüş desteklenmez (aynı şema/hash olsa da sürücü farkı kanıtlanmadı).
- **DataStore tek örnek kuralı:** aynı dosya için ikinci bir `DataStore` oluşturmak çalışma anında hata verir. Her platform yalnız `AppContainer` üzerinden erişmeli (Android: `AndroidAppContainer.get`).
- **Zaman dilimi/DST:** `kotlinx-datetime` ile `java.time` davranışı testlerle (İstanbul, New York DST) aynı çıktı verdi; Android minSdk 24 için `java.time` hâlâ core-library desugaring ile geliyor (`app` modülünde açık).
- **Tilt işaretleri (iOS)** ve iOS zamanlayıcının uygulama öldürülünce kaybı bilinen açık noktalar.
- `compileSdk 36` ile `kotlin.multiplatform.library` eklentisi `android {}` bloğunu kullanıyor (eski `androidLibrary {}` AGP 9.4'te kullanımdan kalktı).
