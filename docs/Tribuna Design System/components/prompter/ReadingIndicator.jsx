import React from 'react';
export function ReadingIndicator({top=244,variant='arrow',color,lineHeight=62}){
  if(variant==='none') return null;
  if(variant==='band') return <div aria-hidden="true" style={{position:'absolute',left:0,right:0,top:top-lineHeight/2+9,height:lineHeight,background:'var(--accent-halo)',pointerEvents:'none'}}></div>;
  return <svg width="14" height="18" viewBox="0 0 14 18" aria-label="Linha de leitura" role="img" style={{position:'absolute',left:8,top}}><path d="M0 0l14 9-14 9z" fill={color||'var(--prompt-highlight)'}/></svg>;
}
