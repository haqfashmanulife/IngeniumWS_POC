package com.manulife.ingenium.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class PolicyInquiryTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private String appUrl = System.getProperty("app.url");
    private String username = System.getProperty("app.username");
    private String password = System.getProperty("app.password");
    private String company = System.getProperty("company");
    private String policyId = System.getProperty("policy.id");
    private String screenshotDir = System.getProperty("screenshot.dir");

    @BeforeMethod
    public void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        driver.manage().window().setSize(new Dimension(1920, 1080));
    }

    @Test
    public void policyInquiryTest() {

        // Step 1: Open application
        driver.get(appUrl);
        takeScreenshot("01-app-opened.png");

        // Step 2: Click "English Sign On"
        WebElement signOnLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.linkText("English Sign On")));
        signOnLink.click();

        // Switch to latest window
        for (String win : driver.getWindowHandles()) {
            driver.switchTo().window(win);
        }

        takeScreenshot("02-signon-page.png");

        // Step 3: Enter Username
        WebElement userField = waitUntilAvailable(By.name("name"));
        Assert.assertNotNull(userField, "Username field not found");
        userField.clear();
        userField.sendKeys(username);

        // Step 4: Enter Password
        WebElement pwdField = waitUntilAvailable(By.cssSelector("input[type='password']"));
        Assert.assertNotNull(pwdField, "Password field not found");
        pwdField.clear();
        pwdField.sendKeys(password);

        // Step 5: Company (optional)
        WebElement companyField = findElementInCurrentContextOrFrames(By.name("company"));
        if (companyField != null) {
            if (companyField.getTagName().equalsIgnoreCase("select")) {
                new Select(companyField).selectByVisibleText(company);
            } else {
                companyField.clear();
                companyField.sendKeys(company);
            }
        }

        takeScreenshot("03-login-filled.png");

        // Step 6: Click Sign On
        WebElement submitBtn = waitUntilAvailable(By.cssSelector("input[type='submit']"));
        Assert.assertNotNull(submitBtn, "Submit button not found");
        submitBtn.click();

        // Step 7: Wait for navigation after login
        waitUntilPageLoads();

        takeScreenshot("04-after-login.png");

        // Step 8: Basic post-login validation
        String currentUrl = driver.getCurrentUrl();
        System.out.println("Post login URL: " + currentUrl);

        Assert.assertFalse(currentUrl.contains("SignOn"),
                "Login did not succeed, still on login page");

        // Step 9: (Optional) Policy Inquiry Step (basic placeholder)
        if (policyId != null && !policyId.isEmpty()) {
            System.out.println("Policy ID available: " + policyId);
            // Extend here once UI navigation is confirmed
        }

        System.out.println("Test completed successfully");
    }

    // REQUIRED METHOD FOR PIPELINE
    private WebElement findElementInCurrentContextOrFrames(By locator) {

        try {
            return driver.findElement(locator);
        } catch (Exception ignored) {}

        List<WebElement> frames = driver.findElements(By.tagName("frame"));

        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().defaultContent();
                driver.switchTo().frame(i);
                return driver.findElement(locator);
            } catch (Exception ignored) {}
        }

        driver.switchTo().defaultContent();
        return null;
    }

    // Wait wrapper that uses required method
    private WebElement waitUntilAvailable(By locator) {
        return wait.until(driver -> {
            WebElement element = findElementInCurrentContextOrFrames(locator);
            return (element != null) ? element : null;
        });
    }

    // Wait for page load
    private void waitUntilPageLoads() {
        wait.until(webDriver ->
                ((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState")
                        .equals("complete"));
    }

    // Screenshot utility
    private void takeScreenshot(String fileName) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path dest = Path.of(screenshotDir, fileName);
            Files.createDirectories(dest.getParent());
            Files.copy(src.toPath(), dest);
        } catch (Exception e) {
            System.out.println("Screenshot failed: " + e.getMessage());
        }
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
