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
    private String screenshotDir = System.getProperty("screenshot.dir");

    @BeforeMethod
    public void setUp() throws Exception {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");   // remove if debugging
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        driver.manage().window().setSize(new Dimension(1920, 1080));
    }

    @Test
    public void loginTest() throws Exception {

        // ===== STEP 1: OPEN APPLICATION =====
        driver.get(appUrl);
        takeScreenshot("01-app-opened.png");

        // Click English Sign On
        WebElement signOnLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.linkText("English Sign On")));
        signOnLink.click();

        // Switch to new window if needed
        for (String win : driver.getWindowHandles()) {
            driver.switchTo().window(win);
        }

        takeScreenshot("02-signon-opened.png");

        // ===== STEP 2: SWITCH TO CORRECT FRAME =====
        switchToFrameWithLoginForm();

        // ===== STEP 3: FIND USERNAME FIELD =====
        WebElement userField = findElementFlexible(
                By.name("name"),
                By.name("username"),
                By.cssSelector("input[name*='user' i]")
        );

        Assert.assertNotNull(userField, "Username field not found");
        userField.clear();
        userField.sendKeys(username);

        // ===== STEP 4: FIND PASSWORD FIELD =====
        WebElement pwdField = findElementFlexible(
                By.name("password"),
                By.id("password"),
                By.cssSelector("input[type='password']")
        );

        Assert.assertNotNull(pwdField, "Password field not found");
        pwdField.clear();
        pwdField.sendKeys(password);

        // ===== STEP 5: COMPANY (OPTIONAL) =====
        WebElement companyField = findElementFlexible(
                By.name("company"),
                By.cssSelector("select[name*='company' i]")
        );

        if (companyField != null) {
            if (companyField.getTagName().equalsIgnoreCase("select")) {
                new Select(companyField).selectByVisibleText(company);
            } else {
                companyField.clear();
                companyField.sendKeys(company);
            }
        }

        takeScreenshot("03-login-filled.png");

        // ===== STEP 6: CLICK SIGN ON =====
        WebElement submitBtn = findElementFlexible(
                By.cssSelector("input[type='submit']"),
                By.cssSelector("button[type='submit']"),
                By.xpath("//input[contains(@value,'Sign')]"),
                By.xpath("//button[contains(text(),'Sign')]")
        );

        Assert.assertNotNull(submitBtn, "Submit button not found");
        submitBtn.click();

        // ===== STEP 7: POST LOGIN VALIDATION =====
        wait.until(ExpectedConditions.urlContains("Menu")); // adjust if needed

        takeScreenshot("04-after-login.png");

        System.out.println("✅ Login successful");
    }

    // ===== FRAME HANDLING =====
    private void switchToFrameWithLoginForm() {
        driver.switchTo().defaultContent();

        List<WebElement> frames = driver.findElements(By.tagName("frame"));
        System.out.println("Total frames: " + frames.size());

        for (int i = 0; i < frames.size(); i++) {
            driver.switchTo().defaultContent();
            driver.switchTo().frame(i);

            List<WebElement> inputs = driver.findElements(By.tagName("input"));
            if (inputs.size() > 0) {
                System.out.println("✅ Found login form in frame index: " + i);
                return;
            }
        }

        throw new RuntimeException("❌ No frame contains login elements");
    }

    // ===== FLEXIBLE FINDER =====
    private WebElement findElementFlexible(By... locators) {
        for (By locator : locators) {
            try {
                List<WebElement> elements = driver.findElements(locator);
                if (!elements.isEmpty()) {
                    return elements.get(0);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    // ===== SCREENSHOT =====
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
