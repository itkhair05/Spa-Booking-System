package com.example.spabooking.auth.repository;

import com.example.spabooking.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"tenant", "staff"})
    Optional<User> findByUsername(String username);

    Optional<User> findByStaffId(Long staffId);

    Optional<User> findByStaffIdAndTenantId(Long staffId, Long tenantId);

    boolean existsByUsername(String username);
}
