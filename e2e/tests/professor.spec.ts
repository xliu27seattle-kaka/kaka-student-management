import { expect, test } from "@playwright/test";
import { PROFESSOR_EMAIL, navLink, signInAs } from "./support/app";

test.describe("a professor", () => {
  test.beforeEach(async ({ page }) => {
    await signInAs(page, PROFESSOR_EMAIL);
  });

  test("sees what they teach in each semester", async ({ page }) => {
    await expect(page.getByRole("heading", { name: "Hello, Amara." })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Fall 2026" })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Spring 2027" })).toBeVisible();
    await expect(page.getByRole("link", { name: /CS320-A/ }).first()).toBeVisible();
  });

  test("grades a running section with a live letter preview, and unsaved grades ask before leaving", async ({
    page,
  }) => {
    await page
      .locator("section", { has: page.getByRole("heading", { name: "Fall 2026" }) })
      .getByRole("link", { name: /CS320-A/ })
      .click();
    await expect(page.getByRole("cell", { name: "Lena Fischer" })).toBeVisible();

    await page.getByRole("tab", { name: "Grades" }).click();
    const lena = page.getByLabel(/Lena Fischer/);
    await lena.fill("91");
    await expect(page.getByText("A-", { exact: true })).toBeVisible();

    await page.getByRole("tab", { name: "Roster" }).click();
    await expect(page.getByRole("dialog")).toContainText("Leave without saving?");
    await page.getByRole("button", { name: "Stay" }).click();

    await page.getByRole("button", { name: "Save grades" }).click();
    await expect(page.getByRole("status")).toContainText("Grades saved");
    // put the demo back the way it was
    await lena.fill("89");
    await page.getByRole("button", { name: "Save grades" }).click();
    await expect(page.getByRole("status").last()).toContainText("Grades saved");
  });

  test("the public profile is shown the way students see it", async ({ page }) => {
    await navLink(page, "My profile").click();
    await expect(page.getByRole("heading", { name: "How students see you" })).toBeVisible();
    await expect(page.getByText("Associate Professor of Computer Science")).toBeVisible();
    await expect(page.getByRole("link", { name: "Edit my profile" })).toBeVisible();
  });
});
