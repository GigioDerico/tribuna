Confirmation dialog for destructive or exit actions (Limpar o texto?, Encerrar leitura?).
```jsx
<Dialog title="Limpar o texto?" actions={<>
  <Button variant="text" size="pill">Cancelar</Button>
  <Button variant="textDanger" size="pill">Limpar</Button></>}>
  O texto será apagado do aparelho. Isso não pode ser desfeito.
</Dialog>
```
Put over a `var(--scrim)` overlay. The safe action is on the left, the destructive one (coral) on the right.
