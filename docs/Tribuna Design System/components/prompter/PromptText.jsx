import React from 'react';
export function PromptText({text='',cursor=0,fontSize=44,lineHeight=1.4,weight=500,margin=8,color,highlight,readOpacity,fontFamily,onWordClick,style}){
  const words=String(text).split(/\s+/).filter(Boolean);
  const ro=readOpacity??'var(--prompt-read-opacity)';
  return <div style={{padding:'0 '+margin+'%',fontFamily:fontFamily||'var(--font-body)',fontSize,lineHeight,fontWeight:weight,color:color||'var(--prompt-fg)',textWrap:'pretty',...style}}>
    {words.map((w,i)=>{const cur=i===cursor;const read=i<cursor;
      return <React.Fragment key={i}><span data-word={i} onClick={onWordClick?(e)=>{e.stopPropagation();onWordClick(i);}:undefined} style={{opacity:read?ro:1,color:cur?(highlight||'var(--prompt-highlight)'):undefined,fontWeight:cur?700:undefined,cursor:onWordClick?'pointer':undefined,transition:'color .3s, opacity .3s'}}>{w}</span>{' '}</React.Fragment>;})}
  </div>;
}
