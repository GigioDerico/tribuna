import React from 'react';
export function MicIndicator({level=0.6,active=true,style}){
  const hs=[6,12,18,10,5];
  const n=Math.round(level*hs.length);
  return <div style={{display:'flex',alignItems:'center',gap:12,...style}}>
    <div aria-label={active?'Microfone ativo':'Microfone desligado'} role="img" style={{position:'relative',width:28,height:28,display:'flex',alignItems:'center',justifyContent:'center'}}>
      {active&&<span style={{position:'absolute',left:0,top:0,width:28,height:28,borderRadius:14,background:'var(--accent-halo)',transform:'scale('+(0.7+level*0.3)+')',transition:'transform .12s'}}></span>}
      <span style={{width:12,height:12,borderRadius:6,background:active?'var(--accent)':'var(--fg-disabled)'}}></span>
    </div>
    <div aria-hidden="true" style={{display:'flex',alignItems:'flex-end',gap:3,height:18}}>
      {hs.map((h,i)=><span key={i} style={{width:3,height:h,borderRadius:2,background:i<n?'var(--accent)':'var(--border-strong)'}}></span>)}
    </div>
  </div>;
}
