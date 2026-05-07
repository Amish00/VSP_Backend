package com.final_year.v2.security;

import com.final_year.v2.security.JwtUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract email
        String email = (String) oAuth2User.getAttributes().get("email");
        if (email == null) {
            // Fallback for GitHub when email is private
            email = oAuth2User.getAttributes().get("login") + "@github.com";
        }

        // Generate tokens (you can add role/plan claims if needed)
        String accessToken = jwtUtils.generateTokenForOAuth2(email);
        String refreshToken = jwtUtils.generateRefreshTokenForOAuth2(email); // optional

        // Redirect to frontend with tokens as query parameters
        String redirectUrl = String.format("%s/oauth2/redirect?access_token=%s&refresh_token=%s",
                frontendUrl, accessToken, refreshToken);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}