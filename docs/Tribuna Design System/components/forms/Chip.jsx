import React from 'react';
export function Chip({selected,children,onClick,style}){
  return <button type="button" aria-pressed={!!selected} onClick={onClick} style={{height:44,padding:'0 16px',border:selected?0:'1px solid var(--border-1)',borderRadius:22,background:selected?'var(--accent)':'transparent',color:selected?'var(--on-accent)':'var(--fg-1)',fontFamily:'var(--font-body)',fontSize:14,fontWeight:700,cursor:'pointer',whiteSpace:'nowrap',...style}}>{children}</button>;
}
