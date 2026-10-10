/** The teleprompter text body: words before the cursor fade to the read opacity (A9, 40%), the current word is highlighted (A8, mint, bold). Default 44px / 1.4 / Médio (500), side margin 8%.
 * @startingPoint section="Prompter" subtitle="Voice-following prompt text" viewport="700x300" */
export interface PromptTextProps{ text: string; /** index of the current word */ cursor?: number; fontSize?: number; lineHeight?: number; weight?: 400|500|700; /** % of width per side */ margin?: number; color?: string; highlight?: string; readOpacity?: number; fontFamily?: string; onWordClick?: (index: number)=>void; style?: React.CSSProperties; }
export declare function PromptText(props: PromptTextProps): JSX.Element;
