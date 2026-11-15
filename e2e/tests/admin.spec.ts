import { expect, test } from "@playwright/test";
import {
  ADMIN_EMAIL,
  approveStudent,
  between,
  letters,
  navLink,
  signInAs,
  unique,
} from "./support/app";

test.describe("administration", () => {
  test.beforeEach(async ({ page }) => {
    await signInAs(page, ADMIN_EMAIL);
  });

  test("the dashboard shows what needs a decision and every semester", async ({
    page,
  }) => {
    await expect(
      page.getByRole("heading", { name: "Good to see you, Kaka." }),
    ).toBeVisible();
    await expect(page.getByText("Waiting for approval")).toBeVisible();
    await expect(page.getByText("Graduation requests")).toBeVisible();
    for (const term of ["Fall 2025", "Spring 2026", "Fall 2026", "Spring 2027", "Fall 2027"]) {
      await expect(page.getByRole("link", { name: new RegExp(term) })).toBeVisible();
    }
  });

  test("a pending registration is approved from the list", async ({
    page,
    request,
  }) => {
    const email = `e2e.pending.${unique()}@uni.example`;
    await request.post("/api/v1/auth/register", {
      data: { email, firstName: "Pia", lastName: "Pending", password: "Passw0rd1" },
    });

    await navLink(page, "Students").click();
    await page.getByLabel("Account status").selectOption("PENDING");
    await page.getByLabel("Search students").fill(email);
    const row = page.getByRole("row", { name: new RegExp(email.replace(/\./g, "\\.")) });
    await row.getByRole("button", { name: "Approve" }).click();

    await expect(page.getByRole("status")).toContainText("Pia Pending approved");
  });

  test("courses are created with prerequisites, a cycle is refused with its path, and delete asks first", async ({
    page,
  }) => {
    const digits = String(between(100, 999));
    const base = `Q${letters(2)}${digits}`;
    const next = `Q${letters(2)}${digits}`;

    // the first course
    await page.goto("/admin/courses/new");
    await page.getByLabel("Code").fill(base);
    await page.getByLabel("Department").fill("QA");
    await page.getByLabel("Title").fill("Quality Basics");
    await page.getByRole("button", { name: "Save course" }).click();
    await expect(page.getByRole("heading", { name: `${base} · Quality Basics` })).toBeVisible();

    // the second one requires the first
    await page.goto("/admin/courses/new");
    await page.getByLabel("Code").fill(next);
    await page.getByLabel("Department").fill("QA");
    await page.getByLabel("Title").fill("Quality Beyond");
    await page.getByLabel("Find a prerequisite").fill(base);
    await page.getByRole("group", { name: "Courses" }).getByRole("checkbox").first().check();
    await page.getByRole("button", { name: "Save course" }).click();
    await page.getByRole("tab", { name: "Prerequisites" }).click();
    await expect(page.getByRole("link", { name: new RegExp(base) })).toBeVisible();

    // making the first require the second would be a cycle
    await page.getByRole("link", { name: new RegExp(base) }).click();
    await page.getByRole("link", { name: "Edit" }).click();
    await page.getByLabel("Find a prerequisite").fill(next);
    await page.getByRole("group", { name: "Courses" }).getByRole("checkbox").first().check();
    await page.getByRole("button", { name: "Save course" }).click();
    await expect(page.getByTestId("form-error")).toContainText(`${base} → ${next} → ${base}`);

    // leaving the edited form asks first
    await page.getByRole("link", { name: "Cancel" }).click();
    await expect(page.getByRole("dialog")).toContainText("Leave without saving?");
    await page.getByRole("button", { name: "Leave" }).click();

    // the second course has no sections and nothing requires it, so it can go
    await page.goto("/admin/courses");
    await page.getByLabel("Search courses").fill(next);
    await page.getByRole("link", { name: next }).click();
    await page.getByRole("button", { name: "Delete" }).click();
    await expect(page.getByRole("dialog")).toContainText(`Delete ${next}?`);
    await page.getByRole("dialog").getByRole("button", { name: "Delete" }).click();
    await expect(page).toHaveURL(/\/admin\/courses$/);
  });

  test("switching tabs with an unsaved profile asks, and Stay keeps the edit", async ({
    page,
    request,
  }) => {
    const email = `e2e.tabs.${unique()}@uni.example`;
    await request.post("/api/v1/auth/register", {
      data: { email, firstName: "Tara", lastName: "Tabs", password: "Passw0rd1" },
    });
    await approveStudent(request, email);

    await navLink(page, "Students").click();
    await page.getByLabel("Search students").fill(email);
    await page.getByRole("link", { name: "Tara Tabs" }).click();
    await page.getByLabel("First name").fill("Tamara");

    await page.getByRole("tab", { name: "Enrollments" }).click();
    await expect(page.getByRole("dialog")).toContainText("Leave without saving?");
    await page.getByRole("button", { name: "Stay" }).click();
    await expect(page.getByLabel("First name")).toHaveValue("Tamara");

    await page.getByRole("button", { name: "Save changes" }).click();
    await expect(page.getByRole("heading", { name: "Tamara Tabs" })).toBeVisible();
    await page.getByRole("tab", { name: "Transcript" }).click();
    await expect(page.getByTestId("cumulative-gpa")).toHaveText("—");
  });

  test("a semester is built and opened: the 10-minute rule guards the professor's week", async ({
    page,
  }) => {
    const year = between(3000, 9999);
    await navLink(page, "Semesters").click();
    await page.getByLabel("Code").fill(`${year}SU`);
    await page.getByLabel("Name").fill(`Summer ${year}`);
    await page.getByLabel("Starts").fill(`${year}-06-01`);
    await page.getByLabel("Ends").fill(`${year}-08-01`);
    await page.getByRole("button", { name: "Create draft" }).click();
    await expect(page.getByRole("heading", { name: `Summer ${year}` })).toBeVisible();

    const addSection = async (course: string, start: string, end: string) => {
      await page.getByRole("link", { name: "Add section" }).click();
      const option = page.getByLabel("Course").locator("option", { hasText: `${course} ·` });
      await page.getByLabel("Course").selectOption({ label: ((await option.textContent()) ?? "").trim() });
      await page.getByLabel("Professor").selectOption({ index: 1 });
      await page.getByLabel("Start of meeting 1").fill(start);
      await page.getByLabel("End of meeting 1").fill(end);
      await page.getByRole("button", { name: "Save section" }).click();
    };

    await addSection("CS101", "09:00", "10:15");
    await expect(page.getByRole("link", { name: "CS101-A" })).toBeVisible();
    await addSection("CS220", "10:20", "11:00");
    await expect(page.getByTestId("form-error")).toContainText("already teaches CS101-A");
    await page.getByLabel("Start of meeting 1").fill("10:25");
    await page.getByRole("button", { name: "Save section" }).click();
    await expect(page.getByRole("link", { name: "CS220-A" })).toBeVisible();

    await page.getByRole("button", { name: "Open enrollment" }).click();
    await expect(page.getByRole("dialog")).toContainText("students can enroll");
    await page.getByRole("dialog").getByRole("button", { name: "Open enrollment" }).click();
    await expect(page.getByText("Enrollment open").first()).toBeVisible();
    await expect(page.getByRole("link", { name: "Add section" })).toHaveCount(0);
  });

  test("ending a running semester names the sections still missing grades", async ({ page }) => {
    await navLink(page, "Semesters").click();
    await page.getByRole("link", { name: "Fall 2026" }).click();
    await page.getByRole("button", { name: "End the semester" }).click();

    await expect(page.getByRole("dialog")).toContainText("Grades are still missing in CS301-A");
    await page.getByRole("dialog").getByRole("button", { name: "Cancel" }).click();
    await expect(page.getByRole("dialog")).toBeHidden();
  });
});
