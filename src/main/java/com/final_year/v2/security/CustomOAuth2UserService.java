package com.final_year.v2.security;

import com.final_year.v2.constaint.Plan;
import com.final_year.v2.constaint.Role;
import com.final_year.v2.model.User;
import com.final_year.v2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = null;
        String name = null;
        String providerId = null;
        String picture = null;

        switch (provider) {
            case "google":
                email = (String) attributes.get("email");
                name = (String) attributes.get("name");
                providerId = (String) attributes.get("sub");
                picture = (String) attributes.get("picture");
                break;
            case "github":
                email = (String) attributes.get("email");
                name = (String) attributes.get("login");
                providerId = String.valueOf(attributes.get("id"));
                picture = (String) attributes.get("avatar_url");
                if (email == null) {
                    email = name + "@github.com";
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        email = email.toLowerCase();

        // Find user by providerId first
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            // Check by email (maybe registered via normal signup)
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                user = userByEmail.get();
                // Link OAuth account to existing user
                user.setProvider(provider);
                user.setProviderId(providerId);
                if (picture != null && user.getProfilePicture() == null) {
                    user.setProfilePicture(picture);
                }
                userRepository.save(user);
            } else {
                // Create new user
                user = new User();
                user.setUsername(name != null ? name : email.split("@")[0]);
                user.setEmail(email);
                user.setPassword(""); // OAuth users have no password
                user.setProvider(provider);
                user.setProviderId(providerId);
                user.setRole(Role.VIEWER);
                user.setPlan(Plan.FREE);
                user.setProfilePicture(picture);
                userRepository.save(user);
            }
        }

        // Build authorities
        return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                provider.equals("github") ? "login" : "email"
        );
    }
}