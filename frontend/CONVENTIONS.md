# Frontend conventions

## Before you push

```bash
npm run lint
npm run format:check
npm test -- --watch=false
npm run build
```

`npm run format` fixes formatting and sorts imports.

## Angular

- Components are standalone. OnPush is the default in Angular 22, so don't set `changeDetection`.
- Use `input()`, `output()` and `model()`, not `@Input()` / `@Output()`.
- Use `computed()` for derived values and `effect()` only for side effects.
- Use `inject()` in fields, not constructor parameters.
- Use `@if`, `@for` and `@switch`. Every `@for` has `track`, by id when there is one.
- Don't write `public`. Use `protected` for members the template uses, `private` for the rest, and
  `readonly` when a member is never reassigned.
- No `console.*` in app code.

## Where code goes

| Folder     | What                                         |
| ---------- | -------------------------------------------- |
| `core/`    | Services, models and HTTP helpers, by domain |
| `shared/`  | Reusable components and helpers              |
| `layout/`  | App shell: header, footer, layouts           |
| `pages/`   | Routed pages                                 |
| `testing/` | Test data builders                           |

- Keep business logic in services, not components.
- Move pure functions out of components into a util file next to them (e.g. `form-utils.ts`).
- Put shared constants in a `*.constants.ts` file.

## Errors and feedback

| Situation                                         | Show it as                                                                        |
| ------------------------------------------------- | --------------------------------------------------------------------------------- |
| Client-side validation (required, format, length) | Message under the field                                                           |
| Server rejects a form submit                      | `app-form-alert` above the submit button, plus a red border on the rejected field |
| Success, or the result of a button outside a form | Toast via `ToastService`                                                          |
| A page fails to load                              | Inline message with a retry button                                                |

Never show form errors in a toast. Use `genericErrorMessage()` for network and server errors.

## Styling

- Use Tailwind utilities. Colours and fonts come from the theme in `src/styles.css`, e.g. `brand-600`.
- Reuse `form-label`, `form-input` and `btn-primary` for forms.

## Tests

- Test what the user sees: query the DOM, click, type, then assert.
- Mock HTTP with `HttpTestingController` and check `http.verify()` after each test.
- Use `it.each` for cases that differ only in their input.
