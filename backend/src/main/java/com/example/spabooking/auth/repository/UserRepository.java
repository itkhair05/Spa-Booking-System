package com.example.spabooking.auth.repository;

import com.example.spabooking.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"tenant", "staff"})
    Optional<User> findByUsername(String username);

    Optional<User> findByStaffId(Long staffId);

    List<User> findAllByStaffIdIn(Collection<Long> staffIds);

    Optional<User> findByStaffIdAndTenantId(Long staffId, Long tenantId);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"tenant", "staff"})
    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN u.tenant t
        LEFT JOIN u.staff s
        WHERE (
            (u.role = com.example.spabooking.auth.enums.UserRole.OWNER
             AND (LOWER(t.email) = :email OR LOWER(u.username) = :email))
            OR
            (u.role = com.example.spabooking.auth.enums.UserRole.STAFF
             AND ((s.email IS NOT NULL AND LOWER(s.email) = :email) OR LOWER(u.username) = :email))
        )
    """)
    List<User> findCandidatesForPasswordReset(@Param("email") String email);
}
