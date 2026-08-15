package com.haja.school.repository;

import com.haja.school.model.Track;
import com.haja.school.repository.model.JCourse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JCourseRepository extends JpaRepository<JCourse, UUID> {

  List<JCourse> findBySemesterNumber(Integer semesterNumber);

  List<JCourse> findByTrack(Track track);

  List<JCourse> findBySemesterNumberAndTrack(Integer semesterNumber, Track track);

  @Query(
      "SELECT DISTINCT tca.course FROM JTeacherCourseAssignment tca WHERE tca.teacher.id ="
          + " :teacherId")
  List<JCourse> findByTeacherId(@Param("teacherId") UUID teacherId);

  @Query(
      "SELECT DISTINCT tca.course FROM JTeacherCourseAssignment tca WHERE tca.teacher.id ="
          + " :teacherId AND tca.course.semesterNumber = :semester")
  List<JCourse> findByTeacherIdAndSemester(
      @Param("teacherId") UUID teacherId, @Param("semester") Integer semester);

  @Query(
      "SELECT DISTINCT tca.course FROM JTeacherCourseAssignment tca WHERE tca.teacher.id ="
          + " :teacherId AND tca.course.track = :track")
  List<JCourse> findByTeacherIdAndTrack(
      @Param("teacherId") UUID teacherId, @Param("track") Track track);

  @Query(
      "SELECT DISTINCT tca.course FROM JTeacherCourseAssignment tca WHERE tca.teacher.id ="
          + " :teacherId AND tca.course.semesterNumber = :semester AND tca.course.track = :track")
  List<JCourse> findByTeacherIdAndSemesterAndTrack(
      @Param("teacherId") UUID teacherId,
      @Param("semester") Integer semester,
      @Param("track") Track track);
}
