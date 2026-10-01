# SummitRead — Gereksinim Denetimi (REQUIREMENTS.md vs. gerçek kod)

Tarih: 2026-10-01. Salt okunur denetim: yalnızca bu dosya oluşturuldu, hiçbir kaynak dosyaya dokunulmadı.
Yöntem: Her gereksinim `app\src\main\java\com\example\summitread\` altındaki gerçek koda bakılarak doğrulandı; `docs\reviews\review-data.md` (kısaltma **RD#n**) ve `docs\reviews\review-ui.md` (kısaltma **RU:Y1/O1/D1…**) bulguları kullanıldı. Çalıştırma/cihaz testi YAPILMADI; tüm hükümler kod okumasına dayanır (görsel kontrast, animasyon hissi, sürükleme jestinin cihazdaki davranışı "doğrulanamadı" notuyla işaretlendi).
Yol kısaltması: `S/` = `app\src\main\java\com\example\summitread\`. Satır numaraları bu denetimdeki dosya halidir. (Not: RU:Y2'nin `StopwatchWidget.kt:370-379` atfı hatalı; gerçek yer `StopwatchWidget.kt:67-93`.)

## 1. Özet sayım

| Durum | Adet | Maddeler |
|---|---|---|
| DONE | 24 | Genel, 1, 3, 5, 6, 7, 8, 9, 14, 15, 16, 21, 23, 24, 25, 27, 30, 31, 32, 35, 36, 37, 43, 45 |
| PARTIAL | 17 | 2, 4, 12, 13, 17, 18, 19, 20, 29, 33, 34, 38, 39, 40, 41, 44, 46 |
| BROKEN | 4 | 10, 11, 26, 28 |
| MISSING | 2 | 22, 42 |
| N/A-INTENTIONAL | 1 | 47 |

## 2. Özet tablo

| No | Gereksinim | Durum | Kanıt |
|---|---|---|---|
| Genel | %100 İngilizce UI | DONE | `app\src` altında `.kt`/`.xml` içinde Türkçe harf/kelime grep'i: eşleşme yok (yalnız emoji). Not: ~70 sabit metin `strings.xml` dışında (RU:D2) ve 10 kullanılmayan string — kural ihlali değil, hijyen. |
| 1 | Alt bar yalnız Home+Profile | DONE | `S/Navigation.kt:56-69` (2 öğe), `:79-103` |
| 2 | Kitaba tıkla → o kitabın Mountain ekranı | PARTIAL | Her iki görünümde tıklanabilir: `S/ui/home/HomeScreen.kt:185-190,212-217`. Ama `MountainRoute` id taşımıyor (`S/NavigationKeys.kt:9`); `setActiveBook` fire-and-forget (`S/ui/home/HomeViewModel.kt:88-92`) → yarış, ilk karede eski/boş kitap (RU:Y5) |
| 3 | "[Title] - Summit Climb" başlığı | DONE | `S/ui/mountain/MountainScreen.kt:119-121` |
| 4 | 3D kitap açılma animasyonu (SharedTransition + rotationY/cameraDistance) | PARTIAL | `SharedTransitionLayout` `S/Navigation.kt:108` + `sharedBounds` (`BookSpineItem.kt:57-60`, `BookListLayout.kt:77-80`, `MountainScreen.kt:184-187`). `rotationY`/`cameraDistance` yalnız Relic kartında (`S/ui/profile/RelicShowcase.kt:43-44`); geçişte YOK |
| 5 | Shelf/List geçiş butonu (sağ üst) | DONE | `HomeScreen.kt:140-147` |
| 6 | Derinlikli gölgeli ahşap raf | DONE | `S/ui/home/BookshelfLayout.kt:64-91` (gradyan + shadow + alt çizgi) |
| 7 | Sırtta yatay ilerleme çubuğu "%34 Read" | DONE | `S/ui/home/BookSpineItem.kt:121-145`. Not: metin "34%" (…"Read" yok), yalnız progress>0 iken |
| 8 | Dikey metin taşması / çubuk raf arkasında kaybolmaz | DONE | `BookSpineItem.kt:96-106` (`maxLines=1`, `Ellipsis`, `requiredWidth`); raf ayrı Column (`BookshelfLayout.kt:65`). Kapaklı kitapta sırt metni hiç çizilmiyor |
| 9 | Liste: Coil AsyncImage kapak / vektörel yedek | DONE | `S/ui/home/BookListLayout.kt:135-160`. Not: model düz yol String (`:137`), diğer yerlerde `File.toURI()` — tutarsız, Coil3'te çalışması cihazda doğrulanamadı |
| 10 | 5 sıralama seçeneği | BROKEN | Menü tam: `HomeScreen.kt:92-138`. `RECENTLY_READ` `lastActivityMillis`'e göre (`HomeViewModel.kt:40`) ama alan hiç güncellenmiyor (`S/data/BookRepository.kt:87-92`, RD#9) → "Newest First" ile aynı. Seçim kalıcı değil |
| 11 | Dikey sürükle-bırak sıralama | BROKEN | `BookListLayout.kt:66` (`key(book.id)` yok), `:81` (`pointerInput(book.id…)`) → ilk yer değişiminde jest iptal, `onSaveOrder` çağrılmaz (RU:Y4; cihazda doğrulanmadı). Tüm liste tek `item` (`HomeScreen.kt:180`). `updateBookOrder` bayat kopya yazar (`HomeViewModel.kt:80-86`) |
| 12 | Add Book: ikonlar, renk tekeri, yuvarlak önizleme | PARTIAL | İkonlar var: `S/ui/home/AddBookDialog.kt:86,95,104`; yuvarlatılmış önizleme `:123-133`. Renk: hâlâ sabit 5 swatch (`:142-165`, `S/theme/Color.kt:26+`), tekeri/palet seçici YOK |
| 13 | %100 kitapta parıldayan altın folyo mühür | PARTIAL | Statik altın daire + 🏅 (`BookSpineItem.kt:148-160`, `BookListLayout.kt:162-174`). Parıltı/shimmer animasyonu ve şerit YOK |
| 14 | Dark'ta dağ arkasındaki beyaz dikdörtgen yok | DONE | `S/ui/mountain/MountainCanvas.kt:185-202` gökyüzü tüm tuvali kaplıyor; opak beyaz yok. Uyarı: dark kararı `isSystemInDarkTheme()` (`:172`), uygulamanın `ThemeMode`'u değil (RU:O4b); `surfaceColor/onSurfaceColor` parametreleri kullanılmıyor. Görsel doğrulanamadı |
| 15 | PageInputCard/CalendarStrip dark kontrast | DONE | `S/ui/mountain/PageInputCard.kt:104-116` ve `CalendarStrip.kt:111-126` tema renklerini kullanıyor (onSurface/onSurfaceVariant=#B0B0B0). Görsel kontrast doğrulanamadı |
| 16 | Sayfa sayısına göre dağ (<200/200-400/400+) | DONE | `MountainCanvas.kt:87-91` (peakY 0.5/0.3/0.1). Yalnız zirve yüksekliği değişiyor, kontrol noktaları sabit |
| 17 | Tırmanış fiziği (PathMeasure, eğim, bounce, IDLE/CLIMBING) | PARTIAL | PathMeasure+teğet: `:136,158,319-320`. Bounce yalnız ilerleme tween'i sürerken (`:322-323`). `ClimbingState` enum'u YOK (grep boş); uzuv animasyonu YOK (tek emoji). Açı ±15° kıskaç ve yukarı eğimde negatif açı = geriye yatma olasılığı (doğrulanamadı) |
| 18 | Zamana bağlı gökyüzü (dawn/day/twilight/midnight) | PARTIAL | 4 gradyan `:175-180`. Ama `isSystemInDarkTheme \|\|` gün boyu "gece" zorluyor (`:176`); `LocalTime.now()` kompozisyonda bir kez (`:173`), canlı güncellenmez; `java.time` NewApi lint |
| 19 | Güneş nabzı, yıldız parıltısı, rastgele kayan yıldız | PARTIAL | Güneş `:205-217`, yıldız `:220-227` var. Kayan yıldız rastgele DEĞİL: her 5 sn sabit konum/yön (`:229-243`). Güneş sistem-dark'ta hiç çizilmiyor. Yıldızlar her karede `Random(42)` ile yeniden üretiliyor |
| 20 | Seri aktif=berrak; 36 sa+ sis/fırtına; yazınca dağılır | PARTIAL | Fırtına bulutu `:255-262`. Sis YOK; "rüzgârla dağılma" animasyonu YOK (bulut sadece kalkar). `hoursSinceLastSession` gün başı (00:00) damgasından hesaplanır ve zamanla yenilenmez (`S/ui/mountain/MountainViewModel.kt:73-78`, RD#15) → fırtına yanlış zamanda çıkar |
| 21 | Yatay kesintisiz süzülen bulutlar | DONE | `MountainCanvas.kt:246-252` (iki bulut grubu sürekli kayıyor). Sırt boyunca değil, sabit yükseklik; basit daireler |
| 22 | %25/50/75 tıklanabilir çadır → söz + ara istatistik | MISSING | Çadırlar çiziliyor (`:307-316`) ve tıklama kutuları var (`:360-368`) ama `onMilestoneClick` boş `TODO` (`MountainScreen.kt:200-202`) → tıklayınca HİÇBİR şey olmuyor; söz listesi/istatistik/diyalog yok. `onSignpostClick` de boş (`:199`) |
| 23 | Odak modunda dağcıya zoom + ⛺ | DONE | `MountainCanvas.kt:63-68,160-170,188-194,327` (scale 1.3 + translate, ⛺). Tıklama kutuları ölçeklenmiyor (RU:O9) |
| 24 | Ağaçlar yukarı bakar | DONE | `MountainCanvas.kt:284-296` (tepe `startY-40`, taban `startY`) |
| 25 | Dağcı taşma/ters düşme düzeltmesi | DONE | `MountainCanvas.kt:319-344` (`toDegrees(atan2)`, `translate` sonra `rotate(pivot=Zero)`, baseline `emojiSize/3`, clamp) |
| 26 | Kronometre: başlat/duraklat/durdur, genişleyen/kayan | BROKEN | "Pause" aslında `ACTION_STOP` (`S/ui/mountain/StopwatchWidget.kt:67-79`) → `stopSelf` → `onDestroy` süreyi 0'lar (`S/services/ReadingTimerService.kt:99-104,155-160`). Gerçek pause/resume yok. Genişleyen/kayan animasyon yok (statik Card, `MountainScreen.kt:226-233`) |
| 27 | ReadingTimerService foreground, bildirimde saniye saniye, Android 14+ | DONE | Servis `ReadingTimerService.kt:68-97` (FGS `specialUse` API34+, `:78-87`), bildirim her sn `:89-96,127-130`; manifest `AndroidManifest.xml:5-6,24-29`. Küçük: `delay` tabanlı sapma (RD#12), `bookId==-1` iken startForeground yok (`:56-59`), `String.format` locale (`:150`) |
| 28 | Kronometre bookId'ye bağlı; süre Room'a; sayfa önerisi | BROKEN | Sayfa önerisi VAR (`StopwatchWidget.kt:103-104` → `MountainScreen.kt:229-231`). Süre atılıyor: ikinci parametre `_` (`:229`), `logPages` varsayılan 0 (`MountainViewModel.kt:88,92`) → `timeSpentMillis` HEP 0 (RD#1). Servis `activeBookId` yazılıyor ama hiç okunmuyor (`ReadingTimerService.kt:32,71`); sayaç global, kitaptan bağımsız |
| 29 | Seri: LocalDate.now(), lastReadDate, artar/korunur/sıfırlanır | PARTIAL | `MountainViewModel.kt:101-124`. Sıfırlama yalnız bir SONRAKİ loglamada; UI eski seriyi gösterir (`MountainScreen.kt:89`, `HomeScreen.kt:68`; RD#3a). "Dünü geriye loglama" seriyi 1'e düşürür (`:118-124`); okuma-yazma atomik değil; `selectedDate` gece yarısı bayatlar (RD#4) |
| 30 | Ana ekran üst barda seri alev ikonu | DONE | `HomeScreen.kt:83-89` (🔥 + sayı). Gösterilen değer bayat olabilir (#29) |
| 31 | Kıvılcım parçacıkları → alev; heartbeat | DONE | `S/ui/mountain/ParticleEngine.kt:40-95`, alev nabzı `MountainScreen.kt:97-101,323-333` (tek 1.5x nabız). Parçacık = daire (yaprak yok); sabit `dt=0.016` (`ParticleEngine.kt:68`, RU:D3); alev Mountain üst barında (Home'da parçacık yok) |
| 32 | Summit Logbook (puan+inceleme) | DONE | `S/ui/mountain/SummitLogbookDialog.kt:11-86`, tetik `MountainScreen.kt:300-321`, kayıt `MountainViewModel.kt:135-141`. `showSummitLogbook` `remember` → döndürmede kaybolur (RU:O2) |
| 33 | "Congratulations! … [Title] Relic …" kartı | PARTIAL | `S/ui/mountain/SummitConqueredAnimation.kt:~160-215` "Summit Conquered!", başlık, "Relic Earned!" gösteriyor; istenen cümle HİÇ YOK (grep). Relic emojisi UI'da rastgele (`:60`), DB'de hep 🏔️ (RU:O8, `RelicEntity.kt:11`) |
| 34 | WorkManager: 6 sa ve 1 sa kala bildirim | PARTIAL | Günlük periyodik worker kayıtlı (`S/MainActivity.kt:53-59`); `S/worker/StreakNotificationWorker.kt:22-43` tek genel bildirim ("lastRead<today"). 6s/1s hesabı YOK, ikinci bildirim YOK, `notificationsEnabled` yok sayılıyor, contentIntent yok, ilk gecikme yok (RD#8, RU:O1). `ReadingReminderWorker` ölü kod |
| 35 | ClimbTime = sayfa×1.5 dk, "X Hours, Y Days" | DONE | `S/ui/profile/ProfileViewModel.kt:52-70` (gerçek süre `_` ile yok sayılıyor). Format "N Days, M Hours, Climbed", çoğul yok. **#28 ile çelişki → Karar 1** |
| 36 | Satıra tıkla → M3 AlertDialog açıklama | DONE | `S/ui/profile/ProfileScreen.kt:191,407-419` |
| 37 | Dairesel yıllık hedef kartı | DONE | `ProfileScreen.kt:214-258`. Sayım "tüm zamanlar" (`S/data/db/BookDao.kt:86-87`, RD#7) — yıl filtresi yok |
| 38 | Avatar nefes + Tier 1/2/3 | PARTIAL | Nefes yalnız sayaç çalışırken (`ProfileScreen.kt:112-131`) ✓. Tier eşikleri doğru (`:106-110`) ama yalnız emoji; "Novice / Expedition Leader / Elite Alpinist" adları UI'da HİÇ yok (yalnız yorum, grep) |
| 39 | Relic: ivmeölçer/jiro, kayan gölge, ters shimmer, 3D | PARTIAL | 3D tilt `S/ui/profile/RelicShowcase.kt:41-46`; sensör `SensorUtils.kt:21-22` (rotation vector → accel yedek); shimmer `RelicShowcase.kt:79-93`; emoji ters kayar `:59-60`. Gölge KAYMIYOR (`shadowElevation=8f` sabit, `:45`). Kart başına bir dinleyici + `Pair` destructure → her olayda recompose (RU:Y3) |
| 40 | Madalya → Book Journey sheet | PARTIAL | Tıklama `ProfileScreen.kt:393-395,421-428` → `S/ui/profile/JourneyTimelineSheet.kt`. Var: başlık, inceleme+puan (yalnız inceleme doluysa, `:49,66`), oturumlar, günün notları (`:95-97`). YOK: yazar/sayfa bilgisi, tamamlanma tarihi (`dateEarned` sheet'te yok), toplam süre; süre satırı #28 yüzünden hiç görünmez (`:150`) |
| 41 | Notlar sessionId ile oturuma birebir | PARTIAL | Alan var (`NoteEntity.kt:14`), timeline sessionId ile filtreliyor (`JourneyTimelineSheet.kt:95-97`) → çoğalma yok. Ama bağlama geriye dönük: tüm bağsız notlar bir sonraki oturuma yapışır (`BookDao.kt:56-57`, `BookRepository.kt:75`, RD#17); son oturumdan sonra yazılan notlar timeline'da hiç görünmez |
| 42 | Arşivlense de geçmiş silinmez | MISSING | Arşiv kavramı yok (`BookEntity.kt:7-20` alan yok, grep "archiv" boş); kitap silme UI'ı yok (`HomeViewModel.kt:74-78` çağıran yok). Veri silinmiyor ama sebep özelliğin olmaması → **Karar 6** |
| 43 | Profil tek LazyColumn, relic `items()` chunk | DONE | `ProfileScreen.kt:93` (LazyColumn), `:381-398` (`chunked(3)` + `items`); `LazyVerticalGrid` yok |
| 44 | Bellek sızıntısı/recomposition denetimi | PARTIAL | Açık kalanlar: kart başına sensör (`RelicShowcase.kt:34`), Focus-mode bayrağı sızıntısı/BackHandler yok (`Navigation.kt:54,133`, RU:Y1), composable scope'ta `launch` (`MountainScreen.kt:241,275-292`, RU:O7), tuvalde kare başı ayırma (`MountainCanvas.kt:221,285,299-344`, RU:O5), `remember` ile durum kaybı (RU:O2) |
| 45 | 0.5° eşik; nefes yalnız sayaçta; withFrameNanos | DONE | Eşik `SensorUtils.kt:40,54` (yumuşatılmış açıya uygulanıyor ≈1° gerçek); nefes `ProfileScreen.kt:112`; `withFrameNanos` `MountainCanvas.kt:75-84`, `System.nanoTime` yok (grep). Not: kare döngüsü boşta/odak modunda da sürüyor |
| 46 | Flow'larda `.catch{emit}`; gereksiz flatMapLatest temizliği | PARTIAL | `.catch` yalnız `S/ui/profile/ProfileViewModel.kt:41,45,49,69`. `HomeViewModel.kt:35`, `MountainViewModel.kt:34-56`, repository'de YOK. `flatMapLatest` Mountain'da 4 kez aynı `activeBookId` üzerinde (`:35,41,47,53`), temizlenmemiş |
| 47 | `fallbackToDestructiveMigration()` bilinçli | N/A-INTENTIONAL | `S/data/db/AppDatabase.kt:36` (`dropAllTables = false`) — **Karar 2** |

## 3. DONE olmayan maddelerin ayrıntısı

**#2 (PARTIAL)** — Çalışıyor ama kırılgan: `setActiveBook` coroutine'i beklenmeden `onNavigateToMountain()` çağrılıyor (`HomeScreen.kt:186-189`). Mountain açılırken `activeBook` önceki kitap/`null` olabilir; `sharedBounds` yalnız `activeBook != null` dalında (`MountainScreen.kt:179-187`) → paylaşımlı geçiş hedefi geç/yanlış. Eksik: `MountainRoute(bookId)` + VM'in id'yi rotadan alması (Home, Navigation, MountainViewModel etkilenir). Süreç ölümünde "aktif kitap" DataStore'a bağlı gizli global durum.

**#4 (PARTIAL)** — 3D efekt yok. Gerekli: Home'da kapak/sırt öğesinde `graphicsLayer { rotationY; cameraDistance }` ile açılma, hedefte tersine; `sharedBounds` ile birlikte `boundsTransform`/`renderInSharedTransitionScopeOverlay`. #2'deki rota düzeltmesine bağımlı (hedef anında hazır olmalı).

**#10 (BROKEN)** — `BookRepository.logPages` `book.copy(currentPages, isCompleted)` yazıyor, `lastActivityMillis` değişmiyor (`:87-92`). Düzeltme: `lastActivityMillis = System.currentTimeMillis()`. Ek: `sortOption` kalıcı değil (RU:O2), menü yalnız liste görünümünde görünür (gereksinime uygun).

**#11 (BROKEN)** — Öğeler konuma göre eşleşiyor (`forEachIndexed`, `key` yok) ve `pointerInput(book.id,…)` anahtarı takas sonrası değişiyor → sürükleme coroutine'i iptal; `onDragEnd` → `onSaveOrder` çalışmaz. Düzeltme: `key(book.id)`, `pointerInput(Unit)` + `rememberUpdatedState`, ya da LazyColumn tabanlı reorderable; DAO'da `UPDATE books SET displayOrder` tek transaction. Raf görünümüne yansıma zaten `allBooks` akışıyla mümkün (kaydedilirse).

**#12 (PARTIAL)** — Renk tekeri yok. `BookEntity.accentColorIndex` yalnız 5 `AccentColor` indeksi saklıyor (`BookEntity.kt:16`); keyfî renk için yeni sütun (örn. `accentColorArgb`) + şema sürümü gerekir → Karar 5. Kullanıcı kendi HSV seçicisini yazmalı (kütüphane bağımlılığı yok).

**#13 (PARTIAL)** — `infiniteTransition` ile kayan beyaz parıltı çizgisi (shader/`Brush.linearGradient` offset animasyonu) + sırt boyunca şerit eklenmeli.

**#17 (PARTIAL)** — `ClimbingState { IDLE, CLIMBING }` enum'u ve ona bağlı uzuv animasyonu yok; tek emoji ile uzuv animasyonu mümkün değil (vektör/Canvas çizimli dağcı gerekir → Karar 7). Eğim yönü: yukarı çıkarken `angleDegrees` negatif → saat yönünün tersi; "öne eğilme" için işaret/emoji yönü doğrulanmalı. `isClimbing` yalnız ilerleme tween'i sürerken true; gerçek "sayaç çalışıyor" (`ReadingTimerService.isRunning`) ile bağlanmamış.

**#18 / #19 / #20 (PARTIAL)** — Tek gökyüzü/hava kod bloğu (`MountainCanvas.kt:172-262`): (1) `isSystemInDarkTheme ||` koşulu zaman tabanlı gradyanı dark'ta devre dışı bırakıyor; uygulamanın `ThemeMode`'u okunmuyor (`LocalAppThemeState` tanımlı ama kullanılmıyor, `Theme.kt:17`). (2) Saat bir kez okunuyor. (3) Kayan yıldız deterministik. (4) Sis/rüzgâr yok. (5) `hoursSinceLastSession` gün başı damgası: gün D'de gece 23:59'da okuyan kullanıcıda fırtına D+1 12:00'de (yalnız ~12 sa sonra) çıkar; ayrıca 24 saatlik seri sıfırlanmasından (36 > 24) sonra çıkması mantıksal çelişki → Karar 3.

**#22 (MISSING)** — Tıklama işleyicisi boş. Gerekli: milestone diyaloğu/bottom sheet (İngilizce motivasyon sözleri listesi + o noktaya kadarki istatistikler: sayfa, oturum sayısı, süre, tarih), tıklama kutularının dp/px düzeltmesi (`MountainCanvas.kt:351,364` piksel offset + `60.dp` boyut) ve Focus-mode ölçeklemesiyle uyumu (kutular `graphicsLayer` dışında).

**#26 / #28 (BROKEN)** — Tek kök neden zinciri: servise PAUSE/RESUME yok; `onDestroy` süreyi sıfırlıyor; UI Stop'ta süreyi atıyor; `logPages` süreyi hiç almıyor. Düzeltme: servis `ACTION_PAUSE/RESUME`, `isPaused` + `activeBookId` StateFlow'ları, sıfırlamayı yalnız ACTION_STOP/finish'te; `SystemClock.elapsedRealtime` tabanlı süre; VM'de `pendingTimeMillis` → `logPages(timeSpentMillis)`; Stop düğmesi pause'da da görünür; widget'ta `bookId` eşleşmesi. Kayan/genişleyen UI için `AnimatedVisibility`/`animateContentSize`.

**#29 (PARTIAL)** — Streak hesabı `UserPreferences`/repository'ye taşınıp `dataStore.edit{}` içinde atomik yapılmalı; gösterim `lastReadDate < dün` ise 0 (türetilmiş); geçmiş-gün loglamada zincir birleştirme (distinct gün listesinden türetme en sağlamı); gelecek tarihe log engeli (`CalendarStrip.kt:42-44` 14 gün ileri izin veriyor).

**#33 (PARTIAL)** — Metin kartı eklenmeli (`"Congratulations! You have successfully conquered this summit and added the $title Relic to your profile showcase."`); relic emojisi repository'de seçilip kaydedilmeli, animasyon onu göstermeli.

**#34 (PARTIAL)** — Kök sorun: tek günlük periyodik iş. Gerekli: son oturumdan "seri sıfırlanma anı" hesabı, ona 6 sa ve 1 sa kala iki ayrı bildirim (OneTimeWork `setInitialDelay`, her loglamada ve uygulama açılışında yeniden planla; seri 0 veya bildirim kapalıysa iptal), `notificationsEnabled` kontrolü, `contentIntent`, izin kontrolü. WorkManager kesin zamanlı değildir (±birkaç dk, Doze'da daha fazla) → Karar 4.

**#38 (PARTIAL)** — Tier adı metni (Novice / Expedition Leader / Elite Alpinist) avatarın altına eklenmeli.

**#39 (PARTIAL)** — Tek sensör (profil ekranı düzeyinde, `LifecycleResumeEffect` ile) + `State<Float>`'ı `graphicsLayer {}` lambdasında okuma; gölge için kart gölgesi/ikinci katman `translationX/Y` ile sensörle ters yöne; olay başına dizi ayırmayı kaldır (RU:Y3).

**#40 (PARTIAL)** — Sheet başına kitap künyesi (yazar, sayfa), tamamlanma tarihi (`relic.dateEarned`), toplam süre (`sessions.sumOf timeSpentMillis`), puan (inceleme boş olsa da) eklenmeli; sessionId'siz notlar için "Other notes" bölümü.

**#41 (PARTIAL)** — Notu oluştururken aktif oturumu (varsa bugünkü oturum / çalışan kronometre oturumu) belirleyip `sessionId` atamak; `linkLooseNotesToSession` tüm bağsız notları değil yalnız oturum tarihini kapsayanları bağlamalı (RD#17).

**#42 (MISSING)** — Karar 6.

**#44 / #46 (PARTIAL)** — Madde madde kalan iş listesi: Y1, Y3, O5, O7 (UI incelemesi); RD#5 (çift loglama/transaction), RD#10 (foto dosyası temizliği, ana thread kopyası). `.catch { emit(…) }` Home/Mountain VM akışlarına ve gerekirse `BookRepository`'ye eklenmeli; Mountain'daki 4 `flatMapLatest` tek `bookId` akışına (rota argümanı #2 sonrası `flatMapLatest` büyük ölçüde gereksizleşir) indirgenmeli.

## 4. Gereksinim dışı ama düzeltilmesi gereken YÜKSEK/ORTA bulgular (iki incelemeden)

| Kaynak | Sorun | Dosya |
|---|---|---|
| RU:Y1 | Focus mode'da sistem geri → alt çubuk kalıcı gizli (BackHandler/onDispose yok) | `S/Navigation.kt:54,74-75,133`; `MountainScreen.kt:112-114` |
| RU:Y3 | Her relic kartı kendi sensör dinleyicisi, her olayda recompose | `RelicShowcase.kt:34`, `SensorUtils.kt` |
| RD#5 / RU:O7 | Çift dokunuşla çift loglama, `logPages` transaction değil, composable scope'ta çalışıyor | `MountainScreen.kt:240-244`, `MountainViewModel.kt:88-125`, `BookRepository.kt:63-106` |
| RD#6 | Sayfa sayısı doğrulama yok; profil toplamı şişer, Int taşabilir | `MountainViewModel.kt:89-90`, `BookRepository.kt:63-89` |
| RD#4 / RU:D3 | `selectedDate` ve takvim "bugün" gece yarısı bayatlar | `MountainViewModel.kt:61`, `CalendarStrip.kt:40` |
| RD#10 / RU:O6 | Not silinince foto dosyası kalır, kopyalama ana thread'de, tam boyut | `FocusModeNoteInput.kt:103-109`, `AddBookDialog.kt:66-72`, `S/data/ImageStorage.kt` |
| RU:O1 / RD#8 | Bildirim anahtarı işlevsiz, izin akışı gerekçesiz | `ProfileScreen.kt:285-293`, `MainActivity.kt:37-59` |
| RU:O2 | `remember` ile durum kaybı (AddBook formu, Logbook diyaloğu, not taslağı) | HomeScreen, AddBookDialog, MountainScreen, FocusModeNoteInput |
| RU:O3 | İç içe Scaffold → çift inset boşluğu | `Navigation.kt:71-113`, ekran Scaffold'ları |
| RU:O4 | Dark tema tutarsızlıkları: anahtar SYSTEM'i yansıtmıyor, tuval sistem temasını izliyor, `enableEdgeToEdge` tema ile güncellenmiyor, `themes.xml` Light, eksik renk rolleri | `ProfileScreen.kt:306`, `MountainCanvas.kt:172`, `MainActivity.kt:61`, `res\values\themes.xml`, `theme\Theme.kt` |
| RU:O5 | Tuval kare başına ağır ayırma (`Random`, 6 `Path`, 5 `Paint`); kare döngüsü boşta da sürüyor | `MountainCanvas.kt:77-84,221-226,285-296,299-344` |
| RU:O9 | Dokunma kutuları piksel/dp karışık, odak ölçeklemesine uymaz, erişilebilirlik yok | `MountainCanvas.kt:347-368` |
| RU:O10 | Yer tutucu başlangıç değerleri ("Climber", LIGHT, boş liste flaşı), kullanıcı adı her tuşta DataStore | `ProfileViewModel.kt:72-85`, `HomeScreen.kt:67,158` |
| RD#18 | `windowSoftInputMode` `<application>` üstünde geçersiz; `imePadding()` yok | `AndroidManifest.xml:14` |
| RD#19 | Ölü kod: `ReadingReminderWorker`, `DataRepository`, `ui\main\*`, `Converters`+Gson, kullanılmayan DAO/repo metotları | ilgili dosyalar |
| Lint | `lintDebug` ~56 NewApi hatası (java.time, minSdk 24, desugaring yok); 3 dosya: `MountainViewModel.kt`, `StreakNotificationWorker.kt`, `MountainCanvas.kt` | `app\build.gradle.kts` (compileOptions `:24-27`), `build\reports\lint-results-debug.txt` |
| Test | Anlamlı test yok (2 şablon test, kullanılmayan sınıfı sınıyor) | `app\src\test\...\MainScreenViewModelTest.kt`, `androidTest\...\MainScreenTest.kt` |

## 5. Çakışmalar ve kullanıcı kararı gereken noktalar

1. **#35 ↔ #28 (ClimbTime tahmini mi gerçek süre mi).** Kod bugün yalnız sayfa×1.5 dk kullanır (`ProfileViewModel.kt:52-55`; gerçek süre `combine`'da `_` ile atılıyor) ve zaten gerçek süre hiç kaydedilmiyor (#28 BROKEN). #28 düzeltilince iki gereksinim çakışır. Seçenekler: (a) yalnız tahmin (gerçek süre yalnız Journey sheet'te); (b) **melez, önerilen**: süresi kayıtlı oturumlar gerçek, kayıtsızlar sayfa×1.5; diyalog metni (#36) buna göre güncellenir; (c) yalnız gerçek süre.
2. **#47 yıkıcı migration.** Geliştirme için kabul, ama bu denetimin önerdiği düzeltmelerin çoğu şema değiştirir (yeni sütunlar, FK/indeks, arşiv alanı) → her değişiklikte tüm kullanıcı verisi (relic, oturum, not) silinir. Öneri: geliştirme boyunca bırak, yayından önce gerçek `Migration`'lara geç; ayrıca `allowBackup=true` + DB'deki mutlak foto yolları (RD#18) kırılgan. `dropAllTables = false` yalnız Room'un bildiği tabloları düşürür.
3. **#20 36 saat ↔ #29/#34 seri sıfırlanma süresi.** Seri takvim günü esaslı; 24 saat/36 saat/6 sa-1 sa kala tanımları tutarsız. Kod 36 saati gün başı damgasından sayıyor. Karar: tek bir "son gerçek oturum zamanı" (damga) saklanıp (a) seri sıfırlanma anı = "son log gününü izleyen günün 24:00'ü" mü, yoksa kayan 24 saat mi; (b) fırtına eşiği buna göre mi hesaplanacak. Öneri: takvim günü esaslı, fırtına seri bozulduktan sonra (≥ bir tam gün atlanınca).
4. **#34 zamanlama doğruluğu.** WorkManager kesin zamanlı değil (Doze'da dakikalar-onlarca dakika sapma). Kabul mü, yoksa `AlarmManager.setAndAllowWhileIdle` (kesin alarm izni gerektirmeyen) mı?
5. **#12 renk tekeri.** Yalnız kitap kapağı rengi mi, yoksa uygulama vurgu rengi de mi? Keyfî renk saklamak şema değişikliği (+Karar 2) ve `AccentColor` kullanan yerlerin (sırt, dağ, relic, tema) ARGB'ye uyarlanması demek. Alternatif: 5 renk yerine ~12-16 renklik genişletilmiş palet (şema değişmez).
6. **#42 "arşiv".** Bugün ne arşiv ne silme UI'ı var. Seçenekler: (a) `isArchived` alanı + Home'da arşivle/geri al + arşivliler ayrı bölüm (geçmiş korunur); (b) yalnız kitap silme eklenir ve silme sırasında oturum/not/relic KORUNUR ya da cascade ile silinir (RD#11 FK önerisiyle ters düşer). Hangisi?
7. **#17 uzuv animasyonu.** Emoji ile mümkün değil. Seçenek: Canvas ile çizilmiş basit çöp-adam dağcı (IDLE/CLIMBING ile kol-bacak salınımı) ya da emoji + bounce ile yetinip gereksinimi gevşetmek.
8. **Lint/desugaring.** İki yol: `isCoreLibraryDesugaringEnabled = true` + `coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:<güncel sürüm>")` (minSdk 24 korunur, önerilen) ya da `minSdk = 26`. Sürüm numarasını doğrulayıp ekle (burada doğrulanmadı).
9. **#28 sayaç kitaba bağlılığı.** Tek global sayaç mı (başka kitapta "X kitabı için çalışıyor" göstergesi + dokunulamaz), yoksa kitap başına ayrı sayaç mı? Öneri: tek global sayaç + bookId.
10. **#26 pause'da foreground bildirimi.** Duraklatınca bildirim "Paused" olarak kalsın mı, kapansın mı (Android 14 FGS kuralları açısından kalması kolay)? Ayrıca `specialUse` FGS Play Console'da açıklama formu gerektirir (RD#13).
11. **#37 "yıllık".** Tamamlanan kitap sayısı yılla sınırlanmalı (relic `dateEarned >= yılbaşı`) — gereksinimin doğal anlamı, bilgi için.

