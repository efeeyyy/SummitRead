# SummitRead Design System

> **Türkçe özet (Efe için):** Uygulama iOS 26/27 ("Liquid Glass") hissine yaklaştırıldı. Temel taşlar hazır:
> Inter yazı tipi (SF Pro lisansı yüzünden), iOS renk paleti (açık: `#F2F2F7` gruplu zemin; koyu: gerçek siyah),
> tek vurgu rengi (kullanıcının seçtiği `AccentColor`), iOS köşe/boşluk/yay animasyonu değerleri, Haze ile buzlu cam
> (üst bar, alt sekme çubuğu) ve `ui/components` altında 13 hazır bileşen. Ekranlar henüz **yeniden tasarlanmadı**:
> yalnızca tipografi ve renkler tema üzerinden otomatik iyileşir. Ekran ajanları aşağıdaki "Adoption checklist"i
> mekanik olarak uygular. Kod konumları: `app/src/main/java/com/example/summitread/theme/` ve `.../ui/components/`.

## 1. Principles

1. **One tint.** The user-chosen accent (`AppTheme.colors.tint`) is the only brand colour. Everything else is neutral label/fill greys. Semantic extras: `success`, `warning`, `destructive`, `gold` (summits only).
2. **Content first, chrome is glass.** Bars and floating overlays are frosted glass over scrolling content; cards are quiet, rounded, softly shadowed (light) or lifted by fill (dark, true black base).
3. **Large, calm type.** iOS Dynamic Type ramp; big titles, short labels, tight tracking on large sizes.
4. **Springs, not tweens.** Under-damped springs for movement, 0.97 press-scale on every tappable, no ripples on custom components.
5. **Haptics are punctuation.** Light on selection/log, success on summit, warning on destructive confirm.
6. **Accessible by default.** Text pairs meet WCAG AA 4.5:1 (guarded by `ThemeContrastTest`), touch targets >= 44-48dp, icon-only controls require a `contentDescription`, roles/state set on custom controls.
7. **KMP-ready.** The design system uses only Compose common APIs. Android-only spots are isolated: `R.font` in `theme/Type.kt`, `@Preview` + `uiMode` in `ui/components/PreviewSupport.kt`. Haze (blur) is KMP-compatible and wrapped in `ui/components/Glass.kt` only.

## 2. Tokens (`com.example.summitread.theme`)

Access: `AppTheme.colors` (composable), `AppType.*`, `AppShapes.*`, `AppSpacing.*`, `AppMotion.*`, `rememberAppHaptics()`. `MaterialTheme.colorScheme/typography/shapes` are mapped from the same values, so unmigrated M3 code keeps working.

### Colour (`AppColors`, light / dark)
| Role | Light | Dark | Use |
|---|---|---|---|
| `groupedBackground` | `#F2F2F7` | `#000000` | screen canvas (= `colorScheme.background`) |
| `background` | `#FFFFFF` | `#000000` | plain full-bleed canvases |
| `surface` | `#FFFFFF` | `#1C1C1E` | cards, grouped rows |
| `elevatedSurface` | `#FFFFFF` | `#2C2C2E` | sheets, alerts, glass base |
| `label` | `#000000` | `#FFFFFF` | primary text |
| `secondaryLabel` | `#636366` | `#9D9DA3` | captions, subtitles, trailing values |
| `tertiaryLabel` | `#A0A0A5` | `#6E6E73` | placeholders, chevrons, disabled (never body text) |
| `separator` | `#3C3C43` @29% | `#545458` @60% | hairlines (`HairlineWidth` = 0.5dp) |
| `fill` | `#767680` @12% | `#767680` @24% | fields, segmented track, neutral buttons, progress track |
| `tint` / `onTint` / `tintContainer` | accent | accent | primary actions, selection, links |
| `success` `warning` `destructive` `gold` | green / amber / red / dark gold | brighter variants | status, summit trophies |

Accents (`AccentColor`): Forest, Sunset, Ocean, Berry, Autumn. Each has light/dark `Primary`, `OnPrimary`, `Container` tuned for >= 4.5:1. Never hard-code a hex colour in a screen; take it from `AppTheme.colors` (book covers keep using `AccentColor.*.lightPrimary` as data).

