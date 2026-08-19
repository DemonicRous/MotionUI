# MotionUI

Modern, configurable UI animations and Unicode fixes for legacy Minecraft Forge 1.12.2.

MotionUI is a client-side Forge mod. Version 0.1.0 includes an X-only animated hotbar selector, safe container opening animation, FurusatoOLD's odd Unicode GUI-scale fix and font assets, and an F8 catalog for per-GUI animation policies. The catalog scans installed mod bytecode without initializing GUI classes, supports filtering by mod ID and text search, and marks screens observed during play with `*`.

Build with JDK 8 using `gradlew test build`. Release artifacts are written to `build/libs`. MotionUI is licensed under the [MIT License](LICENSE); publication notes are in [docs/PUBLISHING.md](docs/PUBLISHING.md).

---

Ниже — исходное техническое задание MotionUI. Анализ выполнен по состоянию репозиториев:

- Furusato-OLD — commit `a1f383a`, 18 августа 2026 года.
- SmoothUI — commit `1435583`, 3 августа 2025 года.

Репозитории не изменялись.

# Техническое задание: MotionUI

## 1. Назначение продукта

MotionUI — лёгкий независимый client-side мод для Minecraft Forge 1.12.2, добавляющий плавные анимации стандартному и модифицированному интерфейсу.

Основное позиционирование:

> Modern, configurable UI animations for legacy Minecraft Forge 1.12.2.

Мод должен:

- работать без серверной установки;
- не зависеть от Furusato или FurusatoCore;
- не добавлять игровые предметы, блоки, пакеты или сетевой протокол;
- не заменять стандартные GUI собственными экранами;
- сохранять функциональность GUI сторонних модов;
- поддерживать отключение каждого эффекта отдельно;
- безопасно отключать несовместимый эффект вместо падения клиента.

## 2. Целевая платформа

- Minecraft: `1.12.2`.
- Forge: минимум `14.23.5.2847`, рекомендуемая тестовая версия `14.23.5.2860`.
- Java: 8.
- Сторона: `CLIENT`.
- Mod ID: `motionui`.
- Отдельный репозиторий, конфигурация и namespace.
- Отсутствие обязательных зависимостей, кроме Forge.

Предлагаемый package:

```text
com.demonicrous.motionui
```

## 3. Референсы

### SmoothUI

SmoothUI реализует три продуктовых направления:

- плавный selector hotbar;
- смещение GUI при открытии;
- плавную прокрутку.

