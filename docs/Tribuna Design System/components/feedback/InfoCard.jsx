import React from 'react';
import { Icon } from '../brand/Icon.jsx';
export function InfoCard({icon='mic',title,children}){
  return <div style={{display:'flex',gap:16,padding:16,background:'var(--surface-1)',border:'1px solid var(--border-1)',borderRadius:16}}>
    <div style={{flex:'0 0 40px',width:40,height:40,borderRadius:20,background:'var(--accent)',color:'var(--on-accent)',display:'flex',alignItems:'center',justifyContent:'center'}}><Icon name={icon} size={22} strokeWidth={2.2}/></div>
    <div style={{display:'flex',flexDirection:'column',gap:4}}>
      <div style={{font:'var(--type-body)',fontWeight:700,color:'var(--fg-1)'}}>{title}</div>
      <div style={{font:'var(--type-small)',color:'var(--fg-2)'}}>{children}</div>
    </div>
  </div>;
}
