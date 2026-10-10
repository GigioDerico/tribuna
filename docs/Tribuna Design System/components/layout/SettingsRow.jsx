import React from 'react';
export function SettingsRow({label,description,control,stacked,last,children,onClick}){
  const base={padding:'12px 20px',borderBottom:last?0:'1px solid var(--divider)',color:'var(--fg-1)',fontFamily:'var(--font-body)'};
  const text=<div style={{display:'flex',flexDirection:'column',gap:2}}>
    <span style={{font:'var(--type-body)',fontWeight:700}}>{label}</span>
    {description&&<span style={{font:'var(--type-small)',color:'var(--fg-2)'}}>{description}</span>}
  </div>;
  if(stacked||children) return <div style={{...base,display:'flex',flexDirection:'column',gap:8}}>{label&&text}{children}</div>;
  return <div onClick={onClick} style={{...base,minHeight:56,boxSizing:'border-box',display:'flex',alignItems:'center',justifyContent:'space-between',gap:16,cursor:onClick?'pointer':'default'}}>{text}{control}</div>;
}