Это подтверждается README проекта и кодом `InGameHudMixin`, `GuiAnimations` и GUI mixin-классов. При этом текущий SmoothUI предназначен для Fabric/Minecraft 1.21.8, поэтому его mixin-код нельзя непосредственно переносить в Forge 1.12.2. Репозиторий распространяется по MIT. [SmoothUI](https://github.com/RareHyperIonYT/SmoothUI), [README](https://github.com/RareHyperIonYT/SmoothUI/blob/1435583ce4cbba5c2507cc39a4515f372069a479/README.md), [LICENSE](https://github.com/RareHyperIonYT/SmoothUI/blob/1435583ce4cbba5c2507cc39a4515f372069a479/LICENSE).

Smooth scrolling в текущем snapshot SmoothUI фактически не завершён: основная реализация в `ScrollableWidgetMixin` закомментирована. Поэтому SmoothUI следует рассматривать как UX-референс, а не как готовый источник scrolling engine.

### Furusato-OLD

Furusato-OLD уже содержит рабочие для 1.12.2 прототипы:

- time-based анимации selector hotbar;
- минимальный ASM-патч `GuiIngame`;
- открытие `GuiContainer` со смещением;
- FPS-независимое состояние плавной прокрутки;
- безопасное применение и диагностику ASM;
- атомарное хранение конфигурации.

Именно эта реализация должна стать основной технической базой MotionUI — после отделения от Furusato и устранения описанных ниже недостатков.

# 4. Функциональные требования

## 4.1. Анимированный selector hotbar

При смене выбранного слота рамка должна плавно перемещаться к новому слоту.

Требования:

- поддерживаются слоты `0–8`;
- анимация зависит от реального времени, а не от FPS или количества тиков;
- быстрое повторное переключение начинает новый переход с текущей отображаемой позиции;
- после паузы, смены мира, смерти или длительного отсутствия HUD состояние сбрасывается;
- при большом скачке можно использовать сокращённый переход либо мгновенное перемещение;
- анимация не должна менять выбранный игровой слот;
- стандартные предметы и overlay hotbar не перерисовываются MotionUI;
- текстура selector должна оставаться той, которую собиралась рисовать текущая реализация HUD.

Настройки:

```properties
hotbar.enabled=true
hotbar.durationMs=100
hotbar.easing=EASE_OUT_CUBIC
hotbar.longJumpMode=SHORTEN
hotbar.longJumpThreshold=3
hotbar.pulse=false
```

Диапазон длительности: `40–400 ms`.

Важно: из Furusato-OLD не следует без изменений переносить отложенную перерисовку selector через жёстко заданный `textures/gui/widgets.png`. Она может нарушить:

- OptiFine Custom GUIs;
- resource packs;
- HUD-моды, использующие собственную текстуру;
- исходный blend/depth/render order.

Для базовой совместимости ASM-патч должен изменять только вычисленную координату X оригинального вызова отрисовки. Сам вызов, texture binding, UV, размеры, цвет и GL-состояние должны оставаться у исходного HUD.

Pulse можно добавить позднее отдельным необязательным патчем. В MVP он должен быть выключен по умолчанию.

## 4.2. Анимация открытия контейнеров

При открытии `GuiContainer` содержимое контейнера плавно приходит в финальное положение.

MVP-эффект:

- вертикальное смещение снизу вверх;
- начальное смещение: `6–16 px`;
- длительность по умолчанию: `160 ms`;
- easing: `EASE_OUT_CUBIC`;
- фон мира не двигается;
- GUI не масштабируется по умолчанию;
- содержимое не обрезается за пределами экрана.

Настройки:

```properties
screens.containers.enabled=true
screens.containers.durationMs=160
screens.containers.offsetPx=10
screens.containers.easing=EASE_OUT_CUBIC
screens.containers.includeModded=true
```

Критерии:

- стандартный инвентарь, сундук, печь, верстак и creative inventory работают без ошибок;
- `GuiContainer` стороннего мода анимируется без знания его texture/layout;
- `guiLeft`, `guiTop`, координаты `Slot` и размеры контейнера не изменяются;
- MotionUI не вызывает `drawScreen` самостоятельно и не перерисовывает экран во framebuffer.

### Безопасность ввода

Визуальное смещение приводит к кратковременному расхождению между изображением и координатами мыши. Поэтому обязательное правило:

- при первом mouse click, drag, wheel или key press незавершённая анимация немедленно завершается;
- MotionUI не отменяет и не повторяет событие;
- после завершения событие обрабатывается исходным экраном;
- переключение GUI на другой экран создаёт новое состояние, а не продолжает старое.

Так MotionUI не должен вмешиваться в slot clicking, shift-click, drag splitting, JEI recipe transfer или Mouse Tweaks.

## 4.3. Анимация обычных экранов

После стабилизации контейнеров может быть добавана поддержка остальных `GuiScreen`:

- главное меню;
- настройки;
- список миров;
- multiplayer;
- настройки модов;
- экран достижений.

Она должна быть отдельной опцией:

```properties
screens.general.enabled=false
screens.general.durationMs=140
screens.general.offsetPx=8
```

По умолчанию в первых версиях эффект выключен, поскольку многие сторонние GUI самостоятельно управляют матрицами, scissor и framebuffer.

Обязательны:

- blacklist по имени класса;
- whitelist;
- отключение для конкретного mod ID;
- автоматический bypass при обнаружении известного несовместимого GUI.

## 4.4. Плавная прокрутка

Плавную прокрутку нельзя реализовывать глобальной подменой mouse wheel: это может конфликтовать с JEI, Mouse Tweaks и контейнерами, где колесо перемещает предметы.

Нужна adapter-based архитектура:

```java
public interface ScrollAdapter {
    boolean supports(GuiScreen screen);
    double getPosition(GuiScreen screen);
    double getMaximum(GuiScreen screen);
    void setPosition(GuiScreen screen, double position);
}
```

Первая очередь адаптеров:

- ванильный `GuiSlot`;
- собственный config screen MotionUI;
- список серверов;
- список миров;
- список resource packs.

Следующие адаптеры разрабатываются отдельно:

- JEI ingredient/recipe lists;
- Better Advancements;
- mod list;
- конкретные GUI популярных модов.

Требования:

- exponential damping, независимый от FPS;
- clamp диапазона;
- сброс состояния при смене экрана;
- мгновенная синхронизация при drag scrollbar;
- отсутствие overscroll в первой реализации;
- колесо не перехватывается, если курсор находится над `Slot`, JEI overlay или другой областью, уже обработавшей событие;
- отменённые Forge-события не обрабатываются повторно.

## 4.5. Easing engine

Общий внутренний API:

```java
public interface Easing {
    double apply(double progress);
}

public final class TimedAnimation {
    void start(double from, double to, long nowNanos);
    double value(long nowNanos);
    boolean isFinished(long nowNanos);
    void finish();
    void reset(double value);
}
```

Минимальный набор:

- `LINEAR`;
- `EASE_OUT_QUAD`;
- `EASE_OUT_CUBIC`;
- `EASE_OUT_QUINT`;
- `EASE_IN_OUT_CUBIC`.

Правила:

- входной progress всегда clamp в `0..1`;
- длительность не может быть нулевой или отрицательной;
- применяется `System.nanoTime()`, а не системное wall-clock время;
- при FPS 20, 60, 144 и 240 итоговая длительность визуально одинакова;
- никакие animation state update не выполняются на серверном тике.

## 4.6. Конфигурация

Для MVP допустим стандартный Forge Configuration API или самостоятельный `.cfg`.

Файл:

```text
config/motionui.cfg
```

Категории:

```text
general
hotbar
screens
scrolling
compatibility
diagnostics
```

Обязательные настройки:

- master toggle;
- независимые toggles эффектов;
- duration;
- easing;
- offset;
- whitelist/blacklist экранов;
- safe mode;
- diagnostic logging;
- reduced motion preset.

Изменения обычных настроек применяются без перезапуска. Параметры, влияющие на загрузку transformer, должны явно помечаться как требующие перезапуска.

## 4.7. Config GUI

Экран конфигурации доступен через Mods → MotionUI → Config.

Минимальные страницы:

- General;
- Hotbar;
- Screens;
- Scrolling;
- Compatibility;
- Diagnostics.

Пресеты:

- Off;
- Subtle;
- Default;
- Smooth;
- Reduced Motion.

Не следует переносить весь UI Furusato. Для небольшого мода достаточно компактного vanilla-style config screen.

# 5. Архитектура

Предлагаемая структура:

```text
com.demonicrous.motionui
├── MotionUI
├── animation
│   ├── Easing
│   ├── Easings
│   ├── TimedAnimation
│   └── DampedValue
├── client
│   ├── HotbarAnimationController
│   ├── ScreenAnimationController
│   ├── ScrollAnimationController
│   └── MotionUIClientEvents
├── compatibility
│   ├── CompatibilityManager
│   ├── OptiFineCompatibility
│   ├── ScreenPolicy
│   └── adapters
├── config
│   ├── MotionUIConfig
│   └── MotionUIGuiFactory
├── diagnostics
│   ├── PatchDiagnostics
│   └── CompatibilityReport
└── core
    ├── MotionUILoadingPlugin
    └── HotbarSelectorTransformer
```

Принцип приоритетов интеграции:

1. Forge events.
2. Публичный API мода.
3. Совместимый adapter.
4. Минимальный ASM-патч.
5. Если безопасного варианта нет — эффект отключается.

# 6. OptiFine

Целевая версия для обязательного тестирования:

```text
OptiFine 1.12.2 HD U G5
```

## Требования

MotionUI должен работать в комбинациях:

- Forge без OptiFine;
- Forge + OptiFine;
- Forge + OptiFine + Custom GUIs;
- Forge + OptiFine + Fast Render;
- Forge + OptiFine + shader pack;
- Forge + OptiFine + resource pack, меняющий GUI.

OptiFine изменяет значительную часть клиентского render pipeline, а Custom GUIs может подменять GUI-текстуры, поэтому MotionUI не должен жёстко привязывать selector к собственной копии vanilla texture. [Разбор поверхности патчей OptiFine 1.12.2](https://github.com/kaiserproger/minecraft-how-it-works/blob/main/server-how-it-works-book/client-render-case-studies/optifine-1.12.2.md).

Обязательные правила:

- не включать и не выключать shader programs;
- не очищать активный framebuffer;
- не вызывать `EntityRenderer.loadShader`;
- не оставлять изменёнными blend, alpha, depth, scissor, color или matrix stack;
- каждый `pushMatrix()` защищать гарантированным `popMatrix()`;
- не считать, что до вызова установлен белый цвет;
- по возможности восстанавливать только состояние, изменённое MotionUI;
- hotbar patch должен сохранять оригинальный draw call;
- GUI animation не должна изменять world rendering;
- наличие OptiFine определять без compile-time зависимости.

## ASM и порядок transformer

Hotbar transformer должен:

- искать точный шаблон selector;
- проверять owner, descriptor, opcode и количество совпадений;
- понимать MCP и SRG names;
- применять patch только при одном однозначном совпадении;
- при `0` или `>1` совпадениях вернуть исходный bytecode;
- не использовать безусловный `COMPUTE_FRAMES`, если его можно избежать;
- записать статус `APPLIED`, `SKIPPED` или `FAILED`;
- не препятствовать запуску игры при неудаче.

Нужно протестировать оба практически встречающихся порядка загрузки transformer. Нельзя объявлять совместимость с OptiFine только на основании успешного запуска: должны проверяться selector, resource pack и Custom GUIs.

# 7. Совместимость с GUI- и инвентарными модами

## Обязательная матрица

| Мод/категория | Что проверяется |
|---|---|
| JEI 4.x | overlay, search, recipe GUI, bookmarks, recipe transfer, scrolling |
| Inventory Tweaks | сортировка, shortcuts, кнопки, автоматическая замена предметов |
| Mouse Tweaks | drag, wheel transfer, RMB drag, отменённые события |
| Baubles | слоты и tooltips дополнительного инвентаря |
| Iron Chests | контейнеры нестандартного размера |
| Storage Drawers | controller/container GUI |
| Applied Energistics 2 | terminal scrolling, search, crafting terminal |
| Refined Storage | grid scrolling, search, fluid/item tabs |
| Ender IO | вкладки и боковые панели machine GUI |
| Thermal Expansion | side tabs, machine controls |
| Tinkers’ Construct | crafting station и боковой инвентарь |
| Actually Additions | storage crate и machine GUI |
| Extra Utilities 2 | нестандартные container screens |
| Quark | inventory buttons и дополнительные элементы |
| Better Advancements | вкладки, zoom/pan и собственный scale |
| Controlling | search и прокручиваемый список |
| Crafting Tweaks | кнопки над crafting grid |
| OptiFine | Custom GUIs, shaders, Fast Render |

JEI официально имеет ветку/поддержку 1.12.2 и сам не является coremod, поэтому MotionUI не должен требовать специальной модификации JEI. [JEI](https://github.com/mezz/JustEnoughItems).

Особенно важно не перехватывать входные события раньше других GUI-модов: исторически JEI и Mouse Tweaks уже сталкивались с конфликтами из-за разных путей обработки mouse events. [Описание конфликта JEI/Mouse Tweaks](https://github.com/mezz/JustEnoughItems/issues/1342), [заметки Mouse Tweaks о Forge GUI events](https://github.com/YaLTeR/MouseTweaks/blob/master/notes.txt).

## Уровни поддержки

- `FULL` — все эффекты работают.
- `PARTIAL` — hotbar работает, screen/scroll animation отключена.
- `SAFE` — MotionUI обнаружил неподдерживаемый экран и ничего к нему не применяет.
- `INCOMPATIBLE` — только при подтверждённой невозможности совместной загрузки.

Любой неизвестный `GuiContainer` должен начинать с безопасного container-эффекта без вмешательства в input. Smooth scrolling включается только при наличии зарегистрированного адаптера.

# 8. Что переиспользовать из Furusato-OLD

## Переиспользовать после переименования и обобщения

### `client/HotbarAnimation.java`

[Исходный файл](https://github.com/DemonicRous/Furusato-OLD/blob/a1f383a57e05614bd85a19a04419ec79264a9686/src/main/java/com/demonicrous/furusato/client/HotbarAnimation.java)

Полезно:

- `displayedX`, `startX`, `targetX`;
- переход от текущей промежуточной позиции;
- `System.nanoTime()`;
- stale-state reset;
- учёт расстояния между слотами;
- cubic easing;
- clamp progress.

Адаптировать:

- вынести time/easing в общий `TimedAnimation`;
- убрать зависимости от `FurusatoEarlyConfig`;
- убрать жёстко заданный `WIDGETS`;
- убрать `deferSelector()` и собственную перерисовку selector из MVP;
- добавить явный `reset()` при disconnect/world change;
- сделать long-jump behavior конфигурируемым.

### `asm/HotbarSelectorTransformer.java`

[Исходный файл](https://github.com/DemonicRous/Furusato-OLD/blob/a1f383a57e05614bd85a19a04419ec79264a9686/src/main/java/com/demonicrous/furusato/asm/HotbarSelectorTransformer.java)

Переиспользовать:

- минимальный поиск координаты selector;
- MCP/SRG/obfuscated field names;
- требование ровно одного совпадения;
- возврат исходного класса при ошибке;
- диагностические результаты;
- негативные тесты трансформации.

Изменить:

- patch должен менять только X;
- не заменять оригинальный draw call на `deferSelector`;
- усилить проверку owner/method context;
- проверить class bytes после OptiFine transformer;
- использовать namespace MotionUI;
- добавить fingerprint/status в диагностический лог.

### `asm/PatchDiagnostics.java`

[Исходный файл](https://github.com/DemonicRous/Furusato-OLD/blob/a1f383a57e05614bd85a19a04419ec79264a9686/src/main/java/com/demonicrous/furusato/asm/PatchDiagnostics.java)

Можно адаптировать практически целиком:

- регистрация patch;
- `APPLIED`, `DISABLED`, `SAFE_MODE`, `SKIPPED`, `FAILED`;
- snapshot для config GUI;
- safe logging.

Нужно заменить Furusato config/property names на MotionUI.

### `asm/CompatibilityDiagnostics.java`

[Исходный файл](https://github.com/DemonicRous/Furusato-OLD/blob/a1f383a57e05614bd85a19a04419ec79264a9686/src/main/java/com/demonicrous/furusato/asm/CompatibilityDiagnostics.java)

Переиспользовать:

- чтение списка LaunchWrapper transformers без его изменения;
- формирование списка сторонних transformer;
- диагностический отчёт.

Расширить распознаванием:

- OptiFine transformer;
- MixinBootstrap;
- FoamFix;
- BetterFps;
- CoreTweaks/CensoredASM;
- известных HUD coremods.

Само наличие transformer не должно автоматически считаться конфликтом.

### `client/SmoothScrollState.java`

[Исходный файл](https://github.com/DemonicRous/Furusato-OLD/blob/a1f383a57e05614bd85a19a04419ec79264a9686/src/main/java/com/demonicrous/furusato/client/SmoothScrollState.java)

Хорошая база для `DampedValue`:

- target/displayed separation;
- clamp;
- exponential FPS-independent damping;
- ограничение слишком большого `deltaSeconds`;
- snap при малой разнице.

Изменить:

- сделать класс публично тестируемым внутри модуля;
- разрешить настраивать damping;
- добавить `jumpTo`, `isSettled`;
- не связывать его напрямую с GUI;
- использовать только через `ScrollAdapter`.

### Тесты

Переиспользовать структуру:

- `HotbarSelectorTransformerTest`;
- `PatchDiagnosticsTest`;
- `CompatibilityDiagnosticsTest`;
- `SmoothScrollStateTest`.

Добавить fixtures:

- vanilla `GuiIngame`;
- transformed/OptiFine-подобный вариант;
- отсутствующее совпадение;
- двойное совпадение;
- неправильный descriptor;
- повторное применение transformer.

## Использовать только как прототип

### `client/ContainerAnimationEvents.java`

[Исходный файл](https://github.com/DemonicRous/Furusato-OLD/blob/a1f383a57e05614bd85a19a04419ec79264a9686/src/main/java/com/demonicrous/furusato/client/ContainerAnimationEvents.java)

Полезны:

- запуск через `GuiOpenEvent`;
- привязка state к конкретному экземпляру экрана;
- time-based offset;
- Forge GUI events;
- короткий cubic transition.

Но переносить без изменений нельзя:

- `BackgroundDrawnEvent` вызывается не всеми кастомными GUI;
- push/pop разнесены между разными событиями;
- при нестандартном render flow возможна утечка matrix state;
- координаты ввода не учитывают визуальное смещение;
- состояние рассчитано только на один конкретный эффект.

Нужен новый `ScreenAnimationController` с гарантированным lifecycle и принудительным завершением на input.

### `asm/FurusatoEarlyConfig.java`

Переиспользовать только механизмы:

- clamp значений;
- сохранение неизвестных ключей;
- временный файл;
- atomic move;
- backup;
- safe-mode property.

Не переносить:

- Unicode patch;
- настройки шрифтов;
- blur;
- миграцию Lime/Furusato;
- Furusato-specific GUI.

Для MotionUI конфигурация не обязана загружаться ранним loader целиком. Раннему plugin достаточно прочитать `core.hotbarPatch` и safe mode; остальные настройки загружаются в pre-init.

## Не переносить

- Unicode GUI-scale transformer теперь входит в MotionUI по решению правообладателя FurusatoOLD;
- font assets;
- GUI scale policy;
- blur shader и `GuiBackgroundBlur`;
- Furusato config hub;
- диагностические экраны, относящиеся к Unicode/font resources;
- `ClientGuiEvents`;
- branding, локализации и config keys Furusato.

## Лицензионное замечание

В корне исследованного snapshot Furusato-OLD отсутствует файл `LICENSE`. Поэтому до буквального копирования кода в отдельный публичный репозиторий нужно:

- добавить явную лицензию Furusato-OLD либо получить разрешение правообладателя;
- определить лицензию MotionUI;
- сохранить copyright/attribution, если этого потребует выбранная лицензия.

До прояснения лицензии безопасно переиспользовать архитектурные идеи и написать самостоятельную реализацию. SmoothUI имеет MIT-лицензию, но при переносе существенных частей его кода необходимо сохранить MIT notice.

# 9. Нефункциональные требования

## Производительность

- не более одного `System.nanoTime()` на активный animation controller за кадр;
- отсутствие allocations в hotbar render loop;
- отсутствие framebuffer copy в MVP;
- отсутствие reflection в каждом кадре;
- среднее влияние на frame time менее `0.1 ms` на типичном клиенте;
- не запускать render tick, если все эффекты выключены.

## Надёжность

- ошибки MotionUI не должны препятствовать открытию GUI;
- любой ASM patch fail-open;
- animation state очищается при смене мира и закрытии экрана;
- матрица и GL-state восстанавливаются даже при исключении;
- невозможна отправка дополнительных container click packets;
- MotionUI не меняет `Container`, `Slot`, `ItemStack` или player inventory.

## Доступность

- master toggle;
- Reduced Motion preset;
- возможность установить `duration=0`;
- отсутствие обязательного bounce, overshoot или flashing;
- эффекты не должны мешать чтению tooltip;
- закрытие GUI клавишей Esc происходит сразу.

# 10. Критерии готовности MVP

Версия считается готовой, если:

- клиент запускается с Forge и Forge + OptiFine G5;
- сервер без MotionUI принимает подключение;
- hotbar selector корректно работает при 20–240 FPS;
- texture/resource-pack selector сохраняется;
- контейнеры не получают неправильных кликов во время анимации;
- JEI search, recipe transfer и overlay работают;
- Mouse Tweaks и Inventory Tweaks не получают дублированных событий;
- нестандартный `GuiContainer` можно исключить через blacklist;
- ошибка ASM отключает hotbar animation, но не ломает запуск;
- все эффекты выключаются через config;
- собраны `jar` и `sourcesJar`;
- пройдена ручная compatibility matrix;
- опубликованы известные ограничения.

# ROADMAP

## Этап 0 — подготовка проекта

Цель: отдельная чистая кодовая база.

- создать Forge 1.12.2 workspace;
- определить лицензию;
- добавить CI на JDK 8;
- создать mod metadata и базовый config;
- перенести только необходимые тестовые идеи;
- документировать происхождение адаптированного кода.

Результат: запускающийся client-side мод без эффектов.

## 0.1.0 — Hotbar MVP

- общий `TimedAnimation`;
- easing engine;
- минимальный X-only hotbar transformer;
- safe mode;
- patch diagnostics;
- reset при смене мира;
- config hotbar;
- тесты vanilla/obfuscated/failed patch;
- ручной тест с OptiFine G5 и resource packs.

Критерий релиза: selector плавный, исходная отрисовка полностью сохранена.

## 0.2.0 — Container animations

- `ScreenAnimationController`;
- анимация открытия `GuiContainer`;
- завершение анимации при input;
- blacklist/whitelist;
- стандартные контейнеры;
- JEI, Mouse Tweaks, Inventory Tweaks;
- Baubles, Iron Chests, Tinkers’ Construct.

Критерий релиза: ни один эффект не меняет container input semantics.

## 0.3.0 — Compatibility release

- detection OptiFine и сторонних transformers;
- диагностика применённого hotbar patch;
- тест Custom GUIs;
- тест Fast Render/shaders;
- AE2, Refined Storage, Ender IO, Thermal Expansion;
- compatibility report;
- публичный шаблон bug report.

Критерий релиза: опубликована подтверждённая compatibility matrix.

## 0.4.0 — Smooth scrolling

- `DampedValue`;
- `ScrollAdapterRegistry`;
- адаптер vanilla `GuiSlot`;
- world/server/resource-pack lists;
- drag scrollbar synchronization;
- защита mouse wheel events;
- отдельная настройка scrolling.

Критерий релиза: никакой глобальной подмены wheel behavior.

## 0.5.0 — Config GUI и presets

- экран настроек;
- выбор easing;
- duration/offset sliders;
- presets;
- per-screen policies;
- Reduced Motion;
- локализации `en_us` и `ru_ru`.

## 0.6.0 — General screen animations

- обычные `GuiScreen`;
- независимые политики для menus/config/mod screens;
- Better Advancements;
- Controlling;
- Forge mod list;
- расширенный blacklist.

По умолчанию функция может оставаться выключенной до набора достаточной статистики совместимости.

## 0.7.0 — Дополнительные эффекты

После стабильности основы:

- необязательный hotbar pulse;
- GUI closing animation, только если закрытие не задерживает gameplay;
- tooltip fade/slide;
- chat line animation;
- tab list animation.

Каждый эффект — отдельная настройка и отдельная compatibility assessment.

## 0.8.0 — Публичная beta

- profiling;
- крупные 1.12.2 modpacks;
- Windows/Linux;
- разные GUI Scale;
- Unicode font;
- fullscreen/windowed;
- bug-report diagnostics;
- CurseForge и Modrinth beta.

## 1.0.0 — стабильный релиз

- зафиксированная compatibility matrix;
- отсутствие известных item-loss/input bugs;
- OptiFine G5 support;
- стабильный config format;
- migration старых конфигов;
- документация для pack authors;
- CurseForge/Modrinth release;
- публичный список несовместимых GUI и обходных настроек.

Итоговая рекомендация: для первой публичной версии ограничить MotionUI двумя функциями — X-only hotbar selector и безопасное открытие `GuiContainer`. Smooth scrolling следует добавлять только через адаптеры. Главный технический риск проекта — не сами easing-функции, а сохранение исходных render/input semantics в JEI, Mouse Tweaks, OptiFine и нестандартных контейнерах.
