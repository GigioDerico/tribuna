import React from 'react';
import { Icon } from '../brand/Icon.jsx';
export function IconButton({icon,label,variant='ghost',size,iconSize,strokeWidth,onClick,style}){
  if(variant==='tile'){
    return <div style={{flex:'0 0 64px',display:'flex',flexDirection:'column',alignItems:'center',gap:2,...style}}>
      <button type="button" aria-label={label} onClick={onClick} style={{width:56,height:40,border:'1px solid var(--border-1)',borderRadius:16,background:'var(--surface-1)',color:'var(--fg-1)',display:'flex',alignItems:'center',justifyContent:'center',cursor:'pointer',padding:0}}><Icon name={icon} size={iconSize||24}/></button>
      <span aria-hidden="true" style={{font:'var(--type-caption)',color:'var(--fg-1)'}}>{label}</span>
    </div>;
  }
  const px=size||(variant==='outlined'?44:48);
  const outlined=variant==='outlined';
  return <button type="button" aria-label={label} onClick={onClick} style={{width:px,height:px,padding:0,border:outlined?'1px solid var(--border-1)':0,borderRadius:px/2,background:outlined?'var(--surface-1)':'transparent',color:'var(--fg-1)',display:'flex',alignItems:'center',justifyContent:'center',cursor:'pointer',flex:'0 0 auto',...style}}>
    <Icon name={icon} size={iconSize||(outlined?18:24)} strokeWidth={strokeWidth||(outlined?2.4:2)}/>
  </button>;
}
