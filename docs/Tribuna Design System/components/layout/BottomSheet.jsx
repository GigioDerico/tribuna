import React from 'react';
import { IconButton } from '../buttons/IconButton.jsx';
export function BottomSheet({title,onClose,children,variant='sheet',style}){
  const panel=variant==='panel';
  return <div style={{position:'absolute',left:0,right:0,bottom:0,boxSizing:'border-box',padding:panel?'16px 16px 24px':'12px 20px 24px',borderRadius:panel?'24px 24px 0 0':'28px 28px 0 0',background:panel?'var(--surface-1)':'var(--surface-sheet)',display:'flex',flexDirection:'column',gap:panel?12:16,color:'var(--fg-1)',fontFamily:'var(--font-body)',...style}}>
    {!panel&&<div style={{alignSelf:'center',width:40,height:4,borderRadius:2,background:'var(--handle)'}}></div>}
    {title&&<div style={{display:'flex',alignItems:'center',justifyContent:'space-between'}}>
      <div style={{font:'var(--type-title-sm)',fontWeight:700}}>{title}</div>
      {onClose&&<IconButton icon="close" label="Fechar" onClick={onClose} style={{marginRight:-8}}/>}
    </div>}
    {children}
  </div>;
}
