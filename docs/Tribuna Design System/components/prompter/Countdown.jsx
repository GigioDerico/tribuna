import React from 'react';
export function Countdown({value=3,hint='Toque para cancelar'}){
  return <div style={{position:'absolute',inset:0,display:'flex',flexDirection:'column',alignItems:'center',justifyContent:'center',gap:8}}>
    <div aria-label={'Começando em '+value} style={{font:'var(--type-countdown)',fontWeight:700,color:'var(--prompt-highlight)'}}>{value}</div>
    <div style={{fontSize:18,lineHeight:'26px',color:'var(--fg-soft)'}}>{hint}</div>
  </div>;
}
