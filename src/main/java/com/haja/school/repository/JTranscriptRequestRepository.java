package com.haja.school.repository;

import com.haja.school.repository.model.JTranscriptRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JTranscriptRequestRepository extends JpaRepository<JTranscriptRequest, UUID> {}
