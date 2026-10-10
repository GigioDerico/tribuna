import React from 'react';
export function TextArea({value,onChange,placeholder='Cole ou digite seu discurso aqui',label='Texto do discurso',style}){
  return <textarea aria-label={label} value={value} placeholder={placeholder} onChange={e=>onChange&&onChange(e.target.value)} style={{flex:1,minHeight:0,width:'100%',resize:'none',boxSizing:'border-box',padding:20,border:'1px solid var(--border-1)',borderRadius:16,background:'var(--surface-1)',color:'var(--fg-1)',fontFamily:'var(--font-body)',fontSize:20,lineHeight:'30px',outline:'none',...style}}/>;
}
