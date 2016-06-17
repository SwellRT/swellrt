

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Properties;

public class TestWaveJS  {
  public static void main(String[] args) {

    if (System.getProperty("webdriver.chrome.driver") == null)
      System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");

    // Create a new instance of the Chrome driver
    WebDriver driver = new ChromeDriver();


    try {
      (new WebDriverWait(driver, 180)).until(new ExpectedCondition<Boolean>() {
        public Boolean apply(WebDriver d) {
          d.get("http://localhost:9898/test/index.html");

          return d.findElement(By.id("loginForm")).isDisplayed();
        }
      });

      // Find the text input element by its name
      WebElement uid = driver.findElement(By.id("uid"));

      // Enter something to search for
      uid.sendKeys("test@local.net");

      WebElement pwd = driver.findElement(By.id("password"));

      pwd.sendKeys("test");

      // Now submit the form. WebDriver will find the form for us from the element
      uid.submit();

      // Check the title of the page
      //System.out.println("Page title is: " + driver.getTitle());

      // login
      driver.findElement(By.id("login")).click();

      // Wait for the page to load, timeout after 10 seconds
      (new WebDriverWait(driver, 30)).until(new ExpectedCondition<Boolean>() {
        public Boolean apply(WebDriver d) {
          return d.findElement(By.id("logoutForm")).isDisplayed();
        }
      });

      // run test suite
      driver.findElement(By.id("runTestModel")).click();

      // Wait for the page to load, timeout after 10 seconds
      (new WebDriverWait(driver, 60)).until(new ExpectedCondition<Boolean>() {
        public Boolean apply(WebDriver d) {
          return d.findElement(By.cssSelector("div.results")).isDisplayed();
        }
      });

      System.out.println("Failures: ");
      System.out.println(driver.findElement(By.cssSelector("div.failures")).getText());

      if (!driver.findElement(By.cssSelector("span.bar.passed")).isDisplayed())
        throw new RuntimeException("Selenium test not passed");

    } finally {
      // Close the browser
      driver.quit();
    }
  }
}
