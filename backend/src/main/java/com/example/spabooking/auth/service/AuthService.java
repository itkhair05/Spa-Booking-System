package com.example.spabooking.auth.service;

import com.example.spabooking.auth.dto.ChangePasswordRequest;
import com.example.spabooking.auth.entity.User;
import com.example.spabooking.auth.repository.UserRepository;
import com.example.spabooking.auth.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AccessDeniedException("Người dùng chưa được xác thực");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp với mật khẩu mới");
        }

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy thông tin tài khoản người dùng"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
