/** On/off switch (Material 3 shape, Tribuna colors): mint track + ink thumb when on; outlined track when off. */
export interface SwitchProps{ checked?: boolean; onChange?: (v: boolean)=>void; label?: string; id?: string; disabled?: boolean; }
export declare function Switch(props: SwitchProps): JSX.Element;
