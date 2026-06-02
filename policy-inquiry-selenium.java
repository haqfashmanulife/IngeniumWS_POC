package com.manulife.ingenium.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
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
        company = getProperty("company", "COMPANY", "Manulife");
        username = getProperty("app.username", "APP_USERNAME", "gocc");
        password = getProperty("app.password", "APP_PASSWORD", "ingenium");
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
        printAllLinks("after app open");

        clickEnglishSignOnIfNeeded();

        waitForDocumentReady();
        waitQuietly(5000);
        switchToLatestWindow();
        takeScreenshot("02-english-sign-on-opened");
        printPageDiagnostics("after english sign on");
        printPageDiagnosticsDeep("after english sign on");
        printAllLinks("after english sign on");

        enterLoginDetailsAndSubmit();

        waitForDocumentReady();
        waitQuietly(3000);
        takeScreenshot("03-login-submitted");

        /*
         * IMPORTANT:
         * After login, Ingenium shows a bottom-center OK button.
         * This method handles alert, normal DOM OK, image OK, frame OK, and bottom-center fallback.
         */
        clickIngeniumBottomOkAfterLogin();

        waitQuietly(10000);
        waitForDocumentReady();
        takeScreenshot("04-after-login-ok-and-wait");
        printPageDiagnostics("after login ok and wait");
        printPageDiagnosticsDeep("after login ok and wait");
        printAllLinks("after login ok and wait");

        clickPolicyInquiry();

        waitForDocumentReady();
        waitQuietly(3000);
        takeScreenshot("05-policy-inquiry-clicked");
        printPageDiagnostics("after policy inquiry click");
        printAllLinks("after policy inquiry click");

        clickPolicyInquiryAllDetails();

        waitForDocumentReady();
        waitQuietly(3000);
        takeScreenshot("06-policy-inquiry-all-details-opened");
        printPageDiagnostics("after policy inquiry all details");
        printAllLinks("after policy inquiry all details");

        enterPolicyIdAndSubmit();

        waitForDocumentReady();
        waitQuietly(5000);
        takeScreenshot("07-policy-id-submitted");
        printPageDiagnostics("after policy id submitted");

        boolean policyVisible = waitUntilTextAppearsInDefaultOrFrames(policyId, 45);

        takeScreenshot("08-policy-result");

        Assert.assertTrue(policyVisible, "Policy ID was not visible after inquiry: " + policyId);
    }

    private void clickEnglishSignOnIfNeeded() {
        if (loginFieldsExist()) {
            System.out.println("Login fields already visible. English Sign On click is not required.");
            return;
        }

        System.out.println("Trying to click English Sign On.");

        clickFirstAvailable("English Sign On", new By[]{
                By.linkText("English Sign On"),
                By.partialLinkText("English"),
                By.partialLinkText("Sign On"),
                By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'english sign on')]"),
                By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'english')]"),
                By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign on')]"),
                By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'english sign on')]"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'english sign on')]")
        });

        waitForDocumentReady();
        waitQuietly(5000);
        switchToLatestWindow();

        try {
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(webDriver -> {
                webDriver.switchTo().defaultContent();
                int frames = webDriver.findElements(By.cssSelector("frame, iframe")).size();
                int inputs = webDriver.findElements(By.cssSelector("input")).size();
                return frames > 0 || inputs > 0;
            });
        } catch (Exception e) {
            System.out.println("Login page readiness wait did not detect frames/inputs: " + e.getMessage());
        }

        printPageDiagnosticsDeep("after English Sign On readiness wait");
    }

    private boolean loginFieldsExist() {
        return elementExistsInDefaultOrFrames(new By[]{
                By.name("name"),
                By.id("name"),
                By.name("Name"),
                By.id("Name"),
                By.name("username"),
                By.id("username"),
                By.cssSelector("input[type='password']")
        });
    }

    private void enterLoginDetailsAndSubmit() throws Exception {
        System.out.println("Entering login details using standard locators first.");

        boolean standardLoginWorked = tryStandardLoginFields();

        if (!standardLoginWorked) {
            System.out.println("Standard login locators failed. Trying legacy login form fallback.");
            fillLegacyLoginFormByFieldTypeAndPosition();
        }

        takeScreenshot("02b-login-details-entered");

        if (!clickLoginSubmitIfAvailable()) {
            System.out.println("Submit element not found. Trying ENTER key fallback from password field.");
            pressEnterOnPasswordField();
        }

        waitForDocumentReady();
        waitQuietly(3000);
        switchToLatestWindow();

        System.out.println("Login submit action completed.");
    }

    private boolean tryStandardLoginFields() {
        try {
            typeFirstAvailable("name", new By[]{
                    By.name("name"),
                    By.id("name"),
                    By.name("Name"),
                    By.id("Name"),
                    By.name("NAME"),
                    By.id("NAME"),
                    By.name("username"),
                    By.id("username"),
                    By.name("user"),
                    By.id("user"),
                    By.name("USER"),
                    By.id("USER"),
                    By.cssSelector("input[name*='name' i]"),
                    By.cssSelector("input[id*='name' i]"),
                    By.cssSelector("input[name*='user' i]"),
                    By.cssSelector("input[id*='user' i]"),
                    By.cssSelector("input[type='text']")
            }, username);

            typeFirstAvailable("password", new By[]{
                    By.name("password"),
                    By.id("password"),
                    By.name("Password"),
                    By.id("Password"),
                    By.name("PASSWORD"),
                    By.id("PASSWORD"),
                    By.cssSelector("input[type='password']"),
                    By.cssSelector("input[name*='password' i]"),
                    By.cssSelector("input[id*='password' i]")
            }, password);

            typeIfAvailable("company", new By[]{
                    By.name("company"),
                    By.id("company"),
                    By.name("Company"),
                    By.id("Company"),
                    By.name("COMPANY"),
                    By.id("COMPANY"),
                    By.cssSelector("input[name*='company' i]"),
                    By.cssSelector("input[id*='company' i]"),
                    By.cssSelector("select[name*='company' i]"),
                    By.cssSelector("select[id*='company' i]"),
                    By.cssSelector("select")
            }, company);

            return true;
        } catch (Exception e) {
            System.out.println("Standard login field strategy failed: " + e.getMessage());
            takeScreenshotQuietly("standard-login-fields-failed");
            printPageDiagnosticsDeep("standard login failed");
            return false;
        }
    }

    private void fillLegacyLoginFormByFieldTypeAndPosition() {
        WebElement nameInput = findFirstVisibleElementDeep(new By[]{
                By.cssSelector("input[type='text']"),
                By.cssSelector("input:not([type])"),
                By.cssSelector("input[type='']"),
                By.cssSelector("input")
        }, "legacy name text input", true, true, false);

        WebElement passwordInput = findFirstVisibleElementDeep(new By[]{
                By.cssSelector("input[type='password']")
        }, "legacy password input", true, true, false);

        if (nameInput == null) {
            throw new RuntimeException("Unable to find legacy Name input on login page.");
        }

        if (passwordInput == null) {
            throw new RuntimeException("Unable to find legacy Password input on login page.");
        }

        clearAndType(nameInput, username);
        System.out.println("Entered username into legacy Name field.");

        clearAndType(passwordInput, password);
        System.out.println("Entered password into legacy Password field.");

        WebElement companySelect = findFirstVisibleElementDeep(new By[]{
                By.cssSelector("select"),
                By.cssSelector("select[name*='company' i]"),
                By.cssSelector("select[id*='company' i]")
        }, "legacy company select", true, false, true);

        if (companySelect != null) {
            selectCompanyValue(companySelect, company);
        } else {
            System.out.println("No company select found. Assuming Company is already defaulted to Manulife.");
        }
    }

    private boolean clickLoginSubmitIfAvailable() {
        WebElement submit = findFirstVisibleElementDeep(new By[]{
                By.cssSelector("input[type='submit']"),
                By.cssSelector("input[type='image']"),
                By.cssSelector("button[type='submit']"),
                By.cssSelector("button"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                By.xpath("//input[contains(translate(@alt,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                By.xpath("//img[contains(translate(@alt,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                By.xpath("//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]")
        }, "legacy submit", true, false, false);

        if (submit == null) {
            return false;
        }

        try {
            clickElementStrong(submit, "login submit");
            System.out.println("Clicked login submit.");
            return true;
        } catch (Exception e) {
            System.out.println("Login submit click failed: " + e.getMessage());
            return false;
        }
    }

    private void pressEnterOnPasswordField() {
        WebElement passwordInput = findFirstVisibleElementDeep(new By[]{
                By.cssSelector("input[type='password']")
        }, "password enter fallback", true, true, false);

        if (passwordInput == null) {
            throw new RuntimeException("Unable to find password field for ENTER fallback.");
        }

        passwordInput.sendKeys(Keys.ENTER);
        System.out.println("Pressed ENTER on password field.");
    }

    private void clickIngeniumBottomOkAfterLogin() {
        System.out.println("===== POST LOGIN OK: START =====");

        waitForDocumentReady();
        waitQuietly(5000);
        switchToLatestWindow();

        takeScreenshotQuietly("03a-before-post-login-ok");

        if (acceptAlertIfPresent("post-login OK")) {
            waitQuietly(3000);
            takeScreenshotQuietly("03b-after-post-login-alert-ok");
            System.out.println("===== POST LOGIN OK: browser alert accepted =====");
            return;
        }

        try {
            WebElement ok = findIngeniumOkButtonDeep(35);

            if (ok != null) {
                clickElementStrong(ok, "post-login Ingenium OK");
                waitQuietly(5000);
                waitForDocumentReady();
                switchToLatestWindow();
                takeScreenshotQuietly("03b-after-post-login-ok-click");
                System.out.println("===== POST LOGIN OK: clicked using DOM/frame search =====");
                return;
            }
        } catch (Exception e) {
            System.out.println("POST LOGIN OK: DOM/frame search failed: " + e.getMessage());
        }

        System.out.println("POST LOGIN OK: trying bottom-center coordinate fallback.");

        if (clickBottomCenterInDefaultOrFrames()) {
            waitQuietly(5000);
            waitForDocumentReady();
            switchToLatestWindow();
            takeScreenshotQuietly("03b-after-post-login-bottom-center-click");
            System.out.println("===== POST LOGIN OK: clicked using bottom-center fallback =====");
            return;
        }

        takeScreenshotQuietly("03x-post-login-ok-failed");
        printPageDiagnostics("post-login OK failed");
        printPageDiagnosticsDeep("post-login OK failed");
        printAllInputs("post-login OK failed");
        printAllLinks("post-login OK failed");

        throw new RuntimeException("Unable to click post-login Ingenium OK button.");
    }

    private WebElement findIngeniumOkButtonDeep(int timeoutSeconds) {
        WebDriverWait okWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

        return okWait.until(webDriver -> {
            try {
                webDriver.switchTo().defaultContent();

                By[] okLocators = new By[]{
                        By.id("ok"),
                        By.id("OK"),
                        By.name("ok"),
                        By.name("OK"),

                        By.xpath("//input[translate(normalize-space(@value),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"),
                        By.xpath("//input[contains(translate(@value,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),

                        By.xpath("//button[translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"),
                        By.xpath("//button[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),

                        By.xpath("//a[translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"),
                        By.xpath("//a[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),

                        By.xpath("//input[@type='image']"),
                        By.xpath("//input[contains(translate(@alt,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                        By.xpath("//input[contains(translate(@title,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                        By.xpath("//input[contains(translate(@src,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),

                        By.xpath("//img[contains(translate(@alt,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                        By.xpath("//img[contains(translate(@title,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                        By.xpath("//img[contains(translate(@src,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),

                        By.xpath("//area[contains(translate(@alt,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                        By.xpath("//area[contains(translate(@title,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                        By.xpath("//area[contains(translate(@href,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),

                        By.xpath("//*[contains(translate(@onclick,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                        By.xpath("//*[contains(translate(@onclick,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'SUBMIT')]")
                };

                for (By locator : okLocators) {
                    WebElement element = findClickableElementInCurrentContextOrFrames(locator, 0);

                    if (element != null) {
                        System.out.println("POST LOGIN OK: found using locator: " + locator);
                        return element;
                    }
                }

                return null;
            } catch (Exception e) {
                System.out.println("POST LOGIN OK: search retry due to: " + e.getMessage());
                return null;
            }
        });
    }

    private WebElement findClickableElementInCurrentContextOrFrames(By locator, int depth) {
        if (depth > 12) {
            return null;
        }

        WebElement element = findClickableElementInCurrentContext(locator);
        if (element != null) {
            return element;
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));
        System.out.println("POST LOGIN OK: frame count at depth " + depth + " = " + frames.size());

        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);

                WebElement frameElement = findClickableElementInCurrentContextOrFrames(locator, depth + 1);

                if (frameElement != null) {
                    System.out.println("POST LOGIN OK: element found in frame index " + i + " at depth " + depth);
                    return frameElement;
                }

                driver.switchTo().parentFrame();
            } catch (NoSuchFrameException | StaleElementReferenceException ignored) {
                driver.switchTo().defaultContent();
            } catch (Exception e) {
                System.out.println("POST LOGIN OK: frame search issue at depth " + depth + ", frame " + i + ": " + e.getMessage());
                driver.switchTo().defaultContent();
            }
        }

        if (depth == 0) {
            driver.switchTo().defaultContent();
        }

        return null;
    }

    private WebElement findClickableElementInCurrentContext(By locator) {
        List<WebElement> elements = driver.findElements(locator);

        for (WebElement element : elements) {
            try {
                if (!element.isDisplayed() || !element.isEnabled()) {
                    continue;
                }

                System.out.println(
                        "POST LOGIN OK candidate: tag=[" + element.getTagName() + "]" +
                                ", text=[" + safeText(element) + "]" +
                                ", type=[" + safeAttribute(element, "type") + "]" +
                                ", name=[" + safeAttribute(element, "name") + "]" +
                                ", id=[" + safeAttribute(element, "id") + "]" +
                                ", value=[" + safeAttribute(element, "value") + "]" +
                                ", alt=[" + safeAttribute(element, "alt") + "]" +
                                ", title=[" + safeAttribute(element, "title") + "]" +
                                ", src=[" + safeAttribute(element, "src") + "]" +
                                ", onclick=[" + safeAttribute(element, "onclick") + "]"
                );

                return element;
            } catch (StaleElementReferenceException ignored) {
                // Try next candidate
            }
        }

        return null;
    }

    private boolean clickBottomCenterInDefaultOrFrames() {
        try {
            driver.switchTo().defaultContent();
            return Boolean.TRUE.equals(clickBottomCenterRecursive(0));
        } catch (Exception e) {
            System.out.println("POST LOGIN OK: bottom-center fallback failed: " + e.getMessage());
            return false;
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignored) {
                // Ignore
            }
        }
    }

    private Boolean clickBottomCenterRecursive(int depth) {
        if (depth > 12) {
            return false;
        }

        try {
            Object clicked = ((JavascriptExecutor) driver).executeScript(
                    "var points = [" +
                            "  [Math.floor(window.innerWidth/2), Math.max(0, window.innerHeight-15)]," +
                            "  [Math.floor(window.innerWidth/2), Math.max(0, window.innerHeight-25)]," +
                            "  [Math.floor(window.innerWidth/2), Math.max(0, window.innerHeight-35)]," +
                            "  [Math.floor(window.innerWidth/2), Math.max(0, window.innerHeight-45)]" +
                            "];" +
                            "for (var i = 0; i < points.length; i++) {" +
                            "  var x = points[i][0];" +
                            "  var y = points[i][1];" +
                            "  var el = document.elementFromPoint(x, y);" +
                            "  if (el) {" +
                            "    el.click();" +
                            "    return true;" +
                            "  }" +
                            "}" +
                            "return false;"
            );

            if (Boolean.TRUE.equals(clicked)) {
                System.out.println("POST LOGIN OK: clicked bottom-center element at frame depth " + depth);
                return true;
            }
        } catch (Exception e) {
            System.out.println("POST LOGIN OK: bottom-center click failed at depth " + depth + ": " + e.getMessage());
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));

        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);

                Boolean clicked = clickBottomCenterRecursive(depth + 1);

                if (Boolean.TRUE.equals(clicked)) {
                    return true;
                }

                driver.switchTo().parentFrame();
            } catch (Exception e) {
                System.out.println("POST LOGIN OK: bottom-center frame issue depth " + depth + ", frame " + i + ": " + e.getMessage());
                driver.switchTo().defaultContent();
            }
        }

        if (depth == 0) {
            driver.switchTo().defaultContent();
        }

        return false;
    }

    private void clickPolicyInquiry() {
        System.out.println("Clicking left side Policy Inquiry menu.");

        clickFirstAvailable("Policy Inquiry", new By[]{
                By.linkText("Policy Inquiry"),
                By.partialLinkText("Policy Inquiry"),
                By.partialLinkText("Policy"),
                By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry')]"),
                By.xpath("//span[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry')]"),
                By.xpath("//div[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry')]"),
                By.xpath("//td[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry')]"),
                By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry')]"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry')]")
        });
    }

    private void clickPolicyInquiryAllDetails() {
        System.out.println("Clicking Policy Inquiry - All Details.");

        clickFirstAvailable("Policy Inquiry All Details", new By[]{
                By.linkText("Policy Inquiry - All Details"),
                By.linkText("Policy Inquiry – All Details"),
                By.partialLinkText("All Details"),
                By.partialLinkText("Policy Inquiry"),
                By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'all details')]"),
                By.xpath("//span[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'all details')]"),
                By.xpath("//div[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'all details')]"),
                By.xpath("//td[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'all details')]"),
                By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'all details')]"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'all details')]")
        });
    }

    private void enterPolicyIdAndSubmit() throws Exception {
        System.out.println("Entering policy id: " + policyId);

        typeFirstAvailable("policy id", new By[]{
                By.name("policyId"),
                By.id("policyId"),
                By.name("policyNumber"),
                By.id("policyNumber"),
                By.name("policy"),
                By.id("policy"),
                By.name("POL_ID"),
                By.id("POL_ID"),
                By.name("polId"),
                By.id("polId"),
                By.name("Policy"),
                By.id("Policy"),
                By.cssSelector("input[name*='policy' i]"),
                By.cssSelector("input[id*='policy' i]"),
                By.cssSelector("input[name*='pol' i]"),
                By.cssSelector("input[id*='pol' i]"),
                By.cssSelector("input[type='text']")
        }, policyId);

        takeScreenshot("06b-policy-id-entered");

        clickOkIfPresent("before policy submit");

        if (!waitUntilTextAppearsInDefaultOrFrames(policyId, 5)) {
            clickFirstAvailable("OK / policy submit", new By[]{
                    By.id("ok"),
                    By.name("ok"),
                    By.id("OK"),
                    By.name("OK"),
                    By.id("submit"),
                    By.name("submit"),
                    By.cssSelector("button[type='submit']"),
                    By.cssSelector("input[type='submit']"),
                    By.cssSelector("input[type='button']"),
                    By.cssSelector("input[type='image']"),
                    By.xpath("//button[translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"),
                    By.xpath("//input[translate(@value,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"),
                    By.xpath("//button[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                    By.xpath("//input[contains(translate(@value,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                    By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                    By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                    By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]"),
                    By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]")
            });
        }
    }

    private void clickOkIfPresent(String label) {
        System.out.println("Checking OK confirmation: " + label);

        if (acceptAlertIfPresent(label)) {
            return;
        }

        try {
            clickFirstAvailableShort("OK button " + label, new By[]{
                    By.id("ok"),
                    By.name("ok"),
                    By.id("OK"),
                    By.name("OK"),
                    By.cssSelector("input[type='image']"),
                    By.cssSelector("input[alt*='ok' i]"),
                    By.cssSelector("input[title*='ok' i]"),
                    By.cssSelector("img[alt*='ok' i]"),
                    By.cssSelector("img[title*='ok' i]"),
                    By.cssSelector("img[src*='ok' i]"),
                    By.xpath("//button[translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"),
                    By.xpath("//input[translate(@value,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"),
                    By.xpath("//button[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                    By.xpath("//input[contains(translate(@value,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                    By.xpath("//a[translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']")
            });
        } catch (Exception e) {
            System.out.println("No OK button found for: " + label + ". Continuing.");
        }
    }

    private boolean acceptAlertIfPresent(String label) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            Alert alert = shortWait.until(ExpectedConditions.alertIsPresent());

            System.out.println("Alert found for " + label + ". Text: " + alert.getText());
            alert.accept();
            System.out.println("Alert accepted for " + label);

            return true;
        } catch (TimeoutException | NoAlertPresentException e) {
            System.out.println("No browser alert present for " + label);
            return false;
        }
    }

    private void typeFirstAvailable(String fieldName, By[] locators, String value) {
        WebElement element = findFirstAvailableElement(fieldName, locators, true, 45);

        clearAndType(element, value);

        System.out.println("Entered value for field: " + fieldName);
    }

    private void typeIfAvailable(String fieldName, By[] locators, String value) {
        try {
            WebElement element = findFirstAvailableElement(fieldName, locators, true, 8);

            clearAndType(element, value);

            System.out.println("Entered optional value for field: " + fieldName);
        } catch (Exception e) {
            System.out.println("Optional field not found: " + fieldName + ". Continuing.");
        }
    }

    private void clickFirstAvailable(String elementName, By[] locators) {
        WebElement element = findFirstAvailableElement(elementName, locators, false, 45);

        clickElementStrong(element, elementName);

        waitForDocumentReady();
        waitQuietly(2000);
        switchToLatestWindow();

        System.out.println("Clicked element: " + elementName);
    }

    private void clickFirstAvailableShort(String elementName, By[] locators) {
        WebElement element = findFirstAvailableElement(elementName, locators, false, 8);

        clickElementStrong(element, elementName);

        waitForDocumentReady();
        waitQuietly(1000);
        switchToLatestWindow();

        System.out.println("Clicked element: " + elementName);
    }

    private void clickElementStrong(WebElement element, String label) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                    element
            );
            waitQuietly(500);
        } catch (Exception ignored) {
            // Continue
        }

        try {
            element.click();
            System.out.println("Clicked " + label + " using Selenium click.");
            return;
        } catch (Exception e) {
            System.out.println("Selenium click failed for " + label + ": " + e.getMessage());
        }

        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            System.out.println("Clicked " + label + " using JavaScript click.");
            return;
        } catch (Exception e) {
            System.out.println("JavaScript click failed for " + label + ": " + e.getMessage());
        }

        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var el = arguments[0];" +
                            "['mouseover','mousedown','mouseup','click'].forEach(function(type) {" +
                            "  var evt = document.createEvent('MouseEvents');" +
                            "  evt.initMouseEvent(type, true, true, window, 1, 0, 0, 0, 0, false, false, false, false, 0, null);" +
                            "  el.dispatchEvent(evt);" +
                            "});",
                    element
            );
            System.out.println("Clicked " + label + " using dispatched mouse events.");
        } catch (Exception e) {
            throw new RuntimeException("All click strategies failed for " + label + ": " + e.getMessage(), e);
        }
    }

    private boolean elementExistsInDefaultOrFrames(By[] locators) {
        for (By locator : locators) {
            try {
                driver.switchTo().defaultContent();

                WebElement element = findElementInCurrentContextOrFrames(locator, true, 0);

                if (element != null) {
                    System.out.println("Element exists using locator: " + locator);
                    return true;
                }
            } catch (Exception ignored) {
                // Try next locator
            }
        }

        return false;
    }

    private WebElement findFirstAvailableElement(String elementName, By[] locators, boolean requireVisible, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

        for (By locator : locators) {
            try {
                WebElement found = customWait.until((ExpectedCondition<WebElement>) webDriver -> {
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
        printPageDiagnosticsDeep("element not found: " + elementName);
        printAllLinks("element not found: " + elementName);
        printAllInputs("element not found: " + elementName);

        throw new RuntimeException("Unable to find element: " + elementName);
    }

    private WebElement findElementInCurrentContextOrFrames(By locator, boolean requireVisible, int depth) {
        if (depth > 8) {
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

    private WebElement findFirstVisibleElementDeep(
            By[] locators,
            String elementName,
            boolean switchToElementFrame,
            boolean inputOnly,
            boolean selectOnly
    ) {
        driver.switchTo().defaultContent();

        for (By locator : locators) {
            WebElement found = findFirstVisibleElementInContextOrFrames(locator, 0, switchToElementFrame, inputOnly, selectOnly);

            if (found != null) {
                System.out.println("Found " + elementName + " using locator: " + locator);
                return found;
            }

            driver.switchTo().defaultContent();
        }

        System.out.println("Unable to find " + elementName + " using deep frame search.");
        printPageDiagnosticsDeep("unable to find " + elementName);
        return null;
    }

    private WebElement findFirstVisibleElementInContextOrFrames(
            By locator,
            int depth,
            boolean stayInFoundFrame,
            boolean inputOnly,
            boolean selectOnly
    ) {
        if (depth > 10) {
            return null;
        }

        List<WebElement> elements = driver.findElements(locator);

        for (WebElement element : elements) {
            try {
                if (!element.isDisplayed() || !element.isEnabled()) {
                    continue;
                }

                String tag = element.getTagName();

                if (inputOnly && !"input".equalsIgnoreCase(tag)) {
                    continue;
                }

                if (selectOnly && !"select".equalsIgnoreCase(tag)) {
                    continue;
                }

                return element;
            } catch (StaleElementReferenceException ignored) {
                // Try next element
            }
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));
        System.out.println("Deep search frame count at depth " + depth + ": " + frames.size());

        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);

                WebElement found = findFirstVisibleElementInContextOrFrames(
                        locator,
                        depth + 1,
                        stayInFoundFrame,
                        inputOnly,
                        selectOnly
                );

                if (found != null) {
                    return found;
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

    private void clearAndType(WebElement element, String value) {
        try {
            element.clear();
        } catch (Exception e) {
            System.out.println("Clear failed, continuing with typing: " + e.getMessage());
        }

        element.sendKeys(value);
    }

    private void selectCompanyValue(WebElement selectElement, String companyValue) {
        try {
            Select select = new Select(selectElement);

            try {
                select.selectByVisibleText(companyValue);
                System.out.println("Selected company by visible text: " + companyValue);
                return;
            } catch (Exception ignored) {
                // Try value
            }

            try {
                select.selectByValue(companyValue);
                System.out.println("Selected company by value: " + companyValue);
                return;
            } catch (Exception ignored) {
                // Try first option
            }

            if (!select.getOptions().isEmpty()) {
                select.selectByIndex(0);
                System.out.println("Selected first company option as fallback.");
            }
        } catch (Exception e) {
            System.out.println("Unable to select company. Assuming default is correct. Error: " + e.getMessage());
        }
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
        if (depth > 8) {
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

    private void switchToLatestWindow() {
        try {
            String latestHandle = null;

            for (String handle : driver.getWindowHandles()) {
                latestHandle = handle;
            }

            if (latestHandle != null) {
                driver.switchTo().window(latestHandle);
                System.out.println("Switched to latest window. URL: " + driver.getCurrentUrl());
            }
        } catch (Exception e) {
            System.out.println("Unable to switch to latest window: " + e.getMessage());
        }
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

    private void printPageDiagnosticsDeep(String label) {
        try {
            driver.switchTo().defaultContent();

            System.out.println("===== DEEP PAGE DIAGNOSTICS: " + label + " =====");
            printCurrentContextDiagnostics("default content", 0);

            inspectFramesRecursive(0, "default");
            driver.switchTo().defaultContent();
        } catch (Exception e) {
            System.out.println("Unable to print deep diagnostics: " + e.getMessage());
        }
    }

    private void inspectFramesRecursive(int depth, String path) {
        if (depth > 10) {
            return;
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));

        System.out.println("Frame path [" + path + "] depth [" + depth + "] frame count: " + frames.size());

        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);

                String framePath = path + " > frame[" + i + "]";
                printCurrentContextDiagnostics(framePath, depth + 1);

                inspectFramesRecursive(depth + 1, framePath);

                driver.switchTo().parentFrame();
            } catch (Exception e) {
                System.out.println("Unable to inspect frame path " + path + " frame[" + i + "]: " + e.getMessage());
                driver.switchTo().defaultContent();
            }
        }
    }

    private void printCurrentContextDiagnostics(String contextName, int depth) {
        try {
            List<WebElement> inputs = driver.findElements(By.cssSelector("input"));
            List<WebElement> selects = driver.findElements(By.cssSelector("select"));
            List<WebElement> buttons = driver.findElements(By.cssSelector("button"));
            List<WebElement> links = driver.findElements(By.cssSelector("a"));

            System.out.println("----- Context: " + contextName + ", depth: " + depth + " -----");
            System.out.println("URL: " + driver.getCurrentUrl());
            System.out.println("Title: " + driver.getTitle());
            System.out.println("Inputs: " + inputs.size());
            System.out.println("Selects: " + selects.size());
            System.out.println("Buttons: " + buttons.size());
            System.out.println("Links: " + links.size());
            System.out.println("Source length: " + driver.getPageSource().length());

            for (int i = 0; i < inputs.size(); i++) {
                WebElement input = inputs.get(i);
                System.out.println(
                        "Input[" + i + "]" +
                                " type=[" + safeAttribute(input, "type") + "]" +
                                " name=[" + safeAttribute(input, "name") + "]" +
                                " id=[" + safeAttribute(input, "id") + "]" +
                                " value=[" + safeAttribute(input, "value") + "]" +
                                " alt=[" + safeAttribute(input, "alt") + "]" +
                                " displayed=[" + safeIsDisplayed(input) + "]" +
                                " enabled=[" + safeIsEnabled(input) + "]"
                );
            }

            for (int i = 0; i < selects.size(); i++) {
                WebElement select = selects.get(i);
                System.out.println(
                        "Select[" + i + "]" +
                                " name=[" + safeAttribute(select, "name") + "]" +
                                " id=[" + safeAttribute(select, "id") + "]" +
                                " displayed=[" + safeIsDisplayed(select) + "]" +
                                " enabled=[" + safeIsEnabled(select) + "]"
                );
            }

            for (int i = 0; i < buttons.size(); i++) {
                WebElement button = buttons.get(i);
                System.out.println(
                        "Button[" + i + "]" +
                                " text=[" + safeText(button) + "]" +
                                " type=[" + safeAttribute(button, "type") + "]" +
                                " name=[" + safeAttribute(button, "name") + "]" +
                                " id=[" + safeAttribute(button, "id") + "]" +
                                " displayed=[" + safeIsDisplayed(button) + "]" +
                                " enabled=[" + safeIsEnabled(button) + "]"
                );
            }
        } catch (Exception e) {
            System.out.println("Unable to print context diagnostics for " + contextName + ": " + e.getMessage());
        }
    }

    private void printAllLinks(String label) {
        try {
            driver.switchTo().defaultContent();

            List<WebElement> links = driver.findElements(By.cssSelector("a"));

            System.out.println("===== LINK DIAGNOSTICS: " + label + " =====");
            System.out.println("Link count: " + links.size());

            for (int i = 0; i < links.size(); i++) {
                WebElement link = links.get(i);

                System.out.println(
                        "Link[" + i + "] text=[" + safeText(link) + "]" +
                                ", href=[" + safeAttribute(link, "href") + "]" +
                                ", target=[" + safeAttribute(link, "target") + "]" +
                                ", onclick=[" + safeAttribute(link, "onclick") + "]"
                );
            }
        } catch (Exception e) {
            System.out.println("Unable to print link diagnostics: " + e.getMessage());
        }
    }

    private void printAllInputs(String label) {
        try {
            driver.switchTo().defaultContent();

            List<WebElement> inputs = driver.findElements(By.cssSelector("input, select, textarea, button"));

            System.out.println("===== INPUT/BUTTON DIAGNOSTICS: " + label + " =====");
            System.out.println("Input/Button count: " + inputs.size());

            for (int i = 0; i < inputs.size(); i++) {
                WebElement input = inputs.get(i);

                System.out.println(
                        "Element[" + i + "] tag=[" + input.getTagName() + "]" +
                                ", type=[" + safeAttribute(input, "type") + "]" +
                                ", name=[" + safeAttribute(input, "name") + "]" +
                                ", id=[" + safeAttribute(input, "id") + "]" +
                                ", value=[" + safeAttribute(input, "value") + "]" +
                                ", text=[" + safeText(input) + "]"
                );
            }
        } catch (Exception e) {
            System.out.println("Unable to print input diagnostics: " + e.getMessage());
        }
    }

    private boolean safeIsDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean safeIsEnabled(WebElement element) {
        try {
            return element.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private String safeText(WebElement element) {
        try {
            String text = element.getText();
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String safeAttribute(WebElement element, String attributeName) {
        try {
            String value = element.getAttribute(attributeName);
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            return "";
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

    private void waitQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
