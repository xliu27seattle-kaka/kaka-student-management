import { expect, test } from "@playwright/test";
import {
  ADMIN_EMAIL,
  DEMO_PASSWORD,
  STUDENT_EMAIL,
  signIn,
} from "./support/app";

test.describe("signing in", () => {
  test("a visitor who opens the site lands on the login page", async ({
    page,
  }) => {
    await page.goto("/");

    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();
    await expect(page.getByText("Kaka Studio")).toBeVisible();
  });

  test("a wrong password is answered with a clear message", async ({
    page,
  }) => {
    await signIn(page, ADMIN_EMAIL, "not-the-password");

    await expect(page.getByRole("alert")).toContainText(
      "Email or password is incorrect",
    );
    await expect(page).toHaveURL(/\/login$/);
  });

  test("an email that is not an email is caught before anything is sent", async ({
    page,
  }) => {
    await page.goto("/login");
    await page.getByLabel("Email").fill("not-an-email");
    await page.getByLabel("Password", { exact: true }).fill("whatever");
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page.getByText("Enter a valid email address.")).toBeVisible();
    await expect(page.getByRole("alert")).toHaveCount(0);
  });

  test("the demo administrator signs in with one click and lands on administration", async ({
    page,
  }) => {
    await page.goto("/login");
    await page.getByRole("button", { name: "Admin" }).click();
    await expect(page.getByLabel("Email")).toHaveValue(ADMIN_EMAIL);
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page).toHaveURL(/\/admin$/);
    await expect(
      page.getByRole("heading", { name: "Good to see you, Kaka." }),
    ).toBeVisible();
    await expect(page.getByRole("navigation", { name: "Main" })).toContainText("Semesters");
  });

  test("a student who opens the administration area is sent to their own", async ({
    page,
  }) => {
    await signIn(page, STUDENT_EMAIL, DEMO_PASSWORD);
    await expect(page).toHaveURL(/\/student$/);

    await page.goto("/admin");

    await expect(page).toHaveURL(/\/student$/);
    await expect(page.getByRole("navigation", { name: "Main" })).toContainText("Transcript");
  });

  test("the session survives a reload, and signing out locks the area again", async ({
    page,
  }) => {
    await signIn(page, STUDENT_EMAIL, DEMO_PASSWORD);
    await expect(page).toHaveURL(/\/student$/);

    await page.reload();
    await expect(page.getByRole("navigation", { name: "Main" })).toContainText("Transcript");

    await page.getByRole("button", { name: "Sign out" }).click();
    await expect(page).toHaveURL(/\/login$/);

    await page.goto("/student");
    await expect(page).toHaveURL(/\/login$/);
  });

  test("a signed-in user who opens the login page is sent to their home", async ({
    page,
  }) => {
    await signIn(page, STUDENT_EMAIL, DEMO_PASSWORD);
    await expect(page).toHaveURL(/\/student$/);

    await page.goto("/login");

    await expect(page).toHaveURL(/\/student$/);
  });

  test("an unknown address shows the not-found page", async ({ page }) => {
    await page.goto("/no-such-page");

    await expect(
      page.getByRole("heading", { name: "This page does not exist." }),
    ).toBeVisible();
  });
});
