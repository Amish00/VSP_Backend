package com.final_year.v2.security;

import com.final_year.v2.model.User;
import com.final_year.v2.repository.UserRepository;
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

    @Autowired
    private UserRepository userRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = (String) oAuth2User.getAttributes().get("email");
        if (email == null) {
            String login = (String) oAuth2User.getAttributes().get("login");
            email = login + "@github.com";
        }
        email = email.toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found after OAuth2 processing"));

        // Generate tokens with up-to-date role/plan
        String accessToken = jwtUtils.generateAccessTokenForEmail(email, user.getRole().name(), user.getPlan().name());
        String refreshToken = jwtUtils.generateRefreshTokenForEmail(email);

        // Redirect to frontend with tokens
        String redirectUrl = String.format("%s/oauth2/redirect?access_token=%s&refresh_token=%s",
                frontendUrl, accessToken, refreshToken);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
