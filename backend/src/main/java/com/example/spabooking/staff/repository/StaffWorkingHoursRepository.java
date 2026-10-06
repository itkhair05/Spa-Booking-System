package com.example.spabooking.staff.repository;

import com.example.spabooking.staff.entity.StaffWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffWorkingHoursRepository extends JpaRepository<StaffWorkingHours, Long> {

    List<StaffWorkingHours> findByStaffIdAndTenantId(Long staffId, Long tenantId);

    Optional<StaffWorkingHours> findByStaffIdAndTenantIdAndDayOfWeek(Long staffId, Long tenantId, DayOfWeek dayOfWeek);

    boolean existsByStaffIdAndTenantIdAndDayOfWeek(Long staffId, Long tenantId, DayOfWeek dayOfWeek);

    void deleteByStaffIdAndTenantId(Long staffId, Long tenantId);
}
