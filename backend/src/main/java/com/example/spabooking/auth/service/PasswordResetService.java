package com.example.spabooking.auth.service;

import com.example.spabooking.auth.dto.ForgotPasswordRequest;
import com.example.spabooking.auth.dto.ResetPasswordRequest;
import com.example.spabooking.auth.entity.PasswordResetToken;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.enums.UserRole;
import com.example.spabooking.auth.repository.PasswordResetTokenRepository;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    public static final String GENERIC_FORGOT_SUCCESS_MESSAGE =
            "Nếu địa chỉ email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn.";
    public static final String GENERIC_RESET_ERROR_MESSAGE =
            "Token đã hết hạn hoặc không hợp lệ";
    public static final String RESET_SUCCESS_MESSAGE =
            "Đặt lại mật khẩu thành công. Vui lòng đăng nhập với mật khẩu mới.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                Environment environment) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.environment = environment;
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            return;
        }

        String rawEmail = request.getEmail().trim();
        if (!rawEmail.contains("@") || rawEmail.length() < 3 || rawEmail.length() > 254) {
            return;
        }

        String normalizedEmail = rawEmail.toLowerCase(Locale.ROOT);

        List<User> candidates = userRepository.findCandidatesForPasswordReset(normalizedEmail);
        List<User> eligibleUsers = filterEligibleUsers(candidates, normalizedEmail);

        if (eligibleUsers.isEmpty()) {
            log.debug("Password reset requested for unmatched or ineligible email.");
            return;
        }

        if (eligibleUsers.size() > 1) {
            log.warn("Password reset requested for ambiguous email matching multiple distinct accounts. Suppressing delivery.");
            return;
        }

        User user = eligibleUsers.get(0);

        try {
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            String tokenHash = hashToken(rawToken);

            LocalDateTime now = LocalDateTime.now();
            tokenRepository.invalidatePreviousTokensForUser(user.getId(), now);

            PasswordResetToken resetToken = new PasswordResetToken(user, tokenHash, now.plusMinutes(15));
            tokenRepository.save(resetToken);

            String resetUrl = buildResetUrl(rawToken);
            String recipientName = resolveRecipientName(user);
            String spaName = (user.getTenant() != null && user.getTenant().getName() != null)
                    ? user.getTenant().getName() : "TIKEY SPA";

            emailService.sendPasswordResetEmail(normalizedEmail, recipientName, resetUrl, spaName);
        } catch (Exception e) {
            log.error("Failed to process password reset request for user id {}: {}", user.getId(), e.getMessage());
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new IllegalArgumentException(GENERIC_RESET_ERROR_MESSAGE);
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);
        LocalDateTime now = LocalDateTime.now();

        int updatedRows = tokenRepository.markTokenAsUsedIfValid(tokenHash, now);
        if (updatedRows != 1) {
            throw new IllegalArgumentException(GENERIC_RESET_ERROR_MESSAGE);
        }

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(GENERIC_RESET_ERROR_MESSAGE));

        User user = token.getUser();
        if (!isUserEligible(user)) {
            throw new IllegalArgumentException(GENERIC_RESET_ERROR_MESSAGE);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        int currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        user.setTokenVersion(currentVersion + 1);
        userRepository.save(user);

        log.info("Successfully reset password and incremented token_version to {} for user id {}",
                user.getTokenVersion(), user.getId());
    }

    public String buildResetUrl(String rawToken) {
        String baseUrl = frontendBaseUrl != null ? frontendBaseUrl.trim() : "";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        boolean isProd = environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"));
        if (isProd) {
            if (baseUrl.isBlank() || baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) {
                throw new IllegalStateException("Production APP_FRONTEND_BASE_URL must not be empty or point to localhost.");
            }
            if (!baseUrl.startsWith("https://")) {
                throw new IllegalStateException("Production APP_FRONTEND_BASE_URL must require HTTPS.");
            }
        }

        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return baseUrl + "/reset-password?token=" + encodedToken;
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private List<User> filterEligibleUsers(List<User> candidates, String normalizedEmail) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> uniqueEligible = new LinkedHashMap<>();
        for (User user : candidates) {
            if (user == null || user.getId() == null) continue;
            if (!isUserEligible(user)) continue;

            boolean emailMatches = false;
            if (user.getRole() == UserRole.OWNER) {
                if (user.getTenant() != null && user.getTenant().getEmail() != null
                        && normalizedEmail.equalsIgnoreCase(user.getTenant().getEmail().trim())) {
                    emailMatches = true;
                } else if (user.getUsername() != null && normalizedEmail.equalsIgnoreCase(user.getUsername().trim())) {
                    emailMatches = true;
                }
            } else if (user.getRole() == UserRole.STAFF) {
                if (user.getStaff() != null && user.getStaff().getEmail() != null
                        && normalizedEmail.equalsIgnoreCase(user.getStaff().getEmail().trim())) {
                    emailMatches = true;
                } else if (user.getUsername() != null && normalizedEmail.equalsIgnoreCase(user.getUsername().trim())) {
                    emailMatches = true;
                }
            }

            if (emailMatches) {
                uniqueEligible.put(user.getId(), user);
            }
        }

        return new ArrayList<>(uniqueEligible.values());
    }

    private boolean isUserEligible(User user) {
        if (user == null) return false;
        if (!Boolean.TRUE.equals(user.getIsActive())) return false;
        if (user.getRole() != UserRole.OWNER && user.getRole() != UserRole.STAFF) return false;
        if (user.getTenant() == null || !Boolean.TRUE.equals(user.getTenant().getIsActive())) return false;

        if (user.getRole() == UserRole.STAFF) {
            if (user.getStaff() == null) return false;
            if (!Boolean.TRUE.equals(user.getStaff().getIsActive())) return false;
            if (Boolean.TRUE.equals(user.getStaff().getIsDeleted())) return false;
        }

        return true;
    }

    private String resolveRecipientName(User user) {
        if (user.getRole() == UserRole.STAFF && user.getStaff() != null && user.getStaff().getName() != null) {
            return user.getStaff().getName();
        }
        if (user.getRole() == UserRole.OWNER && user.getTenant() != null && user.getTenant().getName() != null) {
            return user.getTenant().getName() + " (Chủ cơ sở)";
        }
        return user.getUsername();
    }
}