### Typography (`AppType`, font = Inter 400/500/600/700, OFL licence in `docs/licenses/Inter-OFL.txt`)
| Style | Size/line | Weight | M3 slot |
|---|---|---|---|
| `LargeTitle` | 34/41 | Bold | display*, big screen titles |
| `Title1` | 28/34 | Bold | headlineLarge |
| `Title2` | 22/28 | SemiBold | headlineMedium, titleLarge |
| `Title3` | 20/25 | SemiBold | headlineSmall |
| `Headline` | 17/22 | SemiBold | titleMedium, buttons, alert title |
| `Body` | 17/22 | Regular | bodyLarge, row titles |
| `Callout` | 16/21 | Regular | - |
| `Subheadline` | 15/20 | Regular | bodyMedium (labelLarge/titleSmall = SemiBold) |
| `Footnote` | 13/18 | Regular | bodySmall, section headers/footers |
| `Caption1` / `Caption2` | 12/16, 11/13 | Reg / Medium | labelSmall, tiny meta |
| `TabLabel` | 10/12 | Medium | tab bar |

Use `Style.tabularNumbers()` for timers/counters. Prefer `AppType.*` in new code; M3 `MaterialTheme.typography.*` still works.

### Shape and spacing
`AppShapes`: `small` 10 (badges), `medium` 14 (fields, grouped sections, alerts), `large` 20 (cards), `sheet` 28 (top corners), `pill` (capsule). Spacing: `AppSpacing.xxs 4, xs 8, sm 12, md 16, lg 20, xl 24, xxl 32`; screen margin `AppSpacing.screenHorizontal` = 16. Section gap between grouped sections / cards: `xl`.

### Motion and haptics
`AppMotion.standard()` (damping 0.86, MediumLow), `snappy()` (0.82, 520), `gentle()` (0.9, Low), `press()`, `fade()`; `PRESSED_SCALE` = 0.97. Haptics: `val haptics = rememberAppHaptics()` then `light()` (selection, tab, log), `toggle(on)`, `logged()`, `success()` (summit), `warning()` (destructive). Components already trigger the right haptic; call manually only for custom gestures.

### Glass
`val glass = rememberGlassState()`; mark the scrolling content `Modifier.glassSource(glass)`; draw bars with `Modifier.glassBackground(glass, shape, GlassThickness.Regular)`. Android 12+ blurs; below, Haze falls back to a translucent tint. `glassBackground(null)` = translucent surface only. Glass is for in-window bars/overlays; **sheets and dialogs are opaque** (separate windows cannot blur the screen).

## 3. Components (`com.example.summitread.ui.components`)

All previews exist as `@LightDarkPreview`. All text params are plain strings (pass `stringResource(...)`).

| Component | File | Notes |
|---|---|---|
| `LargeTitleScaffold`, `LargeTitleTopBar`, `LargeTitleState` | `LargeTitleScaffold.kt` | large title collapses into centred inline title on glass |
| `GlassTabBar`, `GlassTabItem` | `GlassTabBar.kt` | floating capsule (default) or edge-to-edge |
| `InsetGroupedSection`, `GroupedRow`, `RowTrailing`, `IconBadge`, `AppSwitch` | `Grouped.kt` | iOS Settings look |
| `SegmentedControl` | `SegmentedControl.kt` | sliding pill |
| `PillButton`, `IconCircleButton` | `Buttons.kt` | filled/tinted/plain, large/small |
| `AppSheet` | `AppSheet.kt` | ModalBottomSheet wrapper |
| `AppAlertDialog`, `AlertAction` | `AppAlertDialog.kt` | centred iOS alert |
| `AppTextField` | `AppTextField.kt` | filled rounded field |
| `CardContainer`, `StatTile` | `Cards.kt` | shadow in light, fill in dark |
| `ProgressRing`, `LinearProgressCapsule` | `Progress.kt` | animated, with semantics |
| `Modifier.pressScale`, `pressableScale`, `appShadow` | `Modifiers.kt` | feedback and depth |

### Usage snippets

