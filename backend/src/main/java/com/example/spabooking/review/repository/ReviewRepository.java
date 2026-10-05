package com.example.spabooking.review.repository;

import com.example.spabooking.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByTenantId(Long tenantId);

    List<Review> findAllByTenantIdOrderByDisplayOrderAscCreatedAtDesc(Long tenantId);

    List<Review> findAllByTenantIdAndIsPublishedTrueOrderByDisplayOrderAscCreatedAtDesc(Long tenantId);

    Optional<Review> findByIdAndTenantId(Long id, Long tenantId);
}
