# Tribuna Design System

**Tribuna** é um app Android nativo de teleprompter que **acompanha a voz do orador**, 100% offline, sem salvar o texto do discurso. O orador cola o texto, ajusta o visual e fala; o app reconhece a fala localmente (Vosk), alinha as palavras com o roteiro e rola a tela até a frase atual. Público: quem lê um roteiro em pé, num púlpito ou tribuna (ex.: discursos na congregação), com celular ou tablet apoiado à frente, em salões com internet fraca.

Princípios de produto que moldam o design: **Offline sempre · Privacidade (texto só na memória) · Nunca atrapalhar (na dúvida, o texto espera) · Um toque resolve · Legível à distância.**

Product surface: one Android app (Kotlin + Jetpack Compose + Material 3), phone + tablet layouts, 6 screens: Primeiro uso, Editor, Ajustes rápidos (sheet), Teleprompter (leitura), Pausa, Configurações — plus countdown, time warnings and Fim.

## Sources
Attached codebase `Tribuna/` (local, read-only) — docs only, no app code yet:
- `Tribuna/docs/PRD-Tribuna.md` — product requirements (Giorgio Derico, 9–10 out 2026)
- `Tribuna/docs/TRD-Tribuna.md` — technical requirements
- `Tribuna/docs/Tribuna — telas e fluxos.html` — bundled hi-fi mocks of every screen (31 boards). **Ground truth for all visual values.** Unbundled copies of each screen live in `source/s/*.html` (inline-styled markup, fonts stripped).

The mocks are **dark only**. The light theme ("Papel") in this system is a derivation — see caveats.

## Index
- `styles.css` — entry point (imports only) → `tokens/fonts.css`, `colors.css`, `typography.css`, `spacing.css`, `base.css`
- `fonts/` — Atkinson Hyperlegible Next (variable 400–700), Bricolage Grotesque 700 (woff2, from the mocks)
- `assets/` — `logo.svg` (primary mark), `logo-mark-ink.svg`, `logo-mark-mono-black.svg`, `logo-mark-mono-white.svg`, `logo-lockup-on-dark.svg`, `logo-lockup-on-light.svg`; `icons/*.svg` (20 icons + reading arrow)
- `guidelines/` — foundation specimen cards (Colors, Type, Spacing, Brand)
- `components/` — React primitives (below), one `*.card.html` per folder showing dark + light
- `ui_kits/app/` — interactive phone app recreation (dark + light side by side)
- `source/s/` — reference: unbundled source screens
- `SKILL.md` — Agent Skill entry

