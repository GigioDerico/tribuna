import React from 'react';
import { Icon } from '../brand/Icon.jsx';
export function SegmentedControl({options=[],value,onChange,size='md',label,trackColor,style}){
  const fs=size==='sm'?14:16;
  return <div role="group" aria-label={label} style={{display:'flex',padding:4,borderRadius:16,background:trackColor||'var(--surface-track)',gap:4,...style}}>
    {options.map(o=>{const opt=typeof o==='string'?{value:o,label:o}:o;const on=opt.value===value;const dis=opt.disabled;
      return <button key={opt.value} type="button" aria-pressed={on} disabled={dis} onClick={()=>!dis&&onChange&&onChange(opt.value)} style={{flex:1,height:44,border:0,borderRadius:12,background:on?'var(--accent)':'transparent',color:on?'var(--on-accent)':dis?'var(--fg-disabled)':'var(--fg-1)',fontFamily:'var(--font-body)',fontSize:fs,fontWeight:700,display:'flex',alignItems:'center',justifyContent:'center',gap:8,cursor:dis?'default':'pointer',padding:'0 8px',minWidth:0}}>
        {opt.icon&&<Icon name={opt.icon} size={18} strokeWidth={2.2}/>}{opt.label}</button>;})}
  </div>;
}
