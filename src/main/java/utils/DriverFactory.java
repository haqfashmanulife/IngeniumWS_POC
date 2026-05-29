package utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class DriverFactory {

    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public static WebDriver getDriver() {
        return driver.get();
    }

    public static void initDriver() {

        ChromeOptions options = new ChromeOptions();

        // ✅ CRITICAL FLAGS (Playwright equivalent stability)
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1920,1080");

        // ✅ AUTOMATIC DRIVER (no mismatch issues)
        WebDriverManager.chromedriver().setup();

        driver.set(new ChromeDriver(options));
    }

    public static void quit() {
        if (driver.get() != null) {
            driver.get().quit();
        }
    }
}
