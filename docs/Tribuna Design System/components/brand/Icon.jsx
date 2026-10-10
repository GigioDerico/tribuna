import React from 'react';
const P={
'wifi-off':['s','<path d="M2 9a15 15 0 0 1 4-2.5M22 9a15 15 0 0 0-8-4M5.5 13a10 10 0 0 1 3-2M18.5 13a10 10 0 0 0-3.5-2.4M9 17a5 5 0 0 1 6 0M12 21h.01M3 3l18 18"/>'],
lock:['s','<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>'],
'mic-off':['s','<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3M3 3l18 18"/>'],
mic:['s','<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3"/>'],
warning:['s','<path d="M12 3l10 18H2z"/><path d="M12 10v5M12 18v.5"/>'],
settings:['s','<path d="M10.3 3h3.4l.5 2.2 1.6.9 2.1-.8 1.7 3-1.6 1.6v1.8l1.6 1.6-1.7 3-2.1-.8-1.6.9-.5 2.2h-3.4l-.5-2.2-1.6-.9-2.1.8-1.7-3 1.6-1.6V9.9L4.4 8.3l1.7-3 2.1.8 1.6-.9z"/><circle cx="12" cy="12" r="2.5"/>'],
paste:['s','<rect x="6" y="5" width="12" height="16" rx="2"/><path d="M9 5V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1M9 11h6M9 15h4"/>'],
trash:['s','<path d="M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13M10 11v6M14 11v6"/>'],
sliders:['s','<path d="M4 7h10M18 7h2M4 17h2M10 17h10"/><circle cx="16" cy="7" r="2"/><circle cx="8" cy="17" r="2"/>'],
play:['f','<path d="M8 5l11 7-11 7z"/>'],
close:['s','<path d="M6 6l12 12M18 6L6 18"/>'],
speed:['s','<path d="M4 18a8 8 0 1 1 16 0"/><path d="M12 18l4-6"/>'],
pause:['f','<rect x="6" y="5" width="4" height="14" rx="1"/><rect x="14" y="5" width="4" height="14" rx="1"/>'],
restart:['s','<path d="M4 12a8 8 0 1 0 3-6.2M4 4v4h4"/>'],
edit:['s','<path d="M4 20l1-4L16 5l3 3L8 19z"/>'],
stop:['f','<rect x="6" y="6" width="12" height="12" rx="2"/>'],
'chevron-left':['s','<path d="M15 5l-7 7 7 7"/>'],
'chevron-right':['s','<path d="M9 5l7 7-7 7"/>'],
minus:['s','<path d="M5 12h14"/>'],
plus:['s','<path d="M5 12h14M12 5v14"/>']
};
export const ICON_NAMES=Object.keys(P);
export function Icon({name,size=24,strokeWidth=2,color='currentColor',style,...rest}){
  const d=P[name]; if(!d) return null;
  const filled=d[0]==='f';
  return <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true" fill={filled?color:'none'} stroke={filled?'none':color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" style={{flex:'0 0 auto',display:'block',...style}} dangerouslySetInnerHTML={{__html:d[1]}} {...rest}/>;
}
