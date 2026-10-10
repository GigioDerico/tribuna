The Tribuna brand lockup — mark + "Tribuna" wordmark; use in app bars and onboarding.
```jsx
<Logo size="sm" />            {/* app bar, 28px mark, 22px wordmark */}
<Logo size="md" />            {/* onboarding, 40px mark, 24px wordmark */}
<Logo variant="ink" wordmark={false} />
```
Wordmark color follows `--fg-1`, so it flips automatically under `data-theme="light"`. The mint mark works on both themes; use `ink` when the mint square sits on a very light or busy surface.
