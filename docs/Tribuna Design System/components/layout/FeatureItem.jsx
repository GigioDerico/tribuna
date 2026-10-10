import React from 'react';
import { Icon } from '../brand/Icon.jsx';
export function FeatureItem({icon,children}){
  return <div style={{display:'flex',alignItems:'center',gap:16}}>
    <div style={{flex:'0 0 44px',width:44,height:44,borderRadius:22,background:'var(--surface-1)',color:'var(--fg-1)',display:'flex',alignItems:'center',justifyContent:'center'}}><Icon name={icon} size={22}/></div>
    <div style={{font:'var(--type-body)',color:'var(--fg-1)'}}>{children}</div>
  </div>;
}
