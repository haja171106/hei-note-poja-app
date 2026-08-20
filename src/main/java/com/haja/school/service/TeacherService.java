package com.haja.school.service;

import com.haja.school.endpoint.rest.model.TeacherCreateRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Exam;
import com.haja.school.model.Grade;
import com.haja.school.model.Role;
import com.haja.school.model.Student;
import com.haja.school.model.Teacher;
import com.haja.school.model.TeacherCourseBoard;
import com.haja.school.model.TeacherGradeBoard;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JExamRepository;
import com.haja.school.repository.JGradeRepository;
import com.haja.school.repository.JStudentGroupHistoryRepository;
import com.haja.school.repository.JTeacherCourseAssignmentRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JExam;
import com.haja.school.repository.model.JUser;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class TeacherService {

  private final JUserRepository userRepository;
  private final JCourseRepository courseRepository;
  private final JExamRepository examRepository;
  private final JGradeRepository gradeRepository;
  private final JTeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  private final JStudentGroupHistoryRepository studentGroupHistoryRepository;
  private final CourseService courseService;
  private final GradeCalculationService gradeCalculationService;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public Teacher createTeacher(TeacherCreateRequest request) {
    String ref = generateTeacherRef();
    String email = generateInstitutionalEmail(request.getFirstname());
    String rawPassword =
        StringUtils.hasText(request.getPassword())
            ? request.getPassword()
            : UUID.randomUUID().toString().substring(0, 8);
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

  @Transactional(readOnly = true)
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

  @Transactional(readOnly = true)
  public TeacherGradeBoard getTeacherGradeBoard(UUID teacherId, String callerEmail) {
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

    List<JCourse> courses = courseRepository.findByTeacherId(teacher.getId());
    if (courses.isEmpty()) {
      return TeacherGradeBoard.builder().courses(List.of()).build();
    }

    List<JUser> allStudents = userRepository.findByRole(Role.STUDENT);
    Map<UUID, JUser> studentById =
        allStudents.stream().collect(Collectors.toMap(JUser::getId, student -> student));
    Map<UUID, UUID> groupIdByStudent = new HashMap<>();
    studentGroupHistoryRepository
        .findByStudentIdInAndEndDateIsNull(allStudents.stream().map(JUser::getId).toList())
        .forEach(
            history -> {
              if (history.getStudent() != null
                  && history.getStudent().getId() != null
                  && history.getGroup() != null) {
                groupIdByStudent.putIfAbsent(
                    history.getStudent().getId(), history.getGroup().getId());
              }
            });

    List<JCourse> allCourses = courseRepository.findAll();
    Map<UUID, List<JExam>> examsByCourse =
        examRepository.findAll().stream()
            .filter(exam -> exam.getCourse() != null && exam.getCourse().getId() != null)
            .collect(Collectors.groupingBy(exam -> exam.getCourse().getId()));
    Map<UUID, Map<UUID, Double>> gradesByStudent = new HashMap<>();
    gradeRepository
        .findByStudentIdIn(allStudents.stream().map(JUser::getId).toList())
        .forEach(
            grade -> {
              if (grade.getStudent() != null
                  && grade.getStudent().getId() != null
                  && grade.getExam() != null
                  && grade.getExam().getId() != null) {
                gradesByStudent
                    .computeIfAbsent(grade.getStudent().getId(), k -> new HashMap<>())
                    .put(grade.getExam().getId(), grade.getValue());
              }
            });
    Map<UUID, Set<Integer>> teacherAcademicYearsByCourse = new HashMap<>();
    teacherCourseAssignmentRepository
        .findByTeacherId(teacher.getId())
        .forEach(
            assignment -> {
              if (assignment.getCourse() != null
                  && assignment.getCourse().getId() != null
                  && assignment.getAcademicYear() != null) {
                teacherAcademicYearsByCourse
                    .computeIfAbsent(assignment.getCourse().getId(), k -> new HashSet<>())
                    .add(assignment.getAcademicYear());
              }
            });

    List<TeacherCourseBoard> boards =
        courses.stream()
            .map(
                course -> {
                  List<Student> students =
                      courseService.filterStudentsForCourse(course, allStudents, groupIdByStudent);
                  List<Exam> exams =
                      examsByCourse.getOrDefault(course.getId(), List.of()).stream()
                          .map(this::toExamModel)
                          .toList();
                  List<Grade> grades =
                      examsByCourse.getOrDefault(course.getId(), List.of()).stream()
                          .flatMap(exam -> examGrades(exam, gradesByStudent).stream())
                          .toList();
                  int studyYear = (course.getSemesterNumber() + 1) / 2;
                  Map<UUID, Double> studentAverages = new HashMap<>();
                  for (Student student : students) {
                    JUser studentUser = studentById.get(student.getId());
                    if (studentUser != null) {
                      var summary =
                          gradeCalculationService.computeTeacherPartialSummaryInMemory(
                              studentUser,
                              studyYear,
                              teacher,
                              allCourses,
                              examsByCourse,
                              gradesByStudent,
                              teacherAcademicYearsByCourse);
                      if (summary != null) {
                        studentAverages.put(student.getId(), summary.getOverallAverage());
                      }
                    }
                  }
                  return TeacherCourseBoard.builder()
                      .course(toCourseModel(course))
                      .students(students)
                      .exams(exams)
                      .grades(grades)
                      .studentAverages(studentAverages)
                      .build();
                })
            .toList();

    return TeacherGradeBoard.builder().courses(boards).build();
  }

  private List<Grade> examGrades(JExam exam, Map<UUID, Map<UUID, Double>> gradesByStudent) {
    return gradesByStudent.entrySet().stream()
        .filter(entry -> entry.getValue().containsKey(exam.getId()))
        .map(
            entry ->
                Grade.builder()
                    .examId(exam.getId())
                    .studentId(entry.getKey())
                    .value(entry.getValue().get(exam.getId()))
                    .build())
        .toList();
  }

  private Exam toExamModel(JExam jExam) {
    return Exam.builder()
        .id(jExam.getId())
        .courseId(jExam.getCourse().getId())
        .academicYear(jExam.getAcademicYear())
        .label(jExam.getLabel())
        .dateExam(jExam.getDateExam())
        .coefficient(jExam.getCoefficient())
        .build();
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
