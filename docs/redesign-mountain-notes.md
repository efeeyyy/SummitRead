# Mountain ekranı yeniden tasarımı (iOS 26/27 hissi) — notlar

Tarih: 2026-10-02. Kapsam: yalnız `app/src/main/java/com/example/summitread/ui/mountain/*`. `ui/components`, `theme`, `Navigation.kt`, `ui/home`, `ui/profile`, `data`, `services` dokunulmadı. `MountainScreen`'in genel parametreleri aynı (bookId tabanlı VM, `sharedTransitionScope`, `animatedVisibilityScope`, `onFocusModeChange`, `onNavigateBack`).

Doğrulama: `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain` geçti (lint 0 hata). **Emülatör açılmadı; hiçbir görsel/çalışma zamanı doğrulaması yapılmadı** (aşağıdaki "Görsel olarak kontrol edin" listesi bu yüzden önemli).

## Ne değişti

**Düzen (`MountainScreen.kt`, yeni `MountainHeader.kt`)**
- `Scaffold` + `TopAppBar` + FAB kalktı. Tuval durum çubuğunun altına kadar tam ekran (full-bleed). Tuval ve bölümler tek bir kaydırılabilir `Column` içinde; tuval kaydırınca yukarı çıkar.
- Üst başlık "yüzen cam" (Haze): geri oku (cam daire), başlık kapsülü (`[Kitap adı]` + alt başlık "Summit Climb"; erişilebilirlik metni birebir `"[Kitap] - Summit Climb"`), seri hapı (alev + sayı) ve Notes (EditNote) butonu. Tuval ekrandan çıkınca kontroller tek bir tam genişlik cam çubuğa + ince çizgiye dönüşür (durum çubuğu altında içerik görünmez).
- FAB kaldırıldı. Notes: başlıkta cam buton + içerikte "Notes · N" bölüm başlığı yanında tinted "Add note" PillButton.
- Alt boşluk `LocalBottomBarHeight` (yüzen sekme çubuğu) ve `navigationBars` insetinden hesaplanır; klavye açıkken sıkı.
- Durum çubuğu ikonları gökyüzünün parlaklığına göre açık/koyu yapılır, ekrandan çıkınca eski değere döner.
- Kıvılcım vuruşunda `haptics.logged()` (kayıt gerçekten kaydedildiğinde); "Log Pages" PillButton'u çift titreşim olmasın diye `haptic = false`.

**Tuval (`MountainCanvas.kt`, `MountainLogic.kt`, `ClimberPainter.kt`)**
- Pembe/mor zemin bandı kalktı: yerine alçak, vurgu renginden türetilen yuvarlak "foothills" (`MountainGeometry.foreground`), vadi sisi (dikey gradyan), dağın ışık almayan yüzü için yatay gölge gradyanı. Alt köşeler 28dp yuvarlak.
- Gökyüzü paleti rafine edildi (`SkyModel`: serin tepe, sıcak ufuk). Güneş çok katmanlı yumuşak hale, ay hilal. Bulutlar biraz daha şeffaf.
- Dağcı 32dp → 42dp, uzuvlar kalınlaştı, ayak altına yumuşak gölge. Tırmanılan yol hafif parlama ile çiziliyor.
- `topInset`: gökyüzü/yıldız/bulut başlığın altına da uzanır, dağ sahnesi başlığın altından başlar (zirve başlıkla çakışmaz). Dokunma kutuları aynı kayma ile hizalı.
- Odak modunda tuval yeniden boyutlanmıyor (eski kodda her karede geometri yeniden kuruluyordu); onun yerine not sheet'i tuvalin altına biner ve zoom odağı `FOCUS_FOCAL_Y` (0.40) ile sheet'in üstündeki bantta kalır.
- Performans kuralları korundu: tek `withFrameNanos` saati çizimde okunur, Path/Brush/Paint önceden `remember`, çizimde ayırma yok.

**Takvim şeridi:** iOS gün hapları: seçili gün dolu tint daire, bugün yumuşak tint disk + tint gün adı, oturum olan günde nokta, gelecek günler pasif (soluk). Seçince hafif haptic, press-scale.

**Zamanlayıcı:** `CardContainer`, 48sp tabular (`tnum`) rakamlar, Start / Pause / Resume / Stop `PillButton`'ları, çalışırken genişleyen halkalı canlı nokta, bekleyen süre için tinted "Suggested: N pages" çipi + "Use N pages" / "Start again" / "Discard". Başka kitaba bağlıysa mesaj, kontrol yok.

