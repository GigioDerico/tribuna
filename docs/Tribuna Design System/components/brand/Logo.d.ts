/** Tribuna mark — "a tribuna": mint rounded square with an ink T shaped like a lectern (rounded desk bar + tapered stand) with optional wordmark in Bricolage Grotesque.
 * @startingPoint section="Brand" subtitle="Mark + wordmark lockups" viewport="700x320" */
export interface LogoProps{ /** sm=28 (app bar), md=40 (onboarding), lg=64, or px */ size?: 'sm'|'md'|'lg'|number; /** mint = primary; ink = petroleum square for light/print; black/white = mono */ variant?: 'mint'|'ink'|'black'|'white'; wordmark?: boolean; label?: string; style?: React.CSSProperties; }
export declare function Logo(props: LogoProps): JSX.Element;
