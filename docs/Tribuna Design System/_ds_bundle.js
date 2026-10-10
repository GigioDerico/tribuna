/* @ds-bundle: {"format":4,"namespace":"TribunaDesignSystem_cb0bc4","components":[{"name":"ICON_NAMES","sourcePath":"components/brand/Icon.jsx"},{"name":"Icon","sourcePath":"components/brand/Icon.jsx"},{"name":"Logo","sourcePath":"components/brand/Logo.jsx"},{"name":"Button","sourcePath":"components/buttons/Button.jsx"},{"name":"IconButton","sourcePath":"components/buttons/IconButton.jsx"},{"name":"Dialog","sourcePath":"components/feedback/Dialog.jsx"},{"name":"InfoCard","sourcePath":"components/feedback/InfoCard.jsx"},{"name":"Notice","sourcePath":"components/feedback/Notice.jsx"},{"name":"ProgressBar","sourcePath":"components/feedback/ProgressBar.jsx"},{"name":"Chip","sourcePath":"components/forms/Chip.jsx"},{"name":"ColorButton","sourcePath":"components/forms/ColorButton.jsx"},{"name":"SegmentedControl","sourcePath":"components/forms/SegmentedControl.jsx"},{"name":"Slider","sourcePath":"components/forms/Slider.jsx"},{"name":"Stepper","sourcePath":"components/forms/Stepper.jsx"},{"name":"Switch","sourcePath":"components/forms/Switch.jsx"},{"name":"TextArea","sourcePath":"components/forms/TextArea.jsx"},{"name":"ThemeSwatch","sourcePath":"components/forms/ThemeSwatch.jsx"},{"name":"PROMPT_PRESETS","sourcePath":"components/forms/ThemeSwatch.jsx"},{"name":"BottomSheet","sourcePath":"components/layout/BottomSheet.jsx"},{"name":"FeatureItem","sourcePath":"components/layout/FeatureItem.jsx"},{"name":"NavItem","sourcePath":"components/layout/NavItem.jsx"},{"name":"SettingsRow","sourcePath":"components/layout/SettingsRow.jsx"},{"name":"SettingsSection","sourcePath":"components/layout/SettingsSection.jsx"},{"name":"TopBar","sourcePath":"components/layout/TopBar.jsx"},{"name":"Countdown","sourcePath":"components/prompter/Countdown.jsx"},{"name":"MicIndicator","sourcePath":"components/prompter/MicIndicator.jsx"},{"name":"PausePill","sourcePath":"components/prompter/PausePill.jsx"},{"name":"PromptText","sourcePath":"components/prompter/PromptText.jsx"},{"name":"ReadingIndicator","sourcePath":"components/prompter/ReadingIndicator.jsx"},{"name":"SpeedIndicator","sourcePath":"components/prompter/SpeedIndicator.jsx"},{"name":"TimeWarningBorder","sourcePath":"components/prompter/TimeWarningBorder.jsx"},{"name":"TimerBar","sourcePath":"components/prompter/TimerBar.jsx"}],"sourceHashes":{"components/brand/Icon.jsx":"d8098674e68d","components/brand/Logo.jsx":"5a8c82772e52","components/buttons/Button.jsx":"229737e048d3","components/buttons/IconButton.jsx":"fc9438f652de","components/feedback/Dialog.jsx":"80c1043fea47","components/feedback/InfoCard.jsx":"3d3cd012064a","components/feedback/Notice.jsx":"c3deae493393","components/feedback/ProgressBar.jsx":"fce406ac63d0","components/forms/Chip.jsx":"9d5e4cdd28b6","components/forms/ColorButton.jsx":"fb1dba97cb23","components/forms/SegmentedControl.jsx":"1e4bfddcb3cd","components/forms/Slider.jsx":"bf3de0478f1f","components/forms/Stepper.jsx":"5d71b19badb4","components/forms/Switch.jsx":"2aa4e8ad5531","components/forms/TextArea.jsx":"01e854488d43","components/forms/ThemeSwatch.jsx":"5d8ac3fb4ff8","components/layout/BottomSheet.jsx":"fe6da08efa0e","components/layout/FeatureItem.jsx":"5fb02278f42d","components/layout/NavItem.jsx":"ab5596791ad5","components/layout/SettingsRow.jsx":"c44036e3e3eb","components/layout/SettingsSection.jsx":"28f6b8186ece","components/layout/TopBar.jsx":"1acac7521294","components/prompter/Countdown.jsx":"34c4a1cc7acc","components/prompter/MicIndicator.jsx":"56ff2a4ef7a7","components/prompter/PausePill.jsx":"373249531689","components/prompter/PromptText.jsx":"fcb16a2c2e1c","components/prompter/ReadingIndicator.jsx":"0197f080b7b6","components/prompter/SpeedIndicator.jsx":"32fade6861cc","components/prompter/TimeWarningBorder.jsx":"e382177e6f2c","components/prompter/TimerBar.jsx":"339ae7a8598b","ui_kits/app/App.jsx":"d8bbf60c19bd","ui_kits/app/Editor.jsx":"364badf0f0d5","ui_kits/app/Onboarding.jsx":"116418562924","ui_kits/app/Prompter.jsx":"f287b5dbcad6","ui_kits/app/QuickSettings.jsx":"a1d2aa47f7d4","ui_kits/app/Settings.jsx":"dcc524416f5f"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.TribunaDesignSystem_cb0bc4 = window.TribunaDesignSystem_cb0bc4 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/brand/Icon.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const P = {
  'wifi-off': ['s', '<path d="M2 9a15 15 0 0 1 4-2.5M22 9a15 15 0 0 0-8-4M5.5 13a10 10 0 0 1 3-2M18.5 13a10 10 0 0 0-3.5-2.4M9 17a5 5 0 0 1 6 0M12 21h.01M3 3l18 18"/>'],
  lock: ['s', '<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>'],
  'mic-off': ['s', '<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3M3 3l18 18"/>'],
  mic: ['s', '<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3"/>'],
  warning: ['s', '<path d="M12 3l10 18H2z"/><path d="M12 10v5M12 18v.5"/>'],
  settings: ['s', '<path d="M10.3 3h3.4l.5 2.2 1.6.9 2.1-.8 1.7 3-1.6 1.6v1.8l1.6 1.6-1.7 3-2.1-.8-1.6.9-.5 2.2h-3.4l-.5-2.2-1.6-.9-2.1.8-1.7-3 1.6-1.6V9.9L4.4 8.3l1.7-3 2.1.8 1.6-.9z"/><circle cx="12" cy="12" r="2.5"/>'],
  paste: ['s', '<rect x="6" y="5" width="12" height="16" rx="2"/><path d="M9 5V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1M9 11h6M9 15h4"/>'],
  trash: ['s', '<path d="M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13M10 11v6M14 11v6"/>'],
  sliders: ['s', '<path d="M4 7h10M18 7h2M4 17h2M10 17h10"/><circle cx="16" cy="7" r="2"/><circle cx="8" cy="17" r="2"/>'],
  play: ['f', '<path d="M8 5l11 7-11 7z"/>'],
  close: ['s', '<path d="M6 6l12 12M18 6L6 18"/>'],
  speed: ['s', '<path d="M4 18a8 8 0 1 1 16 0"/><path d="M12 18l4-6"/>'],
  pause: ['f', '<rect x="6" y="5" width="4" height="14" rx="1"/><rect x="14" y="5" width="4" height="14" rx="1"/>'],
  restart: ['s', '<path d="M4 12a8 8 0 1 0 3-6.2M4 4v4h4"/>'],
  edit: ['s', '<path d="M4 20l1-4L16 5l3 3L8 19z"/>'],
  stop: ['f', '<rect x="6" y="6" width="12" height="12" rx="2"/>'],
  'chevron-left': ['s', '<path d="M15 5l-7 7 7 7"/>'],
  'chevron-right': ['s', '<path d="M9 5l7 7-7 7"/>'],
  minus: ['s', '<path d="M5 12h14"/>'],
  plus: ['s', '<path d="M5 12h14M12 5v14"/>']
};
const ICON_NAMES = Object.keys(P);
function Icon({
  name,
  size = 24,
  strokeWidth = 2,
  color = 'currentColor',
  style,
  ...rest
}) {
  const d = P[name];
  if (!d) return null;
  const filled = d[0] === 'f';
  return /*#__PURE__*/React.createElement("svg", _extends({
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    "aria-hidden": "true",
    fill: filled ? color : 'none',
    stroke: filled ? 'none' : color,
    strokeWidth: strokeWidth,
    strokeLinecap: "round",
    strokeLinejoin: "round",
    style: {
      flex: '0 0 auto',
      display: 'block',
      ...style
    },
    dangerouslySetInnerHTML: {
      __html: d[1]
    }
  }, rest));
}
Object.assign(__ds_scope, { ICON_NAMES, Icon });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/brand/Icon.jsx", error: String((e && e.message) || e) }); }

