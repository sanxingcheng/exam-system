package com.example.examsystem.service;

import com.example.examsystem.common.BusinessException;
import com.example.examsystem.domain.AppUser;
import com.example.examsystem.domain.Enums.UserRole;
import com.example.examsystem.repository.AppUserRepository;
import com.example.examsystem.security.JwtService;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles registration, login and the built-in administrator password flow. */
@Service
public class AuthService {

  private final AppUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final String adminUsername;
  private final String adminInitialPassword;

  public AuthService(
      AppUserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      @Value("${app.admin.username}") String adminUsername,
      @Value("${app.admin.initial-password}") String adminInitialPassword) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.adminUsername = adminUsername;
    this.adminInitialPassword = adminInitialPassword;
  }

  @Transactional
  public void ensureAdminExists() {
    if (userRepository.existsByUsername(adminUsername)) {
      return;
    }
    AppUser admin = new AppUser();
    admin.setUsername(adminUsername);
    admin.setPasswordHash(passwordEncoder.encode(adminInitialPassword));
    admin.setRole(UserRole.ADMIN);
    admin.setPasswordChangeRequired(true);
    userRepository.save(admin);
  }

  @Transactional
  public LoginResult register(String username, String password) {
    if (userRepository.existsByUsername(username)) {
      throw new BusinessException("用户名已存在");
    }
    AppUser user = new AppUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRole(UserRole.USER);
    user.setPasswordChangeRequired(false);
    user.setPasswordUpdatedAt(Instant.now());
    userRepository.save(user);
    return login(username, password);
  }

  @Transactional(readOnly = true)
  public LoginResult login(String username, String password) {
    AppUser user =
        userRepository.findByUsername(username).orElseThrow(() -> new BusinessException("用户名或密码错误"));
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BusinessException("用户名或密码错误");
    }
    return new LoginResult(
        jwtService.createToken(user), user.getUsername(), user.getRole().name(), user.isPasswordChangeRequired());
  }

  @Transactional
  public void changePassword(AppUser user, String oldPassword, String newPassword) {
    AppUser current =
        userRepository.findById(user.getId()).orElseThrow(() -> new BusinessException("用户不存在"));
    if (!passwordEncoder.matches(oldPassword, current.getPasswordHash())) {
      throw new BusinessException("原密码错误");
    }
    current.setPasswordHash(passwordEncoder.encode(newPassword));
    current.setPasswordChangeRequired(false);
    current.setPasswordUpdatedAt(Instant.now());
  }

  public record LoginResult(
      String token, String username, String role, boolean passwordChangeRequired) {}
}
