import React from 'react';
export function TimerBar({elapsed='00:00',remaining,warning='none',inset=20}){
  const rc=warning==='oneMin'?'var(--danger)':warning==='fiveMin'?'var(--warning-text)':'var(--fg-2)';
  return <div style={{position:'absolute',left:inset,top:inset,right:inset,display:'flex',justifyContent:'space-between',font:'var(--type-timer)',fontWeight:700,fontVariantNumeric:'tabular-nums',color:'var(--prompt-fg)'}}>
    <span aria-label="Tempo decorrido">{elapsed}</span>
    {remaining&&<span style={{color:rc,fontWeight:warning==='none'?500:700}}>restam {remaining}</span>}
  </div>;
}
