package com.final_year.v2;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SignInTest extends BaseTest {

    private final String SIGNIN_URL = BASE_URL + "/signin";

    private void fillSignInForm(String email, String password) {
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
    }

    private void submitSignIn() {
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    // Helper: wait for snackbar with given text (partial or full)
    private String waitForSnackbarText(String expectedSubstring) {
        WebElement snackbar = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'Snackbar') or contains(@class,'notistack')]//*[contains(text(),'" + expectedSubstring + "')]")
        ));
        return snackbar.getText();
    }

    @Test
    public void testSuccessfulSignIn() {
        driver.get(SIGNIN_URL);
        fillSignInForm("menastla8@gmail.com", "alice123");
        submitSignIn();

        wait.until(d -> d.getCurrentUrl().contains("/home") || d.getCurrentUrl().contains("/dashboard"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/home") || driver.getCurrentUrl().contains("/dashboard"));
    }

    @Test
    public void testSignInInvalidPassword() {
        driver.get(SIGNIN_URL);
        fillSignInForm("selenium@test.com", "WrongPassword");
        submitSignIn();

        String snackbarText = waitForSnackbarText("Incorrect email or password");
        Assert.assertTrue(snackbarText.contains("Incorrect email or password"));
        Assert.assertFalse(driver.getCurrentUrl().contains("/home"));
    }

    @Test
    public void testSignInNonExistentEmail() {
        driver.get(SIGNIN_URL);
        fillSignInForm("ghost@example.com", "anypass");
        submitSignIn();

        String snackbarText = waitForSnackbarText("Incorrect email or password");
        Assert.assertTrue(snackbarText.contains("Incorrect email or password"));
    }

    @Test
    public void testSignInEmptyFields() {
        driver.get(SIGNIN_URL);
        submitSignIn();

        String snackbarText = waitForSnackbarText("Please fill all fields");
        Assert.assertTrue(snackbarText.contains("Please fill all fields"),
                "Expected 'Please fill all fields' but got: " + snackbarText);
        Assert.assertFalse(driver.getCurrentUrl().contains("/home"));
    }

    @Test
    public void testSignInMissingPassword() {
        driver.get(SIGNIN_URL);
        fillSignInForm("selenium@test.com", "");
        submitSignIn();

        String snackbarText = waitForSnackbarText("Please fill all fields");
        Assert.assertTrue(snackbarText.contains("Please fill all fields"),
                "Expected 'Please fill all fields' but got: " + snackbarText);
        Assert.assertFalse(driver.getCurrentUrl().contains("/home"));
    }
}