```kotlin
// Screen with large title, glass top bar, content sliding under it
LargeTitleScaffold(
    title = stringResource(R.string.library),
    actions = { IconCircleButton(Icons.Rounded.Add, stringResource(R.string.add_book), onClick = onAdd) }
) { padding ->
    LazyColumn(contentPadding = padding) { /* items */ }   // padding clears the bars
}

// Settings-like list
InsetGroupedSection(header = "Appearance", separatorInset = GroupedRowDefaults.IconSeparatorInset) {
    row { GroupedRow("Theme", leadingIcon = Icons.Rounded.Palette,
            trailing = RowTrailing.ValueChevron("System"), onClick = ::openTheme) }
    row { GroupedRow("Reminders", leadingIcon = Icons.Rounded.Notifications, leadingTint = Color(0xFFFF9500),
            trailing = RowTrailing.Switch(enabled, ::setEnabled)) }
}

SegmentedControl(listOf("System", "Light", "Dark"), selectedIndex, onSelect = vm::setTheme)
PillButton("Log pages", onClick = ::log, modifier = Modifier.fillMaxWidth())              // filled, large
PillButton("Skip", onClick = ::skip, style = PillStyle.Plain, size = PillSize.Small)
AppSheet(onDismissRequest = ::close, title = "Add book") { AppTextField(title, ::setTitle, placeholder = "Title") }
AppAlertDialog("Delete this book?", ::dismiss, listOf(
    AlertAction("Delete", ::delete, AlertActionStyle.Destructive), AlertAction("Cancel", ::dismiss, AlertActionStyle.Cancel)),
    message = "Its sessions will be removed too.")
CardContainer(onClick = ::open) { Text("Title", style = AppType.Headline) }
StatTile("12", "Day streak", icon = Icons.Rounded.LocalFireDepartment, iconTint = AppTheme.colors.warning)
ProgressRing(progress = 0.68f) { Text("68%", style = AppType.Headline) }
LinearProgressCapsule(progress = 0.4f)
Modifier.pressableScale(onClick = ::tap)   // any custom tappable (book spine, polaroid, ...)
```

### Root navigation recipe (replaces Material `NavigationBar`)
```kotlin
val glass = rememberGlassState()
var barHeight by remember { mutableStateOf(0.dp) }; val density = LocalDensity.current
Box(Modifier.fillMaxSize()) {
    CompositionLocalProvider(LocalBottomBarHeight provides barHeight) {
        NavHost(modifier = Modifier.fillMaxSize().glassSource(glass), ...)   // no Scaffold padding
    }
    GlassTabBar(items, selectedIndex, onSelect = ::navigate, glassState = glass,
        modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { barHeight = with(density) { it.height.toDp() } })
}
```
`LargeTitleScaffold` adds `LocalBottomBarHeight` to its bottom padding. Screens that do not use it must add the same inset to their own bottom padding.

### Icons
Use `Icons.Rounded.*` from `material-icons-extended` everywhere (never mix `Filled`/`Outlined` in new code); 24dp in bars, 20dp in rows, 18dp inside badges/buttons. Tab bar may pass a different `selectedIcon` but Rounded for both is fine.

## 4. Adoption checklist for screen agents

Rules for everyone: no new hard-coded colours/font sizes/radii (use tokens); replace Material components per the table; keep behaviour and view-model code untouched; strings stay in `strings.xml`; check light + dark + each accent; run `assembleDebug testDebugUnitTest lintDebug`.

| Replace | With |
|---|---|
| `Scaffold` + `TopAppBar` | `LargeTitleScaffold` (or `LargeTitleTopBar` over a custom canvas) |
| `NavigationBar` | `GlassTabBar` (root recipe above) |
| `Card` / `ElevatedCard` / `OutlinedCard` | `CardContainer` |
| `Button` / `FilledTonalButton` / `TextButton` / `OutlinedButton` | `PillButton` Filled / Tinted / Plain |
| `IconButton`, `FloatingActionButton` | `IconCircleButton` (FAB: `PillButton` large with `leadingIcon`, or Filled `IconCircleButton` size 56) |
| `AlertDialog` / `Dialog` for confirmations | `AppAlertDialog` |
| `Dialog` / `ModalBottomSheet` for forms and pickers | `AppSheet` |
| `OutlinedTextField` | `AppTextField` |
| `SegmentedButton` rows, `FilterChip` for mutually exclusive choice | `SegmentedControl` |
| `Switch` | `AppSwitch` / `RowTrailing.Switch` |
| `LinearProgressIndicator` / `CircularProgressIndicator` (determinate) | `LinearProgressCapsule` / `ProgressRing` |
| `clickable { }` on cards/covers | `pressableScale` |

