package com.example.examsystem.controller;

import com.example.examsystem.common.ApiResponse;
import com.example.examsystem.domain.AppUser;
import com.example.examsystem.service.AuthService;
import com.example.examsystem.service.AuthService.LoginResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints for administrators and normal users. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ApiResponse<LoginResult> register(@RequestBody AuthRequest request) {
    return ApiResponse.ok(authService.register(request.username(), request.password()));
  }

  @PostMapping("/login")
  public ApiResponse<LoginResult> login(@RequestBody AuthRequest request) {
    return ApiResponse.ok(authService.login(request.username(), request.password()));
  }

  @PostMapping("/change-password")
  public ApiResponse<Void> changePassword(
      @AuthenticationPrincipal AppUser user, @RequestBody ChangePasswordRequest request) {
    authService.changePassword(user, request.oldPassword(), request.newPassword());
    return ApiResponse.ok(null);
  }

  public record AuthRequest(String username, String password) {}

  public record ChangePasswordRequest(String oldPassword, String newPassword) {}
}