// components/brand/Logo.jsx
try { (() => {
const MARK = {
  mint: ['var(--mint-500)', 'var(--mint-ink)'],
  ink: ['var(--petrol-900)', 'var(--mint-500)'],
  black: ['#000', '#fff'],
  white: ['#fff', '#000']
};
function Logo({
  size = 'md',
  variant = 'mint',
  wordmark = true,
  label = 'Tribuna',
  style
}) {
  const px = typeof size === 'number' ? size : size === 'sm' ? 28 : size === 'lg' ? 64 : 40;
  const [bg, bar] = MARK[variant] || MARK.mint;
  const fs = size === 'sm' ? 22 : size === 'lg' ? 40 : Math.max(20, Math.round(px * 0.6));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: size === 'sm' ? 10 : size === 'lg' ? 18 : 12,
      ...style
    }
  }, /*#__PURE__*/React.createElement("svg", {
    width: px,
    height: px,
    viewBox: "0 0 40 40",
    role: "img",
    "aria-label": "Logo do Tribuna"
  }, /*#__PURE__*/React.createElement("rect", {
    width: "40",
    height: "40",
    rx: "12",
    fill: bg
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 12.5h22",
    stroke: bar,
    strokeWidth: "4",
    strokeLinecap: "round"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16.5 16h7l1.5 15h-10z",
    fill: bar
  })), wordmark && /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: fs,
      lineHeight: '28px',
      color: 'var(--fg-1)'
    }
  }, label));
}
Object.assign(__ds_scope, { Logo });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/brand/Logo.jsx", error: String((e && e.message) || e) }); }

// components/buttons/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const SIZES = {
  xl: {
    h: 64,
    r: 20,
    fs: 20,
    gap: 10,
    ic: 22,
    px: 24
  },
  lg: {
    h: 56,
    r: 16,
    fs: 18,
    gap: 8,
    ic: 22,
    px: 24
  },
  md: {
    h: 52,
    r: 16,
    fs: 16,
    gap: 8,
    ic: 20,
    px: 16
  },
  pill: {
    h: 48,
    r: 24,
    fs: 16,
    gap: 8,
    ic: 20,
    px: 16
  }
};
const VARIANTS = {
  primary: {
    background: 'var(--accent)',
    color: 'var(--on-accent)',
    border: '0'
  },
  outline: {
    background: 'transparent',
    color: 'var(--accent-text)',
    border: '1px solid var(--accent-text)'
  },
  outlineNeutral: {
    background: 'transparent',
    color: 'var(--fg-1)',
    border: '1px solid var(--border-strong)'
  },
  secondary: {
    background: 'transparent',
    color: 'var(--fg-1)',
    border: '1px solid var(--border-1)'
  },
  tonal: {
    background: 'var(--surface-2)',
    color: 'var(--fg-1)',
    border: '1px solid var(--border-1)'
  },
  tonalDanger: {
    background: 'var(--surface-2)',
    color: 'var(--danger)',
    border: '1px solid var(--danger-border)'
  },
  text: {
    background: 'transparent',
    color: 'var(--fg-1)',
    border: '0'
  },
  textAccent: {
    background: 'transparent',
    color: 'var(--accent-text)',
    border: '0'
  },
  textDanger: {
    background: 'transparent',
    color: 'var(--danger)',
    border: '0'
  }
};
function Button({
  variant = 'primary',
  size = 'lg',
  icon,
  iconFilled,
  fullWidth,
  disabled,
  children,
  onClick,
  style,
  type = 'button',
  ...rest
}) {
  const s = SIZES[size] || SIZES.lg;
  const v = VARIANTS[variant] || VARIANTS.primary;
  const isText = variant.startsWith('text');
  return /*#__PURE__*/React.createElement("button", _extends({
    type: type,
    disabled: disabled,
    onClick: onClick
  }, rest, {
    style: {
      height: s.h,
      padding: '0 ' + (isText ? 16 : s.px) + 'px',
      borderRadius: isText ? 24 : s.r,
      fontFamily: 'var(--font-body)',
      fontSize: s.fs,
      fontWeight: 700,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: s.gap,
      boxSizing: 'border-box',
      cursor: disabled ? 'default' : 'pointer',
      width: fullWidth ? '100%' : undefined,
      opacity: disabled ? .38 : 1,
      whiteSpace: 'nowrap',
      ...v,
      ...style
    }
  }), icon && /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: s.ic
  }), children);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/buttons/Button.jsx", error: String((e && e.message) || e) }); }

// components/buttons/IconButton.jsx
try { (() => {
function IconButton({
  icon,
  label,
  variant = 'ghost',
  size,
  iconSize,
  strokeWidth,
  onClick,
  style
}) {
  if (variant === 'tile') {
    return /*#__PURE__*/React.createElement("div", {
      style: {
        flex: '0 0 64px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 2,
        ...style
      }
    }, /*#__PURE__*/React.createElement("button", {
      type: "button",
      "aria-label": label,
      onClick: onClick,
      style: {
        width: 56,
        height: 40,
        border: '1px solid var(--border-1)',
        borderRadius: 16,
        background: 'var(--surface-1)',
        color: 'var(--fg-1)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'pointer',
        padding: 0
      }
    }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
      name: icon,
      size: iconSize || 24
    })), /*#__PURE__*/React.createElement("span", {
      "aria-hidden": "true",
      style: {
        font: 'var(--type-caption)',
        color: 'var(--fg-1)'
      }
    }, label));
  }
  const px = size || (variant === 'outlined' ? 44 : 48);
  const outlined = variant === 'outlined';
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": label,
    onClick: onClick,
    style: {
      width: px,
      height: px,
      padding: 0,
      border: outlined ? '1px solid var(--border-1)' : 0,
      borderRadius: px / 2,
      background: outlined ? 'var(--surface-1)' : 'transparent',
      color: 'var(--fg-1)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      cursor: 'pointer',
      flex: '0 0 auto',
      ...style
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: iconSize || (outlined ? 18 : 24),
    strokeWidth: strokeWidth || (outlined ? 2.4 : 2)
  }));
}
Object.assign(__ds_scope, { IconButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/buttons/IconButton.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Dialog.jsx
try { (() => {
function Dialog({
  title,
  children,
  actions
}) {
  return /*#__PURE__*/React.createElement("div", {
    role: "alertdialog",
    "aria-label": title,
    style: {
      boxSizing: 'border-box',
      padding: 24,
      borderRadius: 28,
      background: 'var(--surface-dialog)',
      display: 'flex',
      flexDirection: 'column',
      gap: 12,
      color: 'var(--fg-1)',
      fontFamily: 'var(--font-body)',
      width: '100%',
      maxWidth: 340,
      border: 'var(--dialog-border, 0)'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-title-sm)',
      fontWeight: 700
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-paragraph)',
      color: 'var(--fg-dialog)'
    }
  }, children), actions && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      display: 'flex',
      justifyContent: 'flex-end',
      gap: 8
    }
  }, actions));
}
Object.assign(__ds_scope, { Dialog });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Dialog.jsx", error: String((e && e.message) || e) }); }

// components/feedback/InfoCard.jsx
try { (() => {
function InfoCard({
  icon = 'mic',
  title,
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 16,
      padding: 16,
      background: 'var(--surface-1)',
      border: '1px solid var(--border-1)',
      borderRadius: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: '0 0 40px',
      width: 40,
      height: 40,
      borderRadius: 20,
      background: 'var(--accent)',
      color: 'var(--on-accent)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 22,
    strokeWidth: 2.2
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-body)',
      fontWeight: 700,
      color: 'var(--fg-1)'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-small)',
      color: 'var(--fg-2)'
    }
  }, children)));
}
Object.assign(__ds_scope, { InfoCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/InfoCard.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Notice.jsx
try { (() => {
function Notice({
  icon = 'mic-off',
  tone = 'warning',
  children,
  action,
  onAction
}) {
  const c = tone === 'danger' ? 'var(--danger)' : 'var(--warning-text)';
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      padding: '12px 14px',
      borderRadius: 14,
      background: 'var(--surface-1)',
      border: '1px solid ' + (tone === 'danger' ? 'var(--danger-border)' : 'var(--warning-border)')
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 22,
    color: c
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-small)',
      color: 'var(--fg-soft)'
    }
  }, children, " ", action && /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onAction,
    style: {
      display: 'inline',
      padding: 0,
      border: 0,
      background: 'transparent',
      color: 'var(--accent-text)',
      fontFamily: 'inherit',
      fontSize: 14,
      lineHeight: '20px',
      fontWeight: 700,
      textDecoration: 'underline',
      cursor: 'pointer'
    }
  }, action)));
}
Object.assign(__ds_scope, { Notice });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Notice.jsx", error: String((e && e.message) || e) }); }

