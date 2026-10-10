/** One settings line: label (+ optional description) with a trailing control, or stacked with children below (chips, segmented). 1px divider below. */
export interface SettingsRowProps{ label?: string; description?: string; control?: React.ReactNode; stacked?: boolean; last?: boolean; children?: React.ReactNode; onClick?: ()=>void; }
export declare function SettingsRow(props: SettingsRowProps): JSX.Element;
