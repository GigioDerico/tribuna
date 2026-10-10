/** Top corners of the prompter: elapsed time (left) and "restam mm:ss" (right), tabular numerals. Remaining turns amber at 5 min and coral at 1 min. */
export interface TimerBarProps{ elapsed?: string; remaining?: string; warning?: 'none'|'fiveMin'|'oneMin'; inset?: number; }
export declare function TimerBar(props: TimerBarProps): JSX.Element;
