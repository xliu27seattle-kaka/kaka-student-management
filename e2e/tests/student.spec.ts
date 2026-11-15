import { expect, test } from "@playwright/test";
import { Page } from "@playwright/test";
import {
  ADMIN_EMAIL,
  STUDENT_EMAIL,
  navLink,
  signInAs,
  tokenFor,
  unique,
} from "./support/app";

/** Other tests may open more semesters, so the demo semester is chosen by name. */
async function openSpring2027(page: Page): Promise<void> {
  await navLink(page, "Enroll").click();
  await page.getByLabel("Semester").selectOption({ label: "Spring 2027" });
}

test.describe("a student", () => {
  test.beforeEach(async ({ page }) => {
    await signInAs(page, STUDENT_EMAIL);
  });

  test("sees GPA and progress on the overview", async ({ page }) => {
    await expect(page.getByRole("heading", { name: "Welcome back, Lena." })).toBeVisible();
    await expect(page.getByTestId("dashboard-gpa")).toHaveText(/^\d\.\d\d$/);
    await expect(page.getByText("Earned credits")).toBeVisible();
  });

  test("sections she cannot take are greyed out with the reason", async ({ page }) => {
    await openSpring2027(page);

    const reasons: Record<string, string> = {
      "CS101-A": "Already completed",
      "CS220-A": "Overlaps CS340-A on Mon/Wed",
      "CS301-A": "Already taking CS301 in Fall 2026",
      "CS340-B": "Already enrolled in section CS340-A",
      "CS401-A": "Requires CS301, CS320",
      "MATH140-A": "Needs 10 minutes after CS340-A",
      "MATH140-B": "Section full",
    };
    for (const [code, reason] of Object.entries(reasons)) {
      const row = page.getByTestId(`offering-${code}`);
      await expect(row).toContainText(reason);
      await expect(row.getByRole("button", { name: "Enroll" })).toBeDisabled();
    }
  });

  test("enrolling updates the timetable and the credits; dropping asks first", async ({ page }) => {
    await openSpring2027(page);
    const credits = page.getByTestId("enrolled-credits");
    await expect(credits).toHaveText("3");

    const row = page.getByTestId("offering-CS250-A");
    await row.getByRole("button", { name: "Enroll" }).click();
    await expect(page.getByRole("status")).toContainText("Enrolled in CS250-A");
    await expect(credits).toHaveText("6");
    await expect(page.locator("app-schedule-grid").getByText("CS250-A").first()).toBeVisible();

    await row.getByRole("button", { name: "Drop" }).click();
    await expect(page.getByRole("dialog")).toContainText("Drop CS250-A?");
    await page.getByRole("dialog").getByRole("button", { name: "Drop" }).click();
    await expect(credits).toHaveText("3");
  });

  test("the transcript shows every semester and can be printed", async ({ page }) => {
    await navLink(page, "Transcript").click();
    await expect(page.getByRole("heading", { name: "Fall 2025" })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Spring 2026" })).toBeVisible();
    await expect(page.getByText("Unofficial", { exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "Print" })).toBeEnabled();
  });

  test("graduation shows how many credits are missing", async ({ page }) => {
    await navLink(page, "Graduation").click();
    await expect(page.getByText(/more credits needed/)).toBeVisible();
    await expect(page.getByRole("button", { name: "Apply for graduation" })).toBeDisabled();
  });

  test("a professor's public profile opens from a section", async ({ page }) => {
    await openSpring2027(page);
    await page.getByTestId("offering-CS401-A").getByRole("link", { name: "Amara Okafor" }).click();
    await expect(page.getByRole("heading", { name: "Amara Okafor" }).first()).toBeVisible();
    await expect(page.getByText("Database systems").first()).toBeVisible();
    await expect(page.getByRole("img", { name: /Portrait of Amara Okafor/ })).toBeVisible();
  });

  test("the inbox marks a message read and the badge follows", async ({ page, request }) => {
    // a fresh message every run, so the test does not depend on what earlier runs read
    const subject = `Library hours ${unique()}`;
    const admin = await tokenFor(request, ADMIN_EMAIL);
    const sent = await request.post("/api/v1/admin/notifications", {
      headers: { Authorization: `Bearer ${admin}` },
      data: { audience: "ALL_STUDENTS", subject, body: "The library stays open late this week." },
    });
    expect(sent.ok()).toBeTruthy();
    const mine = await tokenFor(request, STUDENT_EMAIL);
    const unread = async () =>
      (
        await (
          await request.get("/api/v1/notifications/unread-count", {
            headers: { Authorization: `Bearer ${mine}` },
          })
        ).json()
      ).unread as number;
    const before = await unread();

    await navLink(page, "Inbox").click();
    const badge = page.getByRole("navigation", { name: "Main" }).locator(".count");
    await expect(badge).toHaveText(String(before));
    await page.getByRole("button", { name: new RegExp(subject) }).click();
    await expect(page.getByText("The library stays open late this week.")).toBeVisible();

    if (before > 1) await expect(badge).toHaveText(String(before - 1));
    else await expect(badge).toHaveCount(0);
  });
});

test.describe("themes", () => {
  test("a theme chosen on the account page is kept after signing in again", async ({ page }) => {
    await signInAs(page, STUDENT_EMAIL);
    await page.goto("/student/account");
    await page.getByRole("radio", { name: /Midnight & Amber/ }).click();
    await expect(page.locator("html")).toHaveAttribute("data-theme", "midnight");

    await page.getByRole("button", { name: "Sign out" }).click();
    await expect(page.locator("html")).not.toHaveAttribute("data-theme", "midnight");
    await signInAs(page, STUDENT_EMAIL);
    await expect(page.locator("html")).toHaveAttribute("data-theme", "midnight");

    await page.goto("/student/account");
    await page.getByRole("radio", { name: /Graphite & Lime/ }).click();
    await expect(page.locator(".shell.top")).toBeVisible();
    await page.getByRole("radio", { name: /Sage Ledger/ }).click();
  });
});
