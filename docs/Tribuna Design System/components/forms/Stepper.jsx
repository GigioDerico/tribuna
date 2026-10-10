import React from 'react';
import { IconButton } from '../buttons/IconButton.jsx';
export function Stepper({value,onDecrement,onIncrement,decLabel='Diminuir',incLabel='Aumentar'}){
  return <div style={{flex:'0 0 auto',display:'flex',alignItems:'center',gap:4}}>
    <IconButton variant="outlined" icon="minus" label={decLabel} onClick={onDecrement}/>
    <span style={{minWidth:56,textAlign:'center',font:'var(--type-body)',fontWeight:700,color:'var(--fg-1)'}}>{value}</span>
    <IconButton variant="outlined" icon="plus" label={incLabel} onClick={onIncrement}/>
  </div>;
}
