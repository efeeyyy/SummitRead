# SummitRead — Gereksinimler (Gemini/Antigravity sürecinde Efe'nin istekleri)

Kaynak: Efe'nin Antigravity (Gemini) oturum dökümünden çıkarılmış liste, 2026-10-01'de Claude oturumuna yapıştırıldı.

## Genel kural
- **%100 İngilizce arayüz:** Tüm buton, başlık, placeholder, diyalog, etiket ve bildirimler İngilizce; kodda/UI'da Türkçe metin kalmayacak.

## 1. Navigasyon, akış ve ekran mimarisi
1. Alt barda yalnızca **Home** ve **Profile**; ayrı "Mountain" sekmesi yok.
2. Ana ekrandaki tüm kitaplar tıklanabilir; tıklanan kitap aktif seçilip doğrudan o kitaba özel Mountain Tracking/Detail ekranına gidilir.
3. Takip ekranının üstünde `"[Book Title] - Summit Climb"` başlık çubuğu.
4. 3D kitap açılma animasyonu: `SharedTransitionLayout` + `graphicsLayer` (rotationY, cameraDistance) ile kapağın açılarak takip ekranına dönüşmesi.

## 2. Ana ekran ve görünüm seçenekleri
5. Sağ üstte Shelf View / List View geçiş butonu.
6. Raf görünümünde derinlikli, gölgeli modern ahşap raf.
7. Kitap sırtında/altında yatay ilerleme çubuğu ("%34 Read").
8. Sırttaki dikey metin taşması font küçültme veya `TextOverflow.Ellipsis` ile engellenir; ilerleme çubuğu raf grafiği arkasında kaybolmaz.
9. Liste görünümünde Coil `AsyncImage` ile gerçek kapak; kapak yoksa şık vektörel kapak.
10. Liste üst barında sıralama: "Manual Order", "Newest First", "Recently Read", "Progress Percentage", "Alphabetical".
11. Liste görünümünde dikey sürükle-bırak sıralama; sıra raf görünümüne de anında yansır.
12. Add Book diyaloğu: sol ikonlar (başlık=kitap, yazar=kişi, sayfa=belge), sabit 5 renk yerine renk tekeri/palet seçici, kapak seçilince yuvarlatılmış köşeli önizleme.
13. %100 tamamlanan kitapların sırtında parıldayan altın folyo şerit/mühür.

## 3. Dağ ve takip ekranı (MountainCanvas & climbing)
14. Dark mode'da dağın arkasındaki beyaz dikdörtgen kaldırılır (`Color.Transparent` veya `surface`).
15. PageInputCard kenarlık/yazı renkleri ve CalendarStrip tarih etiketleri koyu modda yüksek kontrastlı.
16. Sayfa sayısına göre dağ: <200 küçük tepe, 200–400 standart dağ, 400+ Everest gibi dik zirve.
17. Tırmanma fiziği: doğrusal kayma yok; `PathMeasure` ile teğet açısı; dik yamaçta öne eğilme (`rotationZ`); adım/sekme (bounce) animasyonu; `ClimbingState.IDLE`/`CLIMBING` durumlarına göre uzuv animasyonu.
18. Zamana bağlı gökyüzü (`LocalTime.now()`): dawn, daylight, twilight, deep midnight gradyanları.
19. Gündüz nabız gibi parlayan güneş; gece parıldayan yıldızlar ve rastgele kayan yıldızlar.
20. Seri aktifken berrak hava; 36 saatten uzun süre okuma girilmezse zirvede sis ve fırtına bulutları; sayfa girilince fırtına rüzgarla dağılır.
21. Dağ sırtı boyunca yatay, kesintisiz süzülen bulutlar.
22. %25/%50/%75 noktalarında tıklanabilir çadır/kamp ateşi ikonları; tıklanınca motivasyon sözü ve ara istatistikler.
23. Odak modu/not paneli açılınca dağcının koordinatına zoom (`graphicsLayer scaleX/scaleY`) ve dağcı simgesinin "⛺"e dönüşmesi.
24. Taban çizgisindeki üçgen ağaçlar ters değil, yukarı bakar.
25. Dağcının tuvalden taşıp takvim şeridinin altına ters düşme hatası düzeltilir (`Math.toDegrees(atan2)`, pivot merkezde, Y-baseline ofseti dengeli).

