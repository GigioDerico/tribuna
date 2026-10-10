const { TopBar, TextArea, Button: EButton, IconButton: EIconButton, SegmentedControl: ESeg, Notice: ENotice } = window.TribunaDesignSystem_cb0bc4;

function wordStats(text) {
  const n = text.trim() ? text.trim().split(/\s+/).length : 0;
  const secs = Math.round(n / 130 * 60);
  const m = Math.floor(secs / 60), s = secs % 60;
  return { n, label: n ? `${n} palavras · ~${m} min ${s} s` : '0 palavras' };
}

function Editor({ text, setText, mode, setMode, micOk, onStart, onSettings, onAppearance, onClear }) {
  const st = wordStats(text);
  return <div style={{ height: '100%', boxSizing: 'border-box', padding: '32px 16px 24px', display: 'flex', flexDirection: 'column', gap: 12 }}>
    <TopBar onSettings={onSettings} />
    <TextArea value={text} onChange={setText} />
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <EButton variant="outline" size="pill" icon="paste" onClick={() => setText(window.SAMPLE_SPEECH)}>Colar</EButton>
      <EButton variant="outlineNeutral" size="pill" icon="trash" onClick={onClear} disabled={!text}>Limpar</EButton>
      <div style={{ flex: 1, textAlign: 'right', font: 'var(--type-small)', color: 'var(--fg-2)' }}>{st.label}</div>
    </div>
    {!micOk && <ENotice icon="mic-off" action="Permitir microfone">Microfone desligado. Só o modo Constante está disponível.</ENotice>}
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <ESeg label="Modo de rolagem" value={mode} onChange={setMode} style={{ flex: 1 }}
        options={[{ value: 'voz', label: 'Voz', icon: micOk ? 'mic' : 'mic-off', disabled: !micOk }, { value: 'constante', label: 'Constante' }]} />
      <EIconButton variant="tile" icon="sliders" label="Aparência" onClick={onAppearance} />
    </div>
    <EButton size="xl" icon="play" fullWidth disabled={!text.trim()} onClick={onStart}>Iniciar</EButton>
  </div>;
}

window.SAMPLE_SPEECH = `Boa noite a todos. Hoje vamos falar sobre ouvir com atenção.

Quem escuta de verdade entende mais e responde com cuidado. Por isso, vamos treinar juntos hoje.

Primeiro, olhe para quem fala. Depois, guarde o celular. Por fim, espere a pessoa terminar antes de responder, mesmo quando você já sabe o que vai dizer.

Ouvir não é ficar em silêncio esperando a vez. É querer entender o que o outro está dizendo, e também o que ele não consegue dizer.`;

Object.assign(window, { Editor, wordStats });
