"Aa" tile previewing a prompter preset (text on background) — lay four out in a 4-column grid.
```jsx
<div style={{display:'grid',gridTemplateColumns:'repeat(4,minmax(0,1fr))',gap:8}}>
  {PROMPT_PRESETS.map(p=><ThemeSwatch key={p.id} {...p} selected={p.id==='branco'} />)}
</div>
```
Presets are the prompter's own colors and do not change with the app light/dark theme.
