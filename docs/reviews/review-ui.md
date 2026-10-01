# SummitRead — UI katmanı kod incelemesi (salt okunur)

Kapsam: Navigation.kt, NavigationKeys.kt, MainActivity.kt (yalnız UI ile ilgili kısım), ui/home, ui/mountain, ui/profile (+SensorUtils), ui/main/MainScreen.kt, theme/*, testler. ViewModel/DB iç mantığı kapsam dışı (yalnız UI'yi etkilediği yerde çapraz referans verildi).
Yol kısaltması: `S/` = `app/src/main/java/com/example/summitread/`.
Doğrulama: her bulgu ilgili kod okunarak teyit edildi. Derleme/test çalıştırılmadı (proje dosyası değiştirmemek için); çalışma zamanı davranışları kod okumaya dayalı çıkarımdır, belirsiz olanlar "(belirsiz)" ile işaretlendi.

---

## 1. Uygulama gerçekte ne yapıyor (özellik özeti)

- **Ana ekran (Home):** Kitap ekleme (başlık, yazar, sayfa sayısı, kapak fotoğrafı seçimi — sistem fotoğraf seçicisi ile, renk teması). İki görünüm: "raf" (4'lü sıralar, ahşap raf, sırt görünümlü kitaplar) ve liste (uzun basıp sürükleyerek sıralama, 5 sıralama seçeneği). Üstte seri (streak) sayacı. Kitaba dokununca "aktif kitap" olarak kaydedilip Mountain ekranına geçilir.
- **Mountain (kitap = dağ) ekranı:** Canvas ile çizilmiş dağ, ilerlemeye göre tırmanan karakter, gün saatine göre gökyüzü/güneş/yıldız/bulut, 36 saatten uzun ara verilince fırtına bulutu, çadır (kilometre taşı) ve not tabelaları. 45 günlük takvim şeridi, kronometre (ön plan servisi + bildirim), sayfa sayısı girişi ve "Log Pages" (kaydedince partikül patlaması ve seri alevine uçuş), notların listesi. "Focus mode": dağ küçülür, alt menü gizlenir; Notlar (metin + sayfa + fotoğraf → polaroid) ve Karakterler sekmeleri. Kitap bitince "Summit Conquered" konfeti animasyonu + "Summit Logbook" (puan/yorum) diyaloğu.
- **Profil:** Avatar (bitirilen kitap sayısına göre emoji), kullanıcı adı, tamamlanan kitap/okunan sayfa istatistikleri, tahmini süre (sayfa × 1,5 dk), yıllık hedef halkası, koyu mod anahtarı, 5 vurgu rengi, bildirim anahtarı, "Relic" (hatıra) vitrini (cihaz eğim sensörüyle 3B eğilen kartlar) ve bir relic'e dokununca kitabın "yolculuk" zaman çizelgesi (oturumlar + notlar).
- **Arka plan:** Günlük periyodik seri hatırlatma bildirimi (WorkManager), okuma kronometresi ön plan servisi.
- Navigasyon: Home ↔ Profile alt çubuğu; Home → Mountain (alt çubuk Mountain'da da görünür, Focus mode'da gizlenir). Tip-güvenli rotalar, SharedTransitionLayout ile kitap→dağ geçişi.
- Şablon artığı (kullanılmayan) kod: `ui/main/MainScreen.kt`, `MainScreenViewModel.kt`, `data/DataRepository.kt` ("Hello Android"), `ReadingReminderWorker` hiçbir yerden çağrılmıyor.
- Eksik/yarım özellikler: kitap silme için arayüz yok (`HomeViewModel.deleteBook` hiç çağrılmıyor; `delete_book` string'i kullanılmıyor); dağdaki tabela ve çadır tıklamaları boş (`TODO`); Notlar yalnız Focus mode'da silinebilir.

---

## 2. Bulgular (ciddiyet sırasıyla)

### YÜKSEK

**Y1. Focus mode'da sistem "geri" tuşu alt çubuğu kalıcı olarak gizli bırakıyor**
- Dosya: `S/Navigation.kt:54` (`isFocusMode` yalnız `remember`), `:74-75` (görünürlük), `:133` (callback); `S/ui/mountain/MountainScreen.kt:112-114`.
- Sorun: Focus mode'da TopAppBar (geri düğmesi) gizli ama sistem geri hareketi çalışır. `BackHandler` yok, ayrıca `LaunchedEffect(isFocusMode)` yalnız değer değişince çalışır; ekran kompozisyondan çıkınca (`DisposableEffect/onDispose`) `onFocusModeChange(false)` çağrılmıyor.
- Senaryo: Mountain → FAB ile Focus mode → sistem geri → Home açılır, `isFocusMode` hâlâ true → alt çubuk yok, Profil'e geçilemez. Mountain'a tekrar girip Focus'u aç-kapa yapmadan düzelmez. (MountainViewModel yeni oluşur, false; ama Navigation'daki bayrak true kalır.)
- Çözüm: Mountain içinde `BackHandler(enabled = isFocusMode) { viewModel.toggleFocusMode() }` + `DisposableEffect(Unit) { onDispose { onFocusModeChange(false) } }`. Daha sağlam: bayrağı Navigation'da değil, mevcut rota + ViewModel/paylaşılan state ile türet.

**Y2. Kronometre: "Pause" aslında durdur+sıfırla; geçen süre hiçbir zaman kaydedilmiyor; sayaç kitaptan bağımsız**
- Dosyalar: `S/ui/mountain/StopwatchWidget.kt:370-379` (Pause → `ACTION_STOP`), `S/services/ReadingTimerService.kt:99-104,155-160` (stop → `stopSelf` → `onDestroy` `_elapsedTime=0`), `S/ui/mountain/MountainScreen.kt:229-231,240-244`.
- Sorun 1: Pause düğmesi servise `ACTION_STOP` yolluyor; servis duruyor ve `onDestroy` süreyi 0'a çekiyor. Kullanıcı mola verince 40 dakikalık süre kaybolur. Gerçek bir "pause" yok.
- Sorun 2: Stop'ta `onStopSession(pagesEstimate, elapsedMillis)` çağrılıyor ama Mountain'da ikinci parametre `_` ile atılıyor ve `viewModel.logPages()` varsayılan `timeSpentMillis = 0` ile çağrılıyor. Yani oturumlara gerçek süre yazılmıyor; JourneyTimeline'daki "Time spent" satırı ve toplam süre gerçek veriden hiç beslenmiyor (ProfileViewModel de süreyi `_` ile yok sayıp sayfa×1,5 kullanıyor — çapraz referans, VM ajanına).
- Sorun 3: Sayaç servis companion'ında global (kitap id tutulmuyor). A kitabı için başlatılıp B kitabına geçilirse B'nin ekranında A'nın süresi akıyor; Stop'taki sayfa tahmini B'nin girişine yazılıyor.
- Çözüm: Servise ayrı PAUSE/RESUME eylemi; süreyi UI'dan `logPages(timeSpentMillis)`'e geçir (`onStopSession` değerini ViewModel'e kaydet); `activeBookId`'yi companion state'e koyup widget'ta `bookId` eşleşmesini kontrol et.

**Y3. Her relic kartı kendi sensör dinleyicisini kaydediyor + her olayda yeniden kompozisyon (pil/performans)**
- Dosyalar: `S/ui/profile/RelicShowcase.kt:34`, `S/ui/profile/SensorUtils.kt:447-505`; kullanım `ProfileScreen.kt:393`.
- Sorun: `RelicCardNode` içinde `rememberGyroscopeSensor()` — N relic = N `registerListener` (SENSOR_DELAY_UI). Dönüş değeri `Pair` olarak kompozisyonda destructure ediliyor (`val (pitch, roll) = ...`), bu yüzden eşik (0,5°) aşıldıkça her kart yeniden kompoze oluyor; her olayda `FloatArray(9)` + `FloatArray(3)` ayrılıyor. Dinleyici yaşam döngüsüne bağlı değil: Profil ekranı kompozisyondaysa ama uygulama arka plandaysa (Activity stop) `DisposableEffect` onDispose çalışmaz, kayıt sürer (Android 9+ arka planda olay gelmez ama kayıt kalır; ön planda ise sensör sürekli açık).
- `unregisterListener` doğru yerde (onDispose) yapılıyor — sızıntı değil, ama ölçek ve yaşam döngüsü sorunu var.
- Çözüm: Tek bir sensör (Profil ekranı düzeyinde), `LifecycleResumeEffect`/`LocalLifecycleOwner` ile yalnız RESUMED iken kayıt; değer `State<Float>` olarak verilip `graphicsLayer { }` lambdasında okunmalı (kompozisyonu tetiklemez); olay başına dizi ayırma yerine önbelleğe alınmış diziler. Düşük-pass filtre.

**Y4. Liste görünümünde sürükle-bırak sıralama ilk yer değişiminden sonra kopuyor**
- Dosya: `S/ui/home/BookListLayout.kt:66` (`forEachIndexed` içinde `key(book.id)` yok), `:81` (`pointerInput(book.id, ...)`).
- Sorun: Öğeler konuma göre eşleşiyor. Liste yer değiştirince aynı konumdaki `pointerInput`'un anahtarı (`book.id`) değişir → sürükleme coroutine'i iptal edilir; `onDragEnd` çağrılmaz. `draggedItemIndex`/`dragOffset` takılı kalır ve `onSaveOrder` hiç çağrılmaz (sıra DB'ye yazılmaz). Not: iptal `onDragCancel`'ı da tetiklemez (coroutine iptali).
- Senaryo: Bir kartı uzun basıp ~100dp aşağı çek → ilk takasta jest kesilir; görsel sıra değişir ama kaydedilmez, ekran yeniden açılınca eski sıra (belirsiz: Compose'un kesin yeniden başlatma davranışı cihazda doğrulanmadı, ama anahtar değişimi kodda net).
- Çözüm: `key(book.id) { ... }` ile sar, `pointerInput(Unit)` kullan ve güncel index'i `rememberUpdatedState`/id ile bul; ya da hazır bir reorderable-LazyColumn kütüphanesi. Ayrıca bütün liste tek `item` içinde (`HomeScreen.kt:180`) — LazyColumn'un tembelliği boşa gidiyor.

**Y5. Aktif kitap yarışı: `MountainRoute` kitap id taşımıyor; navigasyon DataStore yazımından önce oluyor**
- Dosyalar: `S/NavigationKeys.kt:8-9` (`data object MountainRoute`), `S/ui/home/HomeScreen.kt:185-190,212-217`, `S/ui/home/HomeViewModel.kt:88-92`, `S/ui/mountain/MountainViewModel.kt:34-38`, `S/Navigation.kt:121`.
- Sorun: `viewModel.setActiveBook(id)` içinde `viewModelScope.launch { ... }` var — çağrı hemen döner (`scope.launch { setActiveBook; onNavigateToMountain() }` bekleme sağlamıyor). Mountain açılırken `activeBook` önceki kitap (veya ilk karede `null`) olabilir; DataStore yazımı bitince içerik değişir. `activeBook == null` iken ekranda "Select a book…" metni ve `sharedBounds` yok, kitap gelince sonradan ekleniyor → paylaşımlı geçiş animasyonu (kitap→dağ) hedefi geç bulur, büyük olasılıkla bozuk/atlanır (belirsiz). Ayrıca sistem tarafından süreç öldürülüp geri gelinirse ekran "aktif kitap" olarak eski tercihi gösterir (DataStore'a bağımlı gizli global durum).
- Çözüm: `@Serializable data class MountainRoute(val bookId: Long)`; MountainViewModel `SavedStateHandle`/`toRoute()` ile id'yi alsın; "aktif kitap" yalnız son açılanı hatırlama amaçlı kalsın. Geçiş sırasında kitap verisini `null` değil "yükleniyor" olarak ele al.

### ORTA

**O1. Bildirim anahtarı hiçbir şeyi değiştirmiyor; hatırlatma saati kontrolsüz**
- `S/ui/profile/ProfileScreen.kt:345-351` (anahtar), `S/worker/StreakNotificationWorker.kt:225-246` (tercihi okumuyor), `S/MainActivity.kt:53-59` (günlük periyodik iş, sabit saat yok, `KEEP`).
- Anahtar kapalı olsa bile işçi bildirim gönderiyor. Periyodik iş uygulamanın ilk açıldığı saate göre çalışır; "dün okudun, bugün okumadın" koşulu gece saatlerinde de doğru olur → gece 03:00 bildirimi mümkün. İzin istemi (`MainActivity.kt:43-51`) açılışta, gerekçesiz, her açılışta (reddedilene kadar) tekrar ediyor. Çözüm: işçide `notificationsEnabled` kontrolü, `setInitialDelay` ile sabit akşam saati, izni anahtar açılırken iste.

**O2. Rotasyon/süreç ölümünde state kaybı (`remember` yerine `rememberSaveable` gerekenler)**
- `S/ui/home/HomeScreen.kt:70-71` (`showAddDialog`, `showSortMenu`), `S/ui/home/AddBookDialog.kt:58-62` (başlık/yazar/sayfa/kapak/renk), `S/ui/mountain/FocusModeNoteInput.kt:60,98-100,206-208`, `S/ui/mountain/MountainScreen.kt:91` (`showSummitLogbook`), `S/ui/mountain/SummitLogbookDialog.kt:611-612`, `S/ui/profile/ProfileScreen.kt:84,251,260`.
- Senaryo: Kitap ekleme formunu doldururken ekran döner → diyalog kapanır, girilen veri gider; seçilen kapak dosyası `filesDir/images`'e kopyalanmış olarak öksüz kalır. "Summit Conquered" animasyonu bittikten sonra açılan logbook diyaloğu rotasyonda kaybolur (`showSummitAnimation` ViewModel'de, `showSummitLogbook` değil) → kullanıcı puan/yorum yazma fırsatını kaybeder.
- Ek: Notlar ↔ Karakterler sekmesi değişince `if/else` yüzünden `NotesTab` kompozisyondan çıkar; yazılmış not taslağı ve eklenmiş fotoğraf (`:98-100`) silinir.
- Not: `MountainViewModel._pagesInput` rotasyonda korunur ama süreç ölümünde kaybolur (SavedStateHandle yok). `HomeViewModel.sortOption` hiç kalıcı değil (her VM oluşumunda MANUAL).
- Çözüm: `rememberSaveable` (+ gerekirse `Saver`), taslağı sekme dışına (FocusModeNoteInput düzeyine) taşı.

**O3. İç içe Scaffold → çift sistem çubuğu boşluğu**
- `S/Navigation.kt:71-113` (dış Scaffold, `padding(innerPadding)` var ama `consumeWindowInsets` yok); iç Scaffold'lar: `HomeScreen.kt:73`, `MountainScreen.kt:116`, `ProfileScreen.kt:87`.
- `enableEdgeToEdge()` açık; iç Scaffold/TopAppBar kendi `windowInsets`'ini (status bar + nav bar) tekrar uyguluyor → üstte durum çubuğu yüksekliği kadar fazladan boşluk, altta FAB/içerik için fazladan nav-bar boşluğu (görsel; cihazda doğrulanmadı). Çözüm: NavHost'a `.padding(innerPadding).consumeWindowInsets(innerPadding)` veya dış Scaffold'ta `contentWindowInsets = WindowInsets(0)`.

**O4. Koyu tema tutarsızlıkları**
- a) Profil "Dark Mode" anahtarı yalnız `DARK` mı diye bakıyor (`ProfileScreen.kt:306`). Varsayılan `ThemeMode.SYSTEM`; sistem koyuyken uygulama koyu görünür ama anahtar "kapalı" gösterir; ilk dokunuşta `LIGHT` olur, anahtar `SYSTEM` seçeneğini sunmuyor.
- b) MountainCanvas koyu/aydınlık kararını `isSystemInDarkTheme()` ile veriyor (`MountainCanvas.kt:172`), uygulamanın kendi tema ayarını (`ThemeMode`) değil → uygulama koyu, sistem açıkken dağ gündüz renklerinde (ve tersi). `LocalAppThemeState` tanımlı ama hiçbir yerde okunmuyor (`Theme.kt:17`).
- c) `enableEdgeToEdge()` temadan bağımsız bir kez çağrılıyor (`MainActivity.kt:61`): uygulama koyu + sistem açık ise durum çubuğu simgeleri koyu zemin üstünde koyu → görünmez (tersi de). `SystemBarStyle` tema ile güncellenmeli.
- d) `themes.xml`: `android:Theme.Material.Light.NoActionBar` → koyu modda ilk karede beyaz pencere arka planı (flaş); `collectAsState(initial = SYSTEM)` (`MainActivity.kt:63`) kullanıcı DARK seçmişse ilk karede yanlış tema.
- e) `lightColorScheme/darkColorScheme` yalnız birkaç rol veriyor (`Theme.kt:31-56`); `secondaryContainer`, `onSecondaryContainer`, `outline`, `outlineVariant`, `surfaceContainer*` M3 temel mor/lavanta paletinden geliyor. PageInputCard +/- düğmeleri, JourneyTimeline "Final Thoughts" kartı ve NavigationBar seçili göstergesi seçilen vurgu renginden bağımsız mor tonda.
- f) Dağ ilerleme çubukları/kitap sırtı `AccentColor.lightPrimary`'yi koyu modda da kullanıyor (`MountainScreen.kt:176`, `BookSpineItem.kt:80-127`); kontrast düşük olabilir (kozmetik).

**O5. MountainCanvas: sonsuz kare döngüsü ve kare başına ağır ayırma**
- `S/ui/mountain/MountainCanvas.kt:77-84` (sonsuz `withFrameNanos`, odak/ilerleme durumundan bağımsız 60 fps çizim), `:221-226` (her karede `java.util.Random(42)` + 51 yıldız), `:285-296` (6 `Path`), `:299-344` (her karede 5 adet `android.graphics.Paint` + emoji `drawText`), `:101-158` (Path, PathMeasure, notePositions, milestones her yeniden kompozisyonda; `animatedProgress`/odak animasyonları kompozisyonda okunduğundan animasyon süresince her karede yeniden kompoze).
- `animationTime` yalnız çizimde okunduğu için kompozisyon taşması yok (iyi), ama çizim sürekli; Mountain ekranı açıkken (Focus mode dahil) ekran hiç dinlenmiyor → pil. Ekran durma/arka plan: `withFrameNanos` pencere görünmezken durur (Compose saati), yani asıl yük ön plandadır.
- Çözüm: `remember` ile Paint/Path/Random, yıldız konumlarını önceden hesapla, `drawWithCache`, çok sayıda sabit sahneyi bitmap/graphicsLayer önbelleğine al, gerektiğinde (animasyon yokken) kare döngüsünü ~30 fps ile sınırla veya `rememberInfiniteTransition` kullan; `hoursSince...` ve `currentHour` değiştiğinde yeniden hesapla (bugünkü hâl: `LocalTime.now()` kompozisyonda bir kez, uzun açık kalan ekranda saat geçişi görünmez).

**O6. Görsel kopyalama ana iş parçacığında; yetim dosyalar**
- `S/ui/home/AddBookDialog.kt:66-72`, `S/ui/mountain/FocusModeNoteInput.kt:103-109` → `ImageStorage.copyImageToInternalStorage` (dosyayı tam boyutta kopyalıyor, `S/data/ImageStorage.kt:180-201`) `ActivityResult` callback'inde, yani UI thread'inde. 5–10 MB fotoğrafta takılma/ANR riski. Dosya adı `currentTimeMillis` (aynı ms çakışması mümkün), uzantı her zaman `.jpg`. İptal edilen/kaydedilmeyen diyalogda kopya silinmiyor.
- İyi taraf: Fotoğraf için `PickVisualMedia` (sistem seçicisi) kullanılıyor → depolama izni gerekmiyor, doğru yaklaşım.
- Çözüm: `Dispatchers.IO`'da kopyala (ViewModel/`rememberCoroutineScope`), gerekirse yeniden boyutlandır; iptalde dosyayı sil.

**O7. Yazma işlemleri `rememberCoroutineScope` ile (ekran ölürse yarıda kalır)**
- `S/ui/mountain/MountainScreen.kt:109,241-293`: `scope.launch { viewModel.logPages() }` vb. `logPages` içinde önce DB güncellenir, sonra partikül tetiklenir, sonra seri (streak) hesaplanır (`MountainViewModel.kt:88-124`). Rotasyon/geri tuşu anında kompozisyon scope'u iptal edilirse sayfa kaydedilir ama seri güncellenmez/kitap tamamlandı animasyonu atlanır (tutarsız durum). `HomeScreen.kt:228-231` benzer (`addBook` ViewModel'de, bu yüzden sorun yok).
- Çözüm: `logPages`, `addNote` vb. non-suspend olup `viewModelScope.launch` içinde çalışsın.

**O8. Relic emojisi gösterimle kayıt uyuşmuyor**
- `S/ui/mountain/SummitConqueredAnimation.kt:424-425` (UI'da rastgele emoji, `remember { random }`) ↔ `S/data/db/RelicEntity.kt:11` + `BookRepository.kt:96-101` (kayıtlı relic her zaman varsayılan 🏔️). Kullanıcı "🗡️ Relic Earned!" görür, Profil'de 🏔️ bulur. Çözüm: relic emojisini repository'de seç/kaydet, animasyonda onu göster.

**O9. Dağdaki tıklanabilir alanlar: boş işlev + yanlış birim + erişilebilirlik**
- `S/ui/mountain/MountainCanvas.kt:347-368`: `offset { IntOffset(x.toInt() - 30, y.toInt() - 40) }` piksel cinsinden, boyut `60.dp` → yoğunluğa göre dokunma alanı çizilen emojiden kayık. Focus mode'da Canvas `graphicsLayer` ile ölçeklenip kaydırılıyor, ama bindirme kutuları ölçeklenmiyor → dokunma ile görsel uyumsuz. `MountainScreen.kt:199-202`: `onSignpostClick`/`onMilestoneClick` boş (TODO) → dokunuş geri bildirimsiz. Kutularda `contentDescription`/rol yok.
- Not konumları: aynı sayfaya ait birden çok not üst üste biniyor; `note.pageNumber > totalPages` olabilir (sadece `coerceIn`).

**O10. Yer tutucu başlangıç değerleri açılışta yanlış içerik gösteriyor**
- `ProfileViewModel`: `username` başlangıcı `"Climber"` (DataStore varsayılanı `""`, `UserPreferences.kt:64`) → kullanıcı adı alanı önce "Climber", sonra boş/gerçek değer; `themeMode` LIGHT, `annualGoal` 20, `relics = emptyList` → "No summits conquered yet" önce görünüp sonra relic'ler geliyor. Profil sekmesi her girişte yeni ViewModel oluştuğundan (popUpTo+saveState VM'yi korumaz) bu her ziyarette tekrarlanabilir (belirsiz: tam yaşam döngüsü cihazda doğrulanmadı).
- `HomeScreen.kt:158,67`: `allBooks` başlangıç `emptyList` → soğuk açılışta "Your bookshelf is empty" flaşı; `isListView` başlangıç false → liste tercihi olanlarda önce raf görünümü.
- Kullanıcı adı alanı her tuş vuruşunda DataStore'a yazıp değeri asenkron akıştan geri alıyor (`ProfileScreen.kt:135-136`): imleç sıçraması/harf kaybı riski (Compose'da bilinen asenkron TextField sorunu). Çözüm: yerel state + debounce/odak kaybında kaydet.

**O11. Seri (streak) arayüzde bayat gösterilebilir**
- `HomeScreen.kt:68`, `MountainScreen.kt:89`: ham `currentStreak` gösteriliyor; seri yalnız `logPages` anında güncellendiği için 5 gün okumayan kullanıcı eski seriyi görmeye devam eder (hesaplama VM/data tarafı; UI'da `lastReadDate`'e göre 0'a indirgeme yok). Çapraz referans.

### DÜŞÜK

**D1. Erişilebilirlik**
- Renk seçici daireler `clickable` ama `contentDescription`/`role`/seçili durum bilgisi yok (`AddBookDialog.kt:142-163`, `ProfileScreen.kt:317-340`; Profil'de 24dp, hedef 48dp altı).
- `CalendarStrip` hücreleri: seçili/bugün/oturum var durumu yalnız renk/nokta; semantics yok. Mountain Canvas'ın ilerlemesi ekran okuyucuya hiç sunulmuyor. NoteCard silme düğmesi 36dp. `Icons.Default.Edit` FAB'ın açıklaması "Add Note" ama Focus mode açıyor (`MountainScreen.kt:159`). Alt çubuk simgesi `contentDescription` etiketiyle aynı → okuyucu iki kez okur (`Navigation.kt:98`).
- İnce yazı kontrastı: `onSurfaceVariant.copy(alpha=0.6f)` 12sp (`NoteCard.kt:247`).

**D2. Yerelleştirme/sabit metinler**
- `strings.xml`'de 37 giriş var, 10'u hiç kullanılmıyor (`mountain, pages_read_today, log_pages, page_number, characters, settings, summit_conquered, no_relics_yet, delete_book, focus_mode`). Kodda ~70 yerde doğrudan İngilizce sabit metin var (örn. `HomeScreen.kt:77,95-135`, `MountainScreen.kt:121`, `StopwatchWidget.kt:355,378,391,408`, `PageInputCard.kt:480,554,579`, `FocusModeNoteInput.kt:61,140,279`, `ProfileScreen.kt:218-263,374,410-414`, `SummitLogbookDialog.kt`, `JourneyTimelineSheet.kt`, bildirim metinleri). Aynı metin hem `strings.xml`'de hem sabit olarak var (`no_relics_yet` ↔ `ProfileScreen.kt:374`, `log_pages` ↔ `PageInputCard.kt:579`). Türkçe çeviri (values-tr) yok; dil karışımı yok ama tamamen İngilizce + emoji.
- Biçimlendirme: `String.format(Locale.US, ...)`, `"$percent%"`, "p.", çoğul yok ("1 Days"/"Read 1 pages"); `ProfileViewModel` metni de VM'de İngilizce üretiyor.

**D3. Diğer**
- `CalendarStrip.kt:40`: `today` `remember {}` anahtarsız → gece yarısını geçen oturumda "bugün" bayat; Focus mode'dan çıkınca `LaunchedEffect(Unit)` takvimi yeniden "bugün"e kaydırıyor (seçilen güne değil). Takvim yalnız 30 gün geri/14 gün ileri; geleceğe sayfa girilebiliyor (VM'de doğrulama yok).
- `ParticleEngine.kt:438`: sabit `dt = 0.016f` + çizim içinde durum değiştirme → 120 Hz ekranda 2× hızlı; başlangıç koordinatları `positionInWindow` (pencere), çizim yerel koordinat (Scaffold dolgusu kadar kayma). `triggerParticles` zaten çalışırken gelen ikinci tetik yok sayılıyor (`MountainScreen.kt:323`).
- `FocusModeNoteInput`: Karakterler sekmesinde Focus mode'dan çıkış düğmesi yok (yalnız Notlar sekmesinde "Cancel"); IME: manifestte `adjustResize` var ama targetSdk 36 + edge-to-edge'de etkisiz; hiçbir yerde `imePadding()` yok (`grep` ile doğrulandı) → klavye form alanlarını örtebilir (cihazda doğrulanmadı).
- `Modifier.weight(1f)` `AnimatedVisibility` içinde (`MountainScreen.kt:295`) etkisiz.
- Coil modeli tutarsız: `File(path).toURI().toString()` (`AddBookDialog.kt:126`, `BookSpineItem.kt:67`), düz `String` (`BookListLayout.kt:137`), `File.toUri()` (`PolaroidPhoto.kt:343`). Biri bozulursa diğerleri çalışır → tek yardımcıya indir; model olarak doğrudan `File` verilebilir.
- `NoteCard.onSignpostClick` kullanılmıyor; `PolaroidPhoto` her kompozisyonda `ColorMatrix` kuruyor ve tüm fotoğraflara zorunlu sepya uyguluyor.
- `NavigationBar` seçimi: Mountain rotasında iki sekme de seçili değil (`Navigation.kt:85`). `MountainScreen` `onNavigateBack = popBackStack()` çift dokunuşta Home'u da çıkarıp boş ekran bırakabilir (`launchSingleTop` yalnız ileri gitmeyi korur) — düşük olasılık.
- `Navigation.kt:51` `context` parametresi kullanılmıyor, `Icons.Filled.Terrain` içe aktarımı gereksiz. `app/proguard-rules.pro` dosyası yok (`isMinifyEnabled=false` olduğundan şimdilik zararsız; release'te minify açılırsa derleme sorunu).
- Çapraz referans (VM/data ajanı için): `HomeViewModel.updateBookOrder` eski `book` kopyasıyla `updateBook(copy(displayOrder))` yazıyor — arada ilerleme değiştiyse ezer (`HomeViewModel.kt:80-86`).
- Başlatma: `PeriodicWorkRequest` `MainActivity.onCreate`'te her açılışta enqueue (KEEP ile zararsız).

---

## 3. ViewModel oluşturma / yaşam döngüsü değerlendirmesi

- Üç ekran `viewModel(factory = ...)` ile **NavBackStackEntry kapsamında** ViewModel alıyor (`HomeScreen.kt:62`, `MountainScreen.kt:76`, `ProfileScreen.kt:67`); yeniden kompozisyonda yeniden oluşmuyor, rotasyonda korunuyor → **doğru**. Fabrikalar Repository/Preferences'ı `MainActivity`'den taşıyor; Activity'de elle kurulan `BookRepository`/`UserPreferences` (DI yok) rotasyonda yeniden oluşuyor ama durumsuz sarmalayıcılar, DataStore tekil (`preferencesDataStore` delegesi) → sorun yok.
- ViewModel'ler `SavedStateHandle` kullanmıyor → süreç ölümünde `pagesInput`, `selectedDate`, `isFocusMode`, `selectedJourneyBookId`, sıralama kaybolur (aktif kitap DataStore'da olduğu için o korunur).
- Yan etkiler: `LaunchedEffect` kullanımları doğru yerde. `MountainScreen.kt:103-107` SharedFlow toplama `LaunchedEffect(Unit)`'te doğru; `finishedListener` ile alev nabzı doğru. Kompozisyon dışı yan etki bulunmadı (yan etkisi olan `particles` mutasyonu çizim içinde, bkz. D3).
- Sensör kaydı: `DisposableEffect` ile unregister var (bkz. Y3 — sorun sayısı/yaşam döngüsü).
- Fotoğraf seçimi: `PickVisualMedia` → izin gerekmiyor, doğru.

## 4. Testlerin değerlendirmesi

- `app/src/test/.../MainScreenViewModelTest.kt`: Android Studio şablonundan kalma. İki test aynı şeyi yapıyor (`uiState_onItemSaved_isDisplayed` adı yalan: yine `Loading` doğruluyor); `stateIn(WhileSubscribed)` başlangıç değeri zaten `Loading` olduğundan test hiçbir davranışı sınamıyor. Üstelik test edilen sınıf (`MainScreenViewModel`) uygulamada kullanılmayan şablon artığı. Başarılı bile olsa anlamsız; `Dispatchers.Main` ayarı (`Dispatchers.setMain`) yok — `viewModelScope` ile JVM testte çalışması lifecycle sürümüne bağlı (belirsiz, çalıştırılmadı).
- `app/src/androidTest/.../MainScreenTest.kt`: yine şablon; yalnız `Greeting` "Hello Sample1!" metnini arıyor. Gerçek ekranların (Home, Mountain, Profile) hiçbiri test edilmiyor.
- Sonuç: **Anlamlı test yok.** Gerçek mantık (sayfa kaydı/tamamlanma, seri hesabı, relic oluşturma, sıralama, ilerleme yüzdesi, navigasyon, Focus mode çubuk durumu) kapsanmıyor. Öncelikli yazılması gerekenler: `logPages`/streak birim testleri (fake repository + `StandardTestDispatcher`), `HomeViewModel` sıralama, Compose UI testi: Focus mode → geri → alt çubuk geri geliyor mu (Y1), AddBookDialog doğrulaması.

## 5. Öncelikli aksiyon önerisi

1. Y1 (alt çubuk takılması) — küçük düzeltme, yüksek etki.
2. Y2 (kronometre pause/süre kaydı/kitap eşleşmesi) — veri kaybı.
3. Y5 (rota argümanı) — doğru mimari; Y4 (sürükleme) ve Y3 (sensör) ayrı küçük işler.
4. O1, O2, O3, O4 kullanıcıya görünen kalite; sonra O5–O11, D'ler.
