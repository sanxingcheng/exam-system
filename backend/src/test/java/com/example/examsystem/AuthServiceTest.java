package com.example.examsystem;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.examsystem.repository.AppUserRepository;
import com.example.examsystem.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthServiceTest {

  @Autowired private AuthService authService;
  @Autowired private AppUserRepository userRepository;

  @Test
  void builtInAdminRequiresPasswordChangeOnFirstLogin() {
    authService.ensureAdminExists();

    var login = authService.login("admin", "Admin@123");

    assertThat(login.role()).isEqualTo("ADMIN");
    assertThat(login.passwordChangeRequired()).isTrue();
    assertThat(userRepository.findByUsername("admin")).isPresent();
  }
}
