package com.final_year.v2.security;

import com.final_year.v2.model.User;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

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
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Extract provider ID: for Google/Azure it's "sub", for GitHub it's "id"
        String providerId;
        if ("github".equals(provider)) {
            providerId = String.valueOf(attributes.get("id"));
        } else {
            providerId = (String) attributes.get("sub");
        }

        if (providerId == null || providerId.isBlank()) {
            throw new IllegalStateException("Provider ID not found for provider: " + provider);
        }

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new IllegalStateException("User not found after OAuth2 processing for provider: " + provider));

        // Generate JWT tokens
        String accessToken = jwtUtils.generateAccessTokenForEmail(user.getEmail(), user.getRole().name(), user.getPlan().name());
        String refreshToken = jwtUtils.generateRefreshTokenForEmail(user.getEmail());

        // Redirect to frontend OAuth callback route
        String redirectUrl = String.format("%s/oauth2/redirect?access_token=%s&refresh_token=%s",
                frontendUrl, accessToken, refreshToken);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}