package com.haja.school.service;

import com.haja.school.endpoint.rest.model.TeacherCreateRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Role;
import com.haja.school.model.Teacher;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JUser;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class TeacherService {

  private final JUserRepository userRepository;
  private final JCourseRepository courseRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public Teacher createTeacher(TeacherCreateRequest request) {
    String ref = generateTeacherRef();
    String email = generateInstitutionalEmail(request.getFirstname());
    String rawPassword = UUID.randomUUID().toString().substring(0, 8);
    String encodedPassword = passwordEncoder.encode(rawPassword);

    JUser jUser =
        JUser.builder()
            .ref(ref)
            .name(request.getName())
            .firstname(request.getFirstname())
            .email(email)
            .password(encodedPassword)
            .role(Role.TEACHER)
            .birthdate(request.getBirthdate())
            .address(request.getAddress())
            .build();

    JUser savedUser = userRepository.save(jUser);

    return Teacher.builder()
        .id(savedUser.getId())
        .ref(savedUser.getRef())
        .name(savedUser.getName())
        .firstname(savedUser.getFirstname())
        .email(savedUser.getEmail())
        .build();
  }

  public List<Course> getCoursesByTeacher(UUID teacherId, String callerEmail) {
    JUser caller =
        userRepository
            .findByEmail(callerEmail)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

    if (caller.getRole() == Role.TEACHER && !caller.getId().equals(teacherId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Teachers can only view their own courses");
    }

    JUser teacher =
        userRepository
            .findById(teacherId)
            .filter(u -> u.getRole() == Role.TEACHER)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

    return courseRepository.findByTeacherId(teacher.getId()).stream()
        .map(this::toCourseModel)
        .toList();
  }

  private Course toCourseModel(JCourse jCourse) {
    return Course.builder()
        .id(jCourse.getId())
        .ref(jCourse.getRef())
        .title(jCourse.getTitle())
        .credit(jCourse.getCredit())
        .semesterNumber(jCourse.getSemesterNumber())
        .track(jCourse.getTrack())
        .build();
  }

  private String generateTeacherRef() {
    List<JUser> teachers = userRepository.findByRefStartingWith("TCR");
    int maxNumber = 0;
    for (JUser user : teachers) {
      String ref = user.getRef();
      if (ref != null && ref.startsWith("TCR")) {
        try {
          int num = Integer.parseInt(ref.substring(3));
          if (num > maxNumber) {
            maxNumber = num;
          }
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return String.format("TCR%05d", maxNumber + 1);
  }

  private String generateInstitutionalEmail(String firstname) {
    String normalizedName = firstname.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    if (normalizedName.isEmpty()) {
      normalizedName = "teacher";
    }
    String baseEmail = "hei." + normalizedName + "@teacher.com";
    if (!userRepository.existsByEmail(baseEmail)) {
      return baseEmail;
    }

    int counter = 1;
    while (userRepository.existsByEmail("hei." + normalizedName + counter + "@teacher.com")) {
      counter++;
    }
    return "hei." + normalizedName + counter + "@teacher.com";
  }
}
