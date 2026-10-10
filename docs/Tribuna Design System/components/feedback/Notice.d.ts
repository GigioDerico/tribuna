import { IconName } from '../brand/Icon';
/** Inline banner with tinted border + icon, optional underlined inline action (Editor sem microfone). */
export interface NoticeProps{ icon?: IconName; tone?: 'warning'|'danger'; children?: React.ReactNode; action?: string; onAction?: ()=>void; }
export declare function Notice(props: NoticeProps): JSX.Element;
