const ST = window.TribunaDesignSystem_cb0bc4;

function SettingsScreen({ prefs, setPrefs, theme, setTheme, onBack }) {
  const set = (k) => (v) => setPrefs(p => ({ ...p, [k]: v }));
  const [weight, setWeight] = React.useState('Médio');
  const [sens, setSens] = React.useState('Normal');
  const [cd, setCd] = React.useState('3 s');
  const [sw, setSw] = React.useState({ timer: true, screen: true, bright: false, bar: true, vol: true, vib: true, planb: false });
  const tog = (k) => (v) => setSw(s => ({ ...s, [k]: v }));
  const [targetMin, setTargetMin] = React.useState(30);
  return <div style={{ height: '100%', overflowY: 'auto', boxSizing: 'border-box', padding: '32px 0 32px' }}>
    <ST.TopBar title="Configurações" onBack={onBack} />
    <ST.SettingsSection title="Aparência" first>
      <ST.SettingsRow label="Tema do app"><ST.SegmentedControl size="sm" value={theme} onChange={setTheme} options={[{ value: 'dark', label: 'Escuro' }, { value: 'light', label: 'Claro' }]} /></ST.SettingsRow>
      <ST.SettingsRow label="Temas prontos"><div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>{ST.PROMPT_PRESETS.map(p => <ST.Chip key={p.id} selected={p.id === prefs.preset} onClick={() => set('preset')(p.id)}>{p.label}</ST.Chip>)}</div></ST.SettingsRow>
      <ST.SettingsRow stacked><ST.Slider label="Tamanho da fonte" min={24} max={96} value={prefs.size} onChange={set('size')} display={prefs.size + ' sp'} /></ST.SettingsRow>
      <ST.SettingsRow label="Peso da fonte"><ST.SegmentedControl size="sm" value={weight} onChange={setWeight} options={['Normal', 'Médio', 'Negrito']} /></ST.SettingsRow>
      <ST.SettingsRow stacked><ST.Slider label="Margem lateral" min={0} max={30} value={prefs.margin} onChange={set('margin')} display={prefs.margin + '%'} /></ST.SettingsRow>
      <ST.SettingsRow label="Espelho horizontal" control={<ST.Switch checked={prefs.mirror} onChange={set('mirror')} label="Espelho horizontal" />} />
    </ST.SettingsSection>
    <ST.SettingsSection title="Rolagem e voz">
      <ST.SettingsRow label="Sensibilidade do acompanhamento"><ST.SegmentedControl size="sm" value={sens} onChange={setSens} options={['Conservador', 'Normal', 'Ágil']} /></ST.SettingsRow>
      <ST.SettingsRow label="Plano B automático" description="Sem fala reconhecida por um tempo, muda para o modo Constante." control={<ST.Switch checked={sw.planb} onChange={tog('planb')} />} />
    </ST.SettingsSection>
    <ST.SettingsSection title="Discurso">
      <ST.SettingsRow label="Contagem regressiva"><ST.SegmentedControl size="sm" value={cd} onChange={setCd} options={['0 s', '3 s', '5 s', '10 s']} /></ST.SettingsRow>
      <ST.SettingsRow label="Mostrar o cronômetro" control={<ST.Switch checked={sw.timer} onChange={tog('timer')} />} />
      <ST.SettingsRow label="Tempo-alvo" description="Aviso visual aos 5 min (amarelo) e 1 min (vermelho), sem som." control={<ST.Stepper value={targetMin + ' min'} onDecrement={() => setTargetMin(m => Math.max(5, m - 5))} onIncrement={() => setTargetMin(m => m + 5)} decLabel="Diminuir tempo-alvo" incLabel="Aumentar tempo-alvo" />} />
      <ST.SettingsRow label="Manter a tela sempre ligada" control={<ST.Switch checked={sw.screen} onChange={tog('screen')} />} />
    </ST.SettingsSection>
    <ST.SettingsSection title="Controles">
      <ST.SettingsRow label="Botões de volume" description="Pausar ou retomar e voltar uma frase." control={<ST.Switch checked={sw.vol} onChange={tog('vol')} />} />
      <ST.SettingsRow label="Vibração ao pausar" control={<ST.Switch checked={sw.vib} onChange={tog('vib')} />} />
    </ST.SettingsSection>
    <ST.SettingsSection title="Sobre">
      <ST.SettingsRow label="Licenças" description="Vosk, Apache 2.0" control={<ST.Icon name="chevron-right" size={22} color="var(--fg-2)" />} />
      <ST.SettingsRow last label="Privacidade" description="Nada sai do aparelho. O texto fica só na memória e nenhum áudio é gravado." />
    </ST.SettingsSection>
  </div>;
}

Object.assign(window, { SettingsScreen });
