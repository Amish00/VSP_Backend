package com.final_year.v2.service;

import com.final_year.v2.constaint.Plan;
import com.final_year.v2.constaint.Role;
import com.final_year.v2.constaint.UserStatus;
import com.final_year.v2.dto.UserResponse;
import com.final_year.v2.dto.UserUpdateRequest;
import com.final_year.v2.model.User;
import com.final_year.v2.model.UserProfile;
import com.final_year.v2.repository.UserProfileRepository;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.security.UserDetailsImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Sync role based on plan – non‑admin users get VIEWER for FREE/VIEW, CREATOR for CREATE.
     */
    public void syncRoleFromPlan(User user) {
        if (user.getRole() == Role.ADMIN) return;
        user.setRole(user.getPlan() == Plan.CREATE ? Role.CREATOR : Role.VIEWER);
    }

    /**
     * Converts User entity to UserResponse DTO, including profile fields.
     */
    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setPlan(user.getPlan());
        response.setJoined(user.getJoined());
        response.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        response.setVideos(user.getVideos());
        response.setProfilePicture(user.getProfilePicture());
        response.setSubscriptionExpiry(user.getSubscriptionExpiry());
        response.setPreviousPlan(user.getPreviousPlan());
        response.setBillingCycle(user.getBillingCycle());

        if (user.getProfile() != null) {
            UserProfile p = user.getProfile();
            response.setFullName(p.getFullName());
            response.setPhone(p.getPhone());
            response.setDob(p.getDob());
            response.setGender(p.getGender());
            response.setCountry(p.getCountry());
            response.setBio(p.getBio());
            response.setFacebook(p.getFacebook());
            response.setTwitter(p.getTwitter());
            response.setInstagram(p.getInstagram());
            response.setBannerUrl(p.getBannerUrl());
        }
        return response;
    }

    /**
     * Checks if the user's subscription has expired; if so, reverts to FREE,
     * stores the previous plan, and clears expiry.
     */
    @Transactional
    public void checkAndExpireSubscription(User user) {
        if (user.getSubscriptionExpiry() != null && user.getSubscriptionExpiry().isBefore(LocalDateTime.now())) {
            log.info("Plan expired for user {} (was {}). Reverting to FREE.", user.getEmail(), user.getPlan());
            user.setPreviousPlan(user.getPlan());
            user.setPlan(Plan.FREE);
            user.setSubscriptionExpiry(null);
            user.setBillingCycle(null);
            syncRoleFromPlan(user);
            userRepository.save(user);
            log.info("User {} reverted to FREE, previous plan: {}", user.getEmail(), user.getPreviousPlan());
        }
        // If user is FREE but still has an expiry (should not happen), clear it
        if (user.getPlan() == Plan.FREE && user.getSubscriptionExpiry() != null) {
            log.warn("User {} is FREE but has expiry set. Clearing expiry.", user.getEmail());
            user.setSubscriptionExpiry(null);
            user.setBillingCycle(null);
            userRepository.save(user);
        }
    }

    /**
     * Returns the currently authenticated user (with profile) after checking expiry.
     */
    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        checkAndExpireSubscription(user);
        return user;
    }

    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new UsernameNotFoundException("User not found");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getEmail().toLowerCase();
        }
        return auth.getName().toLowerCase();
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAllWithProfile().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdWithProfile(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        checkAndExpireSubscription(user);
        return convertToResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, boolean isAdmin, String currentUserEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!isAdmin && !user.getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("You can only update your own profile");
        }

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getProfilePicture() != null) user.setProfilePicture(request.getProfilePicture());

        if (isAdmin) {
            if (request.getRole() != null) user.setRole(request.getRole());
            if (request.getPlan() != null) {
                user.setPlan(request.getPlan());
                syncRoleFromPlan(user);
            }
            if (request.getStatus() != null) user.setStatus(UserStatus.valueOf(request.getStatus().toUpperCase()));
            if (request.getVideos() != null) user.setVideos(request.getVideos());
        }

        UserProfile profile = user.getOrCreateProfile();
        if (request.getFullName() != null) profile.setFullName(request.getFullName());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getDob() != null) profile.setDob(request.getDob());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getCountry() != null) profile.setCountry(request.getCountry());
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getFacebook() != null) profile.setFacebook(request.getFacebook());
        if (request.getTwitter() != null) profile.setTwitter(request.getTwitter());
        if (request.getInstagram() != null) profile.setInstagram(request.getInstagram());
        if (request.getBannerUrl() != null) profile.setBannerUrl(request.getBannerUrl());

        userProfileRepository.save(profile);
        User updated = userRepository.save(user);
        return convertToResponse(updated);
    }

    @Transactional
    public void deleteUser(Long id, boolean isAdmin, String currentUserEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!isAdmin && !user.getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("You can only delete your own account");
        }

        cleanupUserDependencies(id);

        userRepository.delete(user);
    }

    private void cleanupUserDependencies(Long userId) {
        // Auth/session tokens
        entityManager.createQuery("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM RefreshToken t WHERE t.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();

        // User-owned direct records
        entityManager.createQuery("DELETE FROM Notification n WHERE n.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM RevenueRecord r WHERE r.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM PayoutRequest p WHERE p.creator.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM Subscription s WHERE s.subscriber.id = :userId OR s.subscribedTo.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();

        // User-owned video graph
        entityManager.createQuery("DELETE FROM Comment c WHERE c.user.id = :userId OR c.video.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM Like l WHERE l.user.id = :userId OR l.video.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM History h WHERE h.user.id = :userId OR h.video.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM ViewRecord v WHERE v.user.id = :userId OR v.video.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM WatchHistory w WHERE w.user.id = :userId OR w.video.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM Video v WHERE v.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Transactional
    public String uploadProfilePictureForUser(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String publicId = "user_" + user.getId() + "_profile";
        String folder = "user_profiles";
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            deleteFileFromCloudinary(user.getProfilePicture());
        }
        String imageUrl = cloudinaryService.uploadFile(file, folder, publicId);
        user.setProfilePicture(imageUrl);
        userRepository.save(user);
        return imageUrl;
    }

    @Transactional
    public String uploadProfilePicture(MultipartFile file) {
        User user = getCurrentUser();
        String publicId = "user_" + user.getId() + "_profile";
        String folder = "user_profiles";
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            deleteFileFromCloudinary(user.getProfilePicture());
        }
        String imageUrl = cloudinaryService.uploadFile(file, folder, publicId);
        user.setProfilePicture(imageUrl);
        userRepository.save(user);
        return imageUrl;
    }

    @Transactional
    public String uploadBanner(MultipartFile file) {
        User user = getCurrentUser();
        UserProfile profile = user.getOrCreateProfile();
        String publicId = "user_" + user.getId() + "_banner";
        String folder = "user_banners";
        if (profile.getBannerUrl() != null && !profile.getBannerUrl().isEmpty()) {
            deleteFileFromCloudinary(profile.getBannerUrl());
        }
        String bannerUrl = cloudinaryService.uploadFile(file, folder, publicId);
        profile.setBannerUrl(bannerUrl);
        userProfileRepository.save(profile);
        return bannerUrl;
    }

    private void deleteFileFromCloudinary(String url) {
        try {
            String[] parts = url.split("/");
            String folderAndId = parts[parts.length - 2] + "/" + parts[parts.length - 1];
            if (folderAndId.contains(".")) {
                folderAndId = folderAndId.substring(0, folderAndId.lastIndexOf('.'));
            }
            cloudinaryService.deleteFile(folderAndId, "image");
        } catch (Exception e) {
            System.err.println("Failed to delete old file: " + e.getMessage());
        }
    }

    /**
     * Upgrades a user to a new plan (used by PaymentService after successful payment).
     */
    @Transactional
    public void upgradePlan(Long userId, Plan newPlan, String billingCycle) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Admin users cannot change plan via this method");
        }
        user.setPlan(newPlan);
        user.setPreviousPlan(null);      // fresh paid plan, clear any expired previous
        if (newPlan != Plan.FREE) {
            user.setBillingCycle(billingCycle);
        } else {
            user.setBillingCycle(null);
        }
        syncRoleFromPlan(user);
        userRepository.save(user);
        log.info("User {} upgraded to {} plan with billing cycle {}", user.getEmail(), newPlan, billingCycle);
    }

    /**
     * Manual downgrade to FREE (triggered by user). Clears previousPlan and billingCycle.
     */
    @Transactional
    public void upgradeToFreePlan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPlan(Plan.FREE);
        user.setSubscriptionExpiry(null);
        user.setPreviousPlan(null);
        user.setBillingCycle(null);
        syncRoleFromPlan(user);
        userRepository.save(user);
        log.info("User {} manually downgraded to FREE", user.getEmail());
    }

    public void notifyAdminsOfNewUser(User newUser) {
        List<User> admins = userRepository.findAll().stream()
                .filter(admin -> admin.getRole() == Role.ADMIN)
                .collect(Collectors.toList());

        for (User admin : admins) {
            notificationService.createNotification(
                    admin,
                    "New User Registered",
                    String.format("New user '%s' (%s) has joined the platform.", newUser.getUsername(), newUser.getEmail()),
                    "NEW_USER_REGISTRATION",
                    newUser.getId().toString()
            );
        }
    }
}