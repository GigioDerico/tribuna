/** "Aa" preview tile for a prompter color preset (Ajustes rápidos). */
export interface ThemeSwatchProps{ bg: string; fg: string; label: string; selected?: boolean; onClick?: ()=>void; }
export declare function ThemeSwatch(props: ThemeSwatchProps): JSX.Element;
export declare const PROMPT_PRESETS: {id:string;label:string;bg:string;fg:string}[];
