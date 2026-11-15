import { APIRequestContext, Page, expect } from "@playwright/test";

export const INBOX_URL =
  process.env["E2E_INBOX_URL"] ?? "http://localhost:8025";
export const DEMO_PASSWORD = "KakaDemo2026";
export const ADMIN_EMAIL = "admin@kaka.example";
export const STUDENT_EMAIL = "lena.fischer@uni.example";

/** Fills the login form and submits it. */
export async function signIn(
  page: Page,
  email: string,
  password: string,
): Promise<void> {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
}

/** An administrator token, obtained through the API rather than the page. */
export async function adminToken(request: APIRequestContext): Promise<string> {
  return tokenFor(request, ADMIN_EMAIL);
}

/** Approves the pending student with this email, as an administrator would. */
export async function approveStudent(
  request: APIRequestContext,
  email: string,
): Promise<void> {
  const headers = { Authorization: "Bearer " + (await adminToken(request)) };
  const found = await request.get("/api/v1/admin/students", {
    headers,
    params: { status: "PENDING", q: email },
  });
  const page = await found.json();
  expect(page.totalItems).toBe(1);
  const approved = await request.post(
    "/api/v1/admin/students/" + page.items[0].id + "/approve",
    {
      headers,
    },
  );
  expect(approved.ok()).toBeTruthy();
}

/** Subjects of the emails the local inbox holds for this address. */
export async function inboxSubjects(
  request: APIRequestContext,
  email: string,
): Promise<string[]> {
  const response = await request.get(INBOX_URL + "/api/v1/search", {
    params: { query: "to:" + email },
  });
  const body = await response.json();
  return (body.messages as { Subject: string }[]).map(
    (message) => message.Subject,
  );
}

export const PROFESSOR_EMAIL = "amara.okafor@faculty.example";

/** Signs in through the page and waits for the role's home. */
export async function signInAs(page: Page, email: string): Promise<void> {
  await signIn(page, email, DEMO_PASSWORD);
  await expect(page).toHaveURL(/\/(admin|professor|student)$/);
}

/** A token for any demo account, through the API. */
export async function tokenFor(
  request: APIRequestContext,
  email: string,
  password = DEMO_PASSWORD,
): Promise<string> {
  const response = await request.post("/api/v1/auth/login", {
    data: { email, password },
  });
  expect(response.ok()).toBeTruthy();
  return (await response.json()).accessToken as string;
}

/** A unique tag for addresses and subjects, so tests can run again and again. */
export function unique(): string {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
}

/** Random capital letters, for course codes like QXY123. */
export function letters(count: number): string {
  return Array.from({ length: count }, () =>
    String.fromCharCode(65 + Math.floor(Math.random() * 26)),
  ).join("");
}

/** A random whole number from min to max. */
export function between(min: number, max: number): number {
  return min + Math.floor(Math.random() * (max - min + 1));
}

/** A link in the main navigation (the sidebar or the top bar). */
export function navLink(page: Page, name: string) {
  return page.getByRole("navigation", { name: "Main" }).getByRole("link", { name });
}
