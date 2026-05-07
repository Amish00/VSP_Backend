package com.final_year.v2.controller;

import com.final_year.v2.constaint.Plan;
import com.final_year.v2.dto.*;
import com.final_year.v2.model.RefreshToken;
import com.final_year.v2.model.User;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.security.JwtUtils;
import com.final_year.v2.security.UserDetailsImpl;
import com.final_year.v2.service.PasswordResetService;
import com.final_year.v2.service.RefreshTokenService;
import com.final_year.v2.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already taken!"));
        }

        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));
        user.setPlan(Plan.VIEW);
        userService.syncRoleFromPlan(user);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtUtils.generateAccessToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findByEmail(userDetails.getEmail()).orElseThrow();
        userService.syncRoleFromPlan(user);
        userRepository.save(user);

        refreshTokenService.deleteByUserId(userDetails.getId());
        refreshTokenService.createRefreshToken(userDetails.getId());



        return ResponseEntity.ok(new JwtResponse(
                accessToken,
                refreshToken,
                userDetails.getUsername(),
                userDetails.getRole(),
                userDetails.getPlan()
        ));
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // 1. Validate JWT refresh token structure and signature
        if (!jwtUtils.validateToken(requestRefreshToken)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid refresh token"));
        }

        // 2. Extract email from the refresh token
        String email = jwtUtils.getUserNameFromToken(requestRefreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. (Optional) Verify that this refresh token exists in DB (if you store them)
        refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found in DB"));

        // 4. Build UserDetails from the existing user (no password check needed)
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        // Create an Authentication object manually (the token is already trusted)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        // 5. Generate new tokens using the authentication object
        String newAccessToken = jwtUtils.generateAccessToken(authentication);
        String newRefreshToken = jwtUtils.generateRefreshToken(authentication);

        // 6. Update the stored refresh token in DB (optional)
        refreshTokenService.deleteByUserId(user.getId());
        refreshTokenService.createRefreshToken(user.getId());

        return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, newRefreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            refreshTokenService.deleteByUserId(userDetails.getId());
        }
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        boolean sent = passwordResetService.sendOtp(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(
                sent ? "OTP sent to your email." : "If email exists, OTP has been sent."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        boolean verified = passwordResetService.verifyOtp(request.getEmail(), request.getOtp());
        if (!verified) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid or expired OTP."));
        }
        return ResponseEntity.ok(new MessageResponse("OTP verified. You can now reset your password."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean reset = passwordResetService.resetPassword(
                request.getEmail(),
                encoder.encode(request.getNewPassword()),
                request.getOtp());
        if (!reset) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid or expired OTP."));
        }
        return ResponseEntity.ok(new MessageResponse("Password reset successfully."));
    }
}