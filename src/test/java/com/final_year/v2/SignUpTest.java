package com.final_year.v2;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;

public class SignUpTest extends BaseTest {

    private final String SIGNUP_URL = BASE_URL + "/signup";

    private void fillSignUpForm(String name, String email, String password, String confirm) {
        driver.findElement(By.id("name")).sendKeys(name);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("confirmPassword")).sendKeys(confirm);
    }

    private void submitForm() {
        driver.findElement(By.xpath("//button[contains(text(),'Create Account')]")).click();
    }


    private String waitForSnackbarText(String expectedSubstring) {
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(15));

        By snackbarLocator = By.xpath(
                "//div[@role='alert'] | " +
                        "//div[contains(@class, 'SnackbarItem')] | " +
                        "//div[contains(@class, 'notistack')]"
        );

        try {
            return longWait.until(d -> {
                for (WebElement el : d.findElements(snackbarLocator)) {
                    try {
                        if (el.isDisplayed()) {
                            String text = el.getText();
                            if (text != null && text.contains(expectedSubstring)) {
                                return text;
                            }
                        }
                    } catch (StaleElementReferenceException ignored) {
                    }
                }
                return null;
            });
        } catch (TimeoutException e) {
            StringBuilder seen = new StringBuilder();
            for (WebElement el : driver.findElements(snackbarLocator)) {
                try {
                    if (el.isDisplayed()) {
                        seen.append("[").append(el.getText()).append("] ");
                    }
                } catch (StaleElementReferenceException ignored) {
                    // best-effort diagnostics only
                }
            }
            throw new AssertionError(
                    "Timed out after 15s waiting for a snackbar containing: \"" + expectedSubstring +
                            "\". Visible snackbar text was: " +
                            (seen.length() == 0 ? "<none>" : seen.toString().trim()),
                    e
            );
        }
    }

    // ---------- Positive test ----------
    @Test
    public void testSuccessfulSignUp() {
        driver.get(SIGNUP_URL);

        String uniqueEmail = "testuser_" + System.currentTimeMillis() + "@example.com";
        fillSignUpForm("John Doe112", uniqueEmail, "Password@123", "Password@123");
        submitForm();

        String successMsg = waitForSnackbarText("Account created successfully!"); // no period
        Assert.assertTrue(successMsg.contains("Account created successfully!"));

        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/"));
        Assert.assertEquals(driver.getCurrentUrl(), BASE_URL + "/");
    }

    // ---------- Negative tests ----------
    @Test
    public void testSignUpEmptyFields() {
        driver.get(SIGNUP_URL);
        submitForm();

        // Frontend message ends with a period
        String errorMsg = waitForSnackbarText("Full name is required.");
        Assert.assertTrue(errorMsg.contains("Full name is required."));
    }

    @Test
    public void testSignUpPasswordMismatch() {
        driver.get(SIGNUP_URL);
        // Both passwords must individually satisfy the app's password policy
        // (8+ chars, uppercase, digit, special char) so validation reaches the
        // confirm-password check instead of stopping earlier on a policy error.
        fillSignUpForm("Jane Doe11", "jane@test.com", "Password@123", "Different@456");
        submitForm();

        String errorMsg = waitForSnackbarText("Passwords don't match."); // period
        Assert.assertTrue(errorMsg.contains("Passwords don't match."));
    }

    @Test
    public void testSignUpShortPassword() {
        driver.get(SIGNUP_URL);
        fillSignUpForm("Weak User", "weak@test.com", "123", "123");
        submitForm();

        String errorMsg = waitForSnackbarText("Password must be at least 8 characters."); // period
        Assert.assertTrue(errorMsg.contains("at least 8 characters"));
    }

    @Test
    public void testSignUpDuplicateEmail() {
        // 1. Create a user first (successful signup)
        driver.get(SIGNUP_URL);
        String email = "duplicates1_" + System.currentTimeMillis() + "@example.com";
        fillSignUpForm("Duplicate User11", email, "ValidPass1!", "ValidPass1!");
        submitForm();

        // Wait for success and ensure we are redirected
        waitForSnackbarText("Account created successfully!");
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/"));

        // 2. Navigate back to signup and try to register with the same email
        driver.get(SIGNUP_URL);
        fillSignUpForm("Another User", email, "DifferentPass2@", "DifferentPass2@");
        submitForm();

        // Expect an error snackbar – the backend message contains "already" or "exists"
        String errorMsg = waitForSnackbarText("already");
        Assert.assertTrue(errorMsg.toLowerCase().contains("already") || errorMsg.toLowerCase().contains("exists"));

        // Ensure we are NOT redirected to home
        Assert.assertNotEquals(driver.getCurrentUrl(), BASE_URL + "/");
    }

    @Test
    public void testSignUpInvalidEmailFormat() {
        driver.get(SIGNUP_URL);
        fillSignUpForm("Bad Email", "not-an-email", "ValidPass1", "ValidPass1");
        submitForm();

        String errorMsg = waitForSnackbarText("Please enter a valid email address."); // period
        Assert.assertTrue(errorMsg.toLowerCase().contains("email"));
        Assert.assertNotEquals(driver.getCurrentUrl(), BASE_URL + "/");
    }
}