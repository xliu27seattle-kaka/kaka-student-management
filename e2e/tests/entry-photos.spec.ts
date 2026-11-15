import { expect, Page, test } from "@playwright/test";

/** The five buttons that switch the photograph behind the entry page. */
const bars = (page: Page) =>
  page.getByRole("group", { name: "Photographs" }).getByRole("button");

/** Waits until the photograph with this index is the one in front. */
async function waitForPhoto(page: Page, index: number) {
  await expect(page.locator(".bg img").nth(index)).toHaveClass(/\bon\b/);
}

/** Where the slow zoom of a photograph is, in milliseconds; null when it has no zoom running. */
function zoomTimeOf(page: Page, index: number) {
  return page
    .locator(".bg img")
    .nth(index)
    .evaluate((image) => {
      // getAnimations() also lists the opacity fade; the zoom is the keyframe animation.
      const zoom = image
        .getAnimations()
        .find((animation) => animation instanceof CSSAnimation);
      return zoom ? Number(zoom.currentTime) : null;
    });
}

test.describe("the photographs behind the entry page", () => {
  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto("/login");
    // Every photograph is downloaded up front, so indexes in .bg img match the buttons.
    await expect(page.locator(".bg img")).toHaveCount(5);
  });

  test("the switch buttons stay in exactly the same place for every photograph", async ({
    page,
  }) => {
    const group = page.getByRole("group", { name: "Photographs" });
    const boxes = [];
    for (let i = 0; i < 5; i++) {
      await bars(page).nth(i).click();
      await waitForPhoto(page, i);
      boxes.push(await group.boundingBox());
    }

    for (const box of boxes) {
      expect(box).toEqual(boxes[0]);
    }
  });

  test("the caption lives apart from the switch buttons and names the photograph shown", async ({
    page,
  }) => {
    const caption = page.getByTestId("photo-caption");
    await bars(page).nth(2).click();
    await waitForPhoto(page, 2);

    await expect(caption).toContainText("Assateague Island, Maryland");
    await expect(caption).toContainText("National Park Service");
    await expect(
      caption.getByRole("group", { name: "Photographs" }),
    ).toHaveCount(0);
  });

  test("the photograph that leaves keeps its zoom instead of snapping back", async ({
    page,
  }) => {
    // Move the slow zoom of the first photograph ten seconds ahead.
    await page
      .locator(".bg img")
      .first()
      .evaluate((image) => {
        for (const animation of image.getAnimations()) {
          if (animation instanceof CSSAnimation) animation.currentTime = 10_000;
        }
      });

    await bars(page).nth(1).click();
    await waitForPhoto(page, 1);

    // Still the same zoom, carrying on from where it was: not removed, not restarted.
    expect(await zoomTimeOf(page, 0)).toBeGreaterThanOrEqual(10_000);
  });

  test("the photograph that leaves stays fully visible while the next one fades in over it", async ({
    page,
  }) => {
    await bars(page).nth(1).click();
    await waitForPhoto(page, 1);

    const style = (index: number) =>
      page
        .locator(".bg img")
        .nth(index)
        .evaluate((image) => {
          const css = getComputedStyle(image);
          return {
            opacity: Number(css.opacity),
            zIndex: Number(css.zIndex),
            fade: css.transitionDuration,
          };
        });
    const outgoing = await style(0);
    const incoming = await style(1);

    expect(outgoing.opacity).toBe(1);
    expect(incoming.zIndex).toBeGreaterThan(outgoing.zIndex);
    expect(incoming.fade).not.toBe("0s");
  });
});

test("before signing in a visitor learns what the portal offers each role", async ({
  page,
}) => {
  await page.goto("/login");

  const intro = page.getByRole("region", { name: "What you can do here" });
  await expect(intro.getByText("Students", { exact: true })).toBeVisible();
  await expect(intro.getByText("Professors", { exact: true })).toBeVisible();
  await expect(
    intro.getByText("Administrators", { exact: true }),
  ).toBeVisible();
});