## 6. Düzeltme planı (3-4 toplu iş + ön adım)

Ortak dosyalar (Navigation.kt, HomeScreen.kt, HomeViewModel.kt, MountainViewModel.kt, MountainScreen.kt) birden çok işte geçtiği için önce küçük bir ön adım atılır; sonra 4 iş büyük ölçüde ayrık dosya kümelerinde paralel yürür.

### Adım 0 — Rota sözleşmesi (#2, RU:Y5) — önce, tek işçi, kolay-orta (sonnet-medium)
- İçerik: `@Serializable data class MountainRoute(val bookId: Long)`; `HomeScreen(onNavigateToMountain: (Long) -> Unit)`; `Navigation.kt` `composable<MountainRoute>` içinde `toRoute<MountainRoute>().bookId`; `MountainViewModel.factory(repository, userPreferences, bookId)` — VM bugünkü `activeBookId`-flatMapLatest yerine sabit `bookId` kullanır (4 flatMapLatest kalkar, #46 kısmen). `setActiveBook` yalnız "son açılan" için kalır.
- Dosyalar: `NavigationKeys.kt`, `Navigation.kt` (yalnız `:114-136`), `ui\home\HomeScreen.kt` (yalnız `onBookClick` ×2 ve imza), `ui\home\HomeViewModel.kt` (yalnız `setActiveBook`), `ui\mountain\MountainViewModel.kt` (yalnız ilk 60 satır), `ui\mountain\MountainScreen.kt` (yalnız VM oluşturma satırı).
- Bağımlılık: Diğer 4 iş bunun BİTMESİNDEN sonra başlamalı (aynı satırlara dokunuyorlar).

### İş 1 — Veri / servis / ViewModel çekirdeği — zor (sonnet-high; seri+transaction tasarımı için opus-high düşünülebilir)
- Maddeler: #26 + #28 (servis pause/resume, süre kaydı, bookId bağı, `StopwatchWidget`), #29 (atomik/türetilmiş seri, geçmiş-gün, gelecek-gün engeli), #20 veri tarafı (gerçek son-oturum damgası → `hoursSince`), #34 (6s/1s bildirimler, ayar/izin/intent), #41 (oturum-not bağlama), #10 veri tarafı (`lastActivityMillis`), #46 (`.catch`), RD#5/#6 (transaction, çift-tık, doğrulama, `viewModelScope`), RD#10 (foto temizleme, IO), #33 veri tarafı (relic emojisi kayıt), #42 (karara göre), RU:O10 yer tutucular (`ProfileViewModel`), RD#19 ölü kod temizliği.
- Dosyalar: `services\ReadingTimerService.kt`, `ui\mountain\StopwatchWidget.kt`, `ui\mountain\MountainViewModel.kt`, `data\BookRepository.kt`, `data\db\BookDao.kt`, `data\db\*Entity.kt`, `data\db\AppDatabase.kt` (+`Converters.kt` kaldırma), `data\UserPreferences.kt`, `data\ImageStorage.kt`, `worker\StreakNotificationWorker.kt`, `services\ReadingReminderWorker.kt` (sil veya yeniden kullan), `MainActivity.kt` (worker planlama, izin, `enableEdgeToEdge` stili — RU:O4c), `AndroidManifest.xml`, `ui\profile\ProfileViewModel.kt` (#35 gerçek süre + #37 yıl filtreli sayım), `ui\home\HomeViewModel.kt` (sıra kalıcılığı: tek-sorgu `updateBookOrder`, sıralama tercihini kalıcılama).
- Sözleşme (İş 2/3/4'e): `MountainViewModel.logPages()` non-suspend olur (çağıran `scope.launch { viewModel.logPages() }` derlenmeye devam eder); yeni `fun onSessionStopped(pagesEstimate: Int, timeSpentMillis: Long)`; repository'ye `completedBooksThisYear` akışı; servis `isPaused`/`activeBookId` StateFlow'ları.
- Bağımlılık: Karar 1, 2, 3, 4, 6, 9, 10 gerekir. `MountainScreen.kt` bu işte DEĞİL (İş 2'de).

### İş 2 — Mountain ekranı, tuval ve navigasyon UI — zor (sonnet-high)
- Maddeler: #4 (3D açılma), #14/#18/#19/#20 görsel (uygulama `ThemeMode`'una bağlı gökyüzü, canlı saat, rastgele kayan yıldız, sis, rüzgârla dağılma), #17 (`ClimbingState`, eğim yönü, kronometre durumuna bağlı; Karar 7), #21 (isteğe bağlı sırt boyunca), #22 (milestone diyaloğu + hit-box düzeltmesi), #23 hit-box ölçekleme, #33 kart metni, #26 kayan/genişleyen UI kabuğu, RU:Y1 (BackHandler+onDispose), RU:O3 (inset), RU:O5 (Paint/Path/Random `remember`, yıldız önhesap, `drawWithCache`), RU:O9 (a11y), RU:O2 (Mountain tarafı `rememberSaveable`), RU:D3 (ParticleEngine `dt`, takvim "bugün" yenileme).
- Dosyalar: `Navigation.kt` (geri kalan kısım), `ui\mountain\MountainScreen.kt`, `MountainCanvas.kt`, `SummitConqueredAnimation.kt`, `SummitLogbookDialog.kt`, `CalendarStrip.kt`, `PageInputCard.kt`, `ParticleEngine.kt`, `FocusModeNoteInput.kt`, `NoteCard.kt`, `PolaroidPhoto.kt`.
- Bağımlılık: Adım 0 sonrası; `onStopSession` lambda'sı için İş 1'in `onSessionStopped` sözleşmesi (tek satır, iş sonunda birleşirken eklenebilir). #20 doğru çalışması için İş 1'in gerçek-damga akışı gerekir.

### İş 3 — Ana ekran UI — orta (sonnet-medium; #11 için sonnet-high)
- Maddeler: #10 (UI/kalıcılık; veri tarafı İş 1), #11 (sürükle-bırak düzeltme), #12 (palet/renk seçici; Karar 5), #13 (altın folyo parıltısı), #7 ("Read" etiketi), #9 (tek Coil model yardımcısı), RU:O2 (AddBook formu `rememberSaveable`), RU:O6 UI tarafı (kopyalama IO), RU:O10 (boş liste flaşı, `isListView` başlangıcı), RU:D1 a11y (renk daireleri), Karar 6'ya göre arşiv UI'ı (#42).
- Dosyalar: `ui\home\HomeScreen.kt`, `BookListLayout.kt`, `BookSpineItem.kt`, `BookshelfLayout.kt`, `AddBookDialog.kt`, `theme\Color.kt` (yalnız palet genişlerse).
- Bağımlılık: Adım 0 sonrası. #12 keyfî renk seçilirse `BookEntity` sütunu İş 1'den gelir; #10 sıralamanın gerçekten çalışması İş 1'in `lastActivityMillis` düzeltmesine bağlı.

### İş 4 — Profil / Relic / tema / build — orta (sonnet-medium)
- Maddeler: #38 (tier adları), #39 + RU:Y3 (tek sensör, gölge, lifecycle), #40 (Journey sheet içeriği), #36 (metni Karar 1'e göre), #37 UI (İş 1'in yıl filtreli akışı), RU:O4 (dark anahtar SYSTEM seçeneği, `Theme.kt` renk rolleri, `themes.xml` DayNight), RU:O10 profil yer tutucuları, lint desugaring (Karar 8), anlamlı birim testler (streak/logPages/sıralama — İş 1 bitince), şablon artıklarının temizliği (`ui\main\*`, `data\DataRepository.kt`, şablon testler).
- Dosyalar: `ui\profile\ProfileScreen.kt`, `RelicShowcase.kt`, `SensorUtils.kt`, `JourneyTimelineSheet.kt`, `theme\Theme.kt`, `res\values\themes.xml`, `app\build.gradle.kts`, `gradle\libs.versions.toml`, `app\src\test\**`, `app\src\androidTest\**`, `ui\main\*`, `data\DataRepository.kt`.
- Bağımlılık: Yalnız #37/#35'in veri akışları için İş 1; geri kalanı bağımsız. Desugaring tek başına ilk yapılabilecek, çakışmasız iştir (isteyen ayrı dal/commit ile hemen yapabilir).

### Çakışma matrisi (dokunulan dosya sahipliği)
| Dosya | Sahibi |
|---|---|
| `NavigationKeys.kt` | Adım 0 |
| `Navigation.kt` | Adım 0 → İş 2 |
| `HomeScreen.kt`, `ui\home\*` | Adım 0 → İş 3 |
| `HomeViewModel.kt` | Adım 0 → İş 1 |
| `MountainViewModel.kt`, `data\*`, `services\*`, `worker\*`, `MainActivity.kt`, manifest, `StopwatchWidget.kt`, `ProfileViewModel.kt` | Adım 0 → İş 1 |
| `ui\mountain\*` (StopwatchWidget hariç) | İş 2 |
| `ui\profile\*` (ProfileViewModel hariç), `theme\Theme.kt`, `themes.xml`, build dosyaları, testler | İş 4 |

Önerilen sıra: Adım 0 → (İş 1 ‖ İş 3 ‖ İş 4) → İş 2 (İş 1'in sözleşmesine dayandığı için sonda ya da paralel ama en son birleştirilir) → entegrasyon derleme + `lintDebug` + `testDebugUnitTest`.
