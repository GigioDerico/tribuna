/** setup = 12px rounded bar (model install); thin = 4px edge bar at the bottom of the prompter (% read). */
export interface ProgressBarProps{ value: number; variant?: 'setup'|'thin'; label?: string; style?: React.CSSProperties; }
export declare function ProgressBar(props: ProgressBarProps): JSX.Element;
