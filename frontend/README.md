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
├── core/       session, theme, toast, confirm and unread stores; HTTP interceptors; route guards;
│               the app shell (sidebar or top navigation)
├── shared/
│   ├── api/    one client per audience (admin, professor, student, common) and the API models
│   └── ui/     tabs, status badge, empty state, skeleton, schedule grid, transcript sheet,
│               watermark, professor card, sent list, confirm dialog, toast stack, count-up
└── features/
    ├── auth/       entry page with the photographs, login, registration
    ├── admin/      dashboard, students, professors, courses, semesters and sections,
    │               graduation, notifications
    ├── professor/  teaching, a section (roster, grades, message), profile
    ├── student/    overview, enroll (with its page-scoped store), my courses, transcript,
    │               graduation and certificate
    ├── common/     inbox, account and theme, a professor's public profile
    └── not-found/
```

- Shared state lives in RxJS stores (`BehaviorSubject`) and reaches templates through the `async`
  pipe; each page's data is a stream built with `toLoadState()` for loading, data and error.
- Every colour comes from the CSS variables in `src/styles/themes.scss` (four themes, chosen on
  `<html data-theme>`); shared building blocks are in `src/styles/app-ui.scss`.
- The photographs in `public/entry/` are in the public domain or under CC0; `photo-credits.json`
  next to them lists author, licence and source for each. The professor portraits in
  `public/assets/professors/` are illustrations made for this project.
