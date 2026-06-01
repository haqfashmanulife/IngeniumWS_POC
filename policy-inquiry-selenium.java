package com.manulife.ingenium.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class PolicyInquiryTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private final String appUrl = getProperty("app.url", "APP_URL");
    private final String company = getProperty("company", "COMPANY");
    private final String username = getProperty("app.username", "APP_USERNAME");
    private final String password = getProperty("app.password", "APP_PASSWORD");
    private final String policyId = getProperty("policy.id", "POLICY_ID");
    private final String screenshotDir = getProperty("screenshot.dir", "SCREENSHOT_DIR", "screenshots");

    @BeforeMethod
    public void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(40));
    }

    @Test
    public void policyInquiryTest() throws Exception {
        Assert.assertNotNull(appUrl, "APP URL must not be null");
        Assert.assertNotNull(username, "Username must not be null");
        Assert.assertNotNull(password, "Password must not be null");
        Assert.assertNotNull(policyId, "Policy ID must not be null");

        driver.get(appUrl);
        takeScreenshot("01-app-opened");

        /*
         * Frame-based login.
         * This replaces Playwright loginFrame.locator(...) logic.
         *
         * Update frame locator if your actual login frame has a specific name/id.
         */
        switchToLoginFrame();

        typeFirstAvailable(
                new By[]{
                        By.name("company"),
                        By.id("company"),
                        By.cssSelector("input[name='company']"),
                        By.cssSelector("input[id*='company' i]")
                },
                company
        );

        typeFirstAvailable(
                new By[]{
                        By.name("username"),
                        By.id("username"),
                        By.cssSelector("input[name='username']"),
                        By.cssSelector("input[id*='user' i]")
                },
                username
        );

        typeFirstAvailable(
                new By[]{
                        By.name("password"),
                        By.id("password"),
                        By.cssSelector("input[type='password']"),
                        By.cssSelector("input[name='password']")
                },
                password
        );

        clickFirstAvailable(
                new By[]{
                        By.cssSelector("button[type='submit']"),
                        By.cssSelector("input[type='submit']"),
                        By.id("login"),
                        By.name("login"),
                        By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]"),
                        By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]")
                }
        );

        driver.switchTo().defaultContent();

        takeScreenshot("02-login-submitted");

        /*
         * Policy inquiry navigation.
         * Update these locators if your app uses exact menu ids.
         */
        waitForPageAfterLogin();

        switchToApplicationFrameIfPresent();

        typeFirstAvailable(
                new By[]{
                        By.name("policyId"),
                        By.id("policyId"),
                        By.cssSelector("input[name='policyId']"),
                        By.cssSelector("input[id*='policy' i]"),
                        By.cssSelector("input[name*='policy' i]")
                },
                policyId
        );

        clickFirstAvailable(
                new By[]{
                        By.id("search"),
                        By.name("search"),
                        By.cssSelector("button[type='submit']"),
                        By.cssSelector("input[type='submit']"),
                        By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]"),
                        By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]"),
                        By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'inquiry')]")
                }
        );

        takeScreenshot("03-policy-search-submitted");

        boolean policyVisible = waitUntilTextAppears(policyId, 40);

        takeScreenshot("04-policy-result");

        Assert.assertTrue(policyVisible, "Policy ID was not visible after inquiry: " + policyId);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void switchToLoginFrame() {
        driver.switchTo().defaultContent();

        try {
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.cssSelector("frame, iframe")));
            return;
        } catch (TimeoutException ignored) {
            driver.switchTo().defaultContent();
        }

        throw new RuntimeException("Login frame was not found. Please update switchToLoginFrame() locator.");
    }

    private void switchToApplicationFrameIfPresent() {
        driver.switchTo().defaultContent();

        try {
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.cssSelector("frame, iframe")));
        } catch (TimeoutException ignored) {
            driver.switchTo().defaultContent();
        }
    }

    private void waitForPageAfterLogin() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        wait.until(webDriver ->
                ((org.openqa.selenium.JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState")
                        .equals("complete")
        );
    }

    private void typeFirstAvailable(By[] locators, String value) {
        for (By locator : locators) {
            try {
                WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                element.clear();
                element.sendKeys(value);
                return;
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        throw new RuntimeException("Unable to find input field for value: " + value);
    }

    private void clickFirstAvailable(By[] locators) {
        for (By locator : locators) {
            try {
                WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                element.click();
                return;
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        throw new RuntimeException("Unable to find clickable element.");
    }

    private boolean waitUntilTextAppears(String text, int timeoutSeconds) {
        WebDriverWait textWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

        try {
            return textWait.until(webDriver ->
                    webDriver.getPageSource() != null &&
                    webDriver.getPageSource().contains(text)
            );
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void takeScreenshot(String name) throws Exception {
        Path screenshotPath = Path.of(screenshotDir);
        Files.createDirectories(screenshotPath);

        File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Path dest = screenshotPath.resolve(name + ".png");

        Files.copy(src.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        System.out.println("Screenshot saved: " + dest);
    }

    private static String getProperty(String propertyName, String envName) {
        return getProperty(propertyName, envName, null);
    }

    private static String getProperty(String propertyName, String envName, String defaultValue) {
        String value = System.getProperty(propertyName);

        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(envName);
        }

        if (value == null || value.trim().isEmpty()) {
            value = defaultValue;
        }

        return value;
    }
}
