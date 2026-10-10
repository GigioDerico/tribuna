import React from 'react';
import { Icon } from '../brand/Icon.jsx';
const SIZES={xl:{h:64,r:20,fs:20,gap:10,ic:22,px:24},lg:{h:56,r:16,fs:18,gap:8,ic:22,px:24},md:{h:52,r:16,fs:16,gap:8,ic:20,px:16},pill:{h:48,r:24,fs:16,gap:8,ic:20,px:16}};
const VARIANTS={
  primary:{background:'var(--accent)',color:'var(--on-accent)',border:'0'},
  outline:{background:'transparent',color:'var(--accent-text)',border:'1px solid var(--accent-text)'},
  outlineNeutral:{background:'transparent',color:'var(--fg-1)',border:'1px solid var(--border-strong)'},
  secondary:{background:'transparent',color:'var(--fg-1)',border:'1px solid var(--border-1)'},
  tonal:{background:'var(--surface-2)',color:'var(--fg-1)',border:'1px solid var(--border-1)'},
  tonalDanger:{background:'var(--surface-2)',color:'var(--danger)',border:'1px solid var(--danger-border)'},
  text:{background:'transparent',color:'var(--fg-1)',border:'0'},
  textAccent:{background:'transparent',color:'var(--accent-text)',border:'0'},
  textDanger:{background:'transparent',color:'var(--danger)',border:'0'}
};
export function Button({variant='primary',size='lg',icon,iconFilled,fullWidth,disabled,children,onClick,style,type='button',...rest}){
  const s=SIZES[size]||SIZES.lg; const v=VARIANTS[variant]||VARIANTS.primary;
  const isText=variant.startsWith('text');
  return <button type={type} disabled={disabled} onClick={onClick} {...rest} style={{
    height:s.h,padding:'0 '+(isText?16:s.px)+'px',borderRadius:isText?24:s.r,fontFamily:'var(--font-body)',fontSize:s.fs,fontWeight:700,
    display:'flex',alignItems:'center',justifyContent:'center',gap:s.gap,boxSizing:'border-box',cursor:disabled?'default':'pointer',
    width:fullWidth?'100%':undefined,opacity:disabled?.38:1,whiteSpace:'nowrap',...v,...style}}>
    {icon&&<Icon name={icon} size={s.ic}/>}{children}
  </button>;
}
