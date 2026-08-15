package com.haja.school.service;

import com.haja.school.endpoint.rest.model.CourseCreateRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Track;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JUser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class CourseService {

  private final JCourseRepository courseRepository;
  private final JUserRepository userRepository;

  public List<Course> getCourses(String callerEmail, Integer semester, Track track) {
    JUser caller =
        userRepository
            .findByEmail(callerEmail)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

    validateSemester(semester);

    List<JCourse> jCourses =
        switch (caller.getRole()) {
          case ADMIN -> getCoursesForAdmin(semester, track);
          case TEACHER -> getCoursesForTeacher(caller.getId(), semester, track);
          case STUDENT -> getCoursesForStudent(caller, semester, track);
        };

    return jCourses.stream().map(this::toModel).toList();
  }

  @Transactional
  public Course createCourse(CourseCreateRequest request) {
    validateSemester(request.getSemesterNumber());

    if (request.getCredit() == null || request.getCredit() <= 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Credit must be a positive integer");
    }

    if (courseRepository.existsByRef(request.getRef())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Course reference already exists: " + request.getRef());
    }

    validateCreditLimits(request.getSemesterNumber(), request.getTrack(), request.getCredit());

    JCourse jCourse =
        JCourse.builder()
            .ref(request.getRef())
            .title(request.getTitle())
            .credit(request.getCredit())
            .semesterNumber(request.getSemesterNumber())
            .track(request.getTrack())
            .build();

    JCourse saved = courseRepository.save(jCourse);
    return toModel(saved);
  }

  private void validateCreditLimits(int semesterNumber, Track track, int creditToAdd) {
    List<JCourse> currentSemesterCourses = courseRepository.findBySemesterNumber(semesterNumber);

    int currentCommon =
        currentSemesterCourses.stream()
            .filter(c -> c.getTrack() == null)
            .mapToInt(JCourse::getCredit)
            .sum();
    int currentEl =
        currentSemesterCourses.stream()
            .filter(c -> c.getTrack() == Track.EL)
            .mapToInt(JCourse::getCredit)
            .sum();
    int currentTn =
        currentSemesterCourses.stream()
            .filter(c -> c.getTrack() == Track.TN)
            .mapToInt(JCourse::getCredit)
            .sum();

    int newSemesterElCredits;
    int newSemesterTnCredits;
    int newSemesterCommonCredits;

    if (track == null) {
      newSemesterCommonCredits = currentCommon + creditToAdd;
      newSemesterElCredits = currentCommon + creditToAdd + currentEl;
      newSemesterTnCredits = currentCommon + creditToAdd + currentTn;
    } else if (track == Track.EL) {
      newSemesterCommonCredits = currentCommon;
      newSemesterElCredits = currentCommon + currentEl + creditToAdd;
      newSemesterTnCredits = currentCommon + currentTn;
    } else {
      newSemesterCommonCredits = currentCommon;
      newSemesterElCredits = currentCommon + currentEl;
      newSemesterTnCredits = currentCommon + currentTn + creditToAdd;
    }

    if (newSemesterCommonCredits > 30 || newSemesterElCredits > 30 || newSemesterTnCredits > 30) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Total credits for semester " + semesterNumber + " cannot exceed 30");
    }

    int year = (semesterNumber + 1) / 2;
    int otherSemester = (semesterNumber % 2 == 1) ? semesterNumber + 1 : semesterNumber - 1;
    List<JCourse> otherSemesterCourses = courseRepository.findBySemesterNumber(otherSemester);

    int otherCommon =
        otherSemesterCourses.stream()
            .filter(c -> c.getTrack() == null)
            .mapToInt(JCourse::getCredit)
            .sum();
    int otherEl =
        otherSemesterCourses.stream()
            .filter(c -> c.getTrack() == Track.EL)
            .mapToInt(JCourse::getCredit)
            .sum();
    int otherTn =
        otherSemesterCourses.stream()
            .filter(c -> c.getTrack() == Track.TN)
            .mapToInt(JCourse::getCredit)
            .sum();

    int totalYearCommon = newSemesterCommonCredits + otherCommon;
    int totalYearEl = newSemesterElCredits + otherCommon + otherEl;
    int totalYearTn = newSemesterTnCredits + otherCommon + otherTn;

    if (totalYearCommon > 60 || totalYearEl > 60 || totalYearTn > 60) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Total credits for academic year " + year + " cannot exceed 60");
    }
  }

  private List<JCourse> getCoursesForAdmin(Integer semester, Track track) {
    if (semester != null && track != null) {
      return courseRepository.findBySemesterNumberAndTrack(semester, track);
    } else if (semester != null) {
      return courseRepository.findBySemesterNumber(semester);
    } else if (track != null) {
      return courseRepository.findByTrack(track);
    }
    return courseRepository.findAll();
  }

  private List<JCourse> getCoursesForTeacher(UUID teacherId, Integer semester, Track track) {
    if (semester != null && track != null) {
      return courseRepository.findByTeacherIdAndSemesterAndTrack(teacherId, semester, track);
    } else if (semester != null) {
      return courseRepository.findByTeacherIdAndSemester(teacherId, semester);
    } else if (track != null) {
      return courseRepository.findByTeacherIdAndTrack(teacherId, track);
    }
    return courseRepository.findByTeacherId(teacherId);
  }

  private List<JCourse> getCoursesForStudent(JUser student, Integer semester, Track track) {
    Integer studentSemester = resolveStudentSemester(student);
    Track studentTrack = student.getTrack();

    Integer effectiveSemester = semester != null ? semester : studentSemester;
    Track effectiveTrack = track != null ? track : studentTrack;

    if (effectiveSemester != null && effectiveTrack != null) {
      return courseRepository.findBySemesterNumberAndTrack(effectiveSemester, effectiveTrack);
    } else if (effectiveSemester != null) {
      return courseRepository.findBySemesterNumber(effectiveSemester);
    } else if (effectiveTrack != null) {
      return courseRepository.findByTrack(effectiveTrack);
    }
    return courseRepository.findAll();
  }

  private Integer resolveStudentSemester(JUser student) {
    if (student.getCohort() == null || student.getCohort().getEntryYear() == null) {
      return null;
    }
    int entryYear = student.getCohort().getEntryYear();
    int currentYear = LocalDate.now().getYear();
    int currentMonth = LocalDate.now().getMonthValue();
    int yearsPassed = currentYear - entryYear;
    int computed = yearsPassed * 2 + (currentMonth >= 9 ? 1 : 0);
    return Math.max(1, Math.min(6, computed));
  }

  private void validateSemester(Integer semester) {
    if (semester != null && (semester < 1 || semester > 6)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "The 'semester' parameter must be between 1 and 6");
    }
  }

  private Course toModel(JCourse jCourse) {
    return Course.builder()
        .id(jCourse.getId())
        .ref(jCourse.getRef())
        .title(jCourse.getTitle())
        .credit(jCourse.getCredit())
        .semesterNumber(jCourse.getSemesterNumber())
        .track(jCourse.getTrack())
        .build();
  }
}