**Sayfa girişi:** stepper tarzı: büyük ortalı sayı (Large Title boyutu, elle yazılabilir), −/+ `IconCircleButton`, +5/+10/+25 çipleri, `LinearProgressCapsule` + "X / Y pages · Z%" (100%'de altın), kalan sayfadan fazla yazılırsa uyarı, yalnız geçerken aktif dolu "Log Pages".

**Notlar / odak paneli:** `FocusModeNoteInput` bir sheet gibi (28dp üst köşe, opak yüzey) tuvalin altına biner; `SegmentedControl` Notes/Characters, `AppTextField` girişler, sayfa/foto satırı, Save (tam genişlik). Notlar `CardContainer` + sayfa çipi + hafif eğik polaroid; karakterler kart + rol çipi. "Cancel" kaldırıldı (X zaten odak modundan çıkar). Boş durum metinleri eklendi.

**Sheet / diyalog / overlay:** `MilestoneDialog` → `AppSheet` (ProgressRing + söz + gruplu istatistik satırları + "Keep climbing"), `NoteSignpostDialog` → `AppSheet` + `NoteCard`, `SummitLogbookDialog` → `AppSheet` (5 yıldız altın seçici, `AppTextField`, Sign Logbook / Skip). `SummitConqueredAnimation`: koyu perde, altın parıltılı kupa, altın "Summit Conquered!" başlığı, sade altın/beyaz konfeti, `haptics.success()`; tebrik metni `completedSummit.message` **birebir**.

## Görsel olarak kontrol edin (açık/koyu × 5 vurgu, 3 dağ boyutu)
1. Başlıktaki cam kapsüllerin gökyüzü üzerinde okunabilirliği (özellikle gece + açık tema ve gündüz + koyu tema); uzun kitap adı kısalması.
2. Tuval kaydırınca cam çubuğa geçiş, durum çubuğu ikon renginin gökyüzüne uyumu (özellikle şafak/alacakaranlık).
3. Yeni foothills + sis + gölge gradyanı doğal mı; ağaçların foothills üzerindeki oturuşu; Everest zirvesinin başlığa yakınlığı.
4. Odak modu: sheet tuvalin %62'sinden başlar; ⛺ ve zoom kadrajı sheet'in üstünde görünüyor mu; klavye açılınca sheet.
5. `LocalBottomBarHeight` ile alt boşluk (Home ajanının sekme çubuğu birleşince); odak modunda çubuk gizlenirken sheet alt boşluğu (yalnız navigationBars kullanır).
6. 3D açılma + `sharedBounds` + yeni düzen birlikte (önceki notlardaki risk sürüyor).
7. Büyük yazı boyutunda timer rakamları (48sp) ve stepper sayısı (52sp) taşması.
8. Dağcı büyüklüğü/duruşu; tırmanış parlaması.
9. Kayıt sonrası haptic + kıvılcımlar alev hapına uçuyor mu (hedef artık başlık hapındaki alev).

## Bilinen sınırlar / kararlar
- `AppSheet` içinde `SheetState` imzada olduğu için çağıran dosyalara `@OptIn(ExperimentalMaterial3Api::class)` eklendi (ui/components'e dokunulmadı; orada `AppSheet`'e opt-in yayılımı eklenirse kaldırılabilir).
- Milestone için `AppAlertDialog` yerine `AppSheet` kullanıldı: istatistik satırları gruplu liste gerektiriyor (AlertDialog yalnız düz metin mesajı alıyor).
- Tuval kaydırılabilir `Column` içinde tutuldu (LazyColumn değil): LazyColumn tuvali ekrandan çıkınca yok edip tırmanış animasyonunu yeniden başlatırdı.
- Kullanılmayan `MountainContrast` kaldırıldı; `isAppDarkTheme()` artık `AppTheme.isDark`.
- Yeni stringler `strings.xml`'e eklenmedi (dosya sahipliği); mevcut Mountain koduyla aynı şekilde sabit İngilizce metin, yalnız eskiden `R.string` olanlar korundu.
- Polaroid kâğıt rengi bilinçli olarak sabit beyazımsı (fiziksel baskı); konfeti/ay/güneş rengi sanat öğesi olarak MountainTheme/sahne içinde kaldı.
