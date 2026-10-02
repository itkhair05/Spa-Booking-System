package com.example.spabooking.booking.service;

import com.example.spabooking.booking.dto.CreateBookingRequest;
import com.example.spabooking.booking.dto.UpdateBookingRequest;
import com.example.spabooking.booking.dto.UpdateBookingStatusRequest;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.exception.BookingConflictException;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository,
                          CustomerRepository customerRepository,
                          StaffRepository staffRepository,
                          ServiceRepository serviceRepository,
                          TenantRepository tenantRepository) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.serviceRepository = serviceRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<Booking> findAll(Long staffId, LocalDateTime startDate, LocalDateTime endDate, BookingStatus status) {
        Long tenantId = TenantContext.requireTenantId();
        
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("endDate must not be before startDate");
            }
            if (java.time.Duration.between(startDate, endDate).toDays() > 90) {
                throw new IllegalArgumentException("Date range must not exceed 90 days");
            }
        } else if (startDate != null || endDate != null) {
            throw new IllegalArgumentException("Both startDate and endDate must be provided together");
        }
        
        return bookingRepository.findAllByFilters(tenantId, staffId, startDate, endDate, status);
    }

    @Transactional(readOnly = true)
    public Optional<Booking> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return bookingRepository.findByIdAndTenantId(id, tenantId);
    }

    @Transactional
    public Booking create(CreateBookingRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        
        if (request.getEndTime().compareTo(request.getStartTime()) <= 0) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        // Lock ordering: Customer -> Staff
        Customer customer = customerRepository.findByIdAndTenantIdAndIsActiveTrueForUpdate(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
                
        Staff staff = staffRepository.findByIdAndTenantIdAndIsActiveTrueForUpdate(request.getStaffId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        com.example.spabooking.service.entity.Service service = serviceRepository.findByIdAndTenantId(request.getServiceId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!service.getIsActive()) {
            throw new IllegalArgumentException("Service must be active");
        }

        checkOverlaps(tenantId, staff.getId(), customer.getId(), request.getStartTime(), request.getEndTime(), null);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCustomer(customer);
        booking.setStaff(staff);
        booking.setService(service);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPrice(service.getPrice()); // Snapshot price

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking update(Long id, UpdateBookingRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        if (request.getEndTime().compareTo(request.getStartTime()) <= 0) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Lock ordering: Customer -> Staff
        Customer customer = customerRepository.findByIdAndTenantIdAndIsActiveTrueForUpdate(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
                
        Staff staff = staffRepository.findByIdAndTenantIdAndIsActiveTrueForUpdate(request.getStaffId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        com.example.spabooking.service.entity.Service service = serviceRepository.findByIdAndTenantId(request.getServiceId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!service.getIsActive()) {
            throw new IllegalArgumentException("Service must be active");
        }

        Booking existingBooking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Don't modify if terminal
        if (existingBooking.getStatus() == BookingStatus.CANCELLED || existingBooking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot reschedule a cancelled or completed booking");
        }

        checkOverlaps(tenantId, staff.getId(), customer.getId(), request.getStartTime(), request.getEndTime(), id);

        existingBooking.setCustomer(customer);
        existingBooking.setStaff(staff);
        existingBooking.setService(service);
        existingBooking.setStartTime(request.getStartTime());
        existingBooking.setEndTime(request.getEndTime());
        // Do NOT update price on simple reschedule, it stays historical unless explicit pricing update.

        return bookingRepository.save(existingBooking);
    }

    @Transactional
    public Booking updateStatus(Long id, UpdateBookingStatusRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Booking existingBooking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        BookingStatus current = existingBooking.getStatus();
        BookingStatus target = request.getStatus();

        if (current == BookingStatus.CANCELLED || current == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot change status of a terminal booking");
        }

        if (current == BookingStatus.PENDING) {
            if (target != BookingStatus.CONFIRMED && target != BookingStatus.CANCELLED) {
                throw new IllegalArgumentException("Invalid status transition from PENDING");
            }
        } else if (current == BookingStatus.CONFIRMED) {
            if (target != BookingStatus.COMPLETED && target != BookingStatus.CANCELLED) {
                throw new IllegalArgumentException("Invalid status transition from CONFIRMED");
            }
        }

        existingBooking.setStatus(target);
        return bookingRepository.save(existingBooking);
    }

    private void checkOverlaps(Long tenantId, Long staffId, Long customerId, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        long staffOverlaps = bookingRepository.countOverlappingStaffBookings(tenantId, staffId, start, end, excludeBookingId);
        if (staffOverlaps > 0) {
            throw new BookingConflictException("Staff member is already booked for this time slot");
        }

        long customerOverlaps = bookingRepository.countOverlappingCustomerBookings(tenantId, customerId, start, end, excludeBookingId);
        if (customerOverlaps > 0) {
            throw new BookingConflictException("Customer is already booked for this time slot");
        }
    }
}
