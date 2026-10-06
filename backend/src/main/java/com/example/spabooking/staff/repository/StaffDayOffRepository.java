package com.example.spabooking.staff.repository;

import com.example.spabooking.staff.entity.StaffDayOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffDayOffRepository extends JpaRepository<StaffDayOff, Long> {

    List<StaffDayOff> findByStaffIdAndTenantIdOrderByDateAsc(Long staffId, Long tenantId);

    boolean existsByStaffIdAndTenantIdAndDate(Long staffId, Long tenantId, LocalDate date);

    Optional<StaffDayOff> findByIdAndStaffIdAndTenantId(Long id, Long staffId, Long tenantId);

    void deleteByIdAndStaffIdAndTenantId(Long id, Long staffId, Long tenantId);
}
