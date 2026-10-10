Teleprompter text with read/current/unread word states and tap-to-jump.
```jsx
<PromptText text={speech} cursor={12} onWordClick={setCursor} />
<PromptText text={speech} cursor={12} color="#FFE14D" />  {/* Amarelo/preto preset */}
```
Colors default to `--prompt-fg` / `--prompt-highlight` on `--bg-prompt`. Users can override all three (A6–A8).
