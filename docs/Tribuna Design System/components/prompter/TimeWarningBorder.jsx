import React from 'react';
export function TimeWarningBorder({level='fiveMin'}){
  if(level==='none') return null;
  const c=level==='oneMin'?'var(--danger-ring)':'var(--warning)';
  return <div aria-hidden="true" style={{position:'absolute',inset:0,boxShadow:'inset 0 0 0 8px '+c,pointerEvents:'none'}}></div>;
}
