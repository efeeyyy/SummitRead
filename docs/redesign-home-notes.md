# Home yeniden tasarımı — notlar

## Home redesign

Kapsam: `Navigation.kt`, `MainActivity.kt` (yalnız sistem çubuğu rengi), `res/values*/themes.xml`, `ui/home/*`.
Yeni metinler `res/values/strings_home.xml` içinde (strings.xml'e dokunulmadı, birleştirme çakışması olmasın diye).

### Gezinme kabuğu (`Navigation.kt`)
- Material `Scaffold` + `NavigationBar` kalktı. Kök: `Box` + `SharedTransitionLayout`/`NavHost` (`glassSource`) + üstte yüzen `GlassTabBar` ("Library", "Profile").
- `LocalBottomBarHeight` sağlanıyor (ölçülen çubuk yüksekliği; odak modunda animasyonla 0'a iner). Odak modunda çubuk aşağı kayıp gizleniyor; `isFocusMode` + `onFocusModeChange` sözleşmesi ve `MountainScreen` parametreleri aynı.
- iOS davranışı: seçili sekmeye tekrar dokunmak o sekmenin köküne döner (Mountain'dayken Library'ye dokunmak Home'a pop eder). Mountain, Library yığınının içinde olduğu için orada "Library" sekmesi seçili görünür.
- `themes.xml`: ilk kare rengi açık `#F2F2F7`, koyu `#000000`. `MainActivity`: 3 tuşlu gezinmedeki scrim rengi aynı paletle (yalnız bu).

### Home
- `LargeTitleScaffold` ("Library", kaydırınca daralır). Sol: seri hapı (`StreakBadge`, alev + gün sayısı; seri 0 iken gri, değer değişince yay animasyonlu "kalp atışı"). Sağ: görünüm değiştirme ikonu (raf/liste) ve dolu "+" (Add Book).
- Liste/raf başlığında "Sort: …" hapı (menü) ve kitap sayısı. **Davranış değişikliği:** sıralama artık raf görünümünde de görünür (sıralama zaten iki görünüme de uygulanıyordu).
- "Archived" girişi artık üç nokta menüsünde değil, listenin sonunda inset-grouped satır (yalnız arşivli kitap varsa). Arşiv ekranı kendi başlığı ve geri düğmesiyle (`BackHandler` korundu).
- Raf: ceviz tonlu ahşap (aydınlık/koyu ayrı), yumuşak duvar gölgesi, kitap sırtlarında silindirik parlama, Inter tipografi, ilerleme için içe gömülü kapsül + "34% Read" etiketi. Sırtlar `pressScale` + hafif haptic; uzun basış hâlâ Archive menüsü.
- Liste: `CardContainer` kartları; kapak, başlık (Headline), yazar (Subheadline), `LinearProgressCapsule` + "34% Read", altın mühür. Sürükle-bırak aynen çalışıyor; sürüklenen kart hafif büyür. **Sapma:** sağdaki chevron yerine "…" (Archive menüsü) düğmesi var, çünkü kartın tamamı zaten tıklanabilir ve arşivleme için bir giriş gerekiyor.
- Boş durum: yuvarlak ikon + başlık + açıklama + "Add Book" `PillButton`.
- Archive onayı: `AppAlertDialog` (Archive = destructive, Cancel).
- Arşiv listesi: `InsetGroupedSection` satırları (kapak + başlık + "Author · 34% Read" + "Unarchive" hapı), altta açıklama.
- Add Book: `AppSheet` (`AddBookSheet.kt`; dosya `AddBookDialog.kt`'den yeniden adlandırıldı). Kapak önizlemesi (yuvarlak köşe, dokununca seçici) + "Select Cover Image" + "Remove Cover", `AppTextField`'lar (ikonlar korundu), 5 hazır renk (seçili = 2dp tint halka) + renk tekeri, sabitlenmiş "Add Book" `PillButton` + Cancel. Kaydet/İptal önce sheet'i kapatma animasyonuyla indirir.
- **Renk tekeri hatası:** tekerlek artık `fillMaxWidth().widthIn(max=232.dp).aspectRatio(1f)` (her zaman kare), diskin çevresinde 14dp pay var (seçici halka kenarda kesilmez), form sheet içinde kaydırılır. Eski sabit `size(190.dp)` + AlertDialog içi kaydırma düzeni yerine geçti. Emülatörde doğrulanmadı.

### Görsel olarak kontrol edilecekler
1. Alt çubuk: cam bulanıklığı, 3 tuşlu / hareketli gezinmede alt boşluk, odak modunda gizlenme/geri gelme, Mountain'dayken Library'ye dokununca Home'a dönme.
2. Mountain ve Profile içeriği alt çubuğun altında kalıyor mu (`LocalBottomBarHeight` kullanımı o ekranları yapan ajanlarda).
3. Büyük başlığın raf <-> liste <-> arşiv geçişinde yeniden açılması; döndürmede daralma durumunun korunması.
4. Liste görünümünde uzun basıp sürükleme (başlık/alt bar altında otomatik kaydırma bölgeleri artık görünür alana göre hesaplanıyor), sürükleme sonrası raf sırası.
5. Raf ahşap renkleri ve sırt gölgeleri açık/koyu modda; 4 kitaplık satırda 360dp genişlikte taşma var mı.
6. Add Book sheet: klavye açıkken butonun görünürlüğü, renk tekerinin tam daire görünmesi, tekerlekte sürüklerken sheet'in kaymaması.
7. Dar ekranda (360dp) üst çubuk: seri hapı + iki düğme, daralmış "Library" başlığıyla çakışma yok mu.
8. 3D açılma geçişi (sharedBounds anahtarı `book_container_<id>` değişmedi; kitap sırtı/kart `pressScale` dış katmanında).
