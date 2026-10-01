# Parti 2 (Mountain UI) — notlar

Tarih: 2026-10-01. Dal: worktree dalı (main @ 017fbd4 üzerine). Yalnız `ui\mountain\*`, `Navigation.kt` ve yeni bir test dosyası (`app\src\test\...\ui\mountain\MountainLogicTest.kt`) değişti.
Yol kısaltması: `M/` = `app\src\main\java\com\example\summitread\ui\mountain\`.

## Yeni dosyalar
- `M/MountainLogic.kt` — saf mantık: `MountainTier` (<200 HILL, 200-399 STANDARD, 400+ EVEREST; zirve yüksekliği + yol eğrisi), `SkyModel` (saate göre dawn/day/twilight/midnight geçişli gökyüzü), `WeatherRules` (36 sa eşiği), `MilestoneStats` (çadır ara istatistikleri), `MilestoneQuotes`.
- `M/ClimberPainter.kt` — Canvas'ta çizilen çöp-adam dağcı + `ClimbingState`.
- `M/MilestoneDialog.kt` — çadır diyaloğu + tabela (not) diyaloğu.
- `M/MountainTheme.kt` — `isAppDarkTheme()` (arka plan luminance < 0.5) ve yüksek kontrastlı renkler.
- Test: `MountainLogicTest` (7 test).

## Madde madde
- **#3** Başlık `"[Title] - Summit Climb"` doğrulandı (TopAppBar, `MountainScreen`).
- **#4** `MountainScreen`: `animatedVisibilityScope.transition` ile `rotationY = -80° → 0°` + `cameraDistance = genişlik*1.6`, pivot sol kenar (cilt). `sharedBounds` ile birlikte çalışır. Home dosyalarına dokunulmadı. **Cihazda doğrulanmadı:** dönüş yönü (sağ kenar uzağa mı yakına mı) ve süre hissi.
- **#14** Gökyüzü tüm tuvali kaplar, zemin şeridi `background`+vurgu karışımı; alt köşeler yuvarlak kırpılır. Karanlık/aydınlık kararı `MaterialTheme.colorScheme.background.luminance()`; `isSystemInDarkTheme()` hiçbir yerde kullanılmıyor.
- **#15** `PageInputCard` ve `CalendarStrip` koyu modda beyaz/#E0E0E0 yazı, belirgin kenarlık; açık modda tema renkleri. Gerçek kontrast oranı ölçülmedi.
- **#16** Katmanlar `MountainTier` ile; birim testli. Zirve y oranları 0.52 / 0.34 / 0.20, yol eğrisi de katmana göre değişir.
- **#17** Dağcı Canvas'ta (kafa, gövde, kollar, bacaklar, sırt çantası, baston). PathMeasure tangenti ile rotasyon, dik yamaçta gövde öne eğilir, adım/sekme. `ClimbingState.CLIMBING`: ilerleme animasyonu sürerken (girişte 0'dan, loglayınca eski->yeni ilerleme) VEYA bu kitabın kronometresi çalışırken; aksi halde IDLE (nefes). Uzuv sallanma formülleri sezgisel; görünüm cihazda doğrulanmadı.
- **#18** Gökyüzü 30 sn'de bir `LocalTime.now()` ile yeniden hesaplanır (`produceState`), anahtar karelerle yumuşak geçiş. Koyu tema geceyi zorlamaz; yalnız gündüz gökyüzünü %28 karartır.
- **#19** Gündüz nabız atan güneş (yay üzerinde saate göre konum), gece ay, yıldızlar (60 adet, sabit tohumlu konum, rastgele faz) kırpışır; kayan yıldız `ShootingStar` durum makinesiyle 3-11 sn aralıkla RASTGELE konum/açıda.
- **#20** Fırtına: `hoursSinceLastSession > 36` -> gökyüzü kararır, zirvede 3 fırtına bulutu + sis bantları (2.5 sn'de belirir). Sayfa girilince (`hours` düşünce) rüzgâr animasyonu: bulutlar sağa süpürülür, rüzgâr çizgileri, 2.6 sn'de solar. Seri aktifken bulutlar daha az opak.
- **#21** 5 bulut sürekli yatay kayar (süreksiz sıçrama yok, sarma ekran dışında).
- **#22** Çadırlar (25/50/75) tıklanır (48dp, `contentDescription`), `MilestoneDialog`: kamp adı, motivasyon cümlesi (kitap id'sine göre sabit), ulaşıldı/ulaşılmadı, sayfa, tarih, seans sayısı, süre. Tabela tıklayınca not metni diyaloğu. Dokunma kutuları aynı zoom katmanında olduğu için ölçeklemeyle uyumlu; odak modunda devre dışı.
- **#23** Odak modu: `graphicsLayer` scale 1.7 + pivot dağcı + dağcıyı merkeze çeken, kenar boşluğu olmayacak şekilde kırpılmış translation; dağcı ⛺'e dönüşür (crossfade).
- **#24/#25** Ağaçlar ucu yukarı (apex < taban); dağcı konumu tuval içine kırpılır, `atan2`->derece, rotasyon ayağın bulunduğu noktada. Cihazda görsel doğrulama yok.
- **#26** `StopwatchWidget`: boşta kompakt "Start"; çalışırken/duraklatılmışken/bekleyen oturum varken kart genişler (`animateContentSize`), Pause/Resume/Stop düğmeleri kayarak girer; çalışırken nabız atan nokta; bekleyen süre + "Suggested: N pages", "Use N pages", "Start again", "Discard". Başka kitaba bağlıysa "Timer running for another book". Servis API'si: `start/pause/resume/stop(context...)`.
- **#31** `ParticleBurstOverlay`: parçacıklar `PageInputCard` üstünden çıkıp patlar, sonra alev ikonuna (`positionInWindow` -> overlay yerel koordinatı) kavisli uçar; ilerlemenin saf fonksiyonu (kare hızından bağımsız); ~1.05 sn'de çarpma -> alev lub-dub heartbeat (1.5x/1.3x). Yeniden tetikleme artık yok sayılmaz.
- **#33/#32** Kart metni `completedSummit.message` birebir; relic emojisi `completedSummit.relicEmoji`. Dokununca Logbook diyaloğu (VM durumundan türetilir: `completedSummit != null && !showSummitAnimation`, dönmeye dayanıklı); Skip -> `clearCompletedSummit()`, Sign -> `saveSummitLogbook`.
- **Bug: Focus/Back** `BackHandler(isFocusMode)` + `DisposableEffect.onDispose { onFocusModeChange(false) }`; ayrıca `Navigation.kt` alt çubuğu `isFocusMode && onMountain` ile gizler (bayat bayrak Home/Profile'da çubuğu gizleyemez).
- **Bug: çift inset** `Navigation.kt`: `padding(innerPadding).consumeWindowInsets(innerPadding)`. Mountain'da ayrıca `imePadding()`.
- **Bug: durum kaybı** Not/karakter taslakları ve sekme `FocusModeNoteInput` düzeyinde `rememberSaveable` (sekme değiştirince/döndürünce korunur); Logbook puan/inceleme `rememberSaveable`; çadır/tabela diyalogları `rememberSaveable`. Karakterler sekmesine çıkış (X) eklendi.
- **#45** Tek `withFrameNanos` saati yalnız çizimde okunur; Path/Brush/Paint/PathEffect/Random verisi `remember`; çizimde ayırma yok (dağcı `canvas.save/translate/rotate`, `Offset` value class). Parçacık konumları `WindowPoint` (state değil) -> kaydırmada recomposition yok. Kronometre durumu `distinctUntilChanged` akışı (saniyelik recomposition yok).
- **Diğer:** `CalendarStrip(today=VM.today)`, gelecek günler pasif; `PageInputCard.isLogging`; `activeBook == null` iken spinner (1.2 sn sonra "no book" metni); `AutoMirrored` geri oku; `TabRow` -> `PrimaryTabRow`.

## Doğrulanmamış / riskler (cihazda görülmedi)
- Çalıştırma denemesi: `medium_phone` emülatörü bu oturumda açılmadı (adb "offline", ~10 dk), bu yüzden HİÇBİR görsel/çalışma zamanı doğrulaması yapılmadı; yalnız derleme, lint ve birim testler.
- 3D açılma yönü/hissi, dağcının duruşu ve uzuv animasyonu, sis/fırtına/rüzgâr görünümü, yıldız/kayan yıldız yoğunluğu, odak zoom kadrajı, çadır dokunma kutularının emojilerle hizası.
- Emoji çizimi (🚩 ⛺ 🪧) cihaz yazı tipine bağlı.
- `rotationY` + `sharedBounds` birlikte: sharedBounds yer paylaşımı sırasında içerik katmanı bağımsız döner; çakışma/titreme olabilir.
- Girişte dağcı 0'dan başlayıp tırmanır (her ekran girişinde ~1-3 sn); istenmezse `MountainCanvas` içindeki `climb` başlangıcı hedefe eşitlenebilir.
- Fırtına eşiği VM'in `hoursSinceLastSession` değerine bağlı (dakikada bir yenilenir).
- Perf ölçümü (profiler) yapılmadı; yalnız kod düzeyinde ayırmalar kaldırıldı.

## Birleştirmede dikkat
- `Navigation.kt` içindeki `consumeWindowInsets` Home/Profile Scaffold'larını da etkiler (çift inset düzelir); onlarda kendi inset telafisi yapılmışsa kontrol edin.
- Başka dosya/veri katmanı değişikliği gerekmedi.
