package com.haja.school.security;

import static org.junit.jupiter.api.Assertions.*;

import com.haja.school.model.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

  private JwtProvider jwtProvider;
  private final String secret = "defaultSecretKeyWithAtLeast32BytesForHmacSha256Security123456";
  private final long expirationMs = 3600000;

  @BeforeEach
  void setUp() {
    jwtProvider = new JwtProvider(secret, expirationMs);
  }

  @Test
  void generateToken_andExtractClaims_forStudent() {
    UUID userId = UUID.randomUUID();
    String email = "john.doe@student.com";
    Role role = Role.STUDENT;

    String token = jwtProvider.generateToken(userId, email, role);

    assertNotNull(token);
    assertTrue(jwtProvider.validateToken(token));
    assertEquals(email, jwtProvider.getEmailFromToken(token));
    assertEquals("STUDENT", jwtProvider.getRoleFromToken(token));
    assertEquals(userId.toString(), jwtProvider.getUserIdFromToken(token));
  }

  @Test
  void generateToken_andExtractClaims_forTeacher() {
    UUID userId = UUID.randomUUID();
    String email = "jane.smith@teacher.com";
    Role role = Role.TEACHER;

    String token = jwtProvider.generateToken(userId, email, role);

    assertNotNull(token);
    assertTrue(jwtProvider.validateToken(token));
    assertEquals(email, jwtProvider.getEmailFromToken(token));
    assertEquals("TEACHER", jwtProvider.getRoleFromToken(token));
    assertEquals(userId.toString(), jwtProvider.getUserIdFromToken(token));
  }

  @Test
  void generateToken_andExtractClaims_forAdmin() {
    UUID userId = UUID.randomUUID();
    String email = "admin@admin.com";
    Role role = Role.ADMIN;

    String token = jwtProvider.generateToken(userId, email, role);

    assertNotNull(token);
    assertTrue(jwtProvider.validateToken(token));
    assertEquals(email, jwtProvider.getEmailFromToken(token));
    assertEquals("ADMIN", jwtProvider.getRoleFromToken(token));
    assertEquals(userId.toString(), jwtProvider.getUserIdFromToken(token));
  }

  @Test
  void validateToken_invalidToken_returnsFalse() {
    assertFalse(jwtProvider.validateToken("invalid.token.str"));
  }
}
