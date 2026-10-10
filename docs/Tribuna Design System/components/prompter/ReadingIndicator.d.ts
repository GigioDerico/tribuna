/** Fixed reading line marker (A11): side arrow (default), band, or none. Positioned absolutely at A10 (30% of height). */
export interface ReadingIndicatorProps{ top?: number; variant?: 'arrow'|'band'|'none'; color?: string; lineHeight?: number; }
export declare function ReadingIndicator(props: ReadingIndicatorProps): JSX.Element|null;
