package com.final_year.v2.service;

import com.final_year.v2.model.PasswordResetToken;
import com.final_year.v2.model.User;
import com.final_year.v2.repository.PasswordResetTokenRepository;
import com.final_year.v2.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EntityManager entityManager;

    @Transactional
    public boolean sendOtp(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();

        // Delete existing token (if any)
        tokenRepository.deleteByUser(user);
        entityManager.flush();   // force deletion to database

        String otp = String.format("%06d", random.nextInt(1000000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        PasswordResetToken token = new PasswordResetToken(user, otp, expiry);
        tokenRepository.save(token);

        emailService.sendOtpEmail(email, otp);
        return true;
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) {
        return isOtpValid(email, otp, false);
    }

    private boolean isOtpValid(String email, String otp, boolean consume) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByUserAndOtpAndUsedFalse(user, otp);
        if (tokenOpt.isEmpty()) return false;

        PasswordResetToken token = tokenOpt.get();
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) return false;

        if (consume) {
            token.setUsed(true);
            tokenRepository.save(token);
        }
        return true;
    }

    @Transactional
    public boolean resetPassword(String email, String newPassword, String otp) {
        // Verify OTP and consume it only when the password is actually reset
        boolean verified = isOtpValid(email, otp, true);
        if (!verified) return false;

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        user.setPassword(newPassword);  // Password will be encoded in controller
        userRepository.save(user);
        return true;
    }


}