# Parti 3 — Home ekranı UI notları

Dal: `worktree-agent-ad1322fd6abafff2a` (main @ 017fbd4'ten). Yalnız `S/ui/home/*` + testler + bu not değişti.
Yol kısaltması: `S/` = `app\src\main\java\com\example\summitread\`.

## Madde madde

- **#5 Shelf/List geçişi:** Düğme duruyor (`HomeScreen.kt`); ikon/açıklama durumu gösteriyor ("Switch to list view" / "Switch to shelf view"). Liste/raf tercihi DataStore'dan gelene kadar içerik çizilmiyor (yanlış görünümün ve "empty" yazısının flaşı kalktı: `HomeViewModel.booksLoaded` + `isListView` başlangıç `null`).
- **#6 Ahşap raf:** Tasarım korundu. Raf gölgesi (`shadow(4.dp)`) kitap sırtlarının altını eziyordu; kitap satırı `zIndex(1f)` ile rafın üstüne alındı (`BookshelfLayout.kt`). Sırt boyu 150..219 dp, genişlik 54..73 dp (4 kitap 320 dp'ye sığar).
- **#7 İlerleme çubuğu:** Sırtın altında 4 dp çubuk + üstünde "34% Read" etiketi. Sırt yüzünde bu iki öğe için 26 dp ayrılmış alan var; başlık metni bununla çakışmıyor. Etiket `maxLines=1, softWrap=false`.
- **#8 Dikey metin taşması:** Eski `graphicsLayer rotationZ` + `requiredWidth` hilesi yerine özel `verticalText()` layout modifier'ı (`BookSpineItem.kt`): metne dönüşten sonra kullanılabilir yükseklik kadar genişlik verilir, `maxLines=1` + `Ellipsis`. 100% kitaplarda mühür alanı kadar üst boşluk bırakılır.
- **#9 Liste kapağı:** Tek yardımcı `CoverImage` (`BookCover.kt`): yol her zaman `File(path)` olarak verilir (Coil 3 `FileMapper`; düz yol string'i güvenilir değil, `toURI()` biçimi de tutarsızdı). `SubcomposeAsyncImage` yüklenirken ve hata olursa vektörel kapağı (`VectorCover`: renk gradyanı + kitap ikonu) gösterir. Raf sırtı, liste, arşiv listesi ve Add Book önizlemesi aynı yardımcıyı kullanır.
- **#10 Sıralama menüsü:** 5 seçenek (`SortOption.label`), seçili olanda onay işareti, `HomeViewModel.sortOption` (kalıcı) kullanılıyor. Sıralama saf fonksiyona çıkarıldı: `sortBooks` (`HomeSort.kt`), birim testli. Menü yalnız liste görünümünde görünür (gereksinime uygun); seçilen sıra raf görünümüne de yansır.
- **#11 Sürükle-bırak:** `BookListLayout` artık gerçek `LazyColumn` (`items(key = { it.id })`). Jest listenin kendisinde, `pointerInput(Unit)` ile (yeniden başlamaz); sürüklenen kitap **id** ile izlenir, sıra `localOrder` (id listesi) olarak tutulur. Uzun basınca başlar, `Initial` geçişinde dinlenir (kaydırma jesti çalamaz, bırakma olayı tüketilir -> sürüklenen karta yanlışlıkla tıklama olmaz). Her karede (yalnız sürüklerken) altındaki kartla yer değiştirme + kenarlarda otomatik kaydırma. Bırakınca `HomeViewModel.saveManualOrder(ids)` tek transaction'da `reorderBooks` yazar ve sıralamayı **Manual Order**'a çevirir. **Karar:** sürükleme her sıralama modunda açık, bırakınca Manual Order'a geçer (yalnız-manuel modda devre dışı bırakmak yerine). DB yeni sırayı yansıtana kadar yerel sıra gösterilir (en fazla 1,5 sn). Erişilebilirlik: kartta "Move up" / "Move down" özel eylemleri. Saf mantık (`moveId`, `applyOrder`, `findDropTarget`) testli.
- **#12 Add Book:** Başlık/yazar/sayfa için kitap/kişi/belge (`Description`) sol ikonları. Renk: önce 5 hazır renk (seçilince yalnız `accentColorIndex`), ardından HSV renk tekeri + parlaklık kaydırıcısı (`ColorWheelPicker.kt`); tekere dokunulursa seçilen tam renk `coverColorArgb`'ye (opak ARGB) yazılır. Renk her yerde `bookBaseColor(book)` ile çözülür: `coverColorArgb` doluysa o, değilse `accentColorIndex` paleti (sırt, liste, arşiv, ilerleme çubuğu, vektörel kapak). Önizleme: 12 dp yuvarlak köşeli 100x150 kapak (seçili görsel, yoksa seçilen renkte vektörel kapak). Diyalog artık kaydırılabilir. Form durumu `rememberSaveable` (RU:O2). Kaydedilmeyen diyalogda kopyalanan kapak dosyası siliniyor, kapak değiştirilince eskisi siliniyor.
- **#13 Altın folyo:** Tamamlanan kitabın sırtında üst folyo şeridi + yıldızlı yuvarlak mühür + ince alt şerit (`GoldFoil.kt`); listede kapağın köşesinde mühür. Tüm folyolar **tek** paylaşılan `rememberInfiniteTransition` (3,2 sn) kullanır; faz yalnız çizim aşamasında okunur (`drawWithCache` + `translate`, kare başı ayırma yok, recomposition yok). Tamamlanmış kitap yoksa animasyon hiç çalışmaz.
- **#42 Arşiv UI:** Raf: sırta uzun basınca "Archive" menüsü. Liste: kartta üç nokta menüsü -> "Archive" (uzun basış sürükleme için ayrıldı). İkisi de onay diyaloğu açar (`ArchiveConfirmDialog`: oturum/not/relic korunur uyarısı). Üst çubukta üç nokta -> "Archived (n)" ekranı (aynı ekranda, `BackHandler` ile geri); her satırda "Unarchive". Navigation.kt'ye dokunulmadı.
- **#30 Seri alevi:** Üst çubuk `viewModel.currentStreak` (Parti 1'de türetilmiş değer) gösteriyor; `userPreferences.currentStreak` kullanılmıyor. Erişilebilirlik açıklaması "Current streak: N days".
- **Review-ui Home hataları:** `remember` -> `rememberSaveable` (diyalog/menü/arşiv durumu ve Add Book taslağı); `Icons.Default.List/Sort` -> `Icons.AutoMirrored.Filled.*`; geri oku AutoMirrored; contentDescription'lar (sırt, kart, menü düğmeleri, renk daireleri `selectable(RadioButton)` + ad, renk tekeri, parlaklık, alev). Tüm metin İngilizce.

## Testler / derleme
`.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain`: BUILD SUCCESSFUL; 41 JVM testi (yeni: `HomeSortTest` 11, `HsvTest` 4); lint 0 hata, 34 uyarı (ui\home'da yalnız önceden var olan `ModifierParameter`).

## Cihazda doğrulanmadı (hiçbir şey çalıştırılmadı)
1. Sürükle-bırak jesti: `Initial` geçişli özel uzun-basış dedektörü, otomatik kaydırma, kare başı yer değiştirme, `animateItem` ile birlikte hissi. En riskli parça.
2. Coil 3'ün `File` modelini gerçekten yüklediği (belgelenmiş destek; koşulmadı).
3. Altın folyo parıltısının görünümü/hızı; raf gölgesinin sırtlara artık binmediği.
4. "100% Read" etiketinin en dar sırtta (54 dp) sığması; küçük ekranda 4 sırtın yan yana sığması.
5. Renk tekerinin dokunma doğruluğu ve dialog içinde dikey kaydırmayla çakışması (tekerde sürükleme jesti tüketiliyor).
6. Raf modunda uzun basış menüsü ve paylaşılan geçiş (sharedBounds) animasyonunun bozulmaması.
7. Sürükleme sonrası DB sırası gelene kadar (<1,5 sn) yerel sıranın göründüğü ara durum.
