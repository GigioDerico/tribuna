/** App bar. Without title: logo lockup + Configurações gear (Editor). With title: back chevron + Bricolage title (Configurações). 56px tall. */
export interface TopBarProps{ title?: string; onBack?: ()=>void; onSettings?: ()=>void; trailing?: React.ReactNode; }
export declare function TopBar(props: TopBarProps): JSX.Element;
