/** Material-style alert dialog: 28px radius, Bricolage title, right-aligned text-button actions. */
export interface DialogProps{ title: string; children?: React.ReactNode; /** usually 2 <Button variant="text…" size="pill"> */ actions?: React.ReactNode; }
export declare function Dialog(props: DialogProps): JSX.Element;
