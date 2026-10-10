Bottom sheet for quick settings and the pause control panel; always over a `var(--scrim)` layer.
```jsx
<div style={{position:'absolute',inset:0,background:'var(--scrim)'}} />
<BottomSheet title="Aparência" onClose={close}>…sliders…</BottomSheet>
<BottomSheet variant="panel">…Continuar + 2×2 grid…</BottomSheet>
```