// components/feedback/ProgressBar.jsx
try { (() => {
function ProgressBar({
  value = 0,
  variant = 'setup',
  label,
  style
}) {
  const thin = variant === 'thin';
  return /*#__PURE__*/React.createElement("div", {
    role: "progressbar",
    "aria-label": label,
    "aria-valuemin": 0,
    "aria-valuemax": 100,
    "aria-valuenow": value,
    style: {
      height: thin ? 4 : 12,
      borderRadius: thin ? 0 : 6,
      background: thin ? 'var(--track)' : 'var(--track-setup)',
      overflow: 'hidden',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: value + '%',
      height: '100%',
      borderRadius: thin ? 0 : 6,
      background: 'var(--accent)',
      transition: 'width .3s'
    }
  }));
}
Object.assign(__ds_scope, { ProgressBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/ProgressBar.jsx", error: String((e && e.message) || e) }); }

// components/forms/Chip.jsx
try { (() => {
function Chip({
  selected,
  children,
  onClick,
  style
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-pressed": !!selected,
    onClick: onClick,
    style: {
      height: 44,
      padding: '0 16px',
      border: selected ? 0 : '1px solid var(--border-1)',
      borderRadius: 22,
      background: selected ? 'var(--accent)' : 'transparent',
      color: selected ? 'var(--on-accent)' : 'var(--fg-1)',
      fontFamily: 'var(--font-body)',
      fontSize: 14,
      fontWeight: 700,
      cursor: 'pointer',
      whiteSpace: 'nowrap',
      ...style
    }
  }, children);
}
Object.assign(__ds_scope, { Chip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Chip.jsx", error: String((e && e.message) || e) }); }

// components/forms/ColorButton.jsx
try { (() => {
function ColorButton({
  color,
  label,
  onClick,
  style
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onClick,
    style: {
      height: 48,
      padding: '0 12px',
      border: '1px solid var(--border-1)',
      borderRadius: 14,
      background: 'var(--surface-2)',
      color: 'var(--fg-1)',
      fontFamily: 'var(--font-body)',
      fontSize: 14,
      fontWeight: 700,
      display: 'flex',
      alignItems: 'center',
      gap: 8,
      cursor: 'pointer',
      minWidth: 0,
      ...style
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 20,
      height: 20,
      flex: '0 0 20px',
      borderRadius: 10,
      background: color,
      border: '1px solid var(--border-strong)',
      boxSizing: 'border-box'
    }
  }), label);
}
Object.assign(__ds_scope, { ColorButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/ColorButton.jsx", error: String((e && e.message) || e) }); }

// components/forms/SegmentedControl.jsx
try { (() => {
function SegmentedControl({
  options = [],
  value,
  onChange,
  size = 'md',
  label,
  trackColor,
  style
}) {
  const fs = size === 'sm' ? 14 : 16;
  return /*#__PURE__*/React.createElement("div", {
    role: "group",
    "aria-label": label,
    style: {
      display: 'flex',
      padding: 4,
      borderRadius: 16,
      background: trackColor || 'var(--surface-track)',
      gap: 4,
      ...style
    }
  }, options.map(o => {
    const opt = typeof o === 'string' ? {
      value: o,
      label: o
    } : o;
    const on = opt.value === value;
    const dis = opt.disabled;
    return /*#__PURE__*/React.createElement("button", {
      key: opt.value,
      type: "button",
      "aria-pressed": on,
      disabled: dis,
      onClick: () => !dis && onChange && onChange(opt.value),
      style: {
        flex: 1,
        height: 44,
        border: 0,
        borderRadius: 12,
        background: on ? 'var(--accent)' : 'transparent',
        color: on ? 'var(--on-accent)' : dis ? 'var(--fg-disabled)' : 'var(--fg-1)',
        fontFamily: 'var(--font-body)',
        fontSize: fs,
        fontWeight: 700,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        cursor: dis ? 'default' : 'pointer',
        padding: '0 8px',
        minWidth: 0
      }
    }, opt.icon && /*#__PURE__*/React.createElement(__ds_scope.Icon, {
      name: opt.icon,
      size: 18,
      strokeWidth: 2.2
    }), opt.label);
  }));
}
Object.assign(__ds_scope, { SegmentedControl });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/SegmentedControl.jsx", error: String((e && e.message) || e) }); }

// components/forms/Slider.jsx
try { (() => {
function Slider({
  label,
  value,
  min = 0,
  max = 100,
  step = 1,
  display,
  onChange,
  variant = 'settings',
  id
}) {
  const sheet = variant === 'sheet';
  const fid = id || 's-' + String(label).replace(/\W+/g, '-');
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("label", {
    htmlFor: fid,
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      gap: 12,
      fontFamily: 'var(--font-body)',
      fontSize: sheet ? 14 : 16,
      lineHeight: sheet ? '20px' : '22px',
      fontWeight: 700,
      color: sheet ? 'var(--fg-2)' : 'var(--fg-1)'
    }
  }, /*#__PURE__*/React.createElement("span", null, label), /*#__PURE__*/React.createElement("span", {
    style: {
      color: sheet ? 'var(--fg-1)' : 'var(--fg-2)',
      fontWeight: sheet ? 700 : 500
    }
  }, display ?? value)), /*#__PURE__*/React.createElement("input", {
    id: fid,
    type: "range",
    min: min,
    max: max,
    step: step,
    value: value,
    onChange: e => onChange && onChange(Number(e.target.value)),
    style: {
      width: '100%',
      height: 32,
      margin: 0,
      accentColor: 'var(--accent)'
    }
  }));
}
Object.assign(__ds_scope, { Slider });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Slider.jsx", error: String((e && e.message) || e) }); }

// components/forms/Stepper.jsx
try { (() => {
function Stepper({
  value,
  onDecrement,
  onIncrement,
  decLabel = 'Diminuir',
  incLabel = 'Aumentar'
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      flex: '0 0 auto',
      display: 'flex',
      alignItems: 'center',
      gap: 4
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    variant: "outlined",
    icon: "minus",
    label: decLabel,
    onClick: onDecrement
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      minWidth: 56,
      textAlign: 'center',
      font: 'var(--type-body)',
      fontWeight: 700,
      color: 'var(--fg-1)'
    }
  }, value), /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    variant: "outlined",
    icon: "plus",
    label: incLabel,
    onClick: onIncrement
  }));
}
Object.assign(__ds_scope, { Stepper });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Stepper.jsx", error: String((e && e.message) || e) }); }

// components/forms/Switch.jsx
try { (() => {
function Switch({
  checked,
  onChange,
  label,
  id,
  disabled
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    role: "switch",
    id: id,
    "aria-checked": !!checked,
    "aria-label": label,
    disabled: disabled,
    onClick: () => onChange && onChange(!checked),
    style: {
      position: 'relative',
      flex: '0 0 52px',
      width: 52,
      height: 32,
      padding: 0,
      borderRadius: 16,
      boxSizing: 'border-box',
      border: checked ? '0' : '2px solid var(--border-strong)',
      background: checked ? 'var(--accent)' : 'var(--surface-1)',
      cursor: 'pointer',
      opacity: disabled ? .38 : 1,
      transition: 'background .15s'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'absolute',
      top: checked ? 4 : 6,
      left: checked ? 24 : 6,
      width: checked ? 24 : 16,
      height: checked ? 24 : 16,
      borderRadius: 12,
      background: checked ? 'var(--on-accent)' : 'var(--border-strong)',
      transition: 'all .15s'
    }
  }));
}
Object.assign(__ds_scope, { Switch });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Switch.jsx", error: String((e && e.message) || e) }); }

// components/forms/TextArea.jsx
try { (() => {
function TextArea({
  value,
  onChange,
  placeholder = 'Cole ou digite seu discurso aqui',
  label = 'Texto do discurso',
  style
}) {
  return /*#__PURE__*/React.createElement("textarea", {
    "aria-label": label,
    value: value,
    placeholder: placeholder,
    onChange: e => onChange && onChange(e.target.value),
    style: {
      flex: 1,
      minHeight: 0,
      width: '100%',
      resize: 'none',
      boxSizing: 'border-box',
      padding: 20,
      border: '1px solid var(--border-1)',
      borderRadius: 16,
      background: 'var(--surface-1)',
      color: 'var(--fg-1)',
      fontFamily: 'var(--font-body)',
      fontSize: 20,
      lineHeight: '30px',
      outline: 'none',
      ...style
    }
  });
}
Object.assign(__ds_scope, { TextArea });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/TextArea.jsx", error: String((e && e.message) || e) }); }

// components/forms/ThemeSwatch.jsx
try { (() => {
function ThemeSwatch({
  bg,
  fg,
  label,
  selected,
  onClick
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-pressed": !!selected,
    onClick: onClick,
    style: {
      padding: 0,
      border: 0,
      background: 'transparent',
      color: 'var(--fg-1)',
      fontFamily: 'var(--font-body)',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 4,
      cursor: 'pointer',
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: '100%',
      height: 40,
      boxSizing: 'border-box',
      border: '2px solid ' + (selected ? 'var(--accent)' : 'var(--border-1)'),
      borderRadius: 10,
      background: bg,
      color: fg,
      fontSize: 18,
      fontWeight: 700,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, "Aa"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--type-caption)',
      whiteSpace: 'nowrap'
    }
  }, label));
}
const PROMPT_PRESETS = [{
  id: 'branco',
  label: 'Branco/preto',
  bg: '#05141A',
  fg: '#FFFFFF'
}, {
  id: 'preto',
  label: 'Preto/branco',
  bg: '#FFFFFF',
  fg: '#000000'
}, {
  id: 'amarelo',
  label: 'Amarelo/preto',
  bg: '#05141A',
  fg: '#FFE14D'
}, {
  id: 'verde',
  label: 'Verde/preto',
  bg: '#05141A',
  fg: '#5CFF8A'
}];
Object.assign(__ds_scope, { ThemeSwatch, PROMPT_PRESETS });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/ThemeSwatch.jsx", error: String((e && e.message) || e) }); }

// components/layout/BottomSheet.jsx
try { (() => {
function BottomSheet({
  title,
  onClose,
  children,
  variant = 'sheet',
  style
}) {
  const panel = variant === 'panel';
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 0,
      right: 0,
      bottom: 0,
      boxSizing: 'border-box',
      padding: panel ? '16px 16px 24px' : '12px 20px 24px',
      borderRadius: panel ? '24px 24px 0 0' : '28px 28px 0 0',
      background: panel ? 'var(--surface-1)' : 'var(--surface-sheet)',
      display: 'flex',
      flexDirection: 'column',
      gap: panel ? 12 : 16,
      color: 'var(--fg-1)',
      fontFamily: 'var(--font-body)',
      ...style
    }
  }, !panel && /*#__PURE__*/React.createElement("div", {
    style: {
      alignSelf: 'center',
      width: 40,
      height: 4,
      borderRadius: 2,
      background: 'var(--handle)'
    }
  }), title && /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-title-sm)',
      fontWeight: 700
    }
  }, title), onClose && /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    icon: "close",
    label: "Fechar",
    onClick: onClose,
    style: {
      marginRight: -8
    }
  })), children);
}
Object.assign(__ds_scope, { BottomSheet });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/layout/BottomSheet.jsx", error: String((e && e.message) || e) }); }

// components/layout/FeatureItem.jsx
try { (() => {
function FeatureItem({
  icon,
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: '0 0 44px',
      width: 44,
      height: 44,
      borderRadius: 22,
      background: 'var(--surface-1)',
      color: 'var(--fg-1)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 22
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-body)',
      color: 'var(--fg-1)'
    }
  }, children));
}
Object.assign(__ds_scope, { FeatureItem });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/layout/FeatureItem.jsx", error: String((e && e.message) || e) }); }

// components/layout/NavItem.jsx
try { (() => {
function NavItem({
  active,
  children,
  onClick
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-current": active ? 'page' : undefined,
    onClick: onClick,
    style: {
      height: 56,
      padding: '0 20px',
      border: 0,
      borderRadius: 16,
      background: active ? 'var(--surface-2)' : 'transparent',
      color: active ? 'var(--accent-text)' : 'var(--fg-1)',
      fontFamily: 'var(--font-body)',
      fontSize: 18,
      fontWeight: 700,
      textAlign: 'left',
      cursor: 'pointer',
      width: '100%'
    }
  }, children);
}
Object.assign(__ds_scope, { NavItem });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/layout/NavItem.jsx", error: String((e && e.message) || e) }); }

// components/layout/SettingsRow.jsx
try { (() => {
function SettingsRow({
  label,
  description,
  control,
  stacked,
  last,
  children,
  onClick
}) {
  const base = {
    padding: '12px 20px',
    borderBottom: last ? 0 : '1px solid var(--divider)',
    color: 'var(--fg-1)',
    fontFamily: 'var(--font-body)'
  };
  const text = /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 2
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--type-body)',
      fontWeight: 700
    }
  }, label), description && /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--type-small)',
      color: 'var(--fg-2)'
    }
  }, description));
  if (stacked || children) return /*#__PURE__*/React.createElement("div", {
    style: {
      ...base,
      display: 'flex',
      flexDirection: 'column',
      gap: 8
    }
  }, label && text, children);
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    style: {
      ...base,
      minHeight: 56,
      boxSizing: 'border-box',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 16,
      cursor: onClick ? 'pointer' : 'default'
    }
  }, text, control);
}
Object.assign(__ds_scope, { SettingsRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/layout/SettingsRow.jsx", error: String((e && e.message) || e) }); }

// components/layout/SettingsSection.jsx
try { (() => {
function SettingsSection({
  title,
  children,
  first
}) {
  return /*#__PURE__*/React.createElement("section", null, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: (first ? '20px' : '28px') + ' 20px 8px',
      font: 'var(--type-overline)',
      fontWeight: 700,
      letterSpacing: 'var(--tracking-overline)',
      textTransform: 'uppercase',
      color: 'var(--accent-text)'
    }
  }, title), children);
}
Object.assign(__ds_scope, { SettingsSection });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/layout/SettingsSection.jsx", error: String((e && e.message) || e) }); }

// components/layout/TopBar.jsx
try { (() => {
function TopBar({
  title,
  onBack,
  onSettings,
  trailing
}) {
  if (title) return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 4,
      height: 56,
      padding: '0 16px 0 8px'
    }
  }, onBack !== undefined && /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    icon: "chevron-left",
    label: "Voltar",
    onClick: onBack
  }), /*#__PURE__*/React.createElement("h1", {
    style: {
      margin: 0,
      font: 'var(--type-title)',
      fontWeight: 700,
      color: 'var(--fg-1)'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), trailing);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      height: 56,
      padding: '0 0 0 8px'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Logo, {
    size: "sm"
  }), trailing || /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    icon: "settings",
    label: "Configura\xE7\xF5es",
    iconSize: 26,
    onClick: onSettings
  }));
}
Object.assign(__ds_scope, { TopBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/layout/TopBar.jsx", error: String((e && e.message) || e) }); }

// components/prompter/Countdown.jsx
try { (() => {
function Countdown({
  value = 3,
  hint = 'Toque para cancelar'
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("div", {
    "aria-label": 'Começando em ' + value,
    style: {
      font: 'var(--type-countdown)',
      fontWeight: 700,
      color: 'var(--prompt-highlight)'
    }
  }, value), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 18,
      lineHeight: '26px',
      color: 'var(--fg-soft)'
    }
  }, hint));
}
Object.assign(__ds_scope, { Countdown });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/prompter/Countdown.jsx", error: String((e && e.message) || e) }); }

// components/prompter/MicIndicator.jsx
try { (() => {
function MicIndicator({
  level = 0.6,
  active = true,
  style
}) {
  const hs = [6, 12, 18, 10, 5];
  const n = Math.round(level * hs.length);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    "aria-label": active ? 'Microfone ativo' : 'Microfone desligado',
    role: "img",
    style: {
      position: 'relative',
      width: 28,
      height: 28,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, active && /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'absolute',
      left: 0,
      top: 0,
      width: 28,
      height: 28,
      borderRadius: 14,
      background: 'var(--accent-halo)',
      transform: 'scale(' + (0.7 + level * 0.3) + ')',
      transition: 'transform .12s'
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      width: 12,
      height: 12,
      borderRadius: 6,
      background: active ? 'var(--accent)' : 'var(--fg-disabled)'
    }
  })), /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      display: 'flex',
      alignItems: 'flex-end',
      gap: 3,
      height: 18
    }
  }, hs.map((h, i) => /*#__PURE__*/React.createElement("span", {
    key: i,
    style: {
      width: 3,
      height: h,
      borderRadius: 2,
      background: i < n ? 'var(--accent)' : 'var(--border-strong)'
    }
  }))));
}
Object.assign(__ds_scope, { MicIndicator });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/prompter/MicIndicator.jsx", error: String((e && e.message) || e) }); }

