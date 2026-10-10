import React from 'react';
export function ThemeSwatch({bg,fg,label,selected,onClick}){
  return <button type="button" aria-pressed={!!selected} onClick={onClick} style={{padding:0,border:0,background:'transparent',color:'var(--fg-1)',fontFamily:'var(--font-body)',display:'flex',flexDirection:'column',alignItems:'center',gap:4,cursor:'pointer',minWidth:0}}>
    <span style={{width:'100%',height:40,boxSizing:'border-box',border:'2px solid '+(selected?'var(--accent)':'var(--border-1)'),borderRadius:10,background:bg,color:fg,fontSize:18,fontWeight:700,display:'flex',alignItems:'center',justifyContent:'center'}}>Aa</span>
    <span style={{font:'var(--type-caption)',whiteSpace:'nowrap'}}>{label}</span>
  </button>;
}
export const PROMPT_PRESETS=[
  {id:'branco',label:'Branco/preto',bg:'#05141A',fg:'#FFFFFF'},
  {id:'preto',label:'Preto/branco',bg:'#FFFFFF',fg:'#000000'},
  {id:'amarelo',label:'Amarelo/preto',bg:'#05141A',fg:'#FFE14D'},
  {id:'verde',label:'Verde/preto',bg:'#05141A',fg:'#5CFF8A'}
];
