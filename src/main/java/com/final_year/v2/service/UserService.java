package com.final_year.v2.service;

import com.final_year.v2.constaint.Plan;
import com.final_year.v2.constaint.Role;
import com.final_year.v2.dto.UserResponse;
import com.final_year.v2.dto.UserUpdateRequest;
import com.final_year.v2.constaint.UserStatus;
import com.final_year.v2.model.User;
import com.final_year.v2.model.UserProfile;
import com.final_year.v2.repository.UserProfileRepository;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.security.UserDetailsImpl;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    public void syncRoleFromPlan(User user) {
        if (user.getRole() == Role.ADMIN) return;
        user.setRole(user.getPlan() == Plan.CREATE ? Role.CREATOR : Role.VIEWER);
    }

    // MODIFIED: convertToResponse now includes profile fields
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

        // Populate profile fields if profile exists
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

    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        // fetch with profile to avoid lazy loading
        return userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
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

    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdWithProfile(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, boolean isAdmin, String currentUserEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!isAdmin && !user.getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("You can only update your own profile");
        }

        // Update basic fields
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

        // Admin-only fields
        if (isAdmin) {
            if (request.getRole() != null) user.setRole(request.getRole());
            if (request.getPlan() != null) {
                user.setPlan(request.getPlan());
                syncRoleFromPlan(user);
            }
            if (request.getStatus() != null) user.setStatus(UserStatus.valueOf(request.getStatus().toUpperCase()));
            if (request.getVideos() != null) user.setVideos(request.getVideos());
        }

        // Update or create profile
        UserProfile profile = user.getOrCreateProfile();
        if(request.getFullName() != null) profile.setFullName(request.getFullName());
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
        userRepository.delete(user);
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

    @Transactional
    public void upgradePlan(Long userId, Plan newPlan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Admin users cannot change plan via this method");
        }
        user.setPlan(newPlan);
        syncRoleFromPlan(user);
        userRepository.save(user);
    }

    private void checkAndExpireSubscription(User user) {
        if (user.getSubscriptionExpiry() != null && user.getSubscriptionExpiry().isBefore(LocalDateTime.now())) {
            user.setPlan(Plan.FREE);
            syncRoleFromPlan(user);
            user.setSubscriptionExpiry(null);
            userRepository.save(user);
        }
    }
}