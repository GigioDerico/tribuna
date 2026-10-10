Tribuna's action button — mint-filled primary for the one main action per screen, outline/tonal/text for everything else.
```jsx
<Button size="xl" icon="play" fullWidth>Iniciar</Button>
<Button variant="outline" size="pill" icon="paste">Colar</Button>
<Button variant="outlineNeutral" size="pill" icon="trash">Limpar</Button>
<Button variant="tonal" size="md" icon="restart">Voltar ao início</Button>
<Button variant="tonalDanger" size="md" icon="stop">Encerrar</Button>
<Button variant="textDanger" size="pill">Limpar</Button>  {/* dialog action */}
```
- `primary` — mint fill, ink text. Only one per screen.
- `outline` — mint outline (Colar, Voltar ao Editor). `outlineNeutral` — #4F7A85 outline (Limpar). `secondary` — #2B5560 outline (Usar só o modo Constante).
- `tonal` / `tonalDanger` — the 2×2 grid in the Pause sheet.
- `text*` — dialog actions, 48px tall.
Disabled = 38% opacity (Iniciar with empty text).
