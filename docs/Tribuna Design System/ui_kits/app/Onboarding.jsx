const T = window.TribunaDesignSystem_cb0bc4;
const { Logo, FeatureItem, InfoCard, Button, ProgressBar, Icon } = T;

function Welcome({ onAllow }) {
  return <div style={{ height: '100%', boxSizing: 'border-box', padding: '56px 24px 32px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
    <div style={{ display: 'flex', flexDirection: 'column', gap: 40 }}>
      <Logo size="md" />
      <h1 style={{ margin: 0, font: 'var(--type-hero)', fontWeight: 700, textWrap: 'balance' }}>Seu teleprompter que acompanha <span style={{ color: 'var(--accent-text)' }}>sua voz</span>, sem internet</h1>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <FeatureItem icon="wifi-off">Funciona sem rede, em qualquer salão</FeatureItem>
        <FeatureItem icon="lock">O texto fica só na memória e some quando você fecha o app</FeatureItem>
        <FeatureItem icon="mic-off">Nenhum áudio é gravado</FeatureItem>
      </div>
    </div>
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <InfoCard icon="mic" title="Por que o microfone?">O app ouve sua fala só para saber em que ponto do texto você está e rolar a tela até lá. Nada sai do aparelho.</InfoCard>
      <Button size="lg" fullWidth onClick={onAllow}>Permitir microfone</Button>
    </div>
  </div>;
}

function Setup({ onDone }) {
  const [p, setP] = React.useState(0);
  React.useEffect(() => {
    const id = setInterval(() => setP(v => { if (v >= 100) { clearInterval(id); setTimeout(onDone, 300); return 100; } return v + 4; }), 80);
    return () => clearInterval(id);
  }, []);
  return <div style={{ height: '100%', boxSizing: 'border-box', padding: '56px 24px 32px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
    <Logo size="md" />
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <h1 style={{ margin: 0, font: 'var(--type-h1)', fontWeight: 700, textWrap: 'balance' }}>Preparando o reconhecimento de voz</h1>
      <div style={{ font: 'var(--type-paragraph)', color: 'var(--fg-2)' }}>Estamos copiando o modelo de voz para o armazenamento do aparelho. Isso acontece só na primeira vez e não usa internet.</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <ProgressBar value={p} label="Preparação do modelo de voz" />
        <div style={{ display: 'flex', justifyContent: 'space-between', font: 'var(--type-small)', color: 'var(--fg-2)' }}><span>Copiando modelo</span><span style={{ color: 'var(--fg-1)', fontWeight: 700 }}>{p}%</span></div>
      </div>
    </div>
    <div style={{ font: 'var(--type-small)', color: 'var(--fg-2)', textAlign: 'center' }}>Você vai para o Editor assim que terminar.</div>
  </div>;
}

Object.assign(window, { Welcome, Setup });
