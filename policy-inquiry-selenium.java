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

        if (System.getProperty("webdriver.chrome.driver") == null
                || System.getProperty("webdriver.chrome.driver").trim().isEmpty()) {
            WebDriverManager.chromedriver().setup();
        } else {
            System.out.println("Using provided ChromeDriver: " + System.getProperty("webdriver.chrome.driver"));
        }

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
        wait = new WebDriverWait(driver, Duration.ofSeconds(25));
    }

    @Test
    public void policyInquiryTest() throws Exception {
        System.out.println("Opening application URL: " + appUrl);
        driver.get(appUrl);

        waitForDocumentReady();
        takeScreenshot("01-app-opened");
        printPageDiagnostics("after app open");

        clickEnglishSignOnIfNeeded();
        waitForDocumentReady();
        waitQuietly(800);
        switchToLatestWindow();
        takeScreenshot("02-english-sign-on-opened");

        enterLoginDetailsAndSubmit();
        waitForDocumentReady();
        waitQuietly(800);
        takeScreenshot("03-login-submitted");

        clickAnyOk("post-login OK", true);
        waitQuietly(1000);
        waitForDocumentReady();
        takeScreenshot("04-after-login-ok-and-wait");

        clickPolicyInquiry();
        waitForDocumentReady();
        waitQuietly(600);
        takeScreenshot("05-policy-inquiry-clicked");

        clickPolicyInquiryAllDetails();
        waitForDocumentReady();
        waitQuietly(600);
        takeScreenshot("06-policy-inquiry-all-details-opened");

        enterPolicyIdAndSubmit();
        waitForDocumentReady();
        waitQuietly(1000);
        takeScreenshot("08-policy-result");

        boolean policyVisible = waitUntilTextAppearsInDefaultOrFrames(policyId, 12);
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
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'english sign on')]")
        }, 15);

        waitForDocumentReady();
        waitQuietly(800);
        switchToLatestWindow();
    }

    private boolean loginFieldsExist() {
        return elementExistsInDefaultOrFrames(new By[]{
                By.cssSelector("input[type='password']"),
                By.cssSelector("input[name*='user' i]"),
                By.cssSelector("input[id*='user' i]"),
                By.name("name"),
                By.id("name")
        });
    }

    private void enterLoginDetailsAndSubmit() throws Exception {
        System.out.println("Entering login details.");

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
        waitQuietly(800);
        switchToLatestWindow();
        System.out.println("Login submit action completed.");
    }

    private boolean tryStandardLoginFields() {
        try {
            typeFirstAvailable("name", new By[]{
                    By.cssSelector("input[name*='user' i]"),
                    By.cssSelector("input[id*='user' i]"),
                    By.cssSelector("input[name*='name' i]"),
                    By.cssSelector("input[id*='name' i]"),
                    By.cssSelector("input[type='text']"),
                    By.name("name"), By.id("name"), By.name("Name"), By.id("Name"),
                    By.name("username"), By.id("username"), By.name("user"), By.id("user")
            }, username, 12);

            typeFirstAvailable("password", new By[]{
                    By.cssSelector("input[type='password']"),
                    By.cssSelector("input[name*='password' i]"),
                    By.cssSelector("input[id*='password' i]"),
                    By.name("password"), By.id("password"), By.name("Password"), By.id("Password")
            }, password, 8);

            typeIfAvailable("company", new By[]{
                    By.cssSelector("select[name*='company' i]"),
                    By.cssSelector("select[id*='company' i]"),
                    By.cssSelector("select"),
                    By.cssSelector("input[name*='company' i]"),
                    By.cssSelector("input[id*='company' i]"),
                    By.name("company"), By.id("company"), By.name("Company"), By.id("Company")
            }, company, 4);

            return true;
        } catch (Exception e) {
            System.out.println("Standard login field strategy failed: " + e.getMessage());
            takeScreenshotQuietly("standard-login-fields-failed");
            return false;
        }
    }

    private void fillLegacyLoginFormByFieldTypeAndPosition() {
        WebElement nameInput = findFirstVisibleElementDeep(new By[]{
                By.cssSelector("input[type='text']"),
                By.cssSelector("input:not([type])"),
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
        clearAndType(passwordInput, password);

        WebElement companySelect = findFirstVisibleElementDeep(new By[]{
                By.cssSelector("select[name*='company' i]"),
                By.cssSelector("select[id*='company' i]"),
                By.cssSelector("select")
        }, "legacy company select", true, false, true);

        if (companySelect != null) {
            selectCompanyValue(companySelect, company);
        } else {
            System.out.println("No company select found. Assuming Company is already defaulted.");
        }
    }

    private boolean clickLoginSubmitIfAvailable() {
        WebElement submit = findFirstVisibleElementDeep(new By[]{
                By.cssSelector("input[type='submit']"),
                By.cssSelector("input[type='image']"),
                By.cssSelector("button[type='submit']"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                By.xpath("//input[contains(translate(@alt,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                By.xpath("//img[contains(translate(@alt,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                By.cssSelector("button")
        }, "legacy submit", true, false, false);

        if (submit == null) {
            return false;
        }

        try {
            clickElementStrong(submit, "login submit");
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
    }

    private void clickPolicyInquiry() {
        System.out.println("Clicking left side Policy Inquiry menu.");
        clickFirstAvailable("Policy Inquiry", new By[]{
                By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry')]"),
                By.partialLinkText("Policy Inquiry"),
                By.partialLinkText("Policy"),
                By.xpath("//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry')]")
        }, 12);
    }

    private void clickPolicyInquiryAllDetails() {
        System.out.println("Clicking Policy Inquiry - All Details.");
        clickFirstAvailable("Policy Inquiry All Details", new By[]{
                By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'all details')]"),
                By.partialLinkText("All Details"),
                By.linkText("Policy Inquiry - All Details"),
                By.linkText("Policy Inquiry – All Details"),
                By.xpath("//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'policy inquiry') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'all details')]")
        }, 12);
    }

    private void enterPolicyIdAndSubmit() throws Exception {
        System.out.println("===== FAST POLICY ID ENTRY START =====");
        System.out.println("Entering policy id: " + policyId);

        waitForDocumentReady();
        waitQuietly(500);
        switchToLatestWindow();

        WebElement policyInput = findPolicyIdInputFast(8);
        if (policyInput == null) {
            takeScreenshotQuietly("policy-id-input-not-found");
            printPageDiagnosticsDeep("policy id input not found");
            throw new RuntimeException("Policy ID input was not found on Policy Inquiry - All Details screen.");
        }

        try {
            policyInput.click();
            policyInput.clear();
            policyInput.sendKeys(policyId);
            System.out.println("Policy ID entered using Selenium sendKeys.");
        } catch (Exception e) {
            System.out.println("Selenium typing failed. Trying JavaScript typing. Error: " + e.getMessage());
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].focus();" +
                            "arguments[0].value = arguments[1];" +
                            "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                            "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                    policyInput,
                    policyId
            );
        }

        takeScreenshot("06b-policy-id-entered");
        clickAnyOk("policy inquiry footer OK", true);

        waitForDocumentReady();
        waitQuietly(1000);
        takeScreenshot("07-policy-id-submitted");
        System.out.println("===== FAST POLICY ID ENTRY COMPLETE =====");
    }

    private void clickAnyOk(String label, boolean allowCoordinateFallback) {
        System.out.println("===== OK CLICK START: " + label + " =====");

        if (acceptAlertIfPresent(label)) {
            return;
        }

        WebElement ok = findOkButtonDeep(8);
        if (ok != null) {
            clickElementStrong(ok, label);
            waitQuietly(800);
            System.out.println("OK clicked using DOM/frame search: " + label);
            return;
        }

        if (allowCoordinateFallback) {
            System.out.println("Trying OK coordinate fallback: " + label);
            boolean clicked = label.toLowerCase().contains("policy")
                    ? clickFooterOkByCoordinateInFrames()
                    : clickBottomCenterInDefaultOrFrames();
            if (clicked) {
                waitQuietly(800);
                System.out.println("OK clicked using coordinate fallback: " + label);
                return;
            }
        }

        takeScreenshotQuietly("ok-click-failed-" + sanitizeFileName(label));
        printPageDiagnosticsDeep("OK click failed: " + label);
        throw new RuntimeException("Unable to click OK: " + label);
    }

    private WebElement findOkButtonDeep(int timeoutSeconds) {
        WebDriverWait okWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        try {
            return okWait.until(webDriver -> {
                try {
                    webDriver.switchTo().defaultContent();
                    By[] okLocators = new By[]{
                            By.id("OKButton"), By.name("OKButton"), By.id("ok"), By.id("OK"), By.name("ok"), By.name("OK"),
                            By.xpath("//input[translate(normalize-space(@value),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"),
                            By.xpath("//button[translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"),
                            By.xpath("//a[translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"),
                            By.xpath("//input[contains(translate(@alt,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                            By.xpath("//input[contains(translate(@title,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                            By.xpath("//input[contains(translate(@src,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                            By.xpath("//input[@type='image' and (contains(translate(@alt,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK') or contains(translate(@name,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK'))]"),
                            By.xpath("//img[contains(translate(@alt,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                            By.xpath("//img[contains(translate(@title,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                            By.xpath("//area[contains(translate(@alt,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]"),
                            By.xpath("//*[contains(translate(@onclick,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'OK')]")
                    };

                    for (By locator : okLocators) {
                        WebElement element = findClickableElementInCurrentContextOrFrames(locator, 0);
                        if (element != null) {
                            System.out.println("OK found using locator: " + locator);
                            return element;
                        }
                    }
                    return null;
                } catch (Exception e) {
                    return null;
                }
            });
        } catch (TimeoutException e) {
            return null;
        }
    }

    private WebElement findPolicyIdInputFast(int timeoutSeconds) {
        WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        try {
            return fastWait.until(webDriver -> {
                try {
                    webDriver.switchTo().defaultContent();
                    return findPolicyIdInputInCurrentContextOrFrames(0);
                } catch (Exception e) {
                    return null;
                }
            });
        } catch (TimeoutException e) {
            return null;
        }
    }

    private WebElement findPolicyIdInputInCurrentContextOrFrames(int depth) {
        if (depth > 8) {
            return null;
        }

        WebElement found = findPolicyIdInputInCurrentContext();
        if (found != null) {
            return found;
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));
        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);
                WebElement frameFound = findPolicyIdInputInCurrentContextOrFrames(depth + 1);
                if (frameFound != null) {
                    System.out.println("Policy ID input found in frame depth " + depth + ", frame index " + i);
                    return frameFound;
                }
                driver.switchTo().parentFrame();
            } catch (Exception e) {
                driver.switchTo().defaultContent();
            }
        }

        if (depth == 0) {
            driver.switchTo().defaultContent();
        }
        return null;
    }

    private WebElement findPolicyIdInputInCurrentContext() {
        List<WebElement> inputs = driver.findElements(By.cssSelector("input"));
        WebElement firstVisibleTextInput = null;

        for (WebElement input : inputs) {
            try {
                if (!input.isDisplayed() || !input.isEnabled()) {
                    continue;
                }

                String type = safeAttribute(input, "type").toLowerCase();
                String name = safeAttribute(input, "name").toLowerCase();
                String id = safeAttribute(input, "id").toLowerCase();
                String title = safeAttribute(input, "title").toLowerCase();

                if ("hidden".equals(type) || "button".equals(type) || "submit".equals(type) || "image".equals(type)) {
                    continue;
                }

                boolean looksLikeTextInput = type.isEmpty() || "text".equals(type) || "search".equals(type) || "tel".equals(type) || "number".equals(type);
                if (!looksLikeTextInput) {
                    continue;
                }

                if (name.contains("suffix") || id.contains("suffix") || title.contains("suffix")) {
                    continue;
                }

                if (name.contains("policy") || id.contains("policy") || name.contains("pol") || id.contains("pol") || title.contains("policy") || title.contains("pol")) {
                    System.out.println("Found Policy ID input using name/id/title.");
                    return input;
                }

                if (firstVisibleTextInput == null) {
                    firstVisibleTextInput = input;
                }
            } catch (StaleElementReferenceException ignored) {
                // Try next input
            }
        }

        if (firstVisibleTextInput != null) {
            System.out.println("Using first visible text input as Policy ID input.");
            return firstVisibleTextInput;
        }
        return null;
    }

    private WebElement findClickableElementInCurrentContextOrFrames(By locator, int depth) {
        if (depth > 8) {
            return null;
        }

        WebElement element = findClickableElementInCurrentContext(locator);
        if (element != null) {
            return element;
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));
        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);
                WebElement frameElement = findClickableElementInCurrentContextOrFrames(locator, depth + 1);
                if (frameElement != null) {
                    return frameElement;
                }
                driver.switchTo().parentFrame();
            } catch (NoSuchFrameException | StaleElementReferenceException ignored) {
                driver.switchTo().defaultContent();
            } catch (Exception e) {
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
                if (element.isDisplayed() && element.isEnabled()) {
                    return element;
                }
            } catch (StaleElementReferenceException ignored) {
                // Try next candidate
            }
        }
        return null;
    }

    private boolean clickBottomCenterInDefaultOrFrames() {
        try {
            driver.switchTo().defaultContent();
            return Boolean.TRUE.equals(clickCoordinateRecursive(0, false));
        } catch (Exception e) {
            System.out.println("Bottom-center fallback failed: " + e.getMessage());
            return false;
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignored) {
                // Ignore
            }
        }
    }

    private boolean clickFooterOkByCoordinateInFrames() {
        try {
            driver.switchTo().defaultContent();
            return Boolean.TRUE.equals(clickCoordinateRecursive(0, true));
        } catch (Exception e) {
            System.out.println("Footer OK coordinate fallback failed: " + e.getMessage());
            return false;
        } finally {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignored) {
                // Ignore
            }
        }
    }

    private Boolean clickCoordinateRecursive(int depth, boolean footerOk) {
        if (depth > 8) {
            return false;
        }

        try {
            String script;
            if (footerOk) {
                script = "var points = [" +
                        "[Math.floor(window.innerWidth * 0.40), Math.max(0, window.innerHeight - 18)]," +
                        "[Math.floor(window.innerWidth * 0.39), Math.max(0, window.innerHeight - 18)]," +
                        "[Math.floor(window.innerWidth * 0.41), Math.max(0, window.innerHeight - 18)]," +
                        "[Math.floor(window.innerWidth * 0.40), Math.max(0, window.innerHeight - 28)]" +
                        "];" + clickPointScript();
            } else {
                script = "var points = [" +
                        "[Math.floor(window.innerWidth/2), Math.max(0, window.innerHeight-15)]," +
                        "[Math.floor(window.innerWidth/2), Math.max(0, window.innerHeight-25)]," +
                        "[Math.floor(window.innerWidth/2), Math.max(0, window.innerHeight-35)]" +
                        "];" + clickPointScript();
            }

            Object result = ((JavascriptExecutor) driver).executeScript(script);
            if (Boolean.TRUE.equals(result)) {
                System.out.println("Coordinate click succeeded at frame depth " + depth);
                return true;
            }
        } catch (Exception e) {
            System.out.println("Coordinate click failed at depth " + depth + ": " + e.getMessage());
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));
        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);
                Boolean clicked = clickCoordinateRecursive(depth + 1, footerOk);
                if (Boolean.TRUE.equals(clicked)) {
                    return true;
                }
                driver.switchTo().parentFrame();
            } catch (Exception e) {
                driver.switchTo().defaultContent();
            }
        }

        if (depth == 0) {
            driver.switchTo().defaultContent();
        }
        return false;
    }

    private String clickPointScript() {
        return "for (var i = 0; i < points.length; i++) {" +
                "var x = points[i][0]; var y = points[i][1];" +
                "var el = document.elementFromPoint(x, y);" +
                "if (el) { el.click(); return true; }" +
                "}" +
                "return false;";
    }

    private boolean acceptAlertIfPresent(String label) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            Alert alert = shortWait.until(ExpectedConditions.alertIsPresent());
            System.out.println("Alert found for " + label + ". Text: " + alert.getText());
            alert.accept();
            return true;
        } catch (TimeoutException | NoAlertPresentException e) {
            System.out.println("No browser alert present for " + label);
            return false;
        }
    }

    private void typeFirstAvailable(String fieldName, By[] locators, String value, int timeoutSeconds) {
        WebElement element = findFirstAvailableElement(fieldName, locators, true, timeoutSeconds);
        clearAndType(element, value);
        System.out.println("Entered value for field: " + fieldName);
    }

    private void typeIfAvailable(String fieldName, By[] locators, String value, int timeoutSeconds) {
        try {
            WebElement element = findFirstAvailableElement(fieldName, locators, true, timeoutSeconds);
            if ("select".equalsIgnoreCase(element.getTagName())) {
                selectCompanyValue(element, value);
            } else {
                clearAndType(element, value);
            }
            System.out.println("Entered optional value for field: " + fieldName);
        } catch (Exception e) {
            System.out.println("Optional field not found: " + fieldName + ". Continuing.");
        }
    }

    private void clickFirstAvailable(String elementName, By[] locators, int timeoutSeconds) {
        WebElement element = findFirstAvailableElement(elementName, locators, false, timeoutSeconds);
        clickElementStrong(element, elementName);
        waitForDocumentReady();
        waitQuietly(500);
        switchToLatestWindow();
        System.out.println("Clicked element: " + elementName);
    }

    private WebElement findFirstAvailableElement(String elementName, By[] locators, boolean requireVisible, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        final By[] matchedLocator = new By[1];

        try {
            WebElement found = customWait.until((ExpectedCondition<WebElement>) webDriver -> {
                for (By locator : locators) {
                    try {
                        webDriver.switchTo().defaultContent();
                        WebElement element = findElementInCurrentContextOrFrames(locator, requireVisible, 0);
                        if (element == null || !element.isEnabled()) {
                            continue;
                        }
                        matchedLocator[0] = locator;
                        return element;
                    } catch (StaleElementReferenceException ignored) {
                        // Try next locator during same wait cycle
                    } catch (WebDriverException ignored) {
                        // Try next locator during same wait cycle
                    } catch (Exception ignored) {
                        // Try next locator during same wait cycle
                    }
                }
                return null;
            });

            System.out.println("Found " + elementName + " using locator: " + matchedLocator[0]);
            return found;
        } catch (TimeoutException e) {
            System.out.println("Unable to find " + elementName + " within " + timeoutSeconds + " seconds.");
            System.out.println("Tried locators for " + elementName + ":");
            for (By locator : locators) {
                System.out.println(" - " + locator);
            }
            takeScreenshotQuietly("element-not-found-" + sanitizeFileName(elementName));
            printPageDiagnostics("element not found: " + elementName);
            printPageDiagnosticsDeep("element not found: " + elementName);
            printAllLinks("element not found: " + elementName);
            printAllInputs("element not found: " + elementName);
            throw new RuntimeException("Unable to find element: " + elementName);
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

    private WebElement findElementInCurrentContextOrFrames(By locator, boolean requireVisible, int depth) {
        if (depth > 8) {
            return null;
        }

        WebElement element = findElementInCurrentContext(locator, requireVisible);
        if (element != null) {
            return element;
        }

        List<WebElement> frames = driver.findElements(By.cssSelector("frame, iframe"));
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
            } catch (Exception e) {
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

    private WebElement findFirstVisibleElementDeep(By[] locators, String elementName, boolean switchToElementFrame, boolean inputOnly, boolean selectOnly) {
        driver.switchTo().defaultContent();

        for (By locator : locators) {
            WebElement found = findFirstVisibleElementInContextOrFrames(locator, 0, inputOnly, selectOnly);
            if (found != null) {
                System.out.println("Found " + elementName + " using locator: " + locator);
                return found;
            }
            driver.switchTo().defaultContent();
        }

        System.out.println("Unable to find " + elementName + " using deep frame search.");
        return null;
    }

    private WebElement findFirstVisibleElementInContextOrFrames(By locator, int depth, boolean inputOnly, boolean selectOnly) {
        if (depth > 8) {
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
        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);
                WebElement found = findFirstVisibleElementInContextOrFrames(locator, depth + 1, inputOnly, selectOnly);
                if (found != null) {
                    return found;
                }
                driver.switchTo().parentFrame();
            } catch (NoSuchFrameException | StaleElementReferenceException ignored) {
                driver.switchTo().defaultContent();
            } catch (Exception e) {
                driver.switchTo().defaultContent();
            }
        }

        if (depth == 0) {
            driver.switchTo().defaultContent();
        }
        return null;
    }

    private void clickElementStrong(WebElement element, String label) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", element);
            waitQuietly(100);
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
                return;
            } catch (Exception ignored) {
                // Try value
            }
            try {
                select.selectByValue(companyValue);
                return;
            } catch (Exception ignored) {
                // Try first option
            }
            if (!select.getOptions().isEmpty()) {
                select.selectByIndex(0);
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
            } catch (Exception e) {
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
            wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
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
        String lowerLabel = label == null ? "" : label.toLowerCase();
        boolean isFailureDiagnostic = lowerLabel.contains("failed")
                || lowerLabel.contains("not found")
                || lowerLabel.contains("unable")
                || Boolean.parseBoolean(getProperty("debug.diagnostics", "DEBUG_DIAGNOSTICS", "false"));

        if (!isFailureDiagnostic) {
            System.out.println("Deep diagnostics skipped for successful step: " + label);
            return;
        }

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
        if (depth > 8) {
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
                System.out.println("Input[" + i + "] type=[" + safeAttribute(input, "type") + "] name=[" + safeAttribute(input, "name") + "] id=[" + safeAttribute(input, "id") + "] displayed=[" + safeIsDisplayed(input) + "] enabled=[" + safeIsEnabled(input) + "]");
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
                System.out.println("Link[" + i + "] text=[" + safeText(link) + "] href=[" + safeAttribute(link, "href") + "] onclick=[" + safeAttribute(link, "onclick") + "]");
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
                System.out.println("Element[" + i + "] tag=[" + input.getTagName() + "] type=[" + safeAttribute(input, "type") + "] name=[" + safeAttribute(input, "name") + "] id=[" + safeAttribute(input, "id") + "] value=[" + safeAttribute(input, "value") + "] text=[" + safeText(input) + "]");
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
