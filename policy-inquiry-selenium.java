package com.manulife.ingenium.tests;

import io.github.bonigarcia.wdm.WebDriverManager.selenium.By;import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
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
        waitQuietly(3000);
        switchToLatestWindow();
        takeScreenshot("02-english-sign-on-opened");
        printPageDiagnostics("after english sign on");
        printAllLinks("after english sign on");

        enterLoginDetailsAndSubmit();

        waitForDocumentReady();
        waitQuietly(2000);
        takeScreenshot("03-login-submitted");

        clickOkIfPresent("after login submit");

        waitQuietly(10000);
        waitForDocumentReady();
        takeScreenshot("04-after-login-ok-and-wait");
        printPageDiagnostics("after login ok and wait");
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
        System.out.println("Entering login details.");

        typeFirstAvailable("name", new By[]{
                By.name("name"),
                By.id("name"),
                By.name("Name"),
                By.id("Name"),
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

        /*
         * Company is Manulife by default in your flow.
         * If the field exists, we populate it. If it is not present, continue.
         */
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
                By.cssSelector("select[id*='company' i]")
        }, company);

        takeScreenshot("02b-login-details-entered");

        clickFirstAvailable("submit", new By[]{
                By.cssSelector("button[type='submit']"),
                By.cssSelector("input[type='submit']"),
                By.id("submit"),
                By.name("submit"),
                By.id("Submit"),
                By.name("Submit"),
                By.id("SUBMIT"),
                By.name("SUBMIT"),
                By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'submit')]"),
                By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign on')]"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign on')]"),
                By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]"),
                By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]")
        });
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

        element.clear();
        element.sendKeys(value);

        System.out.println("Entered value for field: " + fieldName);
    }

    private void typeIfAvailable(String fieldName, By[] locators, String value) {
        try {
            WebElement element = findFirstAvailableElement(fieldName, locators, true, 8);

            element.clear();
            element.sendKeys(value);

            System.out.println("Entered optional value for field: " + fieldName);
        } catch (Exception e) {
            System.out.println("Optional field not found: " + fieldName + ". Continuing.");
        }
    }

    private void clickFirstAvailable(String elementName, By[] locators) {
        WebElement element = findFirstAvailableElement(elementName, locators, false, 45);

        element.click();
        waitForDocumentReady();
        waitQuietly(2000);
        switchToLatestWindow();

        System.out.println("Clicked element: " + elementName);
    }

    private void clickFirstAvailableShort(String elementName, By[] locators) {
        WebElement element = findFirstAvailableElement(elementName, locators, false, 8);

        element.click();
        waitForDocumentReady();
        waitQuietly(1000);
        switchToLatestWindow();

        System.out.println("Clicked element: " + elementName);
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
import org.openqa.selenium.Alert;
