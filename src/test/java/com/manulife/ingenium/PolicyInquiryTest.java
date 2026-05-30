package com.manulife.ingenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * Selenium + TestNG port of the original Playwright "STABLE Ingenium Policy
 * Inquiry Flow". Mirrors the same 9 steps, iframe handling, detached-frame
 * resilience, the OK-popup "break", and the ~10s reload waits.
 *
 * Config comes from environment variables (same names as before):
 *   APP_URL, APP_USERNAME, APP_PASSWORD, COMPANY, POLICY_ID
 */
public class PolicyInquiryTest {

    private WebDriver driver;

    private final String BASE_URL  = env("APP_URL", null);
    private final String USERNAME  = env("APP_USERNAME", null);
    private final String PASSWORD  = env("APP_PASSWORD", null);
    private final String COMPANY   = env("COMPANY", "Manulife");
    private final String POLICY_ID = env("POLICY_ID", null);

    private static final Path SHOTS = Paths.get("screenshots");

    private static String env(String key, String dflt) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? dflt : v;
    }

    // ---------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------
    @BeforeClass
    public void setUp() throws Exception {
        Assert.assertNotNull(POLICY_ID, "POLICY_ID must be provided");
        Files.createDirectories(SHOTS);

        // Selenium 4 has Selenium Manager built in; WebDriverManager is a
        // belt-and-suspenders driver resolver. Either way the driver is matched
        // to whatever Chrome is in the image.
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // Point at the Chrome installed in the image, if exposed via CHROME_BIN
        // (markhobson/maven-chrome installs it at /usr/bin/google-chrome).
        String chromeBin = System.getenv("CHROME_BIN");
        if (chromeBin == null || chromeBin.isBlank()) {
            if (new File("/usr/bin/google-chrome").exists()) {
                chromeBin = "/usr/bin/google-chrome";
            }
        }
        if (chromeBin != null && !chromeBin.isBlank()) {
            options.setBinary(chromeBin);
            System.out.println("\uD83C\uDF0D Using Chrome binary: " + chromeBin);
        }

        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--ignore-certificate-errors");   // self-signed app cert
        options.addArguments("--window-size=1920,1080");
        options.setAcceptInsecureCerts(true);

        driver = new ChromeDriver(options);
        // No implicit wait: we manage waits explicitly to match the PW timings
        // and to keep iframe scans fast.
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ---------------------------------------------------------------
    // The flow
    // ---------------------------------------------------------------
    @Test
    public void policyInquiryFlow() throws Exception {

        // ======================================================
        // STEP 1: Launch App
        // ======================================================
        driver.get(BASE_URL);
        System.out.println("\u2705 URL: " + driver.getCurrentUrl());
        screenshot("01-launch.png");

        // ======================================================
        // STEP 2: Click English Sign On (if present)
        // ======================================================
        // Selenium has no iframe-agnostic getByText; the English Sign On link
        // is on the top document here.
        List<WebElement> englishLinks =
                driver.findElements(By.xpath("//*[contains(normalize-space(.),'English Sign On')]"));
        WebElement english = firstDisplayed(englishLinks);
        if (english != null) {
            english.click();
            System.out.println("\u2705 Clicked English Sign On");
        }
        sleep(5000);

        // ======================================================
        // STEP 3 & 4: Find LOGIN FRAME and login (if needed)
        // ======================================================
        // Selenium can only query elements in the currently switched frame, so
        // we walk the frames, switching into each, and look for a password box.
        boolean loggedIn = false;
        int frameCount = countTopFrames();

        for (int i = 0; i < frameCount; i++) {
            driver.switchTo().defaultContent();
            try {
                driver.switchTo().frame(i);
            } catch (Exception e) {
                continue; // detached / not switchable, skip
            }

            List<WebElement> pwd = safeFind(By.cssSelector("input[type='password']"));
            if (!pwd.isEmpty()) {
                System.out.println("\u2705 Login frame found");

                WebElement userBox = safeFind(By.cssSelector("input[type='text']"))
                        .stream().findFirst().orElse(null);
                if (userBox != null) userBox.sendKeys(USERNAME);

                pwd.get(0).sendKeys(PASSWORD);

                // Company dropdown
                List<WebElement> selects = safeFind(By.tagName("select"));
                if (!selects.isEmpty()) {
                    new org.openqa.selenium.support.ui.Select(selects.get(0))
                            .selectByVisibleText(COMPANY);
                }

                // Submit button (case-insensitive "submit")
                WebElement submit = findButtonByText("submit", true);
                if (submit != null) submit.click();

                System.out.println("\u2705 Login submitted");
                loggedIn = true;
                sleep(5000);
                break;
            }
        }

        driver.switchTo().defaultContent();
        if (!loggedIn) {
            System.out.println("\u2705 Already logged in");
        }
        screenshot("02-after-login.png");

        // ======================================================
        // STEP 5: Handle OK popup
        // ======================================================
        // Clicking OK triggers a reload that detaches the other frames, so we
        // break right after the click and guard every frame access.
        int popupFrames = countTopFrames();
        boolean clickedOk = false;
        for (int i = 0; i < popupFrames && !clickedOk; i++) {
            driver.switchTo().defaultContent();
            try {
                driver.switchTo().frame(i);
                WebElement ok = findButtonByText("OK", false);
                if (ok != null) {
                    ok.click();
                    System.out.println("\u2705 Clicked OK popup");
                    clickedOk = true;
                }
            } catch (Exception e) {
                System.out.println("\u26a0\ufe0f Skipped detached frame during OK scan");
            }
        }
        driver.switchTo().defaultContent();

        // Page reloads after OK and takes ~10s to render the next view.
        sleep(10000);

        // ======================================================
        // STEP 6: Find APP FRAME (retry until frames settle)
        // ======================================================
        Integer appFrameIdx = null;
        for (int attempt = 0; attempt < 5 && appFrameIdx == null; attempt++) {
            int frames = countTopFrames();
            for (int i = 0; i < frames; i++) {
                driver.switchTo().defaultContent();
                try {
                    driver.switchTo().frame(i);
                    if (!safeFind(By.xpath("//*[contains(normalize-space(.),'Policy Inquiry')]")).isEmpty()) {
                        appFrameIdx = i;
                        break;
                    }
                } catch (Exception ignored) {
                    // detached frame, skip
                }
            }
            if (appFrameIdx == null) {
                System.out.println("\u23f3 App frame not ready, retry " + (attempt + 1) + "/5");
                driver.switchTo().defaultContent();
                sleep(3000);
            }
        }

        driver.switchTo().defaultContent();
        Assert.assertNotNull(appFrameIdx, "\u274c App frame not found");
        System.out.println("\u2705 App frame ready");

        // ======================================================
        // STEP 7: Navigate Menu
        // ======================================================
        driver.switchTo().defaultContent();
        driver.switchTo().frame(appFrameIdx);

        clickByText("Policy Inquiry");
        clickByText("Policy Inquiry - All Details");
        System.out.println("\u2705 Navigation successful");

        driver.switchTo().defaultContent();
        sleep(5000);

        // ======================================================
        // STEP 8: Enter Policy ID (retry until form frame settles)
        // ======================================================
        Integer formFrameIdx = null;
        for (int attempt = 0; attempt < 5 && formFrameIdx == null; attempt++) {
            int frames = countTopFrames();
            for (int i = 0; i < frames; i++) {
                driver.switchTo().defaultContent();
                try {
                    driver.switchTo().frame(i);
                    if (!safeFind(By.tagName("input")).isEmpty()) {
                        formFrameIdx = i;
                        break;
                    }
                } catch (Exception ignored) {
                    // detached frame, skip
                }
            }
            if (formFrameIdx == null) {
                System.out.println("\u23f3 Form frame not ready, retry " + (attempt + 1) + "/5");
                driver.switchTo().defaultContent();
                sleep(3000);
            }
        }

        driver.switchTo().defaultContent();
        Assert.assertNotNull(formFrameIdx, "\u274c Form frame not found");

        driver.switchTo().frame(formFrameIdx);
        List<WebElement> inputs = safeFind(By.tagName("input"));
        inputs.get(0).sendKeys(POLICY_ID);

        WebElement formOk = findButtonByText("OK", false);
        if (formOk != null) formOk.click();
        System.out.println("\u2705 Policy submitted: " + POLICY_ID);

        driver.switchTo().defaultContent();
        sleep(5000);

        // ======================================================
        // STEP 9: Screenshot
        // ======================================================
        String shot = "policy-" + POLICY_ID + ".png";
        screenshot(shot);
        System.out.println("\u2705 Screenshot saved: screenshots/" + shot);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Count frames in the current top document. */
    private int countTopFrames() {
        driver.switchTo().defaultContent();
        return driver.findElements(By.tagName("iframe")).size()
                + driver.findElements(By.tagName("frame")).size();
    }

    /** findElements that never throws on a detached/invalid context. */
    private List<WebElement> safeFind(By by) {
        try {
            return driver.findElements(by);
        } catch (Exception e) {
            return List.of();
        }
    }

    private WebElement firstDisplayed(List<WebElement> els) {
        for (WebElement e : els) {
            try {
                if (e.isDisplayed()) return e;
            } catch (Exception ignored) { }
        }
        return null;
    }

    /**
     * Find a clickable button/element whose visible text contains {@code text}.
     * @param ci case-insensitive match when true
     */
    private WebElement findButtonByText(String text, boolean ci) {
        String xpath = ci
                ? "//button[contains(translate(normalize-space(.),"
                  + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                  + text.toLowerCase() + "')]"
                  + " | //input[(@type='button' or @type='submit') and contains(translate(@value,"
                  + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                  + text.toLowerCase() + "')]"
                : "//button[contains(normalize-space(.),'" + text + "')]"
                  + " | //input[(@type='button' or @type='submit') and contains(@value,'" + text + "')]";
        List<WebElement> els = safeFind(By.xpath(xpath));
        return firstDisplayed(els) != null ? firstDisplayed(els)
                : (els.isEmpty() ? null : els.get(0));
    }

    /** Click the first element containing the given text in the current frame. */
    private void clickByText(String text) {
        List<WebElement> els = safeFind(
                By.xpath("//*[contains(normalize-space(.),'" + text + "')]"));
        WebElement target = firstDisplayed(els);
        if (target == null && !els.isEmpty()) target = els.get(els.size() - 1);
        Assert.assertNotNull(target, "Could not find clickable text: " + text);
        target.click();
    }

    private void screenshot(String name) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), SHOTS.resolve(name),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.out.println("\u26a0\ufe0f Screenshot failed (" + name + "): " + e.getMessage());
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
