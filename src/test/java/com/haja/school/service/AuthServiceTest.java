package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.rest.model.LoginRequest;
import com.haja.school.endpoint.rest.model.LoginResponse;
import com.haja.school.model.Role;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JUser;
import com.haja.school.security.JwtProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private JUserRepository userRepository;
  @Mock private JwtProvider jwtProvider;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private AuthService authService;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Test
  void login_studentSuccess() {
    String email = "john.doe@student.com";
    String password = "password123";
    LoginRequest request = LoginRequest.builder().email(email).password(password).build();

    JUser user =
        JUser.builder()
            .id(userId)
            .email(email)
            .password("encodedPassword")
            .role(Role.STUDENT)
            .build();

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);
    when(jwtProvider.generateToken(userId, email, Role.STUDENT)).thenReturn("mockJwtToken");

    LoginResponse response = authService.login(request);

    assertNotNull(response);
    assertEquals("mockJwtToken", response.getToken());
    assertEquals(Role.STUDENT, response.getRole());
    assertEquals(userId, response.getUserId());
  }

  @Test
  void login_teacherSuccess() {
    String email = "jane.smith@teacher.com";
    String password = "password123";
    LoginRequest request = LoginRequest.builder().email(email).password(password).build();

    JUser user =
        JUser.builder()
            .id(userId)
            .email(email)
            .password("encodedPassword")
            .role(Role.TEACHER)
            .build();

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);
    when(jwtProvider.generateToken(userId, email, Role.TEACHER)).thenReturn("mockJwtToken");

    LoginResponse response = authService.login(request);

    assertNotNull(response);
    assertEquals("mockJwtToken", response.getToken());
    assertEquals(Role.TEACHER, response.getRole());
    assertEquals(userId, response.getUserId());
  }

  @Test
  void login_adminSuccess() {
    String email = "admin@admin.com";
    String password = "password123";
    LoginRequest request = LoginRequest.builder().email(email).password(password).build();

    JUser user =
        JUser.builder()
            .id(userId)
            .email(email)
            .password("encodedPassword")
            .role(Role.ADMIN)
            .build();

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);
    when(jwtProvider.generateToken(userId, email, Role.ADMIN)).thenReturn("mockJwtToken");

    LoginResponse response = authService.login(request);

    assertNotNull(response);
    assertEquals("mockJwtToken", response.getToken());
    assertEquals(Role.ADMIN, response.getRole());
    assertEquals(userId, response.getUserId());
  }

  @Test
  void login_domainMismatch_throwsException() {
    String email = "john.doe@admin.com";
    String password = "password123";
    LoginRequest request = LoginRequest.builder().email(email).password(password).build();

    JUser user =
        JUser.builder()
            .id(userId)
            .email(email)
            .password("encodedPassword")
            .role(Role.STUDENT)
            .build();

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

    assertThrows(ResponseStatusException.class, () -> authService.login(request));
  }

  @Test
  void login_userNotFound_throwsException() {
    String email = "unknown@student.com";
    LoginRequest request = LoginRequest.builder().email(email).password("password").build();

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> authService.login(request));
  }

  @Test
  void login_invalidPassword_throwsException() {
    String email = "john.doe@student.com";
    String password = "wrongpassword";
    LoginRequest request = LoginRequest.builder().email(email).password(password).build();

    JUser user =
        JUser.builder()
            .id(userId)
            .email(email)
            .password("encodedPassword")
            .role(Role.STUDENT)
            .build();

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(false);

    assertThrows(ResponseStatusException.class, () -> authService.login(request));
  }
}
