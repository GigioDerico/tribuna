import React from 'react';
import { Logo } from '../brand/Logo.jsx';
import { IconButton } from '../buttons/IconButton.jsx';
export function TopBar({title,onBack,onSettings,trailing}){
  if(title) return <div style={{display:'flex',alignItems:'center',gap:4,height:56,padding:'0 16px 0 8px'}}>
    {onBack!==undefined&&<IconButton icon="chevron-left" label="Voltar" onClick={onBack}/>}
    <h1 style={{margin:0,font:'var(--type-title)',fontWeight:700,color:'var(--fg-1)'}}>{title}</h1>
    <div style={{flex:1}}></div>{trailing}
  </div>;
  return <div style={{display:'flex',alignItems:'center',justifyContent:'space-between',height:56,padding:'0 0 0 8px'}}>
    <Logo size="sm"/>
    {trailing||<IconButton icon="settings" label="Configurações" iconSize={26} onClick={onSettings}/>}
  </div>;
}
