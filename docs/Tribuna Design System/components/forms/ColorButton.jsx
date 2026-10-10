import React from 'react';
export function ColorButton({color,label,onClick,style}){
  return <button type="button" onClick={onClick} style={{height:48,padding:'0 12px',border:'1px solid var(--border-1)',borderRadius:14,background:'var(--surface-2)',color:'var(--fg-1)',fontFamily:'var(--font-body)',fontSize:14,fontWeight:700,display:'flex',alignItems:'center',gap:8,cursor:'pointer',minWidth:0,...style}}>
    <span style={{width:20,height:20,flex:'0 0 20px',borderRadius:10,background:color,border:'1px solid var(--border-strong)',boxSizing:'border-box'}}></span>{label}
  </button>;
}
