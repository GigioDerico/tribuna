import React from 'react';
import { Icon } from '../brand/Icon.jsx';
export function SpeedIndicator({speed=5,style}){
  return <div aria-label={'Velocidade '+speed} role="img" style={{display:'flex',alignItems:'center',gap:8,color:'var(--fg-2)',font:'var(--type-small)',fontWeight:700,...style}}><Icon name="speed" size={20}/>Velocidade {speed}</div>;
}
