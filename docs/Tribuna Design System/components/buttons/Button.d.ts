import { IconName } from '../brand/Icon';
/** Tribuna button. Heights are fixed by size: xl 64 (Iniciar/Continuar), lg 56 (onboarding), md 52 (pause grid), pill 48 (Colar/Limpar, dialog actions).
 * @startingPoint section="Controls" subtitle="Primary, outline, tonal & text buttons" viewport="700x340" */
export interface ButtonProps{
  variant?: 'primary'|'outline'|'outlineNeutral'|'secondary'|'tonal'|'tonalDanger'|'text'|'textAccent'|'textDanger';
  size?: 'xl'|'lg'|'md'|'pill';
  icon?: IconName; fullWidth?: boolean; disabled?: boolean;
  children?: React.ReactNode; onClick?: (e: any)=>void; style?: React.CSSProperties; type?: 'button'|'submit';
}
export declare function Button(props: ButtonProps): JSX.Element;
