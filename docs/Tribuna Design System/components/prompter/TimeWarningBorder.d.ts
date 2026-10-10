/** 8px inset screen border for time warnings (D3): amber at 5 min, coral at 1 min. Silent by design. */
export interface TimeWarningBorderProps{ level?: 'none'|'fiveMin'|'oneMin'; }
export declare function TimeWarningBorder(props: TimeWarningBorderProps): JSX.Element|null;
