# SummitRead — Devir notu (STATUS)

> **`kmp` dalı (2026-10-02): KMP Aşama A tamam.** `shared` modülü (Room KMP, DataStore KMP, repository, ViewModel'ler, saf mantık, platform arayüzleri) + ince `app`. Android emülatörde yükseltme testi geçti (veri korundu). Ayrıntı, sürümler, Aşama B/C planı: `docs/KMP-PLAN.md`. Aşağıdaki yollar (`app\...\data\`, `app\schemas`) KMP öncesi; artık `shared\src\commonMain\...` ve `shared\schemas`.

## Güncel durum — 2026-10-02 sabah (yönetici özeti)

**Android sürümü tamam ve emülatörde doğrulandı** (`main`). Ayrıntılı geçmiş aşağıda ve `docs/` altındaki raporlarda.

| Aşama | Sonuç | Belge |
|---|---|---|
| Kod incelemesi + 47 madde denetimi | 24 tam / 17 kısmi / 4 bozuk / 2 eksik bulundu | `reviews/`, `REQUIREMENTS-AUDIT.md` |
| Parti 1 (veri/servis/VM) + Parti 2–4 (Dağ/Ana/Profil) | Tüm kısmi/bozuk/eksik maddeler kodlandı | aşağıda §1, `batch*-notes.md` |
| DB v4→v5 göçü | Efe'nin Haziran verisiyle (Pixel_9a) doğrulandı, veri korunuyor. Yedek: `Desktop\Proje.B\SummitRead-emulator-yedek-2026-10-02` | — |
| Emülatör E2E | Çökme/işlev hatası yok | `E2E-REPORT.md` |
| Apple tarzı premium tasarım | Tasarım sistemi (Inter, iOS paleti, Haze cam, büyük başlık, cam tab bar, grouped listeler, sheet'ler) + 3 ekran | `DESIGN.md`, `redesign-*-notes.md` |
| Görsel QA + polish | Profil dik tutulunca çökme (NaN eğim) düzeltildi; açılışta "kütüphane boş" yarışı düzeltildi; 15+ görsel düzeltme | `QA-REDESIGN.md` |

Son kontrol: `assembleDebug testDebugUnitTest lintDebug` → BUILD SUCCESSFUL, 64 test, lint 0 hata.

**Emülatör notu:** `medium_phone` AVD (Android 16) adb'de hep "offline" kalıyor → testlerde **Pixel_9a** (Android 15) kullan.

**Kararlar (Efe, 2026-10-02):** iOS için Kotlin Multiplatform + Compose Multiplatform; Mac yok; iPhone'a ücretsiz Apple ID + Sideloadly/AltStore ile kurulum (TestFlight yok); iOS 27 uyumu hedef; bildirim izni açılışta sorulsun; 47 maddeye birebir bağlı kalmak zorunlu değil.

**Sıradaki aşama:** KMP dönüşümü `kmp` dalında (main'e dokunmadan). iOS derlemesi Mac gerektirir → GitHub Actions macOS; repo'yu GitHub'a yüklemek Efe'yle konuşulacak.

---

## Parti 1 ayrıntısı (2026-10-01)

Son güncelleme: 2026-10-01 — "Adım 0 + Parti 1" (çekirdek veri / servis / ViewModel katmanı) tamamlandı.
Referans: `docs\REQUIREMENTS-AUDIT.md` (durum tablosu + parti planı), `docs\REQUIREMENTS.md`, `docs\reviews\*`.
Yol kısaltması: `S/` = `app\src\main\java\com\example\summitread\`.

## 1. Bu partide yapılanlar

- **Adım 0 (#2):** `MountainRoute(bookId: Long)` tip güvenli rota argümanı. `MountainViewModel` kitabı rotadan alır (`factory(repository, userPreferences, bookId)`); `activeBookId` DataStore yarışı ve 4 `flatMapLatest` kalktı. `HomeScreen.onNavigateToMountain: (Long) -> Unit`. `HomeViewModel.setActiveBook` yalnız "son açılan kitap" kaydı için kaldı.
- **Şema v5 (`MIGRATION_4_5`, veri korunur):** `books.coverColorArgb` (Int?), `books.isArchived` (default 0), `books.completedAtMillis` (Long?, biten kitaplar relic tarihinden geriye doldurulur), `reading_sessions.createdAtMillis` (gerçek log zamanı), `bookId` indeksleri, `relics.bookId` UNIQUE (kopyalar temizlenir). `app\schemas\...\5.json` üretildi. Migration SQL'i 4.json'dan kurulan bir SQLite'a uygulanıp 5.json ile tablo/indeks bilgisi karşılaştırıldı (eşit). `fallbackToDestructiveMigration(dropAllTables=false)` yalnız eski (1-3) sürümler için yedek olarak duruyor.
- **#29 Seri:** Artık saklanmıyor, oturum günlerinden türetiliyor (`S/data/StreakCalculator.kt`, saf fonksiyon; takvim günü, sistem saat dilimi). Kaçırılan gün anında 0 gösterir; gece yarısında ekran açıkken kendiliğinden güncellenir (`DayClock`). Geçmiş güne loglama zincirleri otomatik birleştirir; gelecek güne loglama VM + repository'de engelli. DataStore'daki `lastReadDate/currentStreak/updateStreak` silindi.
- **#26/#28 Zamanlayıcı:** `ReadingTimerService` yeniden yazıldı: kitaba bağlı, gerçek Pause/Resume, `SystemClock.elapsedRealtime` tabanlı, bildirimde "Paused hh:mm:ss" + Resume/Stop eylemleri. Stop'ta süre DataStore'a "bekleyen oturum" olarak yazılır (servis ölse/UI olmasa bile kaybolmaz); sistem servisi öldürürse `onDestroy` de aynısını yapar. `MountainViewModel` bekleyen süreyi görür, önerilen sayfayı (`PageMath.suggestedPages`) kutuya yazar ve bir sonraki `logPages()`'te `timeSpentMillis` olarak oturuma kaydedip bekleyeni temizler.
- **#35/#40 veri:** ClimbTime hâlâ sayfa × 1.5 dk (`ProfileViewModel.totalTimeRead`). Ek olarak gerçek süre: `totalTimedReadMillis`, Journey için `journeyBook`, `journeyTotalTimeMillis`.
- **#5/#6/#10 logPages:** `BookDao.logPagesTransaction` tek `@Transaction`: sayfayı kalan sayfaya kırpar (currentPages > totalPages olamaz), oturumu ekler, aynı günün bağsız notlarını bağlar, kitabı günceller (`lastActivityMillis` -> Recently Read çalışır, `completedAtMillis`), relic'i tam bir kez oluşturur (UNIQUE + IGNORE). VM tarafında çift dokunuşa karşı `isLogging` bekçisi, `viewModelScope`'ta çalışır, `logPages()` artık suspend değil.
- **#41 Notlar:** `addNote` notu oluşturulurken kitabın bugünkü son oturumuna bağlar (varsa). Oturum henüz yoksa not bağsız kalır ve o günün ilk oturumu loglanınca YALNIZ o günün notları bağlanır (eskiden tüm bağsız notlar bağlanıyordu). Hiç oturumu olmayan günlerin notları `sessionId = null` kalır -> Journey'de "Other notes" olarak gösterilmeli.
- **#34 Bildirimler (WorkManager):** `ReminderPlanner` (saf) seri sıfırlanmasından 6 sa ve 1 sa önceyi hesaplar (sıfırlanma = son okunan günden sonraki günün bittiği yerel gece yarısı); `StreakReminderScheduler` benzersiz OneTimeWork (`streak_reminder_6h/1h`, `REPLACE`) planlar; `MainActivity` her log ve tercih değişiminde yeniden planlar, bildirimler kapalıysa iptal eder. `StreakNotificationWorker` tetiklenince her şeyi yeniden doğrular (tercih, POST_NOTIFICATIONS, seri hâlâ aynı mı), dokununca `MainActivity` açılır. Eski günlük periyodik iş iptal edilir; ölü `ReadingReminderWorker` silindi. Kanallar `NotificationChannels.ensureCreated`.
- **#42 Arşiv:** `archiveBook(id, archived)` (repository + `HomeViewModel.archiveBook`); raf/liste sorguları arşivlileri gizler, oturum/not/relic korunur; `archivedBooks` akışı var. `deleteBook` (kullanılmıyor) artık oturum/not/karakter/relic satırlarını ve görsel dosyalarını da siler. FK eklenmedi (tablo yeniden yaratma riski); yerine indeks + manuel cascade.
- **#33 veri tarafı:** `completedSummit: SummitCompletion?` (bookId, title, completedAtMillis, relicEmoji, hazır `message` metni). Relic emojisi artık kitap id'sinden deterministik seçilip DB'ye yazılır (`RelicEmojis`).
- **#46:** Home/Mountain/Profile VM'lerindeki tüm Room/DataStore tabanlı StateFlow'lara `.catch { emit(varsayılan) }`; gereksiz `flatMapLatest`'ler kaldırıldı (Profile'daki Journey akışlarında gerçekten dinamik id olduğu için kaldı).
- **RD#10:** Not silinince foto dosyası da silinir; görsel kopyalama `Dispatchers.IO`'da (`ImageStorage.copyImageToInternalStorage` artık `suspend`, UUID'li dosya adı; `delete/deleteAll`).
- **Diğer:** Home sıralama tercihi kalıcı (`UserPreferences.sortOptionName`); `updateBookOrder` tek transaction (`reorderBooks`); `selectedDate` günü takip eder; `hoursSinceLastSession` gerçek log zamanından (dakikada bir yenilenir); `ProfileViewModel` yer tutucuları (SYSTEM / boş ad); `completedBooksThisYear` (yıl filtreli, yıl değişimini takip eder).
- **Build/temizlik:** core library desugaring (`desugar_jdk_libs 2.1.5`, `libs.versions.toml`), `windowSoftInputMode` `<activity>`'ye taşındı, `maven.xml`, `DataRepository`, `ui\main\*` + şablon testleri, `ReadingReminderWorker`, `Converters` + Gson silindi.
- **Testler (JVM):** `StreakCalculatorTest` (17: boşluk, geriye loglama/zincir birleştirme, aynı gün çoklu log, gelecek günler, gece yarısı sınırı, DST, sıfırlanma anı), `ReminderPlannerTest` (4), `PageMathTest` (5: kırpma, önerilen sayfa, süre biçimi, relic emojisi).

## 2. Verilen kararlar (özet)
Rota `bookId` taşır; zamanlayıcı kitaba bağlı, tek global sayaç; ClimbTime = sayfa × 1.5 dk (gerçek süre ayrıca); seri takvim günü esaslı + türetilmiş; `selectedDate` bugünü izler; bildirimler WorkManager 6 sa / 1 sa; v5 gerçek migration; arşiv kullanıcıya dönük eylem; İngilizce UI korunur. (Tam liste yönetici görev metninde; burada tekrarlanmadı.)

## 3. UI partilerinin kullanacağı sözleşmeler

**MountainViewModel** (`ui\mountain`; `factory(repository, userPreferences, bookId)`; `MountainScreen(bookId = ..., ...)`)
- Akışlar: `activeBook`, `readingSessions`, `notes`, `characters`, `currentStreak: StateFlow<Int>` (üst çubuktaki alev için `userPreferences.currentStreak` KULLANMAYIN, kaldırıldı), `hoursSinceLastSession` (saat, dakikada bir yenilenir; 0 = hiç log yok), `isFocusMode`, `today` (günün başlangıç ms'i, gece yarısı kendiliğinden değişir; `CalendarStrip` "bugün"ü bundan almalı), `selectedDate` (varsayılan bugünü izler), `pagesInput`, `isLogging` (log düğmesini devre dışı bırak), `pendingTimeMillis` (bu kitabın loglanmamış kronometre süresi), `showSummitAnimation`, `completedSummit` (`.message`, `.title`, `.relicEmoji`, `.completedAtMillis`), `triggerParticles`.
- Fonksiyonlar (hepsi non-suspend, `viewModelScope`): `setSelectedDate(ms)` (gelecek günü yok sayar; strip'te gelecek günler pasif gösterilmeli), `setPagesInput`, `logPages()`, `discardPendingSession()`, `toggleFocusMode`, `dismissSummitAnimation`, `clearCompletedSummit`, `saveSummitLogbook(rating, review)`, `addNote(content, page, photo)`, `deleteNote(note)` (foto da silinir), `addCharacter`, `deleteCharacter`.
- Süre akışı: `StopwatchWidget(bookId)` yalnız servisi çağırır; Stop'ta ayrıca callback YOK. Süre DataStore'dan VM'ye gelir, `pagesInput` önerilen sayfayla dolar.
- `completedSummit`'ı `SummitConqueredAnimation` (`relicEmoji` rastgele seçimi yerine VM'nin değeri) ve #33 kartı kullanmalı; Logbook kaydedilince/kapanınca `clearCompletedSummit()`.

**ReadingTimerService** (`S/services`): `state: StateFlow<TimerState(status IDLE/RUNNING/PAUSED, bookId, elapsedMillis)>`, `isRunning` (yalnız sayarken), `isPaused`, `activeBookId`, `elapsedTime`; yardımcılar `start(context, bookId)/pause/resume/stop`; `formatElapsed(ms)`. Başka kitaba bağlıyken widget "Timer running for another book" gösterir.

**HomeViewModel**: `sortOption` (kalıcı), `allBooks` (arşivsiz, sıralı), `archivedBooks`, `currentStreak`, `setSortOption`, `addBook(..., coverColorArgb: Int? = null)`, `archiveBook(id, archived = true)`, `updateBookOrder(list)` (tek transaction), `deleteBook` (UI'da kullanılmaz). Renk için: `BookEntity.coverColorArgb` doluysa onu, değilse `accentColorIndex`'i kullanın.

**ProfileViewModel**: `completedBooksThisYear` (yıllık hedef halkası için; `completedBooksCount` tier için tüm zamanlar), `totalTimeRead` (sayfa × 1.5, metin aynı), `totalTimedReadMillis`, Journey için `journeyBook` (yazar, sayfa, `completedAtMillis`), `journeyTotalTimeMillis`, `journeySessions`, `journeyNotes` (sessionId'siz notlar "Other notes" olarak gösterilmeli).

**Veri**: `BookDao` artık `abstract class`; `LogPagesResult`; `BookRepository.logPages(...)` null döndürebilir (gelecek tarih / kitap tamam / sayfa ≤ 0). `UserPreferences`: `sortOptionName`, `pendingTimerSession` (+ `addPendingTimerSession`, `clearPendingTimerSession`). `ImageStorage.copyImageToInternalStorage` artık suspend (Home `AddBookDialog` ve `FocusModeNoteInput` çağrıları bunun için minimum düzeyde güncellendi).

## 4. Kalan işler
- **Parti 2 (Mountain UI):** #4 3D açılma, #14/#18/#19/#20 gökyüzü + `ThemeMode` + canlı saat + hava (VM `hoursSinceLastSession` hazır), #17, #22 çadır diyaloğu + hit-box, #33 kart metni, #26 kayan/genişleyen kronometre kabuğu, RU:Y1/O2/O3/O5/O9/D3, takvim şeridi (`today`, gelecek günler pasif), log düğmesi `isLogging`.
- **Parti 3 (Home UI):** #10 menü (kalıcılık VM'de hazır), #11 sürükle-bırak, #12 renk tekeri (`coverColorArgb` hazır), #13 altın folyo, #7, #9, arşiv UI'ı (#42: `archiveBook`/`archivedBooks` hazır), RU:O2/O6/O10.
- **Parti 4 (Profil/tema):** #38 tier adları, #39 + RU:Y3, #40 Journey içeriği (veri hazır), #37 UI (`completedBooksThisYear`'e bağla), #36 metni (gerçek süre sheet'te), RU:O4, bildirim izni akışı (RU:O1; `MainActivity` hâlâ açılışta izin istiyor), kalan yer tutucular, Compose UI testleri.
- Kapsam dışı kalanlar: `enableEdgeToEdge` tema stili (RU:O4c), DB'de mutlak foto yolları / `allowBackup` (RD#18), `UserPreferences.username` her tuşta yazma (UI).

## 5. Doğrulanmamış / riskler (cihazda test EDİLMEDİ)
- Hiçbir şey cihaz/emülatörde çalıştırılmadı. Migration yalnız SQLite'ta (Python) 4.json -> SQL -> 5.json karşılaştırmasıyla doğrulandı; Room `MigrationTestHelper`/androidTest yok.
- `lintDebug`: 0 hata (40 uyarı/ipucu, çoğu önceden var: GradleDependency, UnusedResources, ModifierParameter, AutoboxingStateCreation). `testDebugUnitTest`: 26 test, hepsi geçti.
- Foreground service davranışı (özellikle bildirimden Pause/Resume/Stop, Android 14 `specialUse`, servis sistem tarafından öldürülünce kalan sürenin yazılması) ve WorkManager zamanlaması (Doze'da dakikalar sapabilir) gerçek cihazda denenmeli.
- Oturum günleri sistem saat diliminde saklı başlangıç ms'i; saat dilimi değişirse bir günlük kayma mümkün (kabul edildi).
- `MountainViewModel.activeBook` ilk karede `null` (Room yüklenene kadar); ekran bu kısa süre "no active book" metnini gösterebilir -> UI'da yükleniyor durumu eklenebilir.
- Üst bar ve Home'daki seri değeri artık VM'den geliyor; `userPreferences.currentStreak` çağıran başka yer kalmadı (derleme ile doğrulandı).
## Devir — 2026-10-09 (buradan devam)

- `kmp` dalı: Faz A + B1 (tasarım sistemi) + B2 (Ana ekran + gezinme) + B3 (Dağ) + **B4 (Profil)** ortak modülde; `app` artık yalnız MainActivity, servis, worker, bildirim ve Android sensör/zamanlayıcı köprüleri. Android derleniyor, testler/lint yeşil (lint 0 hata / 26 uyarı), iOS klib derlemeleri (`compileKotlinIosSimulatorArm64/Arm64`, `compileTestKotlinIosSimulatorArm64`) yeşil.
- **Bekleyen: B4 emülatör doğrulaması** (Pixel_9a, önce `run-as … tar c databases files` yedeği): Profil açılıyor mu, relic kartı eğilme paralaksı (sensör), yıllık hedef sheet'i, Journey sheet, tarih biçimleri, Library↔Profile geçişi, FATAL yok. Başarısız olursa nedeni B4 commit'inde ara.
- **Sonra Faz C:** `iosApp/` (SwiftUI kabuk + `MainViewController()`), framework linklemesi (Mac/CI), GitHub Actions macOS iş akışı (iOS 27 SDK, imzasız .ipa → Sideloadly/AltStore). iOS'ta bağlanmayanlar: hatırlatıcı gözlemi + bildirim izni, `UNUserNotificationCenterDelegate`, durum çubuğu stili, sayaç durumunun kalıcılığı, tilt işaretleri doğrulaması (bkz. `MainViewController.kt` TODO).
- GitHub: private https://github.com/efeeyyy/SummitRead (main + kmp). Worktree: `Desktop\Proje.B\Kitap-kmp`. Emülatör: Pixel_9a (medium_phone kullanma).