// components/prompter/PausePill.jsx
try { (() => {
function PausePill({
  children = 'Pausado — toque para continuar'
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      padding: '10px 20px',
      borderRadius: 24,
      background: 'var(--surface-2)',
      color: 'var(--fg-1)',
      font: 'var(--type-body)',
      lineHeight: '24px',
      fontWeight: 700
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "pause",
    size: 18
  }), children);
}
Object.assign(__ds_scope, { PausePill });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/prompter/PausePill.jsx", error: String((e && e.message) || e) }); }

// components/prompter/PromptText.jsx
try { (() => {
function PromptText({
  text = '',
  cursor = 0,
  fontSize = 44,
  lineHeight = 1.4,
  weight = 500,
  margin = 8,
  color,
  highlight,
  readOpacity,
  fontFamily,
  onWordClick,
  style
}) {
  const words = String(text).split(/\s+/).filter(Boolean);
  const ro = readOpacity ?? 'var(--prompt-read-opacity)';
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '0 ' + margin + '%',
      fontFamily: fontFamily || 'var(--font-body)',
      fontSize,
      lineHeight,
      fontWeight: weight,
      color: color || 'var(--prompt-fg)',
      textWrap: 'pretty',
      ...style
    }
  }, words.map((w, i) => {
    const cur = i === cursor;
    const read = i < cursor;
    return /*#__PURE__*/React.createElement(React.Fragment, {
      key: i
    }, /*#__PURE__*/React.createElement("span", {
      "data-word": i,
      onClick: onWordClick ? e => {
        e.stopPropagation();
        onWordClick(i);
      } : undefined,
      style: {
        opacity: read ? ro : 1,
        color: cur ? highlight || 'var(--prompt-highlight)' : undefined,
        fontWeight: cur ? 700 : undefined,
        cursor: onWordClick ? 'pointer' : undefined,
        transition: 'color .3s, opacity .3s'
      }
    }, w), ' ');
  }));
}
Object.assign(__ds_scope, { PromptText });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/prompter/PromptText.jsx", error: String((e && e.message) || e) }); }

// components/prompter/ReadingIndicator.jsx
try { (() => {
function ReadingIndicator({
  top = 244,
  variant = 'arrow',
  color,
  lineHeight = 62
}) {
  if (variant === 'none') return null;
  if (variant === 'band') return /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: 'absolute',
      left: 0,
      right: 0,
      top: top - lineHeight / 2 + 9,
      height: lineHeight,
      background: 'var(--accent-halo)',
      pointerEvents: 'none'
    }
  });
  return /*#__PURE__*/React.createElement("svg", {
    width: "14",
    height: "18",
    viewBox: "0 0 14 18",
    "aria-label": "Linha de leitura",
    role: "img",
    style: {
      position: 'absolute',
      left: 8,
      top
    }
  }, /*#__PURE__*/React.createElement("path", {
    d: "M0 0l14 9-14 9z",
    fill: color || 'var(--prompt-highlight)'
  }));
}
Object.assign(__ds_scope, { ReadingIndicator });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/prompter/ReadingIndicator.jsx", error: String((e && e.message) || e) }); }

// components/prompter/SpeedIndicator.jsx
try { (() => {
function SpeedIndicator({
  speed = 5,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    "aria-label": 'Velocidade ' + speed,
    role: "img",
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8,
      color: 'var(--fg-2)',
      font: 'var(--type-small)',
      fontWeight: 700,
      ...style
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "speed",
    size: 20
  }), "Velocidade ", speed);
}
Object.assign(__ds_scope, { SpeedIndicator });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/prompter/SpeedIndicator.jsx", error: String((e && e.message) || e) }); }

// components/prompter/TimeWarningBorder.jsx
try { (() => {
function TimeWarningBorder({
  level = 'fiveMin'
}) {
  if (level === 'none') return null;
  const c = level === 'oneMin' ? 'var(--danger-ring)' : 'var(--warning)';
  return /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: 'absolute',
      inset: 0,
      boxShadow: 'inset 0 0 0 8px ' + c,
      pointerEvents: 'none'
    }
  });
}
Object.assign(__ds_scope, { TimeWarningBorder });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/prompter/TimeWarningBorder.jsx", error: String((e && e.message) || e) }); }

// components/prompter/TimerBar.jsx
try { (() => {
function TimerBar({
  elapsed = '00:00',
  remaining,
  warning = 'none',
  inset = 20
}) {
  const rc = warning === 'oneMin' ? 'var(--danger)' : warning === 'fiveMin' ? 'var(--warning-text)' : 'var(--fg-2)';
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: inset,
      top: inset,
      right: inset,
      display: 'flex',
      justifyContent: 'space-between',
      font: 'var(--type-timer)',
      fontWeight: 700,
      fontVariantNumeric: 'tabular-nums',
      color: 'var(--prompt-fg)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    "aria-label": "Tempo decorrido"
  }, elapsed), remaining && /*#__PURE__*/React.createElement("span", {
    style: {
      color: rc,
      fontWeight: warning === 'none' ? 500 : 700
    }
  }, "restam ", remaining));
}
Object.assign(__ds_scope, { TimerBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/prompter/TimerBar.jsx", error: String((e && e.message) || e) }); }

// ui_kits/app/App.jsx
try { (() => {
const AP = window.TribunaDesignSystem_cb0bc4;
function TribunaApp({
  theme: themeProp,
  start = 'welcome',
  jumpRef
}) {
  const [theme, setTheme] = React.useState(themeProp);
  React.useEffect(() => setTheme(themeProp), [themeProp]);
  const [screen, setScreen] = React.useState(start);
  const [text, setText] = React.useState(start === 'welcome' ? '' : window.SAMPLE_SPEECH);
  const [mode, setMode] = React.useState('voz');
  const [sheet, setSheet] = React.useState(false);
  const [clearAsk, setClearAsk] = React.useState(false);
  const [phase, setPhase] = React.useState('countdown');
  const [cursor, setCursor] = React.useState(0);
  const [elapsed, setElapsed] = React.useState(0);
  const target = 30 * 60;
  const [prefs, setPrefs] = React.useState({
    preset: theme === 'light' ? 'preto' : 'branco',
    size: 44,
    lh: 14,
    margin: 8,
    speed: 5,
    mirror: false
  });
  React.useEffect(() => {
    if (screen !== 'prompter' || phase === 'countdown') return;
    const id = setInterval(() => setElapsed(e => e + 1), 1000);
    return () => clearInterval(id);
  }, [screen, phase]);
  if (jumpRef) jumpRef.current = s => setElapsed(target - s);
  const startReading = () => {
    setCursor(0);
    setElapsed(0);
    setPhase('countdown');
    setScreen('prompter');
  };
  let body;
  if (screen === 'welcome') body = /*#__PURE__*/React.createElement(Welcome, {
    onAllow: () => setScreen('setup')
  });else if (screen === 'setup') body = /*#__PURE__*/React.createElement(Setup, {
    onDone: () => setScreen('editor')
  });else if (screen === 'settings') body = /*#__PURE__*/React.createElement(SettingsScreen, {
    prefs: prefs,
    setPrefs: setPrefs,
    theme: theme,
    setTheme: t => {
      setTheme(t);
      setPrefs(p => ({
        ...p,
        preset: t === 'light' ? 'preto' : 'branco'
      }));
    },
    onBack: () => setScreen('editor')
  });else if (screen === 'prompter') body = /*#__PURE__*/React.createElement(Prompter, {
    text: text,
    prefs: prefs,
    mode: mode,
    setMode: setMode,
    elapsed: elapsed,
    target: target,
    phase: phase,
    setPhase: setPhase,
    cursor: cursor,
    setCursor: setCursor,
    onEnd: () => setScreen('editor'),
    onEdit: () => setScreen('editor'),
    onAppearance: () => setSheet(true),
    onFinish: () => setScreen('end')
  });else if (screen === 'end') body = /*#__PURE__*/React.createElement(EndScreen, {
    total: elapsed,
    target: target,
    onBack: () => setScreen('editor')
  });else body = /*#__PURE__*/React.createElement(Editor, {
    text: text,
    setText: setText,
    mode: mode,
    setMode: setMode,
    micOk: true,
    onStart: startReading,
    onSettings: () => setScreen('settings'),
    onAppearance: () => setSheet(true),
    onClear: () => setClearAsk(true)
  });
  return /*#__PURE__*/React.createElement("div", {
    "data-theme": theme,
    "data-screen-label": screen,
    style: {
      position: 'relative',
      width: 390,
      height: 844,
      overflow: 'hidden',
      background: 'var(--bg-app)',
      color: 'var(--fg-1)',
      fontFamily: 'var(--font-body)',
      borderRadius: 36,
      boxShadow: '0 0 0 10px #1b1f20, 0 30px 60px rgba(0,0,0,.35)'
    }
  }, body, sheet && /*#__PURE__*/React.createElement(QuickSettings, {
    prefs: prefs,
    setPrefs: setPrefs,
    onClose: () => setSheet(false)
  }), clearAsk && /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      background: 'var(--scrim)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(AP.Dialog, {
    title: "Limpar o texto?",
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(AP.Button, {
      variant: "text",
      size: "pill",
      onClick: () => setClearAsk(false)
    }, "Cancelar"), /*#__PURE__*/React.createElement(AP.Button, {
      variant: "textDanger",
      size: "pill",
      onClick: () => {
        setText('');
        setClearAsk(false);
      }
    }, "Limpar"))
  }, "O texto ser\xE1 apagado do aparelho. Isso n\xE3o pode ser desfeito.")));
}
Object.assign(window, {
  TribunaApp
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/app/App.jsx", error: String((e && e.message) || e) }); }

// ui_kits/app/Editor.jsx
try { (() => {
const {
  TopBar,
  TextArea,
  Button: EButton,
  IconButton: EIconButton,
  SegmentedControl: ESeg,
  Notice: ENotice
} = window.TribunaDesignSystem_cb0bc4;
function wordStats(text) {
  const n = text.trim() ? text.trim().split(/\s+/).length : 0;
  const secs = Math.round(n / 130 * 60);
  const m = Math.floor(secs / 60),
    s = secs % 60;
  return {
    n,
    label: n ? `${n} palavras · ~${m} min ${s} s` : '0 palavras'
  };
}
function Editor({
  text,
  setText,
  mode,
  setMode,
  micOk,
  onStart,
  onSettings,
  onAppearance,
  onClear
}) {
  const st = wordStats(text);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: '100%',
      boxSizing: 'border-box',
      padding: '32px 16px 24px',
      display: 'flex',
      flexDirection: 'column',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    onSettings: onSettings
  }), /*#__PURE__*/React.createElement(TextArea, {
    value: text,
    onChange: setText
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(EButton, {
    variant: "outline",
    size: "pill",
    icon: "paste",
    onClick: () => setText(window.SAMPLE_SPEECH)
  }, "Colar"), /*#__PURE__*/React.createElement(EButton, {
    variant: "outlineNeutral",
    size: "pill",
    icon: "trash",
    onClick: onClear,
    disabled: !text
  }, "Limpar"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      textAlign: 'right',
      font: 'var(--type-small)',
      color: 'var(--fg-2)'
    }
  }, st.label)), !micOk && /*#__PURE__*/React.createElement(ENotice, {
    icon: "mic-off",
    action: "Permitir microfone"
  }, "Microfone desligado. S\xF3 o modo Constante est\xE1 dispon\xEDvel."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(ESeg, {
    label: "Modo de rolagem",
    value: mode,
    onChange: setMode,
    style: {
      flex: 1
    },
    options: [{
      value: 'voz',
      label: 'Voz',
      icon: micOk ? 'mic' : 'mic-off',
      disabled: !micOk
    }, {
      value: 'constante',
      label: 'Constante'
    }]
  }), /*#__PURE__*/React.createElement(EIconButton, {
    variant: "tile",
    icon: "sliders",
    label: "Apar\xEAncia",
    onClick: onAppearance
  })), /*#__PURE__*/React.createElement(EButton, {
    size: "xl",
    icon: "play",
    fullWidth: true,
    disabled: !text.trim(),
    onClick: onStart
  }, "Iniciar"));
}
window.SAMPLE_SPEECH = `Boa noite a todos. Hoje vamos falar sobre ouvir com atenção.

Quem escuta de verdade entende mais e responde com cuidado. Por isso, vamos treinar juntos hoje.

Primeiro, olhe para quem fala. Depois, guarde o celular. Por fim, espere a pessoa terminar antes de responder, mesmo quando você já sabe o que vai dizer.

Ouvir não é ficar em silêncio esperando a vez. É querer entender o que o outro está dizendo, e também o que ele não consegue dizer.`;
Object.assign(window, {
  Editor,
  wordStats
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/app/Editor.jsx", error: String((e && e.message) || e) }); }

// ui_kits/app/Onboarding.jsx
try { (() => {
const T = window.TribunaDesignSystem_cb0bc4;
const {
  Logo,
  FeatureItem,
  InfoCard,
  Button,
  ProgressBar,
  Icon
} = T;
function Welcome({
  onAllow
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: '100%',
      boxSizing: 'border-box',
      padding: '56px 24px 32px',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 40
    }
  }, /*#__PURE__*/React.createElement(Logo, {
    size: "md"
  }), /*#__PURE__*/React.createElement("h1", {
    style: {
      margin: 0,
      font: 'var(--type-hero)',
      fontWeight: 700,
      textWrap: 'balance'
    }
  }, "Seu teleprompter que acompanha ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--accent-text)'
    }
  }, "sua voz"), ", sem internet"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(FeatureItem, {
    icon: "wifi-off"
  }, "Funciona sem rede, em qualquer sal\xE3o"), /*#__PURE__*/React.createElement(FeatureItem, {
    icon: "lock"
  }, "O texto fica s\xF3 na mem\xF3ria e some quando voc\xEA fecha o app"), /*#__PURE__*/React.createElement(FeatureItem, {
    icon: "mic-off"
  }, "Nenhum \xE1udio \xE9 gravado"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 20
    }
  }, /*#__PURE__*/React.createElement(InfoCard, {
    icon: "mic",
    title: "Por que o microfone?"
  }, "O app ouve sua fala s\xF3 para saber em que ponto do texto voc\xEA est\xE1 e rolar a tela at\xE9 l\xE1. Nada sai do aparelho."), /*#__PURE__*/React.createElement(Button, {
    size: "lg",
    fullWidth: true,
    onClick: onAllow
  }, "Permitir microfone")));
}
function Setup({
  onDone
}) {
  const [p, setP] = React.useState(0);
  React.useEffect(() => {
    const id = setInterval(() => setP(v => {
      if (v >= 100) {
        clearInterval(id);
        setTimeout(onDone, 300);
        return 100;
      }
      return v + 4;
    }), 80);
    return () => clearInterval(id);
  }, []);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: '100%',
      boxSizing: 'border-box',
      padding: '56px 24px 32px',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement(Logo, {
    size: "md"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 24
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      margin: 0,
      font: 'var(--type-h1)',
      fontWeight: 700,
      textWrap: 'balance'
    }
  }, "Preparando o reconhecimento de voz"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-paragraph)',
      color: 'var(--fg-2)'
    }
  }, "Estamos copiando o modelo de voz para o armazenamento do aparelho. Isso acontece s\xF3 na primeira vez e n\xE3o usa internet."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(ProgressBar, {
    value: p,
    label: "Prepara\xE7\xE3o do modelo de voz"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      font: 'var(--type-small)',
      color: 'var(--fg-2)'
    }
  }, /*#__PURE__*/React.createElement("span", null, "Copiando modelo"), /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--fg-1)',
      fontWeight: 700
    }
  }, p, "%")))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-small)',
      color: 'var(--fg-2)',
      textAlign: 'center'
    }
  }, "Voc\xEA vai para o Editor assim que terminar."));
}
Object.assign(window, {
  Welcome,
  Setup
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/app/Onboarding.jsx", error: String((e && e.message) || e) }); }

// ui_kits/app/Prompter.jsx
try { (() => {
const PR = window.TribunaDesignSystem_cb0bc4;
const {
  PromptText,
  ReadingIndicator,
  TimerBar,
  MicIndicator,
  SpeedIndicator,
  PausePill,
  Countdown,
  TimeWarningBorder,
  Dialog
} = PR;
const fmt = s => {
  s = Math.max(0, Math.round(s));
  return String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0');
};
const READ_LINE = 253; // 30% of 844

function Prompter({
  text,
  prefs,
  mode,
  setMode,
  elapsed,
  target,
  onEnd,
  onEdit,
  onAppearance,
  onFinish,
  phase,
  setPhase,
  cursor,
  setCursor
}) {
  const preset = PR.PROMPT_PRESETS.find(p => p.id === prefs.preset);
  const hl = preset.bg === '#FFFFFF' ? '#0A7F5C' : '#5CF2C0';
  const words = React.useMemo(() => text.split(/\s+/).filter(Boolean), [text]);
  const boxRef = React.useRef(null);
  const [offset, setOffset] = React.useState(0);
  const [count, setCount] = React.useState(3);
  const [confirm, setConfirm] = React.useState(false);
  const fs = prefs.size,
    lh = prefs.lh / 10;
  React.useLayoutEffect(() => {
    const el = boxRef.current && boxRef.current.querySelector('[data-word="' + cursor + '"]');
    if (el) setOffset(READ_LINE - (el.offsetTop + fs * lh / 2));
  }, [cursor, fs, lh, prefs.margin]);
  React.useEffect(() => {
    if (phase !== 'countdown') return;
    if (count === 0) {
      setPhase('reading');
      return;
    }
    const id = setTimeout(() => setCount(c => c - 1), 800);
    return () => clearTimeout(id);
  }, [phase, count]);
  React.useEffect(() => {
    if (phase !== 'reading') return;
    const ms = mode === 'voz' ? 420 : 900 - prefs.speed * 60;
    const id = setInterval(() => setCursor(c => {
      if (c >= words.length - 1) {
        clearInterval(id);
        setTimeout(onFinish, 1200);
        return c;
      }
      return c + 1;
    }), ms);
    return () => clearInterval(id);
  }, [phase, mode, prefs.speed, words.length]);
  const remaining = target - elapsed;
  const warning = remaining <= 60 ? 'oneMin' : remaining <= 300 ? 'fiveMin' : 'none';
  const tapEmpty = () => {
    if (phase === 'countdown' || phase === 'reading') setPhase('paused');else if (phase === 'paused') setPhase('reading');
  };
  const tapWord = i => {
    setCursor(i);
    if (phase !== 'reading') setPhase(phase === 'countdown' ? 'paused' : 'reading');
  };
  const paused = phase === 'paused';
  return /*#__PURE__*/React.createElement("div", {
    onClick: tapEmpty,
    style: {
      position: 'absolute',
      inset: 0,
      background: preset.bg,
      color: preset.fg,
      overflow: 'hidden',
      cursor: 'pointer'
    }
  }, /*#__PURE__*/React.createElement("div", {
    ref: boxRef,
    style: {
      position: 'absolute',
      left: 0,
      right: 0,
      top: 0,
      transform: `translateY(${offset}px)${prefs.mirror ? ' scaleX(-1)' : ''}`,
      transition: 'transform 300ms cubic-bezier(.2,0,0,1)',
      opacity: phase === 'countdown' ? .28 : 1
    }
  }, /*#__PURE__*/React.createElement(PromptText, {
    text: text,
    cursor: phase === 'countdown' ? -1 : cursor,
    fontSize: fs,
    lineHeight: lh,
    margin: prefs.margin,
    color: preset.fg,
    highlight: hl,
    readOpacity: 0.4,
    onWordClick: tapWord
  })), phase !== 'countdown' && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 0,
      top: 0,
      right: 0,
      height: 96,
      background: `linear-gradient(${preset.bg} 55%, transparent)`
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 0,
      right: 0,
      bottom: 0,
      height: 110,
      background: `linear-gradient(transparent, ${preset.bg} 70%)`
    }
  })), /*#__PURE__*/React.createElement(ReadingIndicator, {
    top: 244,
    color: hl
  }), paused && /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      background: 'var(--scrim)'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      color: preset.fg
    }
  }, /*#__PURE__*/React.createElement(TimerBar, {
    elapsed: fmt(elapsed),
    remaining: fmt(remaining),
    warning: warning,
    inset: warning === 'none' ? 20 : 24
  })), phase === 'countdown' && /*#__PURE__*/React.createElement(Countdown, {
    value: count
  }), phase === 'countdown' && /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 20,
      bottom: 28,
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      font: 'var(--type-small)',
      color: 'var(--fg-2)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 12,
      height: 12,
      borderRadius: 6,
      background: 'var(--accent)'
    }
  }), mode === 'voz' ? 'Microfone ligado' : 'Modo Constante'), phase === 'reading' && (mode === 'voz' ? /*#__PURE__*/React.createElement(MicIndicator, {
    level: 0.5 + Math.random() * 0.5,
    style: {
      position: 'absolute',
      left: 20,
      bottom: 28
    }
  }) : /*#__PURE__*/React.createElement(SpeedIndicator, {
    speed: prefs.speed,
    style: {
      position: 'absolute',
      left: 20,
      bottom: 28
    }
  })), !paused && phase !== 'countdown' && /*#__PURE__*/React.createElement(PR.ProgressBar, {
    variant: "thin",
    value: Math.round(cursor / Math.max(1, words.length - 1) * 100),
    label: "Progresso do discurso",
    style: warning === 'none' ? {
      position: 'absolute',
      left: 0,
      right: 0,
      bottom: 0
    } : {
      position: 'absolute',
      left: 8,
      right: 8,
      bottom: 8
    }
  }), /*#__PURE__*/React.createElement(TimeWarningBorder, {
    level: warning
  }), paused && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 0,
      right: 0,
      top: 440,
      display: 'flex',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement(PausePill, null)), /*#__PURE__*/React.createElement("div", {
    onClick: e => e.stopPropagation(),
    style: {
      cursor: 'default'
    }
  }, /*#__PURE__*/React.createElement(PR.BottomSheet, {
    variant: "panel"
  }, /*#__PURE__*/React.createElement(PR.SegmentedControl, {
    label: "Modo de rolagem",
    value: mode,
    onChange: setMode,
    trackColor: "var(--bg-app)",
    options: [{
      value: 'voz',
      label: 'Voz',
      icon: 'mic'
    }, {
      value: 'constante',
      label: 'Constante'
    }]
  }), /*#__PURE__*/React.createElement(PR.Button, {
    size: "xl",
    icon: "play",
    fullWidth: true,
    onClick: () => setPhase('reading')
  }, "Continuar"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: 'repeat(2, minmax(0,1fr))',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PR.Button, {
    variant: "tonal",
    size: "md",
    icon: "restart",
    onClick: () => setCursor(0)
  }, "Voltar ao in\xEDcio"), /*#__PURE__*/React.createElement(PR.Button, {
    variant: "tonal",
    size: "md",
    icon: "edit",
    onClick: onEdit
  }, "Editar texto"), /*#__PURE__*/React.createElement(PR.Button, {
    variant: "tonal",
    size: "md",
    icon: "sliders",
    onClick: onAppearance
  }, "Apar\xEAncia"), /*#__PURE__*/React.createElement(PR.Button, {
    variant: "tonalDanger",
    size: "md",
    icon: "stop",
    onClick: () => setConfirm(true)
  }, "Encerrar"))))), confirm && /*#__PURE__*/React.createElement("div", {
    onClick: e => e.stopPropagation(),
    style: {
      position: 'absolute',
      inset: 0,
      background: 'var(--scrim)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 24,
      cursor: 'default'
    }
  }, /*#__PURE__*/React.createElement(Dialog, {
    title: "Encerrar leitura?",
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PR.Button, {
      variant: "textAccent",
      size: "pill",
      onClick: () => setConfirm(false)
    }, "Continuar lendo"), /*#__PURE__*/React.createElement(PR.Button, {
      variant: "textDanger",
      size: "pill",
      onClick: onEnd
    }, "Encerrar"))
  }, "Voc\xEA volta ao Editor e o texto continua l\xE1, pronto para outra passada.")));
}
function EndScreen({
  total,
  target,
  onBack
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      background: 'var(--bg-prompt)',
      boxSizing: 'border-box',
      padding: '0 24px 32px',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'flex-end'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 0,
      top: 0,
      right: 0,
      bottom: 120,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 32
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      margin: 0,
      font: 'var(--type-end)',
      fontWeight: 700,
      color: 'var(--accent-text)'
    }
  }, "Fim"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-paragraph)',
      color: 'var(--fg-2)'
    }
  }, "Tempo total"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-stat)',
      fontWeight: 700,
      fontVariantNumeric: 'tabular-nums'
    }
  }, fmt(total)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-paragraph)',
      color: 'var(--fg-2)'
    }
  }, "Tempo-alvo ", fmt(target))), /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: 280,
      textAlign: 'center',
      font: 'var(--type-paragraph)',
      color: 'var(--fg-soft)',
      textWrap: 'balance'
    }
  }, "Seu texto continua no Editor, pronto para outra passada.")), /*#__PURE__*/React.createElement(PR.Button, {
    variant: "outline",
    size: "lg",
    fullWidth: true,
    onClick: onBack,
    style: {
      position: 'relative'
    }
  }, "Voltar ao Editor"));
}
Object.assign(window, {
  Prompter,
  EndScreen,
  fmt
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/app/Prompter.jsx", error: String((e && e.message) || e) }); }

// ui_kits/app/QuickSettings.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const {
  BottomSheet,
  ThemeSwatch,
  PROMPT_PRESETS,
  ColorButton,
  Slider: QSlider,
  Switch: QSwitch
} = window.TribunaDesignSystem_cb0bc4;
function QuickSettings({
  prefs,
  setPrefs,
  onClose
}) {
  const set = k => v => setPrefs(p => ({
    ...p,
    [k]: v
  }));
  const preset = PROMPT_PRESETS.find(p => p.id === prefs.preset);
  return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    onClick: onClose,
    style: {
      position: 'absolute',
      inset: 0,
      background: 'var(--scrim)'
    }
  }), /*#__PURE__*/React.createElement(BottomSheet, {
    title: "Apar\xEAncia",
    onClose: onClose
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      boxSizing: 'border-box',
      height: 100,
      padding: '10px 16px',
      borderRadius: 14,
      background: preset ? preset.bg : 'var(--bg-prompt)',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      fontSize: 28,
      lineHeight: '40px',
      fontWeight: 500,
      overflow: 'hidden',
      border: '1px solid var(--border-1)'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      color: preset.fg,
      opacity: .4,
      whiteSpace: 'nowrap'
    }
  }, "Boa noite a todos."), /*#__PURE__*/React.createElement("div", {
    style: {
      color: preset.fg,
      whiteSpace: 'nowrap'
    }
  }, "Hoje ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: preset.bg === '#FFFFFF' ? '#0A7F5C' : '#5CF2C0',
      fontWeight: 700
    }
  }, "vamos"), " falar")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--type-small)',
      fontWeight: 700,
      color: 'var(--fg-2)'
    }
  }, "Temas prontos"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: 'repeat(4, minmax(0,1fr))',
      gap: 8
    }
  }, PROMPT_PRESETS.map(p => /*#__PURE__*/React.createElement(ThemeSwatch, _extends({
    key: p.id
  }, p, {
    selected: p.id === prefs.preset,
    onClick: () => set('preset')(p.id)
  }))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: 'repeat(3, minmax(0,1fr))',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(ColorButton, {
    color: preset.fg,
    label: "Texto"
  }), /*#__PURE__*/React.createElement(ColorButton, {
    color: preset.bg,
    label: "Fundo"
  }), /*#__PURE__*/React.createElement(ColorButton, {
    color: "#5CF2C0",
    label: "Destaque"
  })), /*#__PURE__*/React.createElement(QSlider, {
    variant: "sheet",
    label: "Tamanho da fonte",
    min: 24,
    max: 96,
    value: prefs.size,
    onChange: set('size'),
    display: prefs.size + ' sp'
  }), /*#__PURE__*/React.createElement(QSlider, {
    variant: "sheet",
    label: "Espa\xE7amento entre linhas",
    min: 10,
    max: 25,
    value: prefs.lh,
    onChange: set('lh'),
    display: (prefs.lh / 10).toFixed(1).replace('.', ',') + '×'
  }), /*#__PURE__*/React.createElement(QSlider, {
    variant: "sheet",
    label: "Margem lateral",
    min: 0,
    max: 30,
    value: prefs.margin,
    onChange: set('margin'),
    display: prefs.margin + '%'
  }), /*#__PURE__*/React.createElement(QSlider, {
    variant: "sheet",
    label: "Velocidade (modo Constante)",
    min: 1,
    max: 10,
    value: prefs.speed,
    onChange: set('speed')
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      minHeight: 48
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--type-body)',
      fontWeight: 700
    }
  }, "Espelho horizontal"), /*#__PURE__*/React.createElement(QSwitch, {
    checked: prefs.mirror,
    onChange: set('mirror'),
    label: "Espelho horizontal"
  }))));
}
Object.assign(window, {
  QuickSettings
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/app/QuickSettings.jsx", error: String((e && e.message) || e) }); }

// ui_kits/app/Settings.jsx
try { (() => {
const ST = window.TribunaDesignSystem_cb0bc4;
function SettingsScreen({
  prefs,
  setPrefs,
  theme,
  setTheme,
  onBack
}) {
  const set = k => v => setPrefs(p => ({
    ...p,
    [k]: v
  }));
  const [weight, setWeight] = React.useState('Médio');
  const [sens, setSens] = React.useState('Normal');
  const [cd, setCd] = React.useState('3 s');
  const [sw, setSw] = React.useState({
    timer: true,
    screen: true,
    bright: false,
    bar: true,
    vol: true,
    vib: true,
    planb: false
  });
  const tog = k => v => setSw(s => ({
    ...s,
    [k]: v
  }));
  const [targetMin, setTargetMin] = React.useState(30);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: '100%',
      overflowY: 'auto',
      boxSizing: 'border-box',
      padding: '32px 0 32px'
    }
  }, /*#__PURE__*/React.createElement(ST.TopBar, {
    title: "Configura\xE7\xF5es",
    onBack: onBack
  }), /*#__PURE__*/React.createElement(ST.SettingsSection, {
    title: "Apar\xEAncia",
    first: true
  }, /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Tema do app"
  }, /*#__PURE__*/React.createElement(ST.SegmentedControl, {
    size: "sm",
    value: theme,
    onChange: setTheme,
    options: [{
      value: 'dark',
      label: 'Escuro'
    }, {
      value: 'light',
      label: 'Claro'
    }]
  })), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Temas prontos"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: 8
    }
  }, ST.PROMPT_PRESETS.map(p => /*#__PURE__*/React.createElement(ST.Chip, {
    key: p.id,
    selected: p.id === prefs.preset,
    onClick: () => set('preset')(p.id)
  }, p.label)))), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    stacked: true
  }, /*#__PURE__*/React.createElement(ST.Slider, {
    label: "Tamanho da fonte",
    min: 24,
    max: 96,
    value: prefs.size,
    onChange: set('size'),
    display: prefs.size + ' sp'
  })), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Peso da fonte"
  }, /*#__PURE__*/React.createElement(ST.SegmentedControl, {
    size: "sm",
    value: weight,
    onChange: setWeight,
    options: ['Normal', 'Médio', 'Negrito']
  })), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    stacked: true
  }, /*#__PURE__*/React.createElement(ST.Slider, {
    label: "Margem lateral",
    min: 0,
    max: 30,
    value: prefs.margin,
    onChange: set('margin'),
    display: prefs.margin + '%'
  })), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Espelho horizontal",
    control: /*#__PURE__*/React.createElement(ST.Switch, {
      checked: prefs.mirror,
      onChange: set('mirror'),
      label: "Espelho horizontal"
    })
  })), /*#__PURE__*/React.createElement(ST.SettingsSection, {
    title: "Rolagem e voz"
  }, /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Sensibilidade do acompanhamento"
  }, /*#__PURE__*/React.createElement(ST.SegmentedControl, {
    size: "sm",
    value: sens,
    onChange: setSens,
    options: ['Conservador', 'Normal', 'Ágil']
  })), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Plano B autom\xE1tico",
    description: "Sem fala reconhecida por um tempo, muda para o modo Constante.",
    control: /*#__PURE__*/React.createElement(ST.Switch, {
      checked: sw.planb,
      onChange: tog('planb')
    })
  })), /*#__PURE__*/React.createElement(ST.SettingsSection, {
    title: "Discurso"
  }, /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Contagem regressiva"
  }, /*#__PURE__*/React.createElement(ST.SegmentedControl, {
    size: "sm",
    value: cd,
    onChange: setCd,
    options: ['0 s', '3 s', '5 s', '10 s']
  })), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Mostrar o cron\xF4metro",
    control: /*#__PURE__*/React.createElement(ST.Switch, {
      checked: sw.timer,
      onChange: tog('timer')
    })
  }), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Tempo-alvo",
    description: "Aviso visual aos 5 min (amarelo) e 1 min (vermelho), sem som.",
    control: /*#__PURE__*/React.createElement(ST.Stepper, {
      value: targetMin + ' min',
      onDecrement: () => setTargetMin(m => Math.max(5, m - 5)),
      onIncrement: () => setTargetMin(m => m + 5),
      decLabel: "Diminuir tempo-alvo",
      incLabel: "Aumentar tempo-alvo"
    })
  }), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Manter a tela sempre ligada",
    control: /*#__PURE__*/React.createElement(ST.Switch, {
      checked: sw.screen,
      onChange: tog('screen')
    })
  })), /*#__PURE__*/React.createElement(ST.SettingsSection, {
    title: "Controles"
  }, /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Bot\xF5es de volume",
    description: "Pausar ou retomar e voltar uma frase.",
    control: /*#__PURE__*/React.createElement(ST.Switch, {
      checked: sw.vol,
      onChange: tog('vol')
    })
  }), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Vibra\xE7\xE3o ao pausar",
    control: /*#__PURE__*/React.createElement(ST.Switch, {
      checked: sw.vib,
      onChange: tog('vib')
    })
  })), /*#__PURE__*/React.createElement(ST.SettingsSection, {
    title: "Sobre"
  }, /*#__PURE__*/React.createElement(ST.SettingsRow, {
    label: "Licen\xE7as",
    description: "Vosk, Apache 2.0",
    control: /*#__PURE__*/React.createElement(ST.Icon, {
      name: "chevron-right",
      size: 22,
      color: "var(--fg-2)"
    })
  }), /*#__PURE__*/React.createElement(ST.SettingsRow, {
    last: true,
    label: "Privacidade",
    description: "Nada sai do aparelho. O texto fica s\xF3 na mem\xF3ria e nenhum \xE1udio \xE9 gravado."
  })));
}
Object.assign(window, {
  SettingsScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/app/Settings.jsx", error: String((e && e.message) || e) }); }

__ds_ns.ICON_NAMES = __ds_scope.ICON_NAMES;

__ds_ns.Icon = __ds_scope.Icon;

__ds_ns.Logo = __ds_scope.Logo;

__ds_ns.Button = __ds_scope.Button;

__ds_ns.IconButton = __ds_scope.IconButton;

__ds_ns.Dialog = __ds_scope.Dialog;

__ds_ns.InfoCard = __ds_scope.InfoCard;

__ds_ns.Notice = __ds_scope.Notice;

__ds_ns.ProgressBar = __ds_scope.ProgressBar;

__ds_ns.Chip = __ds_scope.Chip;

__ds_ns.ColorButton = __ds_scope.ColorButton;

__ds_ns.SegmentedControl = __ds_scope.SegmentedControl;

__ds_ns.Slider = __ds_scope.Slider;

__ds_ns.Stepper = __ds_scope.Stepper;

__ds_ns.Switch = __ds_scope.Switch;

__ds_ns.TextArea = __ds_scope.TextArea;

__ds_ns.ThemeSwatch = __ds_scope.ThemeSwatch;

__ds_ns.PROMPT_PRESETS = __ds_scope.PROMPT_PRESETS;

__ds_ns.BottomSheet = __ds_scope.BottomSheet;

__ds_ns.FeatureItem = __ds_scope.FeatureItem;

__ds_ns.NavItem = __ds_scope.NavItem;

__ds_ns.SettingsRow = __ds_scope.SettingsRow;

__ds_ns.SettingsSection = __ds_scope.SettingsSection;

__ds_ns.TopBar = __ds_scope.TopBar;

__ds_ns.Countdown = __ds_scope.Countdown;

__ds_ns.MicIndicator = __ds_scope.MicIndicator;

__ds_ns.PausePill = __ds_scope.PausePill;

__ds_ns.PromptText = __ds_scope.PromptText;

__ds_ns.ReadingIndicator = __ds_scope.ReadingIndicator;

__ds_ns.SpeedIndicator = __ds_scope.SpeedIndicator;

__ds_ns.TimeWarningBorder = __ds_scope.TimeWarningBorder;

__ds_ns.TimerBar = __ds_scope.TimerBar;

})();
