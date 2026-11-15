import { expect, test } from "@playwright/test";
import {
  ADMIN_EMAIL,
  approveStudent,
  navLink,
  signIn,
  signInAs,
  unique,
} from "./support/app";

test.describe("the inbox tells everyone what happened, without opening a mailbox", () => {
  test("a registration alerts the administrator, the approval reaches the student, and the email log shows both emails", async ({
    page,
    request,
  }) => {
    const email = `e2e.inbox.${unique()}@uni.example`;
    const registered = await request.post("/api/v1/auth/register", {
      data: { email, firstName: "Ines", lastName: "Inbox", password: "Passw0rd1" },
    });
    expect(registered.ok()).toBeTruthy();

    // the administrator sees the alert in their own inbox
    await signInAs(page, ADMIN_EMAIL);
    await navLink(page, "Inbox").click();
    await page.getByRole("button", { name: /New registration: Ines Inbox/ }).first().click();
    await expect(page.getByText(new RegExp(`Ines Inbox \\(${email.replace(/\./g, "\\.")}\\)`))).toBeVisible();

    // and the email log lists the registration email, without its body
    await navLink(page, "Email log").click();
    await page.getByLabel("Search emails").fill(email);
    await expect(page.getByRole("row", { name: /We received your registration/ })).toBeVisible();

    // once approved, the student finds the news in the app, marked as also emailed
    await approveStudent(request, email);
    await page.getByRole("button", { name: "Sign out" }).click();
    await signIn(page, email, "Passw0rd1");
    await expect(page).toHaveURL(/\/student$/);
    await navLink(page, "Inbox").click();
    await page.getByRole("button", { name: /Your account is approved/ }).click();
    await expect(page.getByText("Your registration was approved.")).toBeVisible();
    await expect(page.getByText("Also sent to your email")).toBeVisible();
  });

  test("the dashboard's given-up count opens the email log filtered to those emails", async ({ page }) => {
    await signInAs(page, ADMIN_EMAIL);
    await page.getByRole("link", { name: /Emails given up/ }).click();
    await expect(page).toHaveURL(/\/admin\/emails\?status=FAILED$/);
    await expect(page.getByLabel("Delivery status")).toHaveValue("FAILED");
  });
});
