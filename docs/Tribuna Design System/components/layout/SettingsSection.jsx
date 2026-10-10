import React from 'react';
export function SettingsSection({title,children,first}){
  return <section>
    <div style={{padding:(first?'20px':'28px')+' 20px 8px',font:'var(--type-overline)',fontWeight:700,letterSpacing:'var(--tracking-overline)',textTransform:'uppercase',color:'var(--accent-text)'}}>{title}</div>
    {children}
  </section>;
}
