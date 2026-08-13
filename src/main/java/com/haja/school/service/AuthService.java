package com.haja.school.service;

import com.haja.school.endpoint.rest.model.LoginRequest;
import com.haja.school.endpoint.rest.model.LoginResponse;
import com.haja.school.model.Role;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JUser;
import com.haja.school.security.JwtProvider;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class AuthService {

  private final JUserRepository userRepository;
  private final JwtProvider jwtProvider;
  private final PasswordEncoder passwordEncoder;

  public LoginResponse login(LoginRequest request) {
    JUser user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

    // Business rule: Verify email domain matches actual role in DB
    verifyEmailDomainMatchesRole(request.getEmail(), user.getRole());

    // Password verification
    if (user.getPassword() != null
        && !passwordEncoder.matches(request.getPassword(), user.getPassword())
        && !request.getPassword().equals(user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    String token = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole());

    return LoginResponse.builder().token(token).role(user.getRole()).userId(user.getId()).build();
  }

  private void verifyEmailDomainMatchesRole(String email, Role role) {
    int atIndex = email.indexOf('@');
    if (atIndex == -1) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email format");
    }
    String domain = email.substring(atIndex + 1).toLowerCase();

    boolean matches =
        switch (role) {
          case STUDENT -> domain.equals("student.com");
          case TEACHER -> domain.equals("teacher.com");
          case ADMIN -> domain.equals("admin.com");
        };

    if (!matches) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Email domain does not match user's actual role in the system");
    }
  }
}
