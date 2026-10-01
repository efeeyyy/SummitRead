# SummitRead — Veri / Servis / ViewModel katmanı kod incelemesi

Kapsam: data/*, data/db/*, services/*, worker/*, 4 ViewModel, AndroidManifest, Room şemaları. Proje dosyalarına dokunulmadı.
Yol kısaltması: `…` = `app\src\main\java\com\example\summitread\`.
Zaten bilinenler (destructive migration, DI yok, bildirim izni MainActivity'de) tekrarlanmadı.

Şema tutarlılığı: `app\schemas\...\4.json` ile 5 entity (books 12 sütun, reading_sessions, notes, characters, relics) birebir uyumlu; sorun yok.

---

## YÜKSEK

### 1. Okuma süresi hiç kaydedilmiyor (timeSpentMillis her zaman 0), profildeki "toplam süre" gerçek veriyi yok sayıyor
- `…ui\mountain\MountainScreen.kt:229-231` ve `:242`, `…ui\mountain\MountainViewModel.kt:88-92`, `…ui\profile\ProfileViewModel.kt:52-54`
- Sorun: StopwatchWidget `onStopSession(pagesEstimate, timeSpentMillis)` çağırıyor ama MountainScreen ikinci parametreyi `_` ile atıyor, sadece sayfa tahminini input'a yazıyor. Sonra `viewModel.logPages()` varsayılan `timeSpentMillis = 0L` ile çağrılıyor. Sonuç: `reading_sessions.timeSpentMillis` hep 0; `getTotalTimeReadMillis()` hep 0; JourneyTimelineSheet'teki süre satırı (`timeSpentMillis > 0`) hiç görünmez. Ayrıca ProfileViewModel `totalTimeRead` zaten `totalTimeReadMillis`'i `_` ile atıp `pages * 1.5 dk` tahmini gösteriyor.
- Senaryo: Kullanıcı 45 dk kronometreyle okur, Stop'a basar, 30 sayfa yazılır, loglanır. Profilde "45 dk" yerine 30*1.5=45 dk tahmini (tesadüf); 60 dk'da 10 sayfa okuduysa profil "15 Mins" gösterir.
- Düzeltme: Stop'ta süreyi VM'ye (`pendingTimeMillis`) kaydet, `logPages(timeSpentMillis = pendingTime)` ile gönder ve sonra sıfırla. ProfileViewModel `totalTimeReadMillis` değerini kullansın (0 ise yedek olarak tahmin).

### 2. "Pause" (duraklat) butonu aslında Stop + sıfırlama; geçen süre kayboluyor
- `…ui\mountain\StopwatchWidget.kt:68-79`, `…services\ReadingTimerService.kt:99-104` ve `:155-160`
- Sorun: Çalışırken görünen "Pause" butonu `ACTION_STOP` gönderiyor → `stopTimer()` → `stopSelf()` → `onDestroy()` `_elapsedTime = 0L` yapıyor. Duraklatılan süre kaybolur; Play'e basınca 00:00'dan başlar. Stop (kırmızı) butonu yalnızca `elapsedSeconds > 0` iken görünüyor, yani sadece çalışırken (pause'dan sonra süre 0 olduğu için) erişilebilir.
- Senaryo: 20 dk oku, telefonu çağrı için duraklat, geri dönünce süre 0:00.
- Düzeltme: Servise gerçek `ACTION_PAUSE`/`ACTION_RESUME` ekle (süreyi `onDestroy`'da sıfırlama, yalnızca ACTION_STOP/finish'te sıfırla), ya da pause butonunu kaldır.

### 3. Seri (streak) mantığı: arka plana yazma, bayat gösterim ve çoklu hata
Hepsi `…ui\mountain\MountainViewModel.kt:101-124`, gösterim `MountainScreen.kt:89,147`.
a) **Seri hiç sıfırlanmıyor / bayat görünüyor.** `currentStreak` DataStore'da sadece log anında güncelleniyor. Kullanıcı 10 gün okumazsa MountainScreen hâlâ eski seriyi (örn. 7) gösterir; seri yalnızca bir sonraki loglamada 1'e düşer. Düzeltme: gösterimde `lastReadDate < dün` ise 0 say (türetilmiş değer).
b) **Dünü geriye dönük loglama seriyi kıran hata** (`:118-124`): Gün D-2'de okundu (seri 5), D-1 atlandı; bugün D-1 için "dün" log girilince seri `1`'e set ediliyor (5+1=6 olmalıydı). Zincir birleşmiyor.
c) Dünden eski veya gelecek tarihe log: seri hiç etkilenmiyor; takvim şeridi 30 gün geriye + 14 gün ileriye izin veriyor (`CalendarStrip.kt:43-45`), gelecek güne sayfa girilebiliyor (sayfalar toplama dahil oluyor).
d) Okuma-yazma atomik değil: `lastReadDate.first()` / `currentStreak.first()` (`:104-105`) ayrı okunup sonra `updateStreak` ile yazılıyor; `dataStore.edit {}` içinde hesaplanmalı (çift dokunuşta çift artış ihtimali, bkz. #5).
- Düzeltme: Streak'i oturum tablosundan (distinct gün listesi) türet ya da `edit {}` içinde atomik hesapla; hesabı `UserPreferences`/repository'ye taşı.

---

## ORTA

### 4. Gün sınırı: `selectedDate` bayatlıyor (gece yarısını geçince yanlış güne yazıyor)
- `…ui\mountain\MountainViewModel.kt:61` (`startOfDay(now)` yalnızca VM oluşurken), `:102` (`LocalDate.now()` her çağrıda taze), `CalendarStrip.kt:40` (`remember { today }`)
- Senaryo: Uygulama (süreç) 23:50'de açıldı, kullanıcı 00:20'de okuyup loglar: `_selectedDate` hâlâ dünün 00:00'ı, `loggedDate == today.minusDays(1)` dalına düşer → "dün" olarak kaydedilir, bugünün serisi başlamaz. Takvim şeridi "bugün"ü de dünde gösterir.
- Düzeltme: `selectedDate`'i null/“bugün” bayrağı olarak tut ya da ON_RESUME'da güncelle; `today`'i `LaunchedEffect`/lifecycle ile yenile.

### 5. Çift dokunuşla çift loglama + logPages transaction değil + composable scope'ta çalışıyor
- `…ui\mountain\MountainScreen.kt:240-244`, `…ui\mountain\MountainViewModel.kt:88-125`, `…data\BookRepository.kt:63-106`
- Sorun: (i) `_pagesInput` ancak `repository.logPages` bittikten sonra temizleniyor (`MountainViewModel.kt:93`); hızlı iki dokunuş iki oturum ekler (sayfa ikiye katlanır, kitap yanlışlıkla tamamlanabilir). (ii) `logPages` 5 ayrı DAO çağrısı (insertSession, linkLooseNotes, read, updateBook, insertRelic): `@Transaction` yok; ortada hata/iptal kalırsa tutarsız durum (oturum var, kitap güncellenmemiş / relic yok). (iii) `scope.launch` composable `rememberCoroutineScope` ile çağrılıyor: ekran dönerse/çıkılırsa iş iptal olur, streak güncellemesi ve zirve animasyonu atlanır, oturum yine eklenmiştir. 
- Düzeltme: input'u çağrıdan önce yakalayıp temizle ya da `isLogging` bayrağı; `BookDao`'ya `@Transaction suspend fun logPages…` ya da `db.withTransaction`; çağrıyı `viewModelScope.launch` içine al.

### 6. Sayfa sayısı doğrulaması yok, ayrıca toplamlar şişiyor
- `…ui\mountain\MountainViewModel.kt:89-90`, `…data\BookRepository.kt:63-71,89`
- Sorun: `pagesRead` ham olarak kaydediliyor; `currentPages` kitapta `coerceAtMost(totalPages)` ama `getTotalPagesRead()` (profil) oturum toplamı olduğundan aşırı değer şişirir (300 sayfalık kitaba 1000 yazılırsa profilde 1000). Üst sınır/kalan sayfa kontrolü yok; sınır dışı `toIntOrNull` taşması da yok ama çok büyük değer `sumOf` Int taşırabilir.
- Düzeltme: `pages.coerceAtMost(book.totalPages - currentPages)` ile kaydet ya da UI'da engelle.

### 7. Tamamlanan kitap sayısı yıllık hedefe "tüm zamanlar" olarak karşılaştırılıyor
- `…data\db\BookDao.kt:86-87`, `…ui\profile\ProfileScreen.kt:224,238`
- `getCompletedBooksCount()` tarih filtresi yok; "X / yıllık hedef" gösterimi geçen yıllarda bitenleri de sayar. Kitapta tamamlanma tarihi alanı yok (relic `dateEarned` var).
- Düzeltme: `relics.dateEarned` ile yıl filtreli sayım sorgusu (örn. `SELECT COUNT(*) FROM relics WHERE dateEarned >= :yearStart`).

### 8. Streak bildirimi: bayat seri bilgisi, kullanıcı ayarını yok sayma, tıklama aksiyonu yok
- `…worker\StreakNotificationWorker.kt:22-43,60-66`, `…ui\profile\ProfileScreen.kt:349`
- (a) `notificationsEnabled` tercihi hiçbir yerde kullanılmıyor: Profil'deki anahtar sadece DataStore'a yazıyor, worker'a bakmıyor. Kapatan kullanıcı yine bildirim alır. (b) Koşul `lastReadDate.isBefore(today)`; son okuma 3 gün önceyse seri aslında bozuk ama "You have a N day streak" bildirimi eski değeri söyler. (c) Dönem 24 sa, ilk enqueue anına göre (kullanıcı 09:00'da açtıysa her gün ~09:00'da, "bugün oku" için anlamsız saat; sabit akşam saati için InitialDelay hesaplanmalı). (d) contentIntent yok: bildirime dokununca uygulama açılmıyor. (e) `NotificationManagerCompat.areNotificationsEnabled()` / POST_NOTIFICATIONS kontrolü yok (izin yoksa sessizce düşer, çökmez ama gereksiz iş).
- Düzeltme: worker başında `notificationsEnabled.first()` kontrolü; `lastReadDate < dün` ise bildirim atma/seri metnini değiştir; PendingIntent ekle; ilk gecikmeyi hedef saate göre ayarla.

### 9. `lastActivityMillis` hiç güncellenmiyor → "RECENTLY_READ" sıralaması çalışmıyor
- `…data\BookRepository.kt:87-92` (updateBook'ta `lastActivityMillis` değişmiyor), `…ui\home\HomeViewModel.kt:40`
- Alan yalnızca oluşturulurken `System.currentTimeMillis()` alıyor; `SortOption.RECENTLY_READ` fiilen "en yeni eklenen" ile aynı. `createdAtMillis` ile `dateAdded` da aynı bilgiyi tekrar eden iki alan.
- Düzeltme: `logPages` içinde `book.copy(..., lastActivityMillis = System.currentTimeMillis())`.

### 10. Not silinince foto dosyası silinmiyor; vazgeçilen resimler de yetim kalıyor
- `…ui\mountain\MountainViewModel.kt:155-157`, `…data\BookRepository.kt:117-119`, `…data\ImageStorage.kt:19-41`, `…ui\home\AddBookDialog.kt:70`, `…ui\mountain\FocusModeNoteInput.kt:107`
- Sorun: `deleteNote` yalnızca satırı siler, `photoPath` dosyası `filesDir/images`'te kalır (bu yol UI'dan erişilebilir). Ayrıca resim seçilir seçilmez kopyalanıyor; dialog iptal edilir / başka resim seçilirse önceki dosya yetim kalır. ImageStorage'ın silme fonksiyonu yok. Kopyalama ana thread'de (launcher callback) yapılıyor (büyük foto → ANR/jank); boyut küçültme yok (tam çözünürlük kopya, `.jpg` uzantısı içerikten bağımsız).
- Düzeltme: `ImageStorage.delete(path)` ekle; deleteNote/deleteBook'ta çağır; kopyalamayı `Dispatchers.IO`'da yap; kaydetmeden önce geçici dosyayı sil; mümkünse yeniden boyutlandır.

### 11. Kitap silme akışı: UI'dan erişilemiyor (ölü), ama etkinleştirilirse yetim veri bırakır
- `…data\BookRepository.kt:41-43`, `…ui\home\HomeViewModel.kt:74-78`, `…data\db\*Entity.kt` (FK yok), `BookDao.kt:28-29`
- `HomeViewModel.deleteBook` çağıran yok (grep). Yine de: entity'lerde ForeignKey/CASCADE yok; silinirse sessions/notes/characters/relics yetim kalır, kapak ve not foto dosyaları silinmez, relic profilde kalır, `activeBookId` DataStore'da silinmiş id'yi gösterir (MountainViewModel `null` kabul ediyor, çökmez).
- Düzeltme: `@ForeignKey(onDelete = CASCADE)` + `bookId` index'leri (migration gerekir) ya da repository'de `@Transaction` ile elle silme + dosyaları temizleme + `activeBookId` sıfırlama.

---

## DÜŞÜK

### 12. Zamanlayıcı sapması (drift) ve süre uykuda eksik sayılıyor
- `…services\ReadingTimerService.kt:89-96`
- Her tur `delay(1000)` + 1000 ms ekleme: gecikme ve Doze'da CPU uyurken tick kaçar; gerçek süre eksik. Her saniye `notify` çağrısı da gereksiz.
- Düzeltme: `SystemClock.elapsedRealtime()` başlangıcı kaydet, süreyi farktan hesapla; bildirimi `setUsesChronometer(true)` + `setWhen` ile sistemin saymasına bırak.

### 13. Servis ayrıntıları
- `…services\ReadingTimerService.kt:32,71` `activeBookId` yazılıyor, hiç okunmuyor (ölü). Zamanlayıcı kitaba bağlı değil: başka kitap aktif edilince widget aynı süreyi gösterir.
- `:53-66` `ACTION_START`'ta `bookId == -1` ise `startForeground` çağrılmadan servis çalışır durumda kalır (stopSelf yok); `startTimer` zaten çalışıyorsa sessizce döner.
- `:99-104` `stopSelf()` yeni bir `ACTION_START` ile yarışabilir (Pause→hemen Play): `stopSelf()` startId'siz olduğundan sonradan gelen start'ı da öldürür.
- Android 14+: `foregroundServiceType="specialUse"` + `FOREGROUND_SERVICE_SPECIAL_USE` izni + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` ve `ServiceCompat.startForeground(…SPECIAL_USE)` tutarlı, kanal `onCreate`'te oluşturuluyor, `startService` ön plandaki UI'dan çağrılıyor: manifest/kod tutarlılığı açısından sorun yok. Not: `specialUse` Play Console'da açıklama formu gerektirir; zamanlayıcı için `health`/`mediaPlayback` değil, doğru tür bu. POST_NOTIFICATIONS reddedilirse FGS çalışır ama bildirim görünmez (kullanıcı çalıştığını göremez).

### 14. HomeViewModel.updateBookOrder bayat satırla tüm satırı eziyor
- `…ui\home\HomeViewModel.kt:80-86`
- `book.copy(displayOrder = index)` UI'daki eski listeden; arada `logPages` (currentPages) güncellemesi olursa ezilir. Her kitap için ayrı yazma = N Flow emisyonu (titreme).
- Düzeltme: `@Query("UPDATE books SET displayOrder = :order WHERE id = :id")` + tek transaction.

### 15. `hoursSinceLastSession` yanlış ve canlı değil
- `…ui\mountain\MountainViewModel.kt:73-78`
- `lastReadDate` gün başlangıcı (00:00) olarak saklanıyor; "saat" 00:00'dan itibaren hesaplanıyor (öğlen okuduysan 12+ saat görünür) ve sadece DataStore değişince yeniden hesaplanıyor (zamanla güncellenmiyor). MountainCanvas'ın görsel bozunumu bundan beslenir.
- Düzeltme: gerçek son oturum zaman damgasını ayrı sakla; periyodik tick'le yenile.

### 16. `ProfileViewModel.themeMode` başlangıç değeri LIGHT
- `…ui\profile\ProfileViewModel.kt:75-76` (MainActivity SYSTEM kullanıyor, `MainActivity.kt:63`). İlk karede yanlış seçili görünüm/yanıp sönme.

### 17. `linkLooseNotesToSession` tüm bağsız notları bağlıyor
- `…data\db\BookDao.kt:56-57`, `BookRepository.kt:75`
- Geçmişe dönük (örn. 20 gün önceki) tarihe log girilince bugünkü bağsız notlar o eski oturuma bağlanır. (sessionId şu an çoğunlukla gösterim için; etkisi düşük.)

### 18. Manifest: `windowSoftInputMode` yanlış yerde
- `app\src\main\AndroidManifest.xml:14` `<application>` üzerinde geçersiz (yalnızca `<activity>` özniteliği); `adjustResize` etkisizdir. Edge-to-edge ile klavye taşması için `<activity>`'ye taşı ya da Compose `imePadding()` kullan.
- `allowBackup="true"` + varsayılan kurallar: DB ve `filesDir/images` yedeklenir ama DB'de mutlak dosya yolları (`/data/user/0/...`) saklanıyor; farklı cihaz/kullanıcıda yol kırılabilir. Göreli dosya adı sakla.

### 19. Ölü / kullanılmayan kod
- `…services\ReadingReminderWorker.kt`: hiçbir yerde `enqueue` edilmiyor (grep: MainActivity yalnızca StreakNotificationWorker). Ayrıca farklı kanal (`reading_reminders`), `FLAG_ACTIVITY_CLEAR_TASK`, `System.currentTimeMillis().toInt()` bildirim id'si (birikir).
- `…data\DataRepository.kt`, `…ui\main\MainScreenViewModel.kt`, `…ui\main\MainScreen.kt`: Android şablon artığı; uygulama navigasyonunda kullanılmıyor, yalnız test/androidTest'te geçiyor.
- `…data\db\Converters.kt` (Gson): hiçbir entity `List<String>` alanı taşımıyor; Gson bağımlılığı gereksiz.
- `BookDao.getSessionsByBookAndDate` / `BookRepository.getSessionsByBookAndDate` / `getAllSessionsForBook` (getSessionsForBook'un sıralamasız kopyası), `BookRepository.insertRelic`: çağıran yok. `logPages` toplamı için `SELECT SUM(pagesRead)` sorgusu daha ucuz olurdu.
- `UserPreferences.kt` DataStore kullanımı doğru (tek top-level delegate, Worker'daki yeni `UserPreferences(context)` aynı store'u paylaşır; çoklu-örnek sorunu yok).

---

## Doğrulananlar (sorun bulunmadı)
- DataStore: tek örnek, `edit{}` doğru kullanılıyor; `updateStreak` iki alanı tek `edit`'te yazıyor.
- ViewModel yaşam döngüsü: `stateIn(WhileSubscribed(5000))` ve `flatMapLatest` kullanımı doğru; sızıntı yok (`ReadingTimerService` companion StateFlow'ları statik ama uygulama düzeyinde ve `onDestroy`'da sıfırlanıyor).
- Room: `autoGenerate` AUTOINCREMENT, id yeniden kullanımı yok; `id > 0` kontrolü doğru. Eksik: `bookId` sütunlarında index yok (küçük veri için önemsiz).
- Bildirim kanalları her worker/serviste oluşturuluyor; foreground service Android 14 gereksinimleri karşılanmış.

## Doğrulayamadıklarım
- Gerçek cihaz/emülatörde çalıştırılmadı; tüm bulgular kod okumasına dayanıyor.
- `JourneyTimelineSheet` ve `ProfileScreen`'in geri kalanı kapsam dışıydı; yalnızca ilgili satırlar okundu.
