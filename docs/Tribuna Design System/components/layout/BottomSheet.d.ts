/** Bottom-anchored surface. sheet = Ajustes rápidos (28px top radius, drag handle, title + close); panel = Pause controls (24px radius, no handle). Parent must be position:relative. */
export interface BottomSheetProps{ title?: string; onClose?: ()=>void; children?: React.ReactNode; variant?: 'sheet'|'panel'; style?: React.CSSProperties; }
export declare function BottomSheet(props: BottomSheetProps): JSX.Element;
