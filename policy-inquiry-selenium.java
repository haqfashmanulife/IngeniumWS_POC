package com.manulife.ingenium.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class PolicyInquiryTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private String appUrl;
    private String company;
    private String username;
    private String password;
    private String policyId;
    private String screenshotDir;

    @BeforeMethod
    public void setUp() {
        appUrl = getProperty("app.url", "APP_URL");
        company = getProperty("company", "COMPANY");
        username = getProperty("app.username", "APP_USERNAME");
        password = getProperty("app.password", "APP_PASSWORD");
        policyId = getProperty("policy.id", "POLICY_ID");
        screenshotDir = getProperty("screenshot.dir", "SCREENSHOT_DIR", "screenshots");

        Assert.assertNotNull(appUrl, "APP URL must not be null");
        Assert.assertNotNull(company, "Company must not be null");
        Assert.assertNotNull(username, "Username must not be null");
        Assert.assertNotNull(password, "Password must not be null");
        Assert.assertNotNull(policyId, "Policy ID must not be null");

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
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(45));
    }

    @Test
    public void policyInquiryTest() throws Exception {
        System.out.println("Opening application URL: " + appUrl);

        driver.get(appUrl);

        waitForDocumentReady();
        takeScreenshot("01-app-opened");
        printPageDiagnostics("after app open");

        /*
         * Do not assume login is always inside a frame.
         * This test searches default content first, then all frames recursively.
         */

        typeFirstAvailable("company", new By[]{
                By.name("company"),
                By.id("company"),
                By.cssSelector("input[name='company']"),
                By.cssSelector("input[id*='company' i]"),
                By.cssSelector("input[name*='company' i]")
        }, company);

        typeFirstAvailable("username", new By[]{
                By.name("username"),
                By.id("username"),
                By.name("user"),
                By.id("user"),
                By.cssSelector("input[name='username']"),
                By.cssSelector("input[id*='user' i]"),
                By.cssSelector("input[name*='user' i]"),
                By.cssSelector("input[type='text']")
        }, username);

        typeFirstAvailable("password", new By[]{
                By.name("password"),
                By.id("password"),
                By.cssSelector("input[type='password']"),
                By.cssSelector("input[name='password']"),
                By.cssSelector("input[id*='password' i]")
        }, password);

        takeScreenshot("02-login-details-entered");

        clickFirstAvailable("login button", new By[]{
                By.cssSelector("button[type='submit']"),
                By.cssSelector("input[type='submit']"),
                By.id("login"),
                By.name("login"),
                By.cssSelector("button[id*='login' i]"),
                By.cssSelector("input[id*='login' i]"),
                By.cssSelector("button[name*='login' i]"),
                By.cssSelector("input[name*='login' i]"),
                By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]"),
                By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]")
        });

        driver.switchTo().defaultContent();

        waitForDocumentReady();
        takeScreenshot("03-login-submitted");
        printPageDiagnostics("after login submit");

        typeFirstAvailable("policy id", new By[]{
                By.name("policyId"),
                By.id("policyId"),
                By.name("policyNumber"),
                By.id("policyNumber"),
                By.name("policy"),
                By.id("policy"),
                By.cssSelector("input[name='policyId']"),
                By.cssSelector("input[id*='policy' i]"),
                By.cssSelector("input[name*='policy' i]")
        }, policyId);

        takeScreenshot("04-policy-id-entered");

        clickFirstAvailable("policy search/inquiry button", new By[]{
                By.id("search"),
                By.name("search"),
                By.id("inquiry"),
                By.name("inquiry"),
                By.cssSelector("button[type='submit']"),
                By.cssSelector("input[type='submit']"),
                By.cssSelector("button[id*='search' i]"),
                By.cssSelector("input[id*='search' i]"),
                By.cssSelector("button[name*='search' i]"),
                By.cssSelector("input[name*='search' i]"),
                By.cssSelector("button[id*='inquiry' i]"),
                By.cssSelector("input[id*='inquiry' i]"),
                By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]"),
                By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'inquiry')]"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'inquiry')]")
        });

        driver.switchTo().defaultContent();

        waitForDocumentReady();
        takeScreenshot("05-policy-search-submitted");
        printPageDiagnostics("after policy search submit");

        boolean policyVisible = waitUntilTextAppearsInDefaultOrFrames(policyId, 45);

        takeScreenshot("06-policy-result");

        Assert.assertTrue(policyVisible, "Policy ID was not visible after inquiry: " + policyId);
    }

    private void typeFirstAvailable(String fieldName, By[] locators, String value) {
        WebElement element = findFirstAvailableElement(fieldName, locators, true);

        element.clear();
        element.sendKeys(value);

        System.out.println("Entered value for field: " + fieldName);
    }

    private void clickFirstAvailable(String elementName, By[] locators) {
        WebElement element = findFirstAvailableElement(elementName, locators, false);

        element.click();

        System.out.println("Clicked element: " + elementName);
    }

    private WebElement findFirstAvailableElement(String elementName, By[] locators, boolean requireVisible) {
        for (By locator : locators) {
            try {
                WebElement found = wait.until((ExpectedCondition<WebElement>) webDriver -> {
                    try {
                        webDriver.switchTo().defaultContent();

                        WebElement element = findElementInCurrentContextOrFrames(locator, requireVisible, 0);

                        if (element == null) {
                            return null;
                        }

                        if (!element.isEnabled()) {
                            return null;
                        }

                        return element;
                    } catch (StaleElementReferenceException ignored) {
                        return null;
                    } catch (WebDriverException ignored) {
                        return null;
                    }
                });

                if (found != null) {
                    System.out.println("Found " + elementName + " using locator: " + locator);
                    return found;
                }
            } catch (TimeoutException ignored) {
                System.out.println("Not found with locator: " + locator);
            }
        }

        takeScreenshotQuietly("element-not-found-" + sanitizeFileName(elementName));
        printPageDiagnostics("element not found: " + elementName);

        throw new RuntimeException("Unable to find element: " + elementName);
    }

    private WebElement findElementInCurrentContextOrFrames(By locator, boolean requireVisible, int depth) {
        if (depth > 5) {
            return null;
        }

        WebElement element = findElementInCurrentContext(locator, requireVisible);

        if (element != null) {
            return element;
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));

        System.out.println("Frame count at depth " + depth + ": " + frames.size());

        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);

                WebElement frameElement = findElementInCurrentContextOrFrames(locator, requireVisible, depth + 1);

                if (frameElement != null) {
                    return frameElement;
                }

                driver.switchTo().parentFrame();
            } catch (NoSuchFrameException | StaleElementReferenceException ignored) {
                driver.switchTo().defaultContent();
            }
        }

        if (depth == 0) {
            driver.switchTo().defaultContent();
        }

        return null;
    }

    private WebElement findElementInCurrentContext(By locator, boolean requireVisible) {
        List<WebElement> elements = driver.findElements(locator);

        for (WebElement element : elements) {
            try {
                if (requireVisible && !element.isDisplayed()) {
                    continue;
                }

                return element;
            } catch (StaleElementReferenceException ignored) {
                // Try next element
            }
        }

        return null;
    }

    private boolean waitUntilTextAppearsInDefaultOrFrames(String text, int timeoutSeconds) {
        WebDriverWait textWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

        try {
            return textWait.until(webDriver -> {
                try {
                    webDriver.switchTo().defaultContent();

                    return pageSourceContainsTextInCurrentContextOrFrames(text, 0);
                } catch (Exception e) {
                    return false;
                }
            });
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean pageSourceContainsTextInCurrentContextOrFrames(String text, int depth) {
        if (depth > 5) {
            return false;
        }

        String source = driver.getPageSource();

        if (source != null && source.contains(text)) {
            return true;
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));

        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);

                if (pageSourceContainsTextInCurrentContextOrFrames(text, depth + 1)) {
                    return true;
                }

                driver.switchTo().parentFrame();
            } catch (NoSuchFrameException | StaleElementReferenceException ignored) {
                driver.switchTo().defaultContent();
            }
        }

        if (depth == 0) {
            driver.switchTo().defaultContent();
        }

        return false;
    }

    private void waitForDocumentReady() {
        try {
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver)
                            .executeScript("return document.readyState")
                            .equals("complete")
            );
        } catch (Exception ignored) {
            System.out.println("Document ready check skipped or failed. Continuing.");
        }
    }

    private void printPageDiagnostics(String label) {
        try {
            driver.switchTo().defaultContent();

            System.out.println("===== PAGE DIAGNOSTICS: " + label + " =====");
            System.out.println("Current URL: " + driver.getCurrentUrl());
            System.out.println("Title: " + driver.getTitle());
            System.out.println("Frame count: " + driver.findElements(By.cssSelector("frame, iframe")).size());
            System.out.println("Input count: " + driver.findElements(By.cssSelector("input")).size());
            System.out.println("Button count: " + driver.findElements(By.cssSelector("button")).size());
            System.out.println("Link count: " + driver.findElements(By.cssSelector("a")).size());
            System.out.println("Page source length: " + driver.getPageSource().length());
        } catch (Exception e) {
            System.out.println("Unable to print page diagnostics: " + e.getMessage());
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

    private void takeScreenshotQuietly(String name) {
        try {
            takeScreenshot(name);
        } catch (Exception ignored) {
            // Ignore screenshot failure
        }
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9-_]", "-");
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

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
