import React from 'react';
export function Slider({label,value,min=0,max=100,step=1,display,onChange,variant='settings',id}){
  const sheet=variant==='sheet';
  const fid=id||('s-'+String(label).replace(/\W+/g,'-'));
  return <div style={{display:'flex',flexDirection:'column',gap:4}}>
    <label htmlFor={fid} style={{display:'flex',justifyContent:'space-between',gap:12,fontFamily:'var(--font-body)',fontSize:sheet?14:16,lineHeight:sheet?'20px':'22px',fontWeight:700,color:sheet?'var(--fg-2)':'var(--fg-1)'}}>
      <span>{label}</span><span style={{color:sheet?'var(--fg-1)':'var(--fg-2)',fontWeight:sheet?700:500}}>{display??value}</span></label>
    <input id={fid} type="range" min={min} max={max} step={step} value={value} onChange={e=>onChange&&onChange(Number(e.target.value))} style={{width:'100%',height:32,margin:0,accentColor:'var(--accent)'}}/>
  </div>;
}
