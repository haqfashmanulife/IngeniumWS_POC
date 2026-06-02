package com.manulife.ingenium.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.List;

public class PolicyInquiryTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private String appUrl = System.getProperty("app.url");
    private String username = System.getProperty("app.username");
    private String password = System.getProperty("app.password");

    @BeforeMethod
    public void setUp() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    @Test
    public void loginTest() {

        driver.get(appUrl);

        WebElement signOn = wait.until(ExpectedConditions.elementToBeClickable(
                By.linkText("English Sign On")));
        signOn.click();

        for (String win : driver.getWindowHandles()) {
            driver.switchTo().window(win);
        }

        WebElement user = waitUntilAvailable(By.name("name"));
        user.sendKeys(username);

        WebElement pass = waitUntilAvailable(By.cssSelector("input[type='password']"));
        pass.sendKeys(password);

        WebElement login = waitUntilAvailable(By.cssSelector("input[type='submit']"));
        login.click();

        wait.until(d -> d.getCurrentUrl() != null);

        Assert.assertFalse(driver.getCurrentUrl().contains("SignOn"),
                "Login failed");

        System.out.println("Login successful");
    }

    // REQUIRED FOR PIPELINE
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

    private WebElement waitUntilAvailable(By locator) {
        return wait.until(driver -> {
            WebElement el = findElementInCurrentContextOrFrames(locator);
            return (el != null) ? el : null;
        });
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
