import React from 'react';
export function Switch({checked,onChange,label,id,disabled}){
  return <button type="button" role="switch" id={id} aria-checked={!!checked} aria-label={label} disabled={disabled} onClick={()=>onChange&&onChange(!checked)} style={{position:'relative',flex:'0 0 52px',width:52,height:32,padding:0,borderRadius:16,boxSizing:'border-box',border:checked?'0':'2px solid var(--border-strong)',background:checked?'var(--accent)':'var(--surface-1)',cursor:'pointer',opacity:disabled?.38:1,transition:'background .15s'}}>
    <span style={{position:'absolute',top:checked?4:6,left:checked?24:6,width:checked?24:16,height:checked?24:16,borderRadius:12,background:checked?'var(--on-accent)':'var(--border-strong)',transition:'all .15s'}}></span>
  </button>;
}
