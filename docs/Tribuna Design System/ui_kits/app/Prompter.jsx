const PR = window.TribunaDesignSystem_cb0bc4;
const { PromptText, ReadingIndicator, TimerBar, MicIndicator, SpeedIndicator, PausePill, Countdown, TimeWarningBorder, Dialog } = PR;

const fmt = (s) => { s = Math.max(0, Math.round(s)); return String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0'); };
const READ_LINE = 253; // 30% of 844

function Prompter({ text, prefs, mode, setMode, elapsed, target, onEnd, onEdit, onAppearance, onFinish, phase, setPhase, cursor, setCursor }) {
  const preset = PR.PROMPT_PRESETS.find(p => p.id === prefs.preset);
  const hl = preset.bg === '#FFFFFF' ? '#0A7F5C' : '#5CF2C0';
  const words = React.useMemo(() => text.split(/\s+/).filter(Boolean), [text]);
  const boxRef = React.useRef(null);
  const [offset, setOffset] = React.useState(0);
  const [count, setCount] = React.useState(3);
  const [confirm, setConfirm] = React.useState(false);
  const fs = prefs.size, lh = prefs.lh / 10;

  React.useLayoutEffect(() => {
    const el = boxRef.current && boxRef.current.querySelector('[data-word="' + cursor + '"]');
    if (el) setOffset(READ_LINE - (el.offsetTop + fs * lh / 2));
  }, [cursor, fs, lh, prefs.margin]);

  React.useEffect(() => {
    if (phase !== 'countdown') return;
    if (count === 0) { setPhase('reading'); return; }
    const id = setTimeout(() => setCount(c => c - 1), 800);
    return () => clearTimeout(id);
  }, [phase, count]);

  React.useEffect(() => {
    if (phase !== 'reading') return;
    const ms = mode === 'voz' ? 420 : 900 - prefs.speed * 60;
    const id = setInterval(() => setCursor(c => { if (c >= words.length - 1) { clearInterval(id); setTimeout(onFinish, 1200); return c; } return c + 1; }), ms);
    return () => clearInterval(id);
  }, [phase, mode, prefs.speed, words.length]);

  const remaining = target - elapsed;
  const warning = remaining <= 60 ? 'oneMin' : remaining <= 300 ? 'fiveMin' : 'none';
  const tapEmpty = () => { if (phase === 'countdown' || phase === 'reading') setPhase('paused'); else if (phase === 'paused') setPhase('reading'); };
  const tapWord = (i) => { setCursor(i); if (phase !== 'reading') setPhase(phase === 'countdown' ? 'paused' : 'reading'); };
  const paused = phase === 'paused';

  return <div onClick={tapEmpty} style={{ position: 'absolute', inset: 0, background: preset.bg, color: preset.fg, overflow: 'hidden', cursor: 'pointer' }}>
    <div ref={boxRef} style={{ position: 'absolute', left: 0, right: 0, top: 0, transform: `translateY(${offset}px)${prefs.mirror ? ' scaleX(-1)' : ''}`, transition: 'transform 300ms cubic-bezier(.2,0,0,1)', opacity: phase === 'countdown' ? .28 : 1 }}>
      <PromptText text={text} cursor={phase === 'countdown' ? -1 : cursor} fontSize={fs} lineHeight={lh} margin={prefs.margin} color={preset.fg} highlight={hl} readOpacity={0.4} onWordClick={tapWord} />
    </div>
    {phase !== 'countdown' && <>
      <div style={{ position: 'absolute', left: 0, top: 0, right: 0, height: 96, background: `linear-gradient(${preset.bg} 55%, transparent)` }}></div>
      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: 110, background: `linear-gradient(transparent, ${preset.bg} 70%)` }}></div>
    </>}
    <ReadingIndicator top={244} color={hl} />
    {paused && <div style={{ position: 'absolute', inset: 0, background: 'var(--scrim)' }}></div>}
    <div style={{ color: preset.fg }}><TimerBar elapsed={fmt(elapsed)} remaining={fmt(remaining)} warning={warning} inset={warning === 'none' ? 20 : 24} /></div>
    {phase === 'countdown' && <Countdown value={count} />}
    {phase === 'countdown' && <div style={{ position: 'absolute', left: 20, bottom: 28, display: 'flex', alignItems: 'center', gap: 10, font: 'var(--type-small)', color: 'var(--fg-2)' }}><span style={{ width: 12, height: 12, borderRadius: 6, background: 'var(--accent)' }}></span>{mode === 'voz' ? 'Microfone ligado' : 'Modo Constante'}</div>}
    {phase === 'reading' && (mode === 'voz' ? <MicIndicator level={0.5 + Math.random() * 0.5} style={{ position: 'absolute', left: 20, bottom: 28 }} /> : <SpeedIndicator speed={prefs.speed} style={{ position: 'absolute', left: 20, bottom: 28 }} />)}
    {!paused && phase !== 'countdown' && <PR.ProgressBar variant="thin" value={Math.round(cursor / Math.max(1, words.length - 1) * 100)} label="Progresso do discurso" style={warning === 'none' ? { position: 'absolute', left: 0, right: 0, bottom: 0 } : { position: 'absolute', left: 8, right: 8, bottom: 8 }} />}
    <TimeWarningBorder level={warning} />
    {paused && <>
      <div style={{ position: 'absolute', left: 0, right: 0, top: 440, display: 'flex', justifyContent: 'center' }}><PausePill /></div>
      <div onClick={e => e.stopPropagation()} style={{ cursor: 'default' }}>
        <PR.BottomSheet variant="panel">
          <PR.SegmentedControl label="Modo de rolagem" value={mode} onChange={setMode} trackColor="var(--bg-app)" options={[{ value: 'voz', label: 'Voz', icon: 'mic' }, { value: 'constante', label: 'Constante' }]} />
          <PR.Button size="xl" icon="play" fullWidth onClick={() => setPhase('reading')}>Continuar</PR.Button>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0,1fr))', gap: 8 }}>
            <PR.Button variant="tonal" size="md" icon="restart" onClick={() => setCursor(0)}>Voltar ao início</PR.Button>
            <PR.Button variant="tonal" size="md" icon="edit" onClick={onEdit}>Editar texto</PR.Button>
            <PR.Button variant="tonal" size="md" icon="sliders" onClick={onAppearance}>Aparência</PR.Button>
            <PR.Button variant="tonalDanger" size="md" icon="stop" onClick={() => setConfirm(true)}>Encerrar</PR.Button>
          </div>
        </PR.BottomSheet>
      </div>
    </>}
    {confirm && <div onClick={e => e.stopPropagation()} style={{ position: 'absolute', inset: 0, background: 'var(--scrim)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24, cursor: 'default' }}>
      <Dialog title="Encerrar leitura?" actions={<><PR.Button variant="textAccent" size="pill" onClick={() => setConfirm(false)}>Continuar lendo</PR.Button><PR.Button variant="textDanger" size="pill" onClick={onEnd}>Encerrar</PR.Button></>}>Você volta ao Editor e o texto continua lá, pronto para outra passada.</Dialog>
    </div>}
  </div>;
}

function EndScreen({ total, target, onBack }) {
  return <div style={{ position: 'absolute', inset: 0, background: 'var(--bg-prompt)', boxSizing: 'border-box', padding: '0 24px 32px', display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}>
    <div style={{ position: 'absolute', left: 0, top: 0, right: 0, bottom: 120, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 32 }}>
      <h1 style={{ margin: 0, font: 'var(--type-end)', fontWeight: 700, color: 'var(--accent-text)' }}>Fim</h1>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
        <div style={{ font: 'var(--type-paragraph)', color: 'var(--fg-2)' }}>Tempo total</div>
        <div style={{ font: 'var(--type-stat)', fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>{fmt(total)}</div>
        <div style={{ font: 'var(--type-paragraph)', color: 'var(--fg-2)' }}>Tempo-alvo {fmt(target)}</div>
      </div>
      <div style={{ maxWidth: 280, textAlign: 'center', font: 'var(--type-paragraph)', color: 'var(--fg-soft)', textWrap: 'balance' }}>Seu texto continua no Editor, pronto para outra passada.</div>
    </div>
    <PR.Button variant="outline" size="lg" fullWidth onClick={onBack} style={{ position: 'relative' }}>Voltar ao Editor</PR.Button>
  </div>;
}

Object.assign(window, { Prompter, EndScreen, fmt });
