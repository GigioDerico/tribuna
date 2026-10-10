export type IconName='wifi-off'|'lock'|'mic-off'|'mic'|'warning'|'settings'|'paste'|'trash'|'sliders'|'play'|'close'|'speed'|'pause'|'restart'|'edit'|'stop'|'chevron-left'|'chevron-right'|'minus'|'plus';
/** Tribuna's own 24px line icon set (2px stroke, round caps/joins; play/pause/stop are filled). */
export interface IconProps{ name: IconName; /** px, default 24 */ size?: number; /** default 2 (2.2 inside mint segments, 2.4 on stepper) */ strokeWidth?: number; color?: string; style?: React.CSSProperties; }
export declare function Icon(props: IconProps): JSX.Element;
export declare const ICON_NAMES: IconName[];
