import React from 'react';
import { Icon } from '../brand/Icon.jsx';
export function Notice({icon='mic-off',tone='warning',children,action,onAction}){
  const c=tone==='danger'?'var(--danger)':'var(--warning-text)';
  return <div style={{display:'flex',alignItems:'center',gap:12,padding:'12px 14px',borderRadius:14,background:'var(--surface-1)',border:'1px solid '+(tone==='danger'?'var(--danger-border)':'var(--warning-border)')}}>
    <Icon name={icon} size={22} color={c}/>
    <div style={{font:'var(--type-small)',color:'var(--fg-soft)'}}>{children} {action&&<button type="button" onClick={onAction} style={{display:'inline',padding:0,border:0,background:'transparent',color:'var(--accent-text)',fontFamily:'inherit',fontSize:14,lineHeight:'20px',fontWeight:700,textDecoration:'underline',cursor:'pointer'}}>{action}</button>}</div>
  </div>;
}
