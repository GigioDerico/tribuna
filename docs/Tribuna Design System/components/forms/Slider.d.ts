/** Labeled range slider; value readout on the right (e.g. "44 sp", "1,4×", "8%").
 * variant settings = 16px bold label + muted value; sheet = 14px muted label + bright value (Ajustes rápidos). */
export interface SliderProps{ label: string; value: number; min?: number; max?: number; step?: number; /** formatted readout; defaults to value */ display?: string; onChange?: (v: number)=>void; variant?: 'settings'|'sheet'; id?: string; }
export declare function Slider(props: SliderProps): JSX.Element;
