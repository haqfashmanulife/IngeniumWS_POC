package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

public class PolicyInquiryTest extends BaseTest {

    @Test
    public void testPolicySearch() {

        driver.get(System.getProperty("APP_URL"));

        driver.findElement(By.id("username"))
              .sendKeys(System.getProperty("APP_USERNAME"));

        driver.findElement(By.id("password"))
              .sendKeys(System.getProperty("APP_PASSWORD"));

        driver.findElement(By.id("login")).click();

        driver.findElement(By.id("policyInput"))
              .sendKeys(System.getProperty("POLICY_ID"));

        driver.findElement(By.id("searchBtn")).click();
    }
}
