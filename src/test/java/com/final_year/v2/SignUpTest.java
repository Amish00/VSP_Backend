package com.final_year.v2;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SignUpTest extends BaseTest {

    private final String SIGNUP_URL = BASE_URL + "/signup";

    private void fillSignUpForm(String name, String email, String password, String confirm) {
        driver.findElement(By.id("name")).sendKeys(name);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("confirmPassword")).sendKeys(confirm);
    }

    private void submitForm() {
        // Button has no type="submit" – locate by text
        driver.findElement(By.xpath("//button[contains(text(),'Create Account')]")).click();
    }

    // Wait for snackbar containing a substring – with proper XPath quoting
    private String waitForSnackbarText(String expectedSubstring) {
        // Use double quotes for the XPath string literal to avoid escaping issues
        WebElement snackbar = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'Snackbar') or contains(@class,'notistack')]//*[contains(text(),\"" + expectedSubstring + "\")]")
        ));
        return snackbar.getText();
    }

    // ---------- Positive test ----------
    @Test
    public void testSuccessfulSignUp() {
        driver.get(SIGNUP_URL);
        String uniqueEmail = "testUoser" + System.currentTimeMillis() + "@example.com";
        fillSignUpForm("John Does", uniqueEmail, "StrongPass123", "StrongPass123");
        submitForm();

        String successMsg = waitForSnackbarText("Account created successfully!");
        Assert.assertTrue(successMsg.contains("Account created successfully!"));

        // After signup, redirects to home (/) – adjust if your app redirects elsewhere
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/"));
        Assert.assertEquals(driver.getCurrentUrl(), BASE_URL + "/");
    }

    // ---------- Negative tests ----------
    @Test
    public void testSignUpEmptyFields() {
        driver.get(SIGNUP_URL);
        submitForm();

        // Frontend validation order: name first
        String errorMsg = waitForSnackbarText("Full name is required");
        Assert.assertTrue(errorMsg.contains("Full name is required"));
    }

    @Test
    public void testSignUpPasswordMismatch() {
        driver.get(SIGNUP_URL);
        fillSignUpForm("Jane Dose", "jane2@test.com", "Password123", "Different456");
        submitForm();

        // Escaped XPath – using double quotes around the text
        String errorMsg = waitForSnackbarText("Passwords don't match");
        Assert.assertTrue(errorMsg.contains("Passwords don't match"));
    }

    @Test
    public void testSignUpShortPassword() {
        driver.get(SIGNUP_URL);
        fillSignUpForm("Weak User", "weak@test.com", "123", "123");
        submitForm();

        String errorMsg = waitForSnackbarText("Password must be at least 8 characters");
        Assert.assertTrue(errorMsg.contains("at least 8 characters"));
    }

    @Test
    public void testSignUpDuplicateEmail() {

        driver.get(SIGNUP_URL);
        String existingEmail = "jane2@test.com"; // replace with a real existing email
        fillSignUpForm("Duplicate User", existingEmail, "AnyPass123", "AnyPass123");
        submitForm();

        String errorMsg = waitForSnackbarText("Username is already taken!"); // change as needed
        Assert.assertTrue(errorMsg.contains("already") || errorMsg.contains("exists"));
        Assert.assertNotEquals(driver.getCurrentUrl(), BASE_URL + "/", "Should not redirect to home");
    }

    @Test
    public void testSignUpInvalidEmailFormat() {
        driver.get(SIGNUP_URL);
        fillSignUpForm("Bad Email", "not-an-email", "ValidPass1", "ValidPass1");
        submitForm();
        String errorMsg = waitForSnackbarText("Please enter a valid email address.");
        Assert.assertTrue(errorMsg.toLowerCase().contains("email"));
        Assert.assertNotEquals(driver.getCurrentUrl(), BASE_URL + "/");
    }
}