import { expect, test } from "@playwright/test";
import {
  STUDENT_EMAIL,
  approveStudent,
  inboxSubjects,
  signIn,
} from "./support/app";

const stamp = Date.now();
const email = "e2e.student." + stamp + "@uni.example";
const password = "Passw0rd1";

async function fillRegistration(
  page: import("@playwright/test").Page,
  values: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  },
): Promise<void> {
  await page.goto("/register");
  await page.getByLabel("First name").fill(values.firstName);
  await page.getByLabel("Last name").fill(values.lastName);
  await page.getByLabel("Email").fill(values.email);
  await page.getByLabel("Password", { exact: true }).fill(values.password);
  await page.getByRole("button", { name: "Create account" }).click();
}

test.describe("registering", () => {
  test("a new student registers, waits for approval, then signs in", async ({
    page,
    request,
  }) => {
    // The address is typed with capitals and spaces on purpose.
    await fillRegistration(page, {
      firstName: "Enzo",
      lastName: "Tester",
      email: "  " + email.toUpperCase() + " ",
      password,
    });

    await expect(
      page.getByRole("heading", { name: "Registration received" }),
    ).toBeVisible();
    await expect(page.getByTestId("student-number")).toHaveText(
      /^S\d{4}-\d{4}$/,
    );

    await page.getByRole("link", { name: "Back to sign in" }).click();
    await signIn(page, email, password);
    await expect(page.getByRole("alert")).toContainText("waiting for approval");

    await approveStudent(request, email);

    await signIn(page, email, password);
    await expect(page).toHaveURL(/\/student$/);
    await expect(
      page.getByRole("heading", { name: "Welcome back, Enzo." }),
    ).toBeVisible();

    await expect
      .poll(async () => inboxSubjects(request, email), { timeout: 20_000 })
      .toEqual(
        expect.arrayContaining([
          "We received your registration",
          "Your account is approved",
        ]),
      );
  });

  test("an address that is already registered is reported under the email field", async ({
    page,
  }) => {
    await fillRegistration(page, {
      firstName: "Lena",
      lastName: "Again",
      email: STUDENT_EMAIL,
      password,
    });

    await expect(
      page.getByText("This email address is already registered."),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Registration received" }),
    ).toHaveCount(0);
  });

  test("the form says what is wrong before anything is sent", async ({
    page,
  }) => {
    await fillRegistration(page, {
      firstName: "",
      lastName: "",
      email: "nope",
      password: "short",
    });

    await expect(page.getByText("Enter your first name.")).toBeVisible();
    await expect(page.getByText("Enter your last name.")).toBeVisible();
    await expect(page.getByText("Enter a valid email address.")).toBeVisible();
    await expect(
      page.getByText(
        "Use 8 to 72 characters with at least one letter and one digit.",
      ),
    ).toBeVisible();
  });
});
