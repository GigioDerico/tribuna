import React from 'react';
export function Dialog({title,children,actions}){
  return <div role="alertdialog" aria-label={title} style={{boxSizing:'border-box',padding:24,borderRadius:28,background:'var(--surface-dialog)',display:'flex',flexDirection:'column',gap:12,color:'var(--fg-1)',fontFamily:'var(--font-body)',width:'100%',maxWidth:340,border:'var(--dialog-border, 0)'}}>
    <div style={{font:'var(--type-title-sm)',fontWeight:700}}>{title}</div>
    <div style={{font:'var(--type-paragraph)',color:'var(--fg-dialog)'}}>{children}</div>
    {actions&&<div style={{marginTop:12,display:'flex',justifyContent:'flex-end',gap:8}}>{actions}</div>}
  </div>;
}
