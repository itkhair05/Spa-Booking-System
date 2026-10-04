package com.example.spabooking.feedback.repository;

import com.example.spabooking.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    Optional<Feedback> findByIdAndTenantId(Long id, Long tenantId);
}
