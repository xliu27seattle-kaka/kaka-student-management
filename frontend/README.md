# Frontend

The Angular app of Kaka Student Management.

It is built, tested and served through Docker, from the repository root:

| What | Command |
|---|---|
| Run the whole app | `docker compose up --build`, then open http://localhost:4200 |
| Unit tests | `scripts/test.sh frontend` |
| End-to-end tests | `scripts/e2e.sh` (needs Node.js and Google Chrome) |

## Layout

```
src/app
├── core/       session store, HTTP interceptors, route guards, small helpers
└── features/
    ├── auth/        entry layout (photographs), login page, registration page
    ├── home/        the page a user lands on after signing in
    └── not-found/
```

- State lives in RxJS stores (`BehaviorSubject`) and reaches templates through the `async` pipe.
- Every colour comes from the CSS variables in `src/styles.scss`.
- The photographs in `public/entry/` are in the public domain or under CC0; `photo-credits.json`
  next to them lists author, licence and source for each.
