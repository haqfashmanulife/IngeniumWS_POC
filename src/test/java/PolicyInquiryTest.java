package tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

public class PolicyInquiryTest {

    private WebDriver driver;
    private WebDriverWait wait;

    // ========= Jenkins Parameters =========
    private String appUrl;
    private String username;
    private String password;
    private String company;
    private String policyId;

    @BeforeEach
    public void setUp() {

        // ✅ Read Jenkins parameters safely
        appUrl   = System.getProperty("APP_URL", "");
        username = System.getProperty("APP_USERNAME", "");
        password = System.getProperty("APP_PASSWORD", "");
        company  = System.getProperty("COMPANY", "");
        policyId = System.getProperty("POLICY_ID", "");

        System.out.println("===== TEST INPUTS =====");
        System.out.println("URL: " + appUrl);
        System.out.println("User: " + username);
        System.out.println("Company: " + company);
        System.out.println("Policy: " + policyId);

        // ✅ Chrome headless for Docker
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Test
    public void testPolicyInquiry() {

        // ✅ Open application
        driver.get(appUrl);

        // ============================
        // LOGIN STEP
        // ============================

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")))
            .sendKeys(username);

        driver.findElement(By.name("password")).sendKeys(password);

        driver.findElement(By.name("company")).sendKeys(company);

        driver.findElement(By.id("loginButton")).click();

        System.out.println("✅ Login step completed");

        // ============================
        // NAVIGATION (example)
        // ============================

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("policyMenu")));

        WebElement menu = driver.findElement(By.id("policyMenu"));

        Actions actions = new Actions(driver);
        actions.moveToElement(menu).perform();

        driver.findElement(By.id("policyInquiry")).click();

        System.out.println("✅ Navigation completed");

        // ============================
        // POLICY SEARCH
        // ============================

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("policyId")))
            .sendKeys(policyId);

        driver.findElement(By.id("searchBtn")).click();

        System.out.println("✅ Search executed");

        // ============================
        // VALIDATION
        // ============================

        WebElement result = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("policyResult"))
        );

        if (result.getText().contains(policyId)) {
            System.out.println("✅ TEST PASSED: Policy found");
        } else {
            throw new RuntimeException("❌ TEST FAILED: Policy not found");
        }
    }

    @AfterEach
    public void tearDown() {

        if (driver != null) {
            driver.quit();
        }

        // ✅ Optional artifact example
        File targetDir = new File("target");
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }

        System.out.println("✅ Test finished");
    }
}
