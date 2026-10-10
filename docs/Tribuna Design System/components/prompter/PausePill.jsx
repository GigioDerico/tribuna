import React from 'react';
import { Icon } from '../brand/Icon.jsx';
export function PausePill({children='Pausado — toque para continuar'}){
  return <div style={{display:'flex',alignItems:'center',gap:10,padding:'10px 20px',borderRadius:24,background:'var(--surface-2)',color:'var(--fg-1)',font:'var(--type-body)',lineHeight:'24px',fontWeight:700}}><Icon name="pause" size={18}/>{children}</div>;
}
