package com.haja.school.repository;

import com.haja.school.repository.model.JGradeHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JGradeHistoryRepository extends JpaRepository<JGradeHistory, UUID> {}
