# QA-REDESIGN (görsel QA, emülatör Pixel_9a / Android 15, c264c59)

Ekran görüntüleri: `C:\Users\Efe\AppData\Local\Temp\claude\C--Users-Efe-Desktop-Proje-B-Kitap\db2cc408-1f14-43a7-acd4-7386f9495b0f\scratchpad\qa2\`
Not: Test sırasında efe kitabına 2 sayfa kaydedildi (162 -> 164) ve 12 sn'lik zamanlayıcı seansı atıldı (Discard). Tema Sistem'e geri alındı.

## Crash durumu
- **1 CRASH (YÜKSEK)**: Profile sekmesi, cihaz dik (emülatör varsayılanı acceleration 0:9.81:0) durumdayken açılır açılmaz çöküyor.
  `IllegalArgumentException at android.graphics.LinearGradient.nativeCreate <- RelicShowcase.kt:92 (drawRect, shimmer Brush.linearGradient)`. Log: `qa2\crash.txt`.
  Tekrarlandı (3/3). Ivme ölçer yatay (0:0:9.81) yapılınca çökmüyor => tilt değerine bağlı.
  Öneri: `RelicShowcase.kt:88-101` — dx/dy ve start/end'i `isFinite()` ile ve makul aralığa (coerceIn) kısıtla; start==end ise çizme; shimmer gradient'i drawWithContent içinde her karede Brush yaratmak yerine kısıtlanmış offset ile kur. `SensorUtils.kt:64-77` — NaN/Inf ve |açı| sınırı (ör. pitch/roll coerceIn(-90f,90f)) uygula. (Kök neden kesin doğrulanmadı; ham tilt değerini loglamak gerekir.)
- Mountain/Home akışlarında başka crash yok (`adb logcat -b crash` yalnız yukarıdaki).

## Home
PASS: büyük başlık, raf (açık/koyu), liste (açık/koyu), sort menüsü, arşiv onay diyaloğu (iOS tarzı iki butonlu, güzel), cam sekme çubuğu görünür, içerik altında hiçbir şey gizlenmiyor.
- ISSUE (orta) Add Book sheet ilk açılışta renk çarkı alttan kesik (sheet tam ekran, `add_sheet_light/dark.png`); çark dikey sürüklemeyi yutuyor — çarkın üstünden kaydırınca kaydırma yerine renk değişiyor (`add_sheet_scrolled_light.png`). Düzeltme: çark yüksekliğini/ sheet içeriğini küçült veya çarkı Brightness ile birlikte daha kompakt yap; çarkta yalnız yatay/pointerInput sürükleme yakala, dikey kaydırmayı üst Column'a bırak (Add Book sheet bileşeni, ui/home altı).
- ISSUE (düşük) Add Book sheet tam ekran, üstte status bar'a kadar uzanıyor (detent yok); iOS'ta üstte boşluk kalır.
- ISSUE (düşük) Koyu temada pasif "Add Book" butonu: koyu metin / soluk yeşil, kontrast düşük (`add_sheet_dark.png`).
- ISSUE (düşük) Sort ve satır "..." menüleri düz Material DropdownMenu; gölge dikdörtgen artefaktı (`sort_menu_light.png`), "Archive" menüsü kartla çakışıyor (`book_menu_light.png`). iOS context menu benzeri blur/yuvarlak panel önerilir.
- Doğrulanamadı: büyük başlığın kaydırınca daralması (yalnız 3 kitap var, liste kaymıyor), arşiv listesi (arşivlemek veriyi değiştirirdi), uzun basıp sürükleme.

## Mountain
PASS: tam ekran tuval status bar altında, çift üst inset yok, cam başlık (geri/başlık/seri/notlar), kaydırınca cam çubuğa dönüşüm, takvim şeridi, zamanlayıcı (Start->rakamlar, Pause, Resume, Stop, Suggested chip, Use/Start again/Discard), stepper + +5/+10/+25 + Log Pages, kıvılcım alev yönünde uçuyor (`sparks1_light.png`), tent sheet (`tent_sheet_light.png`), odak modu (sheet, Notes/Characters, sekme çubuğu gizli, Back çıkıyor), alt içerik sekme çubuğunun altında kalmıyor, "Library" re-tap Library köküne dönüyor (`tab_retap_light.png`).
- ISSUE (orta) Kaydırılmış cam başlık sanat üzerinde gürültülü: dağ/yol çizgisi başlık metninin ("Summit Climb") ve ikonların arkasında görünüyor, status bar gri yıkanıyor (`mountain_scrolled1_light.png`, `mountain_scrolled_dark.png`). Öneri: MountainHeader.kt cam çubukta HazeStyle blur/tint opaklığını artır (arka yüzey tint >= 0.85) veya tuval tamamen çıkana dek geçişi geciktir.
- ISSUE (orta) Cam sekme çubuğunun blur'ı zayıf: arkasındaki "Notes / Add note" metni neredeyse okunur şekilde görünüyor (`timer_stopped_light.png`, `mountain_top_light.png`). Öneri: tab bar Haze blurRadius artır (>=24dp) + tint opaklığı.
- ISSUE (orta) Odak modunda status bar ikonları koyu gökyüzünde koyu gri, okunmuyor (`focus_light.png`, açık temada gece gökyüzü). Öneri: odak modunda status bar ikon rengini gökyüzü parlaklığına göre (açık ikon) zorla.
- ISSUE (düşük) Başlık kapsülü/notlar butonu: gri cam üstünde yeşil Notes ikonu ve "Summit Climb" alt başlığı düşük kontrast (`mountain_top_light.png`).
- ISSUE (düşük) Takvim şeridi sol kenarda içerik boşluğu yok ("MON 22" ekran kenarına yapışık), sağda "TUE 30" kesik (kaydırılabilir olduğu için kabul edilebilir; sol contentPadding 16dp ekle).
- ISSUE (düşük) "Suggested: 1 pages" / "Use 1 pages" çoğul hatası (MountainTimer, pluralStringResource).
- ISSUE (düşük) Pasif "Log Pages" ve 00:00:00 rakamları çok soluk (özellikle koyu `mountain_bottom_dark.png`).
- ISSUE (düşük) Notes bölümü boşken boş-durum metni ana sayfada görünmüyor (yalnız odak panelinde var, `focus_light.png`).
- ISSUE (düşük) Tent sheet'te %50 halkası tam dolu yeşil (ulaşıldı ise anlamlı ama yüzde ile çelişkili görünüyor).
- Doğrulanamadı: 3D açılma ortası (adb ile ~200 ms altında yakalanamadı; ilk kare zaten bitmiş), Home <-> Mountain sharedBounds, tamamlama (gold) overlay.

## Profile
(Yalnız ivme ölçer yatayken test edilebildi — bkz. crash.)
PASS: hero, 2x2 stat kutuları, ClimbTime diyaloğu (`climbtime_dialog_light.png`), hedef halkası + düzenleme sheet'i, Journey sheet, inset-grouped ayarlar, segmentli tema, Koyu'da Appearance'ta koyu bant YOK (`profile_bottom_dark.png`), Total Pages Read 1408 (1406 + benim 2).
- ISSUE (düşük) Relic kartlarının sert, kaydırmalı gri/altın gölgesi iOS'tan çok eski (`profile_bottom_light.png`). Yumuşak, blur'lu gölge öneri.
- ISSUE (düşük) Hedef sheet'inde stepper altında etiketsiz yinelenen sayı alanı (`goal_sheet_light.png`).
- ISSUE (düşük) Journey zaman çizelgesinde dikey ray kartlar arasında kopuk/hizasız (`journey_sheet_light.png`).
- ISSUE (düşük) Profil büyük başlığı üstte boş bir bant bırakıyor; kaydırınca cam "Profile" çubuğunda arkadaki blur lekeleri (turuncu/kırmızı) görünüyor.

## Öne çıkan ekran görüntüleri
home_list_dark.png, home_shelf_light.png, mountain_top_light.png, mountain_top_dark.png, profile_top_light.png, profile_top_dark.png

## Düzeltmeler (polish)
Doğrulama: emülatör Pixel_9a, açık + koyu tema, ivme ölçer dik (`0:9.81:0`) iken Profile açık, `adb logcat -b crash` boş. Ekran görüntüleri: `scratchpad\qa3\`.
`.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` -> BUILD SUCCESSFUL, 60 test geçti, lint 0 hata (27 uyarı, çoğu eski bağımlılık/sürüm uyarısı).

| # | Madde | Durum | Not |
|---|---|---|---|
| 1 | Add Book sheet: çark kesik / kaydırmayı yutuyor | DÜZELTİLDİ | Sheet kompakt (kapak 80x120, çark 192dp; width sınırı `widthIn` sırası düzeltildi: önceki 232dp sınırı hiç çalışmıyordu), çark + parlaklık kaydırmasız görünüyor. Çark yalnız disk üstünde basışla başlar; dokunma kayma eşiği aşılana dek hiçbir şey tüketilmez, tek dokunuş renk seçer, disk dışı/köşe kaydırmaya bırakılır. Dikey kaydırma disk üstünde başlarsa renk seçer (bilerek). |
| 2 | Mountain kaydırılmış cam başlık gürültülü | DÜZELTİLDİ | Başlık `GlassThickness.Regular` (blur 40dp, tint %86/%80), noise kapalı, cam çubuk ile kapsüller ilk 1/3 kaydırmada çapraz geçer (`barAlpha`), hairline duruyor. |
| 3 | Cam sekme çubuğu blur zayıf | DÜZELTİLDİ | Malzeme token'ları `Glass.kt` içinde tek yerde (`GlassThickness`: Thin 24dp, Regular 40dp, Thick 48dp); sekme çubuğu ve başlık `Regular` kullanır, kayan yuvarlak biçim korundu. |
| 4 | Odak modunda status bar ikonları | DÜZELTİLDİ | `StatusBarIconsFollowSky`: gökyüzü parlaklığına göre; cam başlığın üstündeyken uygulama temasına göre; odak modu girişinde yeniden uygulanır. Odak modu + gece gökyüzünde beyaz ikon doğrulandı (açık ve koyu). |
| 5 | Relic kart gölgesi aşırı kayıyor | DÜZELTİLDİ | Sert kaydırılmış plaka kaldırıldı; yumuşak elevation gölgesi, kayma en çok 4dp, kart eğimi en çok 10 derece, shimmer kayması kart boyutunun %30'u ile sınırlı; NaN/Inf eğim 0 sayılır. |
| 6a | Pasif "Add Book" kontrastı (koyu) | DÜZELTİLDİ | Pasif PillButton: nötr gri kapsül + secondaryLabel (alfa solması yok); Log Pages, Save vb. hepsi. |
| 6b | Sort / satır menüleri iOS tarzı | DÜZELTİLDİ | Yeni `ui/components/Menu.kt` (`AppMenu`, `AppMenuItem`, `AppMenuDivider`): 14dp yuvarlak, hairline kenar, Inter, seçili = tint tik, sağda ikon (Archive). Eski `HomeDropdownMenu` silindi. Not: popup ayrı pencere olduğu için gerçek blur yok (opak yüzey). |
| 6c | Takvim şeridi sol boşluğu | DÜZELTİLDİ | Şerit sol 16dp'de başlar (bugün 3. sırada), kenarlarda 16dp'lik solma maskesi; kırpık gün parçası görünmez. |
| 6d | "Suggested: 1 pages" | DÜZELTİLDİ | `1 page / n pages` (Suggested, Use, "Only 1 page left"). |
| 6e | Pasif Log Pages / zamanlayıcı rakamları | DÜZELTİLDİ | Boştaki rakamlar `secondaryLabel`; pasif buton yukarıdaki gibi. |
| 6f | Notes boş durumu (dağ ekranı) | DÜZELTİLDİ | "No notes yet — tap Add note to capture a thought." (düğme "Add note" olduğu için "Notes" yerine bu ifade). |
| 6g | Hedef sheet'inde çift sayı alanı | DÜZELTİLDİ | Tek etiketli stepper ("N books this year"), metin alanı kaldırıldı. |
| 6h | Journey zaman çizelgesi rayı kopuk | DÜZELTİLDİ | LazyColumn `spacedBy` kaldırıldı, boşluk öğelerin içinde; ray kartlar arasında sürekli. |
| 7a | AppSheet çağıranda `@OptIn` | DÜZELTİLDİ | `AppSheet` dosya düzeyinde opt-in; `AppSheetState`/`rememberAppSheetState()` sarmalayıcısı (`hide()`), çağıranlardaki tüm opt-in'ler silindi. |
| 7b | AppAlertDialog içerik slotu | DÜZELTİLDİ (kısmen kullanıldı) | `content: (@Composable ColumnScope.() -> Unit)? = null` eklendi. Kamp istatistikleri zaten `AppSheet` + gruplu liste olduğundan (daha uygun) taşınmadı. |
| 7c | IconCircleButton dokunma hedefi | DÜZELTİLDİ | Tüm 48dp alan tıklanır, görünür daire `size`; `reserveTouchTarget = false` ile yerleşim tam `size` kalır (cam başlık satırı 44dp). |
| 7d | StatTile onClickLabel | DÜZELTİLDİ | Parametre eklendi; ClimbTime kutusu "Explain ClimbTime" etiketini kullanır. |
| 8 | Kullanılmayan kod | DÜZELTİLDİ | `BookDao.getTotalPagesRead` + `BookRepository.totalPagesRead`, `HomeDropdownMenu/HomeMenuItem`, `menu_item_selected` ve 17 kullanılmayan string silindi (UnusedResources 19 -> 2: backup/data-extraction xml, manifest'ten kullanılıyor). `AddBookDialog` artık yalnız docs'ta geçiyor (kod kalıntısı yok). |

Bilinen / yapılmayanlar: odak panelinde koyu temada iç kart siyah (kapsam dışı), "Archive menüsü kartla çakışıyor" kontrol edilmedi, Mountain %50 halkası ve büyük başlık/Profile boş bant maddeleri bu turda ele alınmadı.
