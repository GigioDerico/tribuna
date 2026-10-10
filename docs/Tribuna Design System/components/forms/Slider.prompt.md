Labeled slider with a live formatted readout — for every numeric appearance/scroll setting.
```jsx
<Slider label="Tamanho da fonte" min={24} max={96} value={44} display="44 sp" />
<Slider variant="sheet" label="Margem lateral" min={0} max={30} value={8} display="8%" />
```
Uses the native range input tinted with `accent-color: var(--accent)` (as in the source). Decimal readouts use a comma: "1,4×".
