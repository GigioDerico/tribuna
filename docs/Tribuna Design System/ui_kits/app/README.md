# Tribuna — Android app UI kit

Click-through recreation of the Tribuna phone app, rebuilt from `Tribuna/docs/Tribuna — telas e fluxos.html` (dark source) with the derived light theme. `index.html` renders two independent phones (dark + light).

Flow: Editor → Colar / Limpar (dialog) → Aparência (Ajustes rápidos sheet) → Iniciar → Contagem 3-2-1 → Lendo (voice simulated: cursor advances) → tap empty = Pausa (panel: Continuar, Voltar ao início, Editar texto, Aparência, Encerrar → dialog) → Fim. Gear → Configurações. Tap any word to jump. Caption buttons jump the timer to 5 min / 1 min remaining to show the warning borders. "Reiniciar" resets; change `start="welcome"` in index.html to see onboarding (1a → 1b).

Files
- `Onboarding.jsx` — Welcome (1a), Setup (1b)
- `Editor.jsx` — Editor (2a/2b/2c), sample speech
- `QuickSettings.jsx` — Ajustes rápidos bottom sheet (3)
- `Prompter.jsx` — Countdown (4a), Reading voz/constante (4b), warnings (4c), Pause (5), Encerrar dialog, Fim (4d)
- `Settings.jsx` — Configurações (6a/6b, abbreviated)
- `App.jsx` — state machine + Limpar dialog

Not recreated: tablet layouts (two-column editor, side-menu settings) — see source screens `Tablet *`; error screen 1d and mic-denied 1c exist in the source but are only reachable via props.
