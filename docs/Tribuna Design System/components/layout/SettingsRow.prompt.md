A settings list item with hairline divider — inline (switch/stepper/value at right) or stacked (chips/segmented under the label).
```jsx
<SettingsRow label="Plano B automático" description="Sem fala reconhecida por um tempo, muda para o modo Constante." control={<Switch />} />
<SettingsRow label="Peso da fonte"><SegmentedControl size="sm" value="Médio" options={['Normal','Médio','Negrito']} /></SettingsRow>
```
For sliders, wrap a <Slider> in `<SettingsRow>` with no label.
