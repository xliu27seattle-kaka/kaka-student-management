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
  const response = await request.post("/api/v1/auth/login", {
    data: { email: ADMIN_EMAIL, password: DEMO_PASSWORD },
  });
  expect(response.ok()).toBeTruthy();
  return (await response.json()).accessToken as string;
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
