import React from 'react';
const MARK={mint:['var(--mint-500)','var(--mint-ink)'],ink:['var(--petrol-900)','var(--mint-500)'],black:['#000','#fff'],white:['#fff','#000']};
export function Logo({size='md',variant='mint',wordmark=true,label='Tribuna',style}){
  const px=typeof size==='number'?size:size==='sm'?28:size==='lg'?64:40;
  const [bg,bar]=MARK[variant]||MARK.mint;
  const fs=size==='sm'?22:size==='lg'?40:Math.max(20,Math.round(px*0.6));
  return <div style={{display:'flex',alignItems:'center',gap:size==='sm'?10:size==='lg'?18:12,...style}}>
    <svg width={px} height={px} viewBox="0 0 40 40" role="img" aria-label="Logo do Tribuna"><rect width="40" height="40" rx="12" fill={bg}/><path d="M9 12.5h22" stroke={bar} strokeWidth="4" strokeLinecap="round"/><path d="M16.5 16h7l1.5 15h-10z" fill={bar}/></svg>
    {wordmark&&<div style={{fontFamily:'var(--font-display)',fontWeight:700,fontSize:fs,lineHeight:'28px',color:'var(--fg-1)'}}>{label}</div>}
  </div>;
}
