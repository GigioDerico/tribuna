const AP = window.TribunaDesignSystem_cb0bc4;

function TribunaApp({ theme: themeProp, start = 'welcome', jumpRef }) {
  const [theme, setTheme] = React.useState(themeProp);
  React.useEffect(() => setTheme(themeProp), [themeProp]);
  const [screen, setScreen] = React.useState(start);
  const [text, setText] = React.useState(start === 'welcome' ? '' : window.SAMPLE_SPEECH);
  const [mode, setMode] = React.useState('voz');
  const [sheet, setSheet] = React.useState(false);
  const [clearAsk, setClearAsk] = React.useState(false);
  const [phase, setPhase] = React.useState('countdown');
  const [cursor, setCursor] = React.useState(0);
  const [elapsed, setElapsed] = React.useState(0);
  const target = 30 * 60;
  const [prefs, setPrefs] = React.useState({ preset: theme === 'light' ? 'preto' : 'branco', size: 44, lh: 14, margin: 8, speed: 5, mirror: false });

  React.useEffect(() => {
    if (screen !== 'prompter' || phase === 'countdown') return;
    const id = setInterval(() => setElapsed(e => e + 1), 1000);
    return () => clearInterval(id);
  }, [screen, phase]);
  if (jumpRef) jumpRef.current = (s) => setElapsed(target - s);

  const startReading = () => { setCursor(0); setElapsed(0); setPhase('countdown'); setScreen('prompter'); };

  let body;
  if (screen === 'welcome') body = <Welcome onAllow={() => setScreen('setup')} />;
  else if (screen === 'setup') body = <Setup onDone={() => setScreen('editor')} />;
  else if (screen === 'settings') body = <SettingsScreen prefs={prefs} setPrefs={setPrefs} theme={theme} setTheme={(t) => { setTheme(t); setPrefs(p => ({ ...p, preset: t === 'light' ? 'preto' : 'branco' })); }} onBack={() => setScreen('editor')} />;
  else if (screen === 'prompter') body = <Prompter text={text} prefs={prefs} mode={mode} setMode={setMode} elapsed={elapsed} target={target} phase={phase} setPhase={setPhase} cursor={cursor} setCursor={setCursor}
    onEnd={() => setScreen('editor')} onEdit={() => setScreen('editor')} onAppearance={() => setSheet(true)} onFinish={() => setScreen('end')} />;
  else if (screen === 'end') body = <EndScreen total={elapsed} target={target} onBack={() => setScreen('editor')} />;
  else body = <Editor text={text} setText={setText} mode={mode} setMode={setMode} micOk={true} onStart={startReading}
    onSettings={() => setScreen('settings')} onAppearance={() => setSheet(true)} onClear={() => setClearAsk(true)} />;

  return <div data-theme={theme} data-screen-label={screen} style={{ position: 'relative', width: 390, height: 844, overflow: 'hidden', background: 'var(--bg-app)', color: 'var(--fg-1)', fontFamily: 'var(--font-body)', borderRadius: 36, boxShadow: '0 0 0 10px #1b1f20, 0 30px 60px rgba(0,0,0,.35)' }}>
    {body}
    {sheet && <QuickSettings prefs={prefs} setPrefs={setPrefs} onClose={() => setSheet(false)} />}
    {clearAsk && <div style={{ position: 'absolute', inset: 0, background: 'var(--scrim)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <AP.Dialog title="Limpar o texto?" actions={<><AP.Button variant="text" size="pill" onClick={() => setClearAsk(false)}>Cancelar</AP.Button><AP.Button variant="textDanger" size="pill" onClick={() => { setText(''); setClearAsk(false); }}>Limpar</AP.Button></>}>O texto será apagado do aparelho. Isso não pode ser desfeito.</AP.Dialog>
    </div>}
  </div>;
}

Object.assign(window, { TribunaApp });
