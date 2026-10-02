# Profil ekranı yeniden tasarım notları

Kapsam: yalnızca `ui/profile/*` (+ ProfileLogicTest). Tasarım sistemi bileşenleri (`ui/components`) olduğu gibi kullanıldı.

## Ne değişti
- `LargeTitleScaffold("Profile")`; tek `LazyColumn` korundu (#43). Alt boşluk: scaffold padding + `LocalBottomBarHeight` (yüzen cam tab bar için).
- **Hero**: yumuşak daire içinde avatar (tier'e göre renk: Novice nötr, Expedition Leader accent, Elite Alpinist altın), kullanıcı adı (Title2), tier adı tint'li kapsül, "N more books to ..." + gün serisi. Zamanlayıcı çalışırken avatar nefes alma animasyonu korundu.
- **İstatistik**: 2x2 `StatTile` (Books Completed, Total Pages Read, ClimbTime, Current Streak). ClimbTime kutusu dokununca `AppAlertDialog` açar (1.5 dk/sayfa açıklaması). Kutuda kompakt değer ("3d 4h"), tam metin ("N Days, M Hours Climbed") ve "Timed sessions: ..." dipnot satırında; erişilebilirlik açıklamasında tam metin var.
- **Yıllık hedef**: `ProgressRing` + "X of Y books in 2026"; düzenleme `AppSheet` içinde (-/+ adımlayıcı + sayı alanı + Save). Hedef tamamlanınca halka altın olur.
- **Relic vitrini**: 2 sütun madalya kartları, altın çerçeve/disk; tek paylaşılan tilt sensörü, gölge ve parlama (shimmer) mantığı aynen korundu. Karanlık temada siyah gölge görünmediği için yumuşak altın parıltı kullanıldı. Basınca ölçek + hafif haptic.
- **Journey**: `AppSheet`; özet kartı (tamamlanma tarihi altın), Summit Logbook kartı, dikey ray üzerinde seans kartları (notlar iç içe), tamamlayan son seans altın nokta.
- **Ayarlar**: iOS Settings tarzı `InsetGroupedSection`lar: Appearance (Theme `SegmentedControl` System/Light/Dark, Accent satırı swatch'larla), Notifications (`AppSwitch` satırı), Account (kullanıcı adı `AppTextField`, 500 ms debounce + ayrılırken kayıt korundu).
- **QA hatası (koyu temada Appearance alt bandı)**: eski `Card` + `ListItem` karışımı iki farklı container rengi çiziyordu. Artık her bölüm tek `surface` dolgulu tek bir `InsetGroupedSection`; bant yok.

## Total Pages Read hatası (1422 vs 1406)
- Kaynak: `BookDao.getTotalPagesRead()` = `SUM(pagesRead)` **seans** tablosundan. Kitap ilerlemesi ise `books.currentPages`. Eski (Haziran) verilerde bir kitabın seans toplamı `totalPages`'i aşabiliyor / düzeltmeler seanslara yansımamış olabilir; fark 1422 - 1406 = 16 sayfa. (Hangi kitaptan geldiği emülatör verisine bakmadan doğrulanmadı; neden varsayımdır, formülün farkı yarattığı kesindir.)
- Karar: "gerçekten okunan sayfa" = tüm kitapların `currentPages` toplamı, her biri `0..totalPages` aralığında kısıtlı. Arşivlenmiş kitaplar dahil (`allBooks` arşivleri gizlediği için `allBooks + archivedBooks`).
- Düzeltme: `ProfileLogic.pagesRead(books)` + `ProfileViewModel.totalPagesRead` (combine). ClimbTime artık bu sayıdan türetilir (tutarlılık). `data/*` değiştirilmedi; `BookDao.getTotalPagesRead` artık profilde kullanılmıyor (başka yerde kullanılıyorsa orkestratör kontrol etsin).
- Test: `ProfileLogicTest.pagesReadSumsBookProgressClamped` (162+1234+10 = 1406, taşan/negatif değerler kısıtlanır), `compactClimbTime`.

## Dosyalar
`ProfileScreen.kt` (ekran, istatistik, hedef kartı), `ProfileParts.kt` (hero, avatar, hedef sheet, ayarlar, swatch), `RelicShowcase.kt`, `JourneyTimelineSheet.kt`, `ProfileLogic.kt`, `ProfileViewModel.kt` (+ `currentStreak`).

## Orkestratör için notlar
- Bileşenlerde eksik bulunmadı. `StatTile` `onClickLabel` parametresi almıyor (tıklama etiketi yerine contentDescription kullanıldı); eklenirse ClimbTime kutusu için faydalı olur.
- `AppSheet` `@OptIn(ExperimentalMaterial3Api)` istiyor (çağıran tarafta opt-in gerekiyor); bileşen içinde `@OptIn` yayılımı kapatılırsa çağıranlar sadeleşir.
- Metinler önceki gibi İngilizce sabit string (ekran içi); `strings.xml` değiştirilmedi (birleştirme çakışması olmasın diye).
- Emülatör/ekran görüntüsü kontrolü yapılmadı (kural gereği emülatör açılmadı); görsel doğrulama orkestratöre kalır.
