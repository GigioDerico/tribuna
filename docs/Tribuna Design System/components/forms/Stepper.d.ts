/** − value + stepper (Tempo-alvo "30 min"). */
export interface StepperProps{ value: React.ReactNode; onDecrement?: ()=>void; onIncrement?: ()=>void; decLabel?: string; incLabel?: string; }
export declare function Stepper(props: StepperProps): JSX.Element;
