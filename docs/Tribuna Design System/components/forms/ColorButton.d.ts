/** Opens a color picker; shows the current color as a 20px dot (Texto / Fundo / Destaque). */
export interface ColorButtonProps{ color: string; label: string; onClick?: ()=>void; style?: React.CSSProperties; }
export declare function ColorButton(props: ColorButtonProps): JSX.Element;
