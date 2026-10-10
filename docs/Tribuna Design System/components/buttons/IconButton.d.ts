import { IconName } from '../brand/Icon';
/** Icon-only control. ghost = 48px transparent circle (Configurações, Fechar, Voltar); outlined = 44px bordered circle (stepper ±); tile = 56×40 bordered tile with caption below (Aparência shortcut). */
export interface IconButtonProps{ icon: IconName; /** required accessible label; shown as caption for tile */ label: string; variant?: 'ghost'|'outlined'|'tile'; size?: number; iconSize?: number; strokeWidth?: number; onClick?: ()=>void; style?: React.CSSProperties; }
export declare function IconButton(props: IconButtonProps): JSX.Element;
