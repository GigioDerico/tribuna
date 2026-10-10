Segmented single-choice control on a rounded track — the Voz/Constante mode switch and every 2–4 option setting.
```jsx
<SegmentedControl label="Modo de rolagem" value="voz" onChange={setMode}
  options={[{value:'voz',label:'Voz',icon:'mic'},{value:'constante',label:'Constante'}]} />
<SegmentedControl size="sm" value="Médio" options={['Normal','Médio','Negrito']} />
```
Disabled option (no mic permission): `{value:'voz',label:'Voz',icon:'mic-off',disabled:true}`.
