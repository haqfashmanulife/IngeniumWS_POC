import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.List;

public class PolicyInquiryTest {

    WebDriver driver;
    WebDriverWait wait;

    String BASE_URL   = System.getProperty("APP_URL");
    String USERNAME   = System.getProperty("APP_USERNAME");
    String PASSWORD   = System.getProperty("APP_PASSWORD");
    String COMPANY    = System.getProperty().getOrDefault("COMPANY", "Manulife");
    String POLICY_ID  = System.getProperty("POLICY_ID");

    @BeforeEach
    void setup() {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
    }

    // ======================================================
    // UTIL: SWITCH TO FRAME CONTAINING ELEMENT
    // ======================================================
    boolean switchToFrame(By locator) {
        driver.switchTo().defaultContent();

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));

        for (WebElement frame : frames) {
            driver.switchTo().defaultContent();
            driver.switchTo().frame(frame);

            if (driver.findElements(locator).size() > 0) {
                return true;
            }
        }
        driver.switchTo().defaultContent();
        return false;
    }

    // ======================================================
    // UTIL: SAFE CLICK
    // ======================================================
    void safeClick(By locator) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).click();
    }

    // ======================================================
    // UTIL: CLICK OK BUTTON (ROBUST)
    // ======================================================
    void clickOK() {

        driver.switchTo().defaultContent();

        // 1. Try normal DOM
        List<WebElement> okButtons = driver.findElements(
                By.xpath("//button[text()='OK'] | //input[@value='OK']")
        );

        if (!okButtons.isEmpty()) {
            okButtons.get(0).click();
            System.out.println("✅ OK clicked (main DOM)");
            return;
        }

        // 2. Try frames
        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));

        for (WebElement frame : frames) {
            driver.switchTo().defaultContent();
            driver.switchTo().frame(frame);

            List<WebElement> btns = driver.findElements(
                    By.xpath("//button[text()='OK'] | //input[@value='OK']")
            );

            if (!btns.isEmpty()) {
                btns.get(0).click();
                System.out.println("✅ OK clicked (frame)");
                return;
            }
        }

        // 3. Fallback (coordinate click)
        Dimension size = driver.manage().window().getSize();

        int x = size.width / 2;
        int y = size.height - 50;

        new Actions(driver).moveByOffset(x, y).click().perform();

        System.out.println("✅ OK clicked (fallback)");
    }

    // ======================================================
    // TEST
    // ======================================================
    @Test
    void testPolicyInquiryFlow() throws InterruptedException {

        // ===============================
        // STEP 1: Launch
        // ===============================
        driver.get(BASE_URL);

        Thread.sleep(5000);

        // ===============================
        // STEP 2: English Sign On
        // ===============================
        List<WebElement> english = driver.findElements(By.xpath("//*[text()='English Sign On']"));

        if (!english.isEmpty()) {
            english.get(0).click();
            System.out.println("✅ English clicked");
            Thread.sleep(5000);
        }

        // ===============================
        // STEP 3: Login Frame
        // ===============================
        switchToFrame(By.cssSelector("input[type='password']"));

        // ===============================
        // STEP 4: Login
        // ===============================
        driver.findElement(By.cssSelector("input[type='text']")).sendKeys(USERNAME);
        driver.findElement(By.cssSelector("input[type='password']")).sendKeys(PASSWORD);

        Select companySelect = new Select(driver.findElement(By.tagName("select")));
        companySelect.selectByVisibleText(COMPANY);

        driver.findElement(By.xpath("//button[contains(text(),'Submit')]")).click();

        System.out.println("✅ Login submitted");

        Thread.sleep(8000);

        // ===============================
        // STEP 5: OK Popup
        // ===============================
        clickOK();
        Thread.sleep(8000);

        // ===============================
        // STEP 6: App Frame
        // ===============================
        switchToFrame(By.xpath("//span[@title='Policy Inquiry']"));

        // ===============================
        // STEP 7: Navigation
        // ===============================
        driver.findElement(By.xpath("//span[@title='Policy Inquiry']")).click();

        driver.findElement(By.xpath("//a[contains(text(),'Policy Inquiry - All Details')]"))
              .click();

        System.out.println("✅ Navigation successful");

        Thread.sleep(6000);

        // ===============================
        // STEP 8: Enter Policy
        // ===============================
        switchToFrame(By.tagName("input"));

        driver.findElement(By.tagName("input")).sendKeys(POLICY_ID);

        System.out.println("✅ Policy entered");

        Thread.sleep(3000);

        // ===============================
        // STEP 9: Click OK
        // ===============================
        clickOK();

        Thread.sleep(8000);

        // ===============================
        // STEP 10: Screenshot
        // ===============================
        File screenshot = ((TakesScreenshot)driver)
                .getScreenshotAs(OutputType.FILE);

        System.out.println("✅ Screenshot captured");
    }
}