## Components
- **brand/** — `Logo`, `Icon` (+ `ICON_NAMES`)
- **buttons/** — `Button` (primary · outline · outlineNeutral · secondary · tonal · tonalDanger · text · textAccent · textDanger; sizes xl 64 / lg 56 / md 52 / pill 48), `IconButton` (ghost · outlined · tile)
- **forms/** — `SegmentedControl`, `Chip`, `Slider`, `Switch`, `Stepper`, `TextArea`, `ThemeSwatch` (+ `PROMPT_PRESETS`), `ColorButton`
- **feedback/** — `Notice`, `InfoCard`, `ProgressBar`, `Dialog`
- **layout/** — `TopBar`, `BottomSheet`, `SettingsSection`, `SettingsRow`, `FeatureItem`, `NavItem`
- **prompter/** — `PromptText`, `ReadingIndicator`, `TimerBar`, `MicIndicator`, `SpeedIndicator`, `PausePill`, `Countdown`, `TimeWarningBorder`

All components read CSS variables only; wrap any subtree in `data-theme="light"` to flip it.

### Intentional additions
- `Icon` — wrapper around the mocks' inline SVG paths so glyphs aren't re-pasted.
- `Switch` — the mocks use a native `<input type=checkbox role=switch>` with `accent-color`; this renders the Material 3 switch it stands for.
- Light theme tokens (`[data-theme="light"]`) and the ink / mono logo variants — requested by the brief; not in the source.

## Content fundamentals
- **Language:** Português do Brasil, always. Interface addresses the user as **você** ("Seu texto continua no Editor"); the app speaks in first person plural only in process copy ("Estamos copiando o modelo de voz…"). Never "eu".
- **Tone:** calm, reassuring, plain. Short declarative sentences. Explains *why* (permission copy) and *what happens next* ("Você vai para o Editor assim que terminar."). Privacy is stated as fact, not marketing: "Nada sai do aparelho." "Nenhum áudio é gravado."
- **Casing:** Sentence case everywhere — buttons ("Permitir microfone", "Usar só o modo Constante"), titles ("Limpar o texto?"). Feature names are capitalized nouns: Editor, Voz, Constante, Aparência, Configurações. Only Settings section overlines are UPPERCASE (via CSS).
- **Buttons are verbs:** Iniciar, Colar, Limpar, Continuar, Encerrar, Tentar de novo. Dialogs ask a question and name the action: "Encerrar leitura?" → [Continuar lendo] [Encerrar].
- **Punctuation:** em dash for state + instruction ("Pausado — toque para continuar"); middle dot for meta ("412 palavras · ~3 min 10 s"); Brazilian decimals ("1,4×"); units spaced ("44 sp", "30 min", "300 ms").
- **No emoji.** No exclamation marks. No jargon (it's "modelo de voz", not "Vosk model"; Vosk appears only under Licenças).
- Hero line: "Seu teleprompter que acompanha **sua voz**, sem internet" — key phrase in mint.

## Visual foundations
- **Mood:** a quiet, dark stage. Deep petroleum blue-green (`#07151A` app, `#05141A` prompter) with one electric accent, **verde-menta `#5CF2C0`**. High contrast, low noise — built to be read at arm's length under hall lighting.
- **Color use:** mint is the *only* accent: primary fills, selected segments/chips, current word, reading arrow, progress, links, section overlines. Text on mint is ink `#04231B`. Amber `#FFD24A` = 5-minute warning / degraded-state notices; coral `#FF8A7E` = destructive text, `#FF6B5E` = 1-minute border. Neutrals are a single petroleum ramp (14 steps).
- **Light theme ("Papel"):** `#F2F7F6` app, white surfaces, petroleum ink `#07151A`; mint stays as the fill, but accent *text* becomes `#0A7F5C` for contrast. The prompter defaults to the "Preto/branco" preset in light.
- **Type:** Bricolage Grotesque 700 for display only (wordmark, titles, countdown 200px, "Fim" 120px). Atkinson Hyperlegible Next for everything else — chosen for legibility; 700 for labels/buttons, 500 for prompt text, 400 body. Tabular numerals for all timers.
- **Spacing:** exact mock values — 4/8/10/12/16/20/24/32/40/56. Screen padding 16 (editor) or 24 (onboarding); top 32–56. Settings rows 12×20 with 1px `#143037` dividers.
- **Corners:** generous — 12 segment, 14 notice/color button, 16 cards/textarea/segment track, 20 main CTA, 24 pills, 28 dialogs and sheets. Circles for icon discs and ghost icon buttons.
- **Elevation:** flat. **No drop shadows** anywhere. Depth = a lighter petroleum step (850 card → 800 sheet → 700 tonal → 650 dialog) + a `rgba(5,20,26,.62)` scrim behind overlays.
- **Borders:** 1px `#2B5560` on cards, inputs, tonal buttons; `#4F7A85` for stronger outlines; 2px on selected swatches; 8px inset box-shadow ring for time warnings.
- **Backgrounds:** solid color only. No imagery, illustration, texture, pattern or gradients — except the two functional fades at the top/bottom of the prompter (bg → transparent) that keep text from colliding with the timer and mic.
- **Cards:** surface-1 fill, 1px border, 16px radius, 16px padding, no shadow.
- **Motion:** functional and quiet. Scroll animates to the reading line (150–600 ms, default 300, smooth ease-out); a tapped word flashes highlight for 300 ms; mic halo pulses with audio level. No bounces, no decorative transitions.
- **States:** selected = mint fill + ink text; disabled = muted text `#4F7A85` / 38% opacity; press = Material ripple (not mocked). Hover isn't a design concern (touch-first).
- **Transparency:** only for read text (40%), countdown text (28%), scrim, mic halo (mint 28%). No blur.
- **Layout:** full-bleed, immersive reading screen; fixed reading line at 30% height; timer pinned top corners, mic/speed pinned bottom-left, 4px progress on the bottom edge. Main action always the bottom-most, widest element (64px).
- **Hit targets:** ≥48dp (44 for segment/chip inside a 52 track).

## Iconography
- A small **in-house line icon set** drawn inline in the mocks: 24×24 grid, 2px stroke (2.2 inside mint segments, 2.4 on stepper), round caps and joins, `currentColor`. Filled exceptions: play, pause, stop. Copied verbatim to `assets/icons/*.svg` and the `Icon` component.
- Set: wifi-off, lock, mic, mic-off, warning, settings, paste, trash, sliders (Aparência), play, pause, stop, close, speed, restart, edit, chevron-left/right, minus, plus; plus a 14×18 filled triangle as the reading-line arrow.
- Icons sit in 44px circular discs on onboarding, 40px mint discs on info cards, inside buttons at 18–22px. No icon font, no PNGs, **no emoji**, no unicode glyph icons (· and — are punctuation, not icons).
- If you need a glyph not in the set, match the style (Lucide is the closest public set — same 2px round stroke) and flag it.

## Logo
Marca escolhida (out 2026) — **"a tribuna" (opção B)**: quadrado menta arredondado (40px, r12) com um T em tinta `#04231B` desenhado como púlpito — barra superior arredondada (o tampo, 22×4, pontas redondas) e base em trapézio que se alarga para baixo (o pedestal). Lê como "T" de Tribuna e como o lugar de onde se fala. Lockup = marca + "Tribuna" em Bricolage Grotesque 700 (24px ao lado da marca de 40px; 22px ao lado da de 28px na barra do app), gap 10–12px. Variantes: menta (primária, nos dois temas), tinta (quadrado petróleo, T menta) para fundos claros/impressos, mono preto/branco. Exploração completa (A linha de leitura, B tribuna, C onda) em `guidelines/brand-logo-new.html`.
