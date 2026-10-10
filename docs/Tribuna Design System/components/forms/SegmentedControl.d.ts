import { IconName } from '../brand/Icon';
export interface SegmentOption{ value: string; label: string; icon?: IconName; disabled?: boolean; }
/** Pill-track segmented control (Voz | Constante, Peso da fonte, Sensibilidade…). Selected segment = mint fill.
 * @startingPoint section="Controls" subtitle="Voz | Constante mode switch" viewport="700x260" */
export interface SegmentedControlProps{ options: (SegmentOption|string)[]; value: string; onChange?: (v: string)=>void; /** md = 16px labels (mode), sm = 14px (settings) */ size?: 'md'|'sm'; label?: string; /** override track (Pause sheet uses --bg-app) */ trackColor?: string; style?: React.CSSProperties; }
export declare function SegmentedControl(props: SegmentedControlProps): JSX.Element;