## 4. Canlı kronometre ve arka plan servisi
26. Takip ekranında genişleyen/kayan kronometre: başlat, **duraklat**, durdur.
27. `ReadingTimerService` foreground service: uygulama kapansa/ekran değişse de bildirimde saniye saniye 00:00:00 sayan, Android 14+ uyumlu.
28. Kronometre aktif `bookId`'ye bağlı; oturum bitince süre (`timeSpentMillis`) Room'a yazılır ve geçen süreye göre otomatik sayfa önerisi sunulur.

## 5. Oyunlaştırma, parçacıklar ve bildirimler
29. Seri `LocalDate.now()` ile yerel gece yarısına göre; `lastReadDate` ile korunur, artar veya **sıfırlanır**.
30. Ana ekran üst barında aktif seri gün sayısını gösteren kamp ateşi/alev ikonu.
31. "Log Pages" anında giriş kutusundan fırlayan kıvılcım/yaprak parçacıklarının üst bardaki alev ikonuna uçması ve çarpınca alevin heartbeat ile büyümesi.
32. Kitap %100 bitince puan (rating) ve inceleme (review) alan Summit Logbook diyaloğu.
33. Tamamlanınca: *"Congratulations! You have successfully conquered this summit and added the [Title] Relic to your profile showcase."* kartı.
34. WorkManager akıllı bildirimler: 24 saat içinde sayfa girilmezse seri sıfırlanmaya 6 saat kala (1.) ve 1 saat kala (2.) yerel bildirim.

## 6. Profil, başarılar ve istatistikler
35. ClimbTime: 1 sayfa = 1.5 dakika kabulüyle toplam süre, "X Hours, Y Days Climbed".
36. Bu satıra tıklayınca 1.5 dk/sayfa hesabını açıklayan Material 3 AlertDialog.
37. Profilde dairesel göstergeli yıllık okuma hedefi kartı.
38. Dağcı avatarı: kronometre çalışırken nefes alma mikro animasyonu; bitirilen kitap sayısına göre Tier 1 (0–1) Novice, Tier 2 (2–5) Expedition Leader, Tier 3 (6+) Elite Alpinist.
39. Relic Showcase: telefon eğildikçe ivmeölçer/jiroskopa göre gölgesi kayan ve ters yönde metalik shimmer hareket eden 3D madalya kartları.
40. Madalyaya tıklayınca Book Journey sheet: kitap bilgisi, tamamlanma tarihi, toplam süre, Logbook incelemesi ve puanı; altında kronolojik oturumlar (okunan sayfalar) ve o günün notları.
41. Zaman çizelgesinde not çoğalma hatası düzeltilir: notlar `sessionId` ile oturuma birebir bağlı.
42. Kitap arşivlense de oturum ve not geçmişi silinmez.
43. Profilde `verticalScroll` içinde `LazyVerticalGrid` crash'i giderilir; profil tek `LazyColumn`, madalyalar kök `LazyColumn` içinde `items()` chunking ile.

## 7. Kod denetimi, mimari ve performans
44. Tüm kod tabanında bellek sızıntısı, sonsuz recomposition ve hata denetimi.
45. Sensör ve animasyon optimizasyonu: jiroskopta 0.5° eşik; profildeki nefes animasyonu yalnız sayaç çalışırken; dağ tuvalinde `System.nanoTime()` yerine `withFrameNanos`.
46. Room Flow'larında `.catch { emit(...) }` güvenliği; gereksiz `flatMapLatest` temizliği.
47. Geliştirme sürecinde `fallbackToDestructiveMigration()` kullanımı (bilinçli tercih).