**Home (`ui/home`)**
- Title "Library" via `LargeTitleScaffold`; sort/add actions become `IconCircleButton`s in `actions`; remove `TopAppBar`.
- Shelf and list layouts: books/cards in `CardContainer` (list) or `pressableScale` on spines/covers; keep the wooden shelf art; cover progress with `LinearProgressCapsule`.
- `AddBookDialog` becomes `AppSheet(title = "Add book")` with `AppTextField`s, accent swatches in a row (selected = 2dp tint ring), `PillButton` Save (full width, large) and Plain Cancel; colour wheel stays.
- Archived books: `InsetGroupedSection` rows.
- Empty state: centred `AppType.Title2` + `secondaryLabel` Subheadline + one Filled `PillButton`.

**Mountain (`ui/mountain`)**
- The canvas stays full-bleed and is the glass source: `Modifier.glassSource(glass)` on the canvas box; header = `LargeTitleTopBar(collapseFraction = { 1f })`-style inline bar (back `IconCircleButton`, book title, trailing actions) with `glassBackground`.
- `MilestoneDialog` -> `AppAlertDialog` (single Cancel-style "Continue"); `SummitLogbookDialog` -> `AppSheet`; `NoteSignpostDialog` -> `AppSheet` with `CardContainer`.
- `PageInputCard`, `NoteCard`, `StopwatchWidget`, `FocusModeNoteInput` -> `CardContainer` + `AppTextField` + `PillButton` ("Log" Filled large); progress bar -> `LinearProgressCapsule` (tint) and summit marker colour `AppTheme.colors.gold`.
- Haptics: `haptics.logged()` on page log, `haptics.success()` on summit (the PillButton already adds a light tap; pass `haptic = false` there to avoid a double buzz).
- FAB for focus mode -> Filled `IconCircleButton` (size 56) with glass-friendly placement above the tab bar inset (`LocalBottomBarHeight`).
- Sky/weather colours in `MountainTheme.kt` stay as art; UI chrome above them uses tokens only.

**Profile (`ui/profile`)**
- `LargeTitleScaffold(title = "Profile")`; body = column of `InsetGroupedSection`s with `AppSpacing.xl` gaps.
- Stats: two-column grid of `StatTile`s (streak, pages, books, time); replace `StatCard`.
- Appearance section: Theme = `SegmentedControl` (System/Light/Dark) inside a row (`RowTrailing.Custom` or a full-width control under the header), Accent = row with swatches (`IconBadge`-sized circles, selected has tint ring).
- Reminders / sensors = `GroupedRow` with `IconBadge` + `RowTrailing.Switch` / `ValueChevron`; destructive items (reset data) = `GroupedRow(destructive = true)` + `AppAlertDialog`.
- `JourneyTimelineSheet` -> `AppSheet`; `RelicShowcase` cards -> `CardContainer`, gold relics use `AppTheme.colors.gold`; loading -> `ProgressRing` indeterminate is not provided: keep `CircularProgressIndicator` with `color = tint` until needed.

## 5. Decisions and caveats

- **Font:** Inter 4.1 static TTFs (Regular/Medium/SemiBold/Bold, ~1.6 MB) in `res/font`. SF Pro is not licensable for bundling; swap `InterFontFamily` if that changes.
- **Haze 1.7.2** (not 2.x): Haze 2.0.x is built with Kotlin 2.4 / Compose 1.12 and 1.7.3 needs compileSdk 37 / AGP 9.1; 1.7.2 resolves to the project's Compose 1.10.6 + Kotlin 2.1.20. Upgrade Haze together with Compose/AGP/Kotlin.
- **Corners:** Compose has no continuous (squircle) corners; `RoundedCornerShape` with iOS radii is used. A custom `Shape` can replace `AppShapes` later without touching components.
- **Window background:** `res/values/themes.xml` (+ `values-night`) still uses the old `#F8F6F2`/navy first-frame colour; set to `#F2F2F7` / `#000000` when touching resources (outside the design-system scope).
- **Scroll sync:** `LargeTitleState` observes nested scroll; after programmatic `scrollToItem`, call `state.reset()`.
- **Not provided yet:** indeterminate progress, toast/snackbar, context menus, search bar. Use Material equivalents and ask for a component rather than inventing local styles.
