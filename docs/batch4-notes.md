# Parti 4 notları — Profil ekranı ve tema

Dal: `worktree-agent-aa31b8ae80a2963a9` (main @ 017fbd4'ten). Doğrulama: `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` -> BUILD SUCCESSFUL, lint 0 hata / 34 uyarı / 4 ipucu, JVM testleri 32 (yeni `ProfileLogicTest` 6).

## Madde madde

- **#35/#36 ClimbTime:** sayfa x 1.5 dk korundu. Metin artık çoğul/tekil doğru ("1 Day, 1 Hour Climbed", "2 Hours, 30 Min Climbed"); sıra Gün, Saat (REQUIREMENTS "X Hours, Y Days" ifadesi gevşek yorumlandı: büyük birim önce). Altında ikinci satır "Timed sessions: 3h 20m" (`totalTimedReadMillis`). Karta dokununca M3 AlertDialog tahmin ile gerçek süre farkını açıklar. Mantık `ProfileLogic.formatClimbTime/formatTimedDuration` (testli).
- **#37 yıllık hedef:** halka ve "x / y Books this year" artık `completedBooksThisYear`; oran `ProfileLogic.goalProgress` (0..1 kıskaç, hedef 0 -> 1 kabul).
- **#38 avatar:** `AvatarTier` (0–1 Novice, 2–5 Expedition Leader, 6+ Elite Alpinist; tüm zamanlar `completedBooksCount`). Tier adı ve "N more books to ..." görünür; avatar daire içinde: T1 yalnız tırmanışçı, T2 + sırt çantası + gümüş/ikincil halka, T3 + kask + kazma + kalın birincil halka. Nefes (ölçek 1 -> 1.07, yavaş) yalnız `ReadingTimerService.state.status == RUNNING` iken (PAUSED'da durur).
- **#39/#45 relic:** tek sensör dinleyicisi (`rememberTiltState()` ProfileScreen'de, `TiltState` snapshot state; kartlar yalnız graphicsLayer/draw lambdasında okur -> recompose yok). Kayıt `LifecycleEventObserver` ile ON_RESUME/ON_PAUSE, `onDispose`'da kesin bırakma; olay başına dizi ayırma yok; eşik 0.5° (`ProfileLogic.exceedsThreshold`, yumuşatılmış değere uygulanır). Gölge katmanı eğimle aynı yönde kayar, metalik shimmer ters yönde; rotationX/Y 25° ile sınırlı.
- **#40/#41 Journey:** başlık, yazar, tamamlanma tarihi (`completedAtMillis`, yoksa relic tarihi), toplam süre (`journeyTotalTimeMillis`), Summit Logbook (puan her zaman, yorum boşsa "No review written."), kronolojik oturumlar (gün, createdAt, id sırası), her notu yalnız kendi oturumunun altında; oturumsuz/bilinmeyen oturumlu notlar "Other notes". `ProfileLogic.groupNotes/sortedSessions` (testli). Tüm sayfa tek LazyColumn.
- **#43:** profil zaten tek LazyColumn; relic'ler `chunked(3)` + `items()`; anahtarlar eklendi (iç içe kaydırma yok).
- **Tema:** Dark Mode anahtarı yerine System/Light/Dark segmentli seçici (varsayılan SYSTEM yansır). `SummitReadTheme` varsayılanı SYSTEM, `resolveIsDark` yardımcı; `summitColorScheme` tüm rolleri vurgu renginden türetir (secondaryContainer, tertiary, outline, surfaceContainer* vb. artık mor değil). `MainActivity`: `enableEdgeToEdge` + `SystemBarStyle.auto { isDark }` uygulama temasına göre (DisposableEffect(isDark)). `values/themes.xml` + `values-night/themes.xml`: windowBackground açık/koyu renk (beyaz flaş yok). Not: platformda `Theme.Material.DayNight.NoActionBar` YOK, bu yüzden gece için ayrı `values-night` stili kullanıldı.
- **Bildirim anahtarı:** `ProfileViewModel.setNotificationsEnabled` -> `UserPreferences.setNotificationsEnabled`; Parti 1'in `StreakReminderScheduler` ve `StreakNotificationWorker`'ı aynı `notificationsEnabled` akışını okuyor (doğrulandı, kod okuma).
- **Hatalar:** diyaloglar (`showTimeExplanationDialog`, `showGoalDialog`, `goalInput`) `rememberSaveable`; kullanıcı adı alanı yerel durum + 500 ms debounce + ekrandan çıkarken kaydet (imleç sıçraması giderildi); renk seçici dairelerine contentDescription, `selectable(RadioButton)` ve 40 dp dokunma alanı; avatar/halka/relic için erişilebilirlik metinleri; yer tutucu "Climber" yok (Parti 1). `@OptIn(ExperimentalCoroutinesApi)` hâlâ gerekli (Journey akışlarında `flatMapLatest`), bırakıldı. Boş durum metni `strings.xml`'e bağlandı.

## Cihazda doğrulanmadı
- Tilt hissi, gölge/shimmer yönleri ve sensör kaydının ön plan/arka plan geçişlerinde gerçekten bırakılması.
- Avatar emoji kompozisyonu (⛑️ 🪓 🎒 konumları ve emoji fontu cihaz farkları), nefes animasyonunun zamanlayıcı durumuna tepkisi.
- Dark/Light/System geçişinde durum çubuğu/gezinme çubuğu ikon renkleri, ilk kare arka planı, tüm ekranlarda renk uyumu (özellikle Home/Mountain'ın `secondary` kullanan yerleri: dark'ta `secondary` açıldı).
- `MainActivity` `collectAsState(initial = SYSTEM)` kullanıcı DARK seçmişse ilk karede kısa süre yanlış tema gösterebilir (DataStore'u eşzamanlı okumak kapsam dışı bırakıldı).
- Bildirim izni akışı (RU:O1) `MainActivity`'de olduğundan değiştirilmedi.

## Kapsam dışı/yöneticiye
- Dosya sahipliği dışına çıkılmadı; veri değişikliği gerekmedi. `MainActivity` yalnız tema/status-bar kablolaması.
