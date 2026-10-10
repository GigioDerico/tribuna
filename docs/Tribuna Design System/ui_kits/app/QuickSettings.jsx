const { BottomSheet, ThemeSwatch, PROMPT_PRESETS, ColorButton, Slider: QSlider, Switch: QSwitch } = window.TribunaDesignSystem_cb0bc4;

function QuickSettings({ prefs, setPrefs, onClose }) {
  const set = (k) => (v) => setPrefs(p => ({ ...p, [k]: v }));
  const preset = PROMPT_PRESETS.find(p => p.id === prefs.preset);
  return <>
    <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'var(--scrim)' }}></div>
    <BottomSheet title="Aparência" onClose={onClose}>
      <div style={{ boxSizing: 'border-box', height: 100, padding: '10px 16px', borderRadius: 14, background: preset ? preset.bg : 'var(--bg-prompt)', display: 'flex', flexDirection: 'column', justifyContent: 'center', fontSize: 28, lineHeight: '40px', fontWeight: 500, overflow: 'hidden', border: '1px solid var(--border-1)' }}>
        <div style={{ color: preset.fg, opacity: .4, whiteSpace: 'nowrap' }}>Boa noite a todos.</div>
        <div style={{ color: preset.fg, whiteSpace: 'nowrap' }}>Hoje <span style={{ color: preset.bg === '#FFFFFF' ? '#0A7F5C' : '#5CF2C0', fontWeight: 700 }}>vamos</span> falar</div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div style={{ font: 'var(--type-small)', fontWeight: 700, color: 'var(--fg-2)' }}>Temas prontos</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, minmax(0,1fr))', gap: 8 }}>
          {PROMPT_PRESETS.map(p => <ThemeSwatch key={p.id} {...p} selected={p.id === prefs.preset} onClick={() => set('preset')(p.id)} />)}
        </div>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0,1fr))', gap: 8 }}>
        <ColorButton color={preset.fg} label="Texto" /><ColorButton color={preset.bg} label="Fundo" /><ColorButton color="#5CF2C0" label="Destaque" />
      </div>
      <QSlider variant="sheet" label="Tamanho da fonte" min={24} max={96} value={prefs.size} onChange={set('size')} display={prefs.size + ' sp'} />
      <QSlider variant="sheet" label="Espaçamento entre linhas" min={10} max={25} value={prefs.lh} onChange={set('lh')} display={(prefs.lh / 10).toFixed(1).replace('.', ',') + '×'} />
      <QSlider variant="sheet" label="Margem lateral" min={0} max={30} value={prefs.margin} onChange={set('margin')} display={prefs.margin + '%'} />
      <QSlider variant="sheet" label="Velocidade (modo Constante)" min={1} max={10} value={prefs.speed} onChange={set('speed')} />
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', minHeight: 48 }}>
        <span style={{ font: 'var(--type-body)', fontWeight: 700 }}>Espelho horizontal</span>
        <QSwitch checked={prefs.mirror} onChange={set('mirror')} label="Espelho horizontal" />
      </div>
    </BottomSheet>
  </>;
}

Object.assign(window, { QuickSettings });
