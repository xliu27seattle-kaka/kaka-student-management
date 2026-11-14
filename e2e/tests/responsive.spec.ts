import { expect, test } from "@playwright/test";

const VIEWPORTS = [
  { name: "laptop", width: 1440, height: 900 },
  { name: "small window", width: 1000, height: 620 },
  { name: "tablet", width: 768, height: 1000 },
  { name: "phone", width: 390, height: 844 },
  { name: "small phone", width: 320, height: 640 },
];

for (const page_ of ["/login", "/register"]) {
  for (const viewport of VIEWPORTS) {
    test(
      page_ +
        " fits a " +
        viewport.name +
        " (" +
        viewport.width +
        "x" +
        viewport.height +
        ")",
      async ({ page }) => {
        await page.setViewportSize({
          width: viewport.width,
          height: viewport.height,
        });
        await page.goto(page_);

        // The greeting over the photograph is there.
        await expect(page.getByRole("heading", { level: 1 })).toBeVisible();

        // Nothing makes the page scroll sideways.
        const overflow = await page.evaluate(
          () => document.documentElement.scrollWidth - window.innerWidth,
        );
        expect(overflow).toBeLessThanOrEqual(0);

        // The submit button can be reached and lies completely inside the window.
        const submit = page.locator("button[type=submit]");
        await submit.scrollIntoViewIfNeeded();
        // (Positions are fractional after scrolling, so allow one pixel of rounding.)
        const box = (await submit.boundingBox())!;
        expect(box.x).toBeGreaterThanOrEqual(-1);
        expect(box.x + box.width).toBeLessThanOrEqual(viewport.width + 1);
        expect(box.y).toBeGreaterThanOrEqual(-1);
        expect(box.y + box.height).toBeLessThanOrEqual(viewport.height + 1);

        // The photograph has loaded behind it.
        const photoLoaded = await page.evaluate(() => {
          const image = document.querySelector<HTMLImageElement>(".bg img.on");
          return !!image && image.complete && image.naturalWidth > 0;
        });
        expect(photoLoaded).toBe(true);
      },
    );
  }
}
