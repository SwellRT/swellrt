import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class TestWaveJS  {
  public static void main(String[] args) {
    // Create a new instance of the Firefox driver
    // Notice that the remainder of the code relies on the interface,
    // not the implementation.
    WebDriver driver = new ChromeDriver();

    (new WebDriverWait(driver, 30)).until(new ExpectedCondition<Boolean>() {
      public Boolean apply(WebDriver d) {
        d.get("http://localhost:9898/test/wavejs.html");

        return d.findElement(By.id("loginForm")).isDisplayed();
      }
    });

    // Find the text input element by its name
    WebElement uid = driver.findElement(By.id("uid"));

    // Enter something to search for
    uid.sendKeys("test");

    WebElement pwd = driver.findElement(By.id("password"));

    pwd.sendKeys("test");

    // Now submit the form. WebDriver will find the form for us from the element
    uid.submit();

    // Check the title of the page
    //System.out.println("Page title is: " + driver.getTitle());

    // login
    driver.findElement(By.id("login")).click();

    // Wait for the page to load, timeout after 10 seconds
    (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
      public Boolean apply(WebDriver d) {
        return d.findElement(By.id("logoutForm")).isDisplayed();
      }
    });

    // run test suite
    driver.findElement(By.id("runP2PvalueTest")).click();

    // Wait for the page to load, timeout after 10 seconds
    (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
      public Boolean apply(WebDriver d) {
        return d.findElement(By.cssSelector("span.bar.passed")).isDisplayed();
      }
    });

    // Close the browser
    driver.quit();
  }
}
