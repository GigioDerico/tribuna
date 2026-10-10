import React from 'react';
export function ProgressBar({value=0,variant='setup',label,style}){
  const thin=variant==='thin';
  return <div role="progressbar" aria-label={label} aria-valuemin={0} aria-valuemax={100} aria-valuenow={value} style={{height:thin?4:12,borderRadius:thin?0:6,background:thin?'var(--track)':'var(--track-setup)',overflow:'hidden',...style}}>
    <div style={{width:value+'%',height:'100%',borderRadius:thin?0:6,background:'var(--accent)',transition:'width .3s'}}></div>
  </div>;
}
