import React from 'react';
export function NavItem({active,children,onClick}){
  return <button type="button" aria-current={active?'page':undefined} onClick={onClick} style={{height:56,padding:'0 20px',border:0,borderRadius:16,background:active?'var(--surface-2)':'transparent',color:active?'var(--accent-text)':'var(--fg-1)',fontFamily:'var(--font-body)',fontSize:18,fontWeight:700,textAlign:'left',cursor:'pointer',width:'100%'}}>{children}</button>;
}